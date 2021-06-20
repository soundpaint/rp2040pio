/*
 * @(#)PinCtrl.java 1.00 21/06/01
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
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "pinctrl" displays or modifies a state machine's
 * pin control.
 */
public class PinCtrl extends Command
{
  private static final String fullName = "pinctrl";
  private static final String singleLineDescription =
    "display or change state machine's pin control";
  private static final String notes =
    "Use options \"-p\" and \"-s\" to select a state machine.%n" +
    "If none of the pin control modification options is specified, the%n" +
    "status of the pin control of the selected state machine is displayed.%n" +
    "For setting pin count or pin base for SET / OUT / IN instructions or%n" +
    "the GPIO pin number to check when executing the JMP PIN instruction,%n" +
    "use the corresponding \"--set-count\", \"--set-base\",%n" +
    "\"--out-count\", \"--out-base\", \"--in-base\" or \"--jmp-pin\"%n" +
    "option.%n" +
    "This command does not support setting the side-set count or%n" +
    "side-set base.  For modifying side-set configuration, use the%n" +
    "monitor command \"side-set\" instead.";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optSetCount =
    CmdOptions.createIntegerOption("COUNT", false, null, "set-count", null,
                                   "number of GPIO pins asserted by SET "+
                                   "instruction (0…5)");
  private static final CmdOptions.IntegerOptionDeclaration optSetBase =
    CmdOptions.createIntegerOption("NUMBER", false, null, "set-base", null,
                                   "lowest-numbered GPIO pin affected by "+
                                   "SET PINS / PINDIRS instruction (0…31)");
  private static final CmdOptions.IntegerOptionDeclaration optOutCount =
    CmdOptions.createIntegerOption("COUNT", false, null, "out-count", null,
                                   "number of GPIO pins asserted by OUT PINS "+
                                   "/ PINDIRS or MOV PINS instruction (0…32)");
  private static final CmdOptions.IntegerOptionDeclaration optOutBase =
    CmdOptions.createIntegerOption("NUMBER", false, null, "out-base", null,
                                   "lowest-numbered GPIO pin affected by OUT "+
                                   "/ MOV PINS / PINDIRS instruction (0…31)");
  private static final CmdOptions.IntegerOptionDeclaration optInBase =
    CmdOptions.createIntegerOption("NUMBER", false, null, "in-base", null,
                                   "GPIO pin mapped to LSB for IN " +
                                   "instruction (0…31)");
  private static final CmdOptions.IntegerOptionDeclaration optJmpPin =
    CmdOptions.createIntegerOption("NUMBER", false, null, "jmp-pin", null,
                                   "GPIO pin to check when executing " +
                                   "JMP PIN instruction (0…31)");

  private final SDK sdk;

