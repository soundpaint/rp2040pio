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
    "Otherwise, for all specified modification options, the corresponding%n" +
    "these modifications will be performed for the selected state machine.";

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
                                   "value to write into FIFO memory");
  private static final CmdOptions.BooleanOptionDeclaration optJoinRx =
    CmdOptions.createBooleanOption(false, 'r', "joinrx", null,
                                   "let RX FIFO steal TX FIFO's storage");
  private static final CmdOptions.BooleanOptionDeclaration optJoinTx =
    CmdOptions.createBooleanOption(false, 't', "jointx", null,
                                   "let TX FIFO steal RX FIFO's storage");

  private enum Type
  {
    RX, TX;
  };

  private final SDK sdk;

  public Fifo(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optAddress, optValue, optJoinRx, optJoinTx });
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
      if (optValueValue != null) {
        final int address = optAddressValue;
        if ((address < 0) || (address > (Constants.FIFO_DEPTH << 1) - 1)) {
          final String message =
            String.format("expected address value in the range 0...%d, " +
                          "but got: %d",
                          (Constants.FIFO_DEPTH << 1) - 1, address);
          throw new CmdOptions.ParseException(message);
        }
      }
      if (((optAddressValue != null) && (optValueValue == null)) ||
          ((optAddressValue == null) && (optValueValue != null))) {
        final String message =
          "either none of options -a and -v, or both of them must be specified";
        throw new CmdOptions.ParseException(message);
      }
    }
  }

  private void displayFifo(final int pioNum, final int smNum)
    throws IOException
  {
    final int addressFLevel =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.FLEVEL);
    final int fLevelValue = sdk.readAddress(addressFLevel);
    final int rxLevel = (fLevelValue >>> (0x4 + 0x4 * smNum)) & 0x3;
    final int txLevel = (fLevelValue >>> (0x4 * smNum)) & 0x3;
    final int addressShiftCtrl =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_SHIFTCTRL);
    final int shiftCtrlValue = sdk.readAddress(addressShiftCtrl);
    final boolean fJoinRxValue =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_FJOIN_RX_BITS) != 0x0;
    final boolean fJoinTxValue =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_FJOIN_TX_BITS) != 0x0;
    final StringBuffer fifoHeader = new StringBuffer();
    Type type = fJoinTxValue ? (fJoinRxValue ? null : Type.TX) : Type.RX;
    int regCount = 0;
    for (int index = 0; index < (Constants.FIFO_DEPTH << 1); index++) {
      if (!fJoinRxValue && !fJoinTxValue && (index == Constants.FIFO_DEPTH)) {
        type = Type.TX;
        regCount = 0;
      }
      fifoHeader.append(type != null ? type : "__");
      fifoHeader.append(String.format("%01x      ", regCount++));
    }
    final StringBuffer fifoContents = new StringBuffer();
    for (int fifoMemAddress = 0; fifoMemAddress < (Constants.FIFO_DEPTH << 1);
         fifoMemAddress++) {
      final int addressFifoMem =
        PIOEmuRegisters.getFIFOMemAddress(pioNum, smNum, fifoMemAddress);
      final int fifoMemValue = sdk.readAddress(addressFifoMem);
      fifoContents.append(String.format("%08x ", fifoMemValue));
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
      console.println(String.format("set join %s ", type));
    } else {
      sdk.hwClearBits(addressShiftCtrl, mask);
      console.println(String.format("unset join %s ", type));
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
    final Boolean optJoinRxValue = options.getValue(optJoinRx);
    final Boolean optJoinTxValue = options.getValue(optJoinTx);
    if ((optAddressValue == null) && (optValueValue == null) &&
        (optJoinRxValue == null) && (optJoinTxValue == null)) {
      displayFifo(pioNum, smNum);
    }
    if (optJoinRxValue != null) {
      setFJoin(pioNum, smNum, Type.RX, optJoinRxValue);
    }
    if (optJoinTxValue != null) {
      setFJoin(pioNum, smNum, Type.TX, optJoinTxValue);
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
