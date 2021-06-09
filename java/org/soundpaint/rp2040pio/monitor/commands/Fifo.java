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
import org.soundpaint.rp2040pio.sdk.PIOSDK;
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
    "Use options \"-p\" and \"-s\" to select a state machine.%n" +
    "If none of the FIFO modification options is specified, the status%n"+
    "of the FIFO of the selected state machine is displayed.%n" +
    "Option '-a' together with option '-v' can be used for directly%n" +
    "low-level write a value into one of the 8 FIFO's data registers.%n" +
    "Otherwise, for all specified modification options \"-d\", \"-e\",%n" +
    "\"-j\", \"-u\", \"--threshold\", \"--shift-left\", \"--shift-right\"%n" +
    "and \"--auto\", the corresponding modification will be performed for%n" +
    "the selected state machine and the selected FIFO (either RX or TX).%n" +
    "Modification option \"-c\" will clear both FIFOs and, if specified%n" +
    "together with one of the other modification options, will be%n" +
    "executed first.  Similarly, options \"--clear-tx-stall\",%n" +
    "\"--clear-tx-over\", \"clear-rx-under\" and \"clear-rx-stall\"%n" +
    "will clear the corresponding FDEBUG flag of the specified%n" +
    "state machine.";

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
  private static final CmdOptions.FlagOptionDeclaration optClear =
    CmdOptions.createFlagOption(false, 'c', "clear", CmdOptions.Flag.OFF,
                                "clear both FIFOs, RX and TX");
  private static final CmdOptions.FlagOptionDeclaration optClearTxStall =
    CmdOptions.createFlagOption(false, null, "clear-tx-stall",
                                CmdOptions.Flag.OFF,
                                "clear FDEBUG flag 'TX Stall'");
  private static final CmdOptions.FlagOptionDeclaration optClearTxOver =
    CmdOptions.createFlagOption(false, null, "clear-tx-over",
                                CmdOptions.Flag.OFF,
                                "clear FDEBUG flag 'TX Over'");
  private static final CmdOptions.FlagOptionDeclaration optClearRxUnder =
    CmdOptions.createFlagOption(false, null, "clear-rx-under",
                                CmdOptions.Flag.OFF,
                                "clear FDEBUG flag 'RX Under'");
  private static final CmdOptions.FlagOptionDeclaration optClearRxStall =
    CmdOptions.createFlagOption(false, null, "clear-rx-stall",
                                CmdOptions.Flag.OFF,
                                "clear FDEBUG flag 'RX Stall'");
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
  private static final CmdOptions.IntegerOptionDeclaration optThreshold =
    CmdOptions.createIntegerOption("NUMBER", false, null, "threshold", null,
                                   "set pull threshold (when TX selected) " +
                                   "or push threshold (when RX selected)");
  private static final CmdOptions.FlagOptionDeclaration optShiftLeft =
    CmdOptions.createFlagOption(false, null, "shift-left", CmdOptions.Flag.OFF,
                                "set shift direction left for OSR (when TX " +
                                "selected or for ISR (when RX selected)");
  private static final CmdOptions.FlagOptionDeclaration optShiftRight =
    CmdOptions.createFlagOption(false, null, "shift-right", CmdOptions.Flag.OFF,
                                "set shift direction left for OSR (when TX " +
                                "selected or for ISR (when RX selected)");
  private static final CmdOptions.BooleanOptionDeclaration optAuto =
    CmdOptions.createBooleanOption(false, null, "auto", null,
                                   "turn on or off auto-pull (when TX " +
                                   "selected) or auto-push (when RX selected)");
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
          { optPio, optSm, optAddress, optValue, optClear,
              optClearTxStall, optClearTxOver, optClearRxUnder, optClearRxStall,
              optDequeue, optEnqueue, optJoin, optUnjoin,
              optThreshold, optShiftLeft, optShiftRight, optAuto,
              optTX, optRX });
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
      if (optValueValue != null) {
        if (!options.isDefined(optAddress) &&
            !options.getValue(optEnqueue).isOn()) {
          throw new CmdOptions.ParseException("missing option: -a or -e");
        }
      }
      final Integer optThresholdValue = options.getValue(optThreshold);
      if (optThresholdValue != null) {
        final int threshold = optThresholdValue;
        if ((threshold < 0) || (threshold > 32 /* assume (32 == 0) */)) {
          final String message =
            String.format("expected threshold value in the range 0…%d, " +
                          "but got: %d", 32, threshold);
          throw new CmdOptions.ParseException(message);
        }
      }
      final Boolean optAutoValue = options.getValue(optAuto);
      if (options.getValue(optRX).isOn() && options.getValue(optTX).isOn()) {
        final String message =
          "either option -r and -t can be specified, but not both";
        throw new CmdOptions.ParseException(message);
      }
      int opCount = 0;
      if (optAddressValue != null) opCount++;
      if (options.getValue(optDequeue).isOn()) opCount++;
      if (options.getValue(optEnqueue).isOn()) opCount++;
      if (options.getValue(optJoin).isOn()) opCount++;
      if (options.getValue(optUnjoin).isOn()) opCount++;
      if (optThresholdValue != null) opCount++;
      if (options.getValue(optShiftLeft).isOn()) opCount++;
      if (options.getValue(optShiftRight).isOn()) opCount++;
      if (optAutoValue != null) opCount++;
      if (opCount > 1) {
        final String message =
          "only one of options -a, -d, -q, -j, -u, --threshold, " +
          "--shift-left, --shift-right and --auto may be specified";
        throw new CmdOptions.ParseException(message);
      }
      if (opCount > 0) {
        if (!options.getValue(optRX).isOn() && !options.getValue(optTX).isOn()) {
          final String message =
            "if one of options -d, -q, -j, -u, --threshold, " +
            "--shift-left, --shift-right or --auto is specified, either " +
            "option -r or -t must be specified to select a FIFO";
          throw new CmdOptions.ParseException(message);
        }
      } else {
        if (options.getValue(optRX).isOn() && options.getValue(optTX).isOn()) {
          final String message =
            "options -r or -t may be specified only if one of " +
            "options -d, -q, -j, -u, --threshold, " +
            "--shift-left, --shift-right and --auto is specified";
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

  private static String shiftDirectionAsString(final boolean isRight)
  {
    return isRight ? "right" : "left";
  }

  private boolean getFDebug(final int smNum, final int fDebugValue,
                            final int lsb, final int bits)
  {
    return (((fDebugValue & bits) >>> (lsb + smNum)) & 0x01) != 0x0;
  }

  private boolean getFTxStall(final int smNum, final int fDebugValue)
  {
    return
      getFDebug(smNum, fDebugValue,
                Constants.FDEBUG_TXSTALL_LSB, Constants.FDEBUG_TXSTALL_BITS);
  }

  private boolean getFTxOver(final int smNum, final int fDebugValue)
  {
    return
      getFDebug(smNum, fDebugValue,
                Constants.FDEBUG_TXOVER_LSB, Constants.FDEBUG_TXOVER_BITS);
  }

  private boolean getFRxUnder(final int smNum, final int fDebugValue)
  {
    return
      getFDebug(smNum, fDebugValue,
                Constants.FDEBUG_RXUNDER_LSB, Constants.FDEBUG_RXUNDER_BITS);
  }

  private boolean getFRxStall(final int smNum, final int fDebugValue)
  {
    return
      getFDebug(smNum, fDebugValue,
                Constants.FDEBUG_RXSTALL_LSB, Constants.FDEBUG_RXSTALL_BITS);
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
    final int addressFDebug =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.FDEBUG);
    final int fDebugValue = sdk.readAddress(addressFDebug);
    final boolean fTxStall = getFTxStall(smNum, fDebugValue);
    final boolean fTxOver = getFTxOver(smNum, fDebugValue);
    final boolean fRxUnder = getFRxUnder(smNum, fDebugValue);
    final boolean fRxStall = getFRxStall(smNum, fDebugValue);
    final int addressShiftCtrl =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_SHIFTCTRL);
    final int shiftCtrlValue = sdk.readAddress(addressShiftCtrl);
    final boolean fJoinRxValue =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_FJOIN_RX_BITS) != 0x0;
    final boolean fJoinTxValue =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_FJOIN_TX_BITS) != 0x0;
    final boolean autoPullValue =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_AUTOPULL_BITS) != 0x0;
    final boolean autoPushValue =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_AUTOPUSH_BITS) != 0x0;
    final boolean outShiftRight =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_OUT_SHIFTDIR_BITS) != 0x0;
    final boolean inShiftRight =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_IN_SHIFTDIR_BITS) != 0x0;
    final int pullThresholdBits =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_PULL_THRESH_BITS) >>>
      Constants.SM0_SHIFTCTRL_PULL_THRESH_LSB;
    final int pushThresholdBits =
      (shiftCtrlValue & Constants.SM0_SHIFTCTRL_PUSH_THRESH_BITS) >>>
      Constants.SM0_SHIFTCTRL_PUSH_THRESH_LSB;
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
    if (!fJoinRxValue) {
      fifoLevels.append(String.format("TX_LEVEL=%01x", txLevel));
    }
    if (!fJoinTxValue) {
      if (fifoLevels.length() > 0) fifoLevels.append(", ");
      fifoLevels.append(String.format("RX_LEVEL=%01x", rxLevel));
    }
    final int pullThreshold =
      Constants.checkBitCount(pullThresholdBits, "TX: OSR threshold");
    final int pushThreshold =
      Constants.checkBitCount(pushThresholdBits, "RX: ISR threshold");
    console.printf("(pio%d:sm%d) %s%n", pioNum, smNum, fifoHeader);
    console.printf("           %s%n", fifoContents);
    if (fifoLevels.length() > 0) {
      console.printf("           (%s)%n", fifoLevels);
    }
    console.printf("(pio%d:sm%d) TX: threshold=%d, shift direction=%s, " +
                   "auto-pull=%s%n", pioNum, smNum, pullThreshold,
                   shiftDirectionAsString(outShiftRight), autoPullValue);
    console.printf("(pio%d:sm%d) RX: threshold=%d, shift direction=%s, " +
                   "auto-push=%s%n", pioNum, smNum, pushThreshold,
                   shiftDirectionAsString(inShiftRight), autoPushValue);
    console.printf("(pio%d:sm%d) FDEBUG: TX Stall: %s, TX Over: %s, " +
                   "RX Under: %s, RX Stall: %s%n",
                   pioNum, smNum, fTxStall, fTxOver, fRxUnder, fRxStall);
  }

  private void setThreshold(final int pioNum, final int smNum, final Type type,
                            final int threshold)
    throws IOException
  {
    final int addressShiftCtrl =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_SHIFTCTRL);
    final int mask;
    final int lsb;
    if (type == Type.TX) {
      mask = Constants.SM0_SHIFTCTRL_PULL_THRESH_BITS;
      lsb = Constants.SM0_SHIFTCTRL_PULL_THRESH_LSB;
    } else {
      mask = Constants.SM0_SHIFTCTRL_PUSH_THRESH_BITS;
      lsb = Constants.SM0_SHIFTCTRL_PUSH_THRESH_LSB;
    }
    sdk.hwWriteMasked(addressShiftCtrl, threshold << lsb, mask);
    console.printf("(pio%d:sm%d) set %s threshold to %d%n",
                   pioNum, smNum, type == Type.TX ? "pull" : "push", threshold);
  }

  private void setShiftDir(final int pioNum, final int smNum, final Type type,
                           final boolean right)
    throws IOException
  {
    final int addressShiftCtrl =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_SHIFTCTRL);
    final int mask;
    final int lsb;
    if (type == Type.TX) {
      mask = Constants.SM0_SHIFTCTRL_OUT_SHIFTDIR_BITS;
      lsb = Constants.SM0_SHIFTCTRL_OUT_SHIFTDIR_LSB;
    } else {
      mask = Constants.SM0_SHIFTCTRL_IN_SHIFTDIR_BITS;
      lsb = Constants.SM0_SHIFTCTRL_IN_SHIFTDIR_LSB;
    }
    sdk.hwWriteMasked(addressShiftCtrl, (right ? 0x1 : 0x0) << lsb, mask);
    console.printf("(pio%d:sm%d) set shift direction for %s to %s%n",
                   pioNum, smNum, type == Type.TX ? "OSR" : "ISR",
                   shiftDirectionAsString(right));
  }

  private void setAuto(final int pioNum, final int smNum, final Type type,
                       final boolean auto)
    throws IOException
  {
    final int addressShiftCtrl =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_SHIFTCTRL);
    final int mask;
    final int lsb;
    if (type == Type.TX) {
      mask = Constants.SM0_SHIFTCTRL_AUTOPULL_BITS;
      lsb = Constants.SM0_SHIFTCTRL_AUTOPULL_LSB;
    } else {
      mask = Constants.SM0_SHIFTCTRL_AUTOPUSH_BITS;
      lsb = Constants.SM0_SHIFTCTRL_AUTOPUSH_LSB;
    }
    sdk.hwWriteMasked(addressShiftCtrl, (auto ? 0x1 : 0x0) << lsb, mask);
    console.printf("(pio%d:sm%d) set auto-%s=%s%n",
                   pioNum, smNum, type == Type.TX ? "pull" : "push", auto);
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

  private void clear(final int pioNum, final int smNum) throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    pioSdk.smClearFIFOs(smNum);
    console.printf("(pio%d:sm%d) cleared FIFOs%n", pioNum, smNum);
  }

  private void clearFDebug(final int pioNum, final int smNum,
                           final int lsb, final String flagName)
    throws IOException
  {
    final int addressFDebug =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.FDEBUG);
    sdk.writeAddress(addressFDebug, 0x1 << (lsb + smNum));
    console.printf("(pio%d:sm%d) cleared FDEBUG flag %s%n",
                   pioNum, smNum, flagName);
  }

  private void dequeue(final int pioNum, final int smNum, final Type type)
    throws IOException
  {
    final int address = type == Type.TX ?
      PIOEmuRegisters.getTXFAddress(pioNum, smNum) :
      PIORegisters.getRXFAddress(pioNum, smNum);
    final int value = sdk.readAddress(address);
    console.printf("(pio%d:sm%d) dequeued 0x%08x from %s%n",
                   pioNum, smNum, value, type);
  }

  private void enqueue(final int pioNum, final int smNum, final Type type,
                       final int value)
    throws IOException
  {
    final int address = type == Type.RX ?
      PIOEmuRegisters.getRXFAddress(pioNum, smNum) :
      PIORegisters.getTXFAddress(pioNum, smNum);
    sdk.writeAddress(address, value);
    console.printf("(pio%d:sm%d) enqueued 0x%08x to %s%n",
                   pioNum, smNum, value, type);
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
      console.printf("(pio%d:sm%d) set join %s%n", pioNum, smNum, type);
    } else {
      sdk.hwClearBits(addressShiftCtrl, mask);
      console.printf("(pio%d:sm%d) unset join %s%n", pioNum, smNum, type);
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
    final boolean optClearValue = options.getValue(optClear).isOn();
    final boolean optClearTxStallValue =
      options.getValue(optClearTxStall).isOn();
    final boolean optClearTxOverValue =
      options.getValue(optClearTxOver).isOn();
    final boolean optClearRxUnderValue =
      options.getValue(optClearRxUnder).isOn();
    final boolean optClearRxStallValue =
      options.getValue(optClearRxStall).isOn();
    final Integer optValueValue = options.getValue(optValue);
    final boolean optDequeueValue = options.getValue(optDequeue).isOn();
    final boolean optEnqueueValue = options.getValue(optEnqueue).isOn();
    final boolean optJoinValue = options.getValue(optJoin).isOn();
    final boolean optUnjoinValue = options.getValue(optUnjoin).isOn();
    final Integer optThresholdValue = options.getValue(optThreshold);
    final boolean optShiftLeftValue = options.getValue(optShiftLeft).isOn();
    final boolean optShiftRightValue = options.getValue(optShiftRight).isOn();
    final Boolean optAutoValue = options.getValue(optAuto);
    final boolean haveModOp =
      optClearValue || optClearTxStallValue || optClearTxOverValue ||
      optClearRxUnderValue || optClearRxStallValue ||
      optDequeueValue || optEnqueueValue ||
      optJoinValue || optUnjoinValue || (optThresholdValue != null) ||
      optShiftLeftValue || optShiftRightValue || (optAutoValue != null) ||
      (optAddressValue != null) || (optValueValue != null);
    if (!haveModOp) {
      displayFifo(pioNum, smNum);
    }
    if (optClearValue) {
      clear(pioNum, smNum);
    }
    if (optClearTxStallValue) {
      clearFDebug(pioNum, smNum, Constants.FDEBUG_TXSTALL_LSB, "TX Stall");
    }
    if (optClearTxOverValue) {
      clearFDebug(pioNum, smNum, Constants.FDEBUG_TXOVER_LSB, "TX Over");
    }
    if (optClearRxUnderValue) {
      clearFDebug(pioNum, smNum, Constants.FDEBUG_RXUNDER_LSB, "RX Under");
    }
    if (optClearRxStallValue) {
      clearFDebug(pioNum, smNum, Constants.FDEBUG_RXSTALL_LSB, "RX Stall");
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
    if (optThresholdValue != null) {
      setThreshold(pioNum, smNum, type, optThresholdValue);
    }
    if (optShiftLeftValue) {
      setShiftDir(pioNum, smNum, type, false);
    }
    if (optShiftRightValue) {
      setShiftDir(pioNum, smNum, type, true);
    }
    if (optAutoValue != null) {
      setAuto(pioNum, smNum, type, optAutoValue);
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