  public PinCtrl(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm,
              optSetCount, optSetBase, optOutCount, optOutBase,
              optInBase, optJmpPin });
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
      final Integer optSetCountValue = options.getValue(optSetCount);
      if (optSetCountValue != null) {
        final int setCount = optSetCountValue;
        if ((setCount < 0) || (setCount > 5)) {
          throw new CmdOptions.
            ParseException("set-count must be in the range 0…5");
        }
      }
      final Integer optSetBaseValue = options.getValue(optSetBase);
      if (optSetBaseValue != null) {
        final int setBase = optSetBaseValue;
        if ((setBase < 0) || (setBase > 31)) {
          throw new CmdOptions.
            ParseException("set-base must be in the range 0…31");
        }
      }
      final Integer optOutCountValue = options.getValue(optOutCount);
      if (optOutCountValue != null) {
        final int outCount = optOutCountValue;
        if ((outCount < 0) || (outCount > 32)) {
          throw new CmdOptions.
            ParseException("out-count must be in the range 0…32");
        }
      }
      final Integer optOutBaseValue = options.getValue(optOutBase);
      if (optOutBaseValue != null) {
        final int outBase = optOutBaseValue;
        if ((outBase < 0) || (outBase > 31)) {
          throw new CmdOptions.
            ParseException("out-base must be in the range 0…31");
        }
      }
      final Integer optInBaseValue = options.getValue(optInBase);
      if (optInBaseValue != null) {
        final int inBase = optInBaseValue;
        if ((inBase < 0) || (inBase > 31)) {
          throw new CmdOptions.
            ParseException("in-base must be in the range 0…31");
        }
      }
      final Integer optJmpPinValue = options.getValue(optJmpPin);
      if (optJmpPinValue != null) {
        final int jmpPin = optJmpPinValue;
        if ((jmpPin < 0) || (jmpPin > 31)) {
          throw new CmdOptions.
            ParseException("jmp-pin must be in the range 0…31");
        }
      }
    }
  }

  private void displayPinCtrl(final int pioNum, final int smNum,
                              final SDK sdk)
    throws IOException
  {
    final int pinCtrlAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    final int pinCtrl = sdk.readAddress(pinCtrlAddress);
    final int execCtrlAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final int execCtrl = sdk.readAddress(execCtrlAddress);
    final int setCount =
      (pinCtrl & Constants.SM0_PINCTRL_SET_COUNT_BITS) >>>
      Constants.SM0_PINCTRL_SET_COUNT_LSB;
    final int setBase =
      (pinCtrl & Constants.SM0_PINCTRL_SET_BASE_BITS) >>>
      Constants.SM0_PINCTRL_SET_BASE_LSB;
    final int outCount =
      (pinCtrl & Constants.SM0_PINCTRL_OUT_COUNT_BITS) >>>
      Constants.SM0_PINCTRL_OUT_COUNT_LSB;
    final int outBase =
      (pinCtrl & Constants.SM0_PINCTRL_OUT_BASE_BITS) >>>
      Constants.SM0_PINCTRL_OUT_BASE_LSB;
    final int inBase =
      (pinCtrl & Constants.SM0_PINCTRL_IN_BASE_BITS) >>>
      Constants.SM0_PINCTRL_IN_BASE_LSB;
    final int jmpPin =
      (execCtrl & Constants.SM0_EXECCTRL_JMP_PIN_BITS) >>>
      Constants.SM0_EXECCTRL_JMP_PIN_LSB;
    console.printf("(pio%d:sm%d) set-count=%d, set-base=%d%n",
                   pioNum, smNum, setCount, setBase);
    console.printf("(pio%d:sm%d) out-count=%d, out-base=%d%n",
                   pioNum, smNum, outCount, outBase);
    console.printf("(pio%d:sm%d) jmp-pin=%d, in-base=%d%n",
                   pioNum, smNum, jmpPin, inBase);
  }

  private void setAny(final int pioNum, final int smNum,
                      final SDK sdk, final String name,
                      final int value, final int mask, final int lsb,
                      final boolean execCtrl)
    throws IOException
  {
    final int address =
      execCtrl ?
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL) :
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    final int bits = value << lsb;
    sdk.hwWriteMasked(address, bits, mask);
    console.printf("(pio%d:sm%d) set %s to %d%n", pioNum, smNum, name, value);
  }

  private void setSetCount(final int pioNum, final int smNum,
                           final SDK sdk, final int count)
    throws IOException
  {
    setAny(pioNum, smNum, sdk, "SET count", count,
           Constants.SM0_PINCTRL_SET_COUNT_BITS,
           Constants.SM0_PINCTRL_SET_COUNT_LSB, false);
  }

  private void setSetBase(final int pioNum, final int smNum,
                          final SDK sdk, final int base)
    throws IOException
  {
    setAny(pioNum, smNum, sdk, "SET base", base,
           Constants.SM0_PINCTRL_SET_BASE_BITS,
           Constants.SM0_PINCTRL_SET_BASE_LSB, false);
  }

  private void setOutCount(final int pioNum, final int smNum,
                           final SDK sdk, final int count)
    throws IOException
  {
    setAny(pioNum, smNum, sdk, "OUT count", count,
           Constants.SM0_PINCTRL_OUT_COUNT_BITS,
           Constants.SM0_PINCTRL_OUT_COUNT_LSB, false);
  }

  private void setOutBase(final int pioNum, final int smNum,
                          final SDK sdk, final int base)
    throws IOException
  {
    setAny(pioNum, smNum, sdk, "OUT base", base,
           Constants.SM0_PINCTRL_OUT_BASE_BITS,
           Constants.SM0_PINCTRL_OUT_BASE_LSB, false);
  }

  private void setInBase(final int pioNum, final int smNum,
                         final SDK sdk, final int base)
    throws IOException
  {
    setAny(pioNum, smNum, sdk, "IN base", base,
           Constants.SM0_PINCTRL_IN_BASE_BITS,
           Constants.SM0_PINCTRL_IN_BASE_LSB, false);
  }

  private void setJmpPin(final int pioNum, final int smNum,
                         final SDK sdk, final int gpioNum)
    throws IOException
  {
    setAny(pioNum, smNum, sdk, "JMP PIN", gpioNum,
           Constants.SM0_EXECCTRL_JMP_PIN_BITS,
           Constants.SM0_EXECCTRL_JMP_PIN_LSB, true);
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
    final Integer optSetCountValue = options.getValue(optSetCount);
    final Integer optSetBaseValue = options.getValue(optSetBase);
    final Integer optOutCountValue = options.getValue(optOutCount);
    final Integer optOutBaseValue = options.getValue(optOutBase);
    final Integer optInBaseValue = options.getValue(optInBase);
    final Integer optJmpPinValue = options.getValue(optJmpPin);
    if ((optSetCountValue == null) && (optSetBaseValue == null) &&
        (optOutCountValue == null) && (optOutBaseValue == null) &&
        (optInBaseValue == null) && (optJmpPinValue == null)) {
      displayPinCtrl(pioNum, smNum, sdk);
    } else {
      if (optSetCountValue != null) {
        setSetCount(pioNum, smNum, sdk, optSetCountValue);
      }
      if (optSetBaseValue != null) {
        setSetBase(pioNum, smNum, sdk, optSetBaseValue);
      }
      if (optOutCountValue != null) {
        setOutCount(pioNum, smNum, sdk, optOutCountValue);
      }
      if (optOutBaseValue != null) {
        setOutBase(pioNum, smNum, sdk, optOutBaseValue);
      }
      if (optInBaseValue != null) {
        setInBase(pioNum, smNum, sdk, optInBaseValue);
      }
      if (optJmpPinValue != null) {
        setJmpPin(pioNum, smNum, sdk, optJmpPinValue);
      }
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
