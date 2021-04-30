/*
 * @(#)Fifo.java 1.00 21/04/05
 *
 * Copyright (C) 2021 Jürgen Reuter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For updates and more info or contacting the author, visit:
 * <https://github.com/soundpaint/rp2040pio>
 *
 * Author's web site: www.juergen-reuter.de
 */
package org.soundpaint.rp2040pio.monitor.commands;

import java.io.IOException;
import java.io.PrintStream;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "fifo" displays or modifies a state machine's
 * FIFO status.
 *
 * TODO: It is unclear whether the FIFO is internally organized as a
 * cyclic buffer, or a shift register.  Therefore, the numbering of
 * the FIFO memory registers may appear wrong, if the emulator's
 * behavior is compared directly with a real RP2040's behavior.
 */
public class Fifo extends Command
{
  private static final String fullName = "fifo";
  private static final String singleLineDescription =
    "display or change internal state machine's FIFO status";
  private static final String notes =
    "If none of the FIFO modification options is specified, the status%n"+
    "of the FIFO of the selected is displayed.%n" +
    "Option '-a' together with option '-v' can be used for directly%n" +
    "low-level write a value into one of the 8 FIFO's data registers.%n" +
    "Otherwise, for all specified modification options, the corresponding%n" +
    "modifications will be performed for the selected state machine and%n" +
    "the selected FIFO (either RX or TX).";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", null,
                                   "FIFO memory address (0x0…0x7) to write " +
                                   "value into");
  private static final CmdOptions.IntegerOptionDeclaration optValue =
    CmdOptions.createIntegerOption("VALUE", false, 'v', "value", null,
                                   "value to enqueue or directly write " +
                                   "into FIFO memory");
  private static final CmdOptions.FlagOptionDeclaration optDequeue =
    CmdOptions.createFlagOption(false, 'd', "dequeue", CmdOptions.Flag.OFF,
                                "dequeue value from either RX or TX FIFO");
  private static final CmdOptions.FlagOptionDeclaration optEnqueue =
    CmdOptions.createFlagOption(false, 'e', "enqueue", CmdOptions.Flag.OFF,
                                "enqueue value provided with option -v " +
                                "into either RX or TX FIFO");
  private static final CmdOptions.FlagOptionDeclaration optJoin =
    CmdOptions.createFlagOption(false, 'j', "join", CmdOptions.Flag.OFF,
                                "let either RX or TX FIFO steal the other " +
                                "FIFO's storage");
  private static final CmdOptions.FlagOptionDeclaration optUnjoin =
    CmdOptions.createFlagOption(false, 'u', "unjoin", CmdOptions.Flag.OFF,
                                "revoke join operation of either RX or TX FIFO");
  private static final CmdOptions.FlagOptionDeclaration optTX =
    CmdOptions.createFlagOption(false, 't', "tx", CmdOptions.Flag.OFF,
                                "apply modification on TX FIFO");
  private static final CmdOptions.FlagOptionDeclaration optRX =
    CmdOptions.createFlagOption(false, 'r', "rx", CmdOptions.Flag.OFF,
                                "apply modification on RX FIFO");

  private enum Type
  {
    RX, TX;
  };

  private final SDK sdk;

  public Fifo(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optAddress, optValue,
              optDequeue, optEnqueue, optJoin, optUnjoin, optTX, optRX });
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    this.sdk = sdk;
  }

  @Override
  protected void checkValidity(final CmdOptions options)
    throws CmdOptions.ParseException
  {
    if (options.getValue(optHelp) != CmdOptions.Flag.ON) {
      final int pioNum = options.getValue(optPio);
      if ((pioNum < 0) || (pioNum > Constants.PIO_NUM - 1)) {
        throw new CmdOptions.
          ParseException("PIO number must be either 0 or 1");
      }
      final int smNum = options.getValue(optSm);
      if ((smNum < 0) || (smNum > Constants.SM_COUNT - 1)) {
        throw new CmdOptions.
          ParseException("SM number must be one of 0, 1, 2 or 3");
      }
      final Integer optAddressValue = options.getValue(optAddress);
      final Integer optValueValue = options.getValue(optValue);
      if (optAddressValue != null) {
        final int address = optAddressValue;
        if ((address < 0) || (address > (Constants.FIFO_DEPTH << 1) - 1)) {
          final String message =
            String.format("expected address value in the range 0…%d, " +
                          "but got: %d",
                          (Constants.FIFO_DEPTH << 1) - 1, address);
          throw new CmdOptions.ParseException(message);
        }
      }
      if (options.isDefined(optAddress) || options.getValue(optEnqueue).isOn()) {
        if (optValueValue == null) {
          throw new CmdOptions.ParseException("missing option: -v");
        }
      }
      if (((optAddressValue != null) && (optValueValue == null)) ||
          ((optAddressValue == null) && (optValueValue != null))) {
        final String message =
          "either none of options -a and -v, or both of them must be specified";
        throw new CmdOptions.ParseException(message);
      }
      if (options.getValue(optRX).isOn() && options.getValue(optTX).isOn()) {
        final String message =
          "either option -r and -t can be specified, but not both";
        throw new CmdOptions.ParseException(message);
      }
      int opCount = 0;
      if (options.getValue(optDequeue).isOn()) opCount++;
      if (options.getValue(optEnqueue).isOn()) opCount++;
      if (options.getValue(optJoin).isOn()) opCount++;
      if (options.getValue(optUnjoin).isOn()) opCount++;
      if (opCount > 1) {
        final String message =
          "only one of options -d, -q, -j and -u may be specified";
        throw new CmdOptions.ParseException(message);
      }
      if (opCount > 0) {
        if (!options.getValue(optRX).isOn() && !options.getValue(optTX).isOn()) {
          final String message =
            "if one of options -d, -q, -j and -u is specified, either " +
            "option -r or -t must be specified to select a FIFO";
          throw new CmdOptions.ParseException(message);
        }
      } else {
        if (options.getValue(optRX).isOn() && options.getValue(optTX).isOn()) {
          final String message =
            "options -r or -t may be specified only if one of " +
            "options -d, -q, -j and -u is specified";
          throw new CmdOptions.ParseException(message);
        }
      }
    }
  }

  private int getSMFReadPtr(final int pioNum, final int smNum)
    throws IOException
  {
    final int addressFReadPtr =
      PIOEmuRegisters.getAddress(pioNum, PIOEmuRegisters.Regs.FREAD_PTR);
    final int fReadPtr = sdk.readAddress(addressFReadPtr);
    return (fReadPtr >>> (smNum << 3)) & 0xff;
  }

  private int getSMFLevel(final int pioNum, final int smNum)
    throws IOException
  {
    final int addressFLevel =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.FLEVEL);
    final int fLevel = sdk.readAddress(addressFLevel);
    return (fLevel >>> (smNum << 3)) & 0xff;
  }

  private void displayFifo(final int pioNum, final int smNum)
    throws IOException
  {
    final int smfReadPtr = getSMFReadPtr(pioNum, smNum);
    final int txReadPtr = smfReadPtr & 0xf;
    final int rxReadPtr = (smfReadPtr & 0xf0) >> 4;
    final int addressFLevel =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.FLEVEL);
    final int smfLevel = getSMFLevel(pioNum, smNum);
    final int txLevel = smfLevel & 0xf;
    final int rxLevel = (smfLevel >>> 4) & 0xf;
    final int addressShiftCtrl =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_SHIFTCTRL);
    final int shiftCtrlValue = sdk.readAddress(addressShiftCtrl);
    final boolean fJoinRxValue =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_FJOIN_RX_BITS) != 0x0;
    final boolean fJoinTxValue =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_FJOIN_TX_BITS) != 0x0;
    final StringBuffer fifoHeader = new StringBuffer();
    Type type = fJoinRxValue ? (fJoinTxValue ? null : Type.RX) : Type.TX;
    int regCount = 0;
    for (int index = 0; index < (Constants.FIFO_DEPTH << 1); index++) {
      if (!fJoinRxValue && !fJoinTxValue && (index == Constants.FIFO_DEPTH)) {
        type = Type.RX;
        regCount = 0;
      }
      fifoHeader.append(type != null ? type : "__");
      fifoHeader.append(String.format("%01x       ", regCount++));
    }
    final StringBuffer fifoContents = new StringBuffer();
    for (int fifoMemAddress = 0; fifoMemAddress < (Constants.FIFO_DEPTH << 1);
         fifoMemAddress++) {
      final int addressFifoMem =
        PIOEmuRegisters.getFIFOMemAddress(pioNum, smNum, fifoMemAddress);
      final int fifoMemValue = sdk.readAddress(addressFifoMem);
      final String readPtr =
        (!fJoinRxValue && (fifoMemAddress == txReadPtr)) ||
        (!fJoinTxValue && (fifoMemAddress == rxReadPtr)) ?
        "→" : " ";
      fifoContents.append(String.format("%08x%s ", fifoMemValue, readPtr));
    }
    final StringBuffer fifoLevels = new StringBuffer();
    if (!fJoinTxValue) {
      fifoLevels.append(String.format("RX_LEVEL=%01x", rxLevel));
    }
    if (!fJoinRxValue) {
      if (fifoLevels.length() > 0) fifoLevels.append(", ");
      fifoLevels.append(String.format("TX_LEVEL=%01x", txLevel));
    }
    console.printf("(pio%d:sm%d) %s%n", pioNum, smNum, fifoHeader);
    console.printf("           %s%n", fifoContents);
    if (fifoLevels.length() > 0) {
      console.printf("           (%s)%n", fifoLevels);
    }
  }

  private void writeFifoAddress(final int pioNum, final int smNum,
                                final int fifoMemAddress, final int value)
    throws IOException
  {
    final int address =
      PIOEmuRegisters.getFIFOMemAddress(pioNum, smNum, fifoMemAddress);
    sdk.writeAddress(address, value);
    console.printf("(pio%d:sm%d) wrote value %08x into FIFO register %1x%n",
                   pioNum, smNum, value, fifoMemAddress);
  }

  private void dequeue(final int pioNum, final int smNum, final Type type)
    throws IOException
  {
    final int address =
      PIOEmuRegisters.getAddress(pioNum, PIOEmuRegisters.Regs.TXF0) + smNum << 2;
    final int value = sdk.readAddress(address);
    console.printf("(pio%d:sm%d) dequeued 0x%08x%n", pioNum, smNum, value);
  }

  private void enqueue(final int pioNum, final int smNum, final Type type,
                       final int value)
    throws IOException
  {
    final int address =
      PIOEmuRegisters.getAddress(pioNum, PIOEmuRegisters.Regs.RXF0) + smNum << 2;
    sdk.writeAddress(address, value);
    console.printf("(pio%d:sm%d) enqueued 0x%08x%n", pioNum, smNum, value);
  }

  private void setFJoin(final int pioNum, final int smNum,
                        final Type type, final boolean join)
    throws IOException
  {
    final int addressShiftCtrl =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_SHIFTCTRL);
    final int mask =
      type == Type.RX ?
      Constants.SM0_SHIFTCTRL_FJOIN_RX_BITS :
      Constants.SM0_SHIFTCTRL_FJOIN_TX_BITS;
    if (join) {
      sdk.hwSetBits(addressShiftCtrl, mask);
      console.printf("(pio%d:sm%d) set join %s ", pioNum, smNum, type);
    } else {
      sdk.hwClearBits(addressShiftCtrl, mask);
      console.printf("(pio%d:sm%d) unset join %s ", pioNum, smNum, type);
    }
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final int pioNum = options.getValue(optPio);
    final int smNum = options.getValue(optSm);
    final Integer optAddressValue = options.getValue(optAddress);
    final Integer optValueValue = options.getValue(optValue);
    final boolean optDequeueValue = options.getValue(optDequeue).isOn();
    final boolean optEnqueueValue = options.getValue(optEnqueue).isOn();
    final boolean optJoinValue = options.getValue(optJoin).isOn();
    final boolean optUnjoinValue = options.getValue(optUnjoin).isOn();
    final boolean haveModOp =
      optDequeueValue || optEnqueueValue || optJoinValue || optUnjoinValue;
    if ((optAddressValue == null) && (optValueValue == null) && !haveModOp) {
      displayFifo(pioNum, smNum);
    }
    final Type type = options.getValue(optRX).isOn() ? Type.RX : Type.TX;
    if (optDequeueValue) {
      dequeue(pioNum, smNum, type);
    }
    if (optEnqueueValue) {
      enqueue(pioNum, smNum, type, optValueValue);
    }
    if (optJoinValue) {
      setFJoin(pioNum, smNum, type, true);
    }
    if (optUnjoinValue) {
      setFJoin(pioNum, smNum, type, false);
    }
    if ((optAddressValue != null) && (optValueValue != null)) {
      writeFifoAddress(pioNum, smNum, optAddressValue, optValueValue);
    }
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
