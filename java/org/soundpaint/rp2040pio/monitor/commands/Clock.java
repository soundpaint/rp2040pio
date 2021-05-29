/*
 * @(#)Clock.java 1.00 21/05/29
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
 * Monitor command "clock" displays or modifies a state machine's
 * clock configuration.
 */
public class Clock extends Command
{
  private static final float FRAC_MUL = 1.0f / 256;
  private static final String fullName = "clock";
  private static final String singleLineDescription =
    "display or change internal state machine's clock configuration";
  private static final String notes =
    "If none of the modification options is specified, the status%n"+
    "of the clock of the selected is displayed.%n" +
    "Otherwise, for all specified options \"-i\", \"-f\" and%n" +
    "\"-r\", the corresponding modification will be performed for%n" +
    "the selected state machine.";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optIntDivider =
    CmdOptions.createIntegerOption("NUMBER", false, 'i', "int-divider", null,
                                   "set clock divider integer part for " +
                                   "selected PIO and SM");
  private static final CmdOptions.IntegerOptionDeclaration optFracDivider =
    CmdOptions.createIntegerOption("NUMBER", false, 'f', "frac-divider", null,
                                   "set clock divider fractional part for " +
                                   "selected PIO and SM");
  private static final CmdOptions.FlagOptionDeclaration optRestart =
    CmdOptions.createFlagOption(false, 'r', "restart", CmdOptions.Flag.OFF,
                                "restart clock for selected PIO and SM");

  private final SDK sdk;

  public Clock(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optIntDivider, optFracDivider, optRestart });
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
      final Integer optIntDividerValue = options.getValue(optIntDivider);
      final Integer optFracDividerValue = options.getValue(optFracDivider);
      if (optIntDividerValue != null) {
        final int intDivider = optIntDividerValue;
        if ((intDivider < 0) || (intDivider > 0x10000)) {
          final String message =
            String.format("expected integer divider value in the range " +
                          "0…0x%5x, but got: 0x%x", 0x10000, intDivider);
          throw new CmdOptions.ParseException(message);
        }
        if (optFracDividerValue != null) {
          if ((intDivider & 0xffff) == 0) {
            if (optFracDividerValue != 0) {
              final String message =
                String.format("if int-divider is 0, frac-divider must " +
                              "also be 0, but got: %x", optFracDividerValue);
              throw new CmdOptions.ParseException(message);
            }
          }
        }
      }
      if (optFracDividerValue != null) {
        final int fracDivider = optFracDividerValue;
        if ((fracDivider < 0) || (fracDivider > 0xff)) {
          final String message =
            String.format("expected fractional divider value in the range " +
                          "0…0x%02x, but got: 0x%x", 0xff, fracDivider);
          throw new CmdOptions.ParseException(message);
        }
      }
    }
  }

  private void displayDivider(final int pioNum, final int smNum)
    throws IOException
  {
    final int addressClkDiv =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_CLKDIV);
    final int clkDivValue = sdk.readAddress(addressClkDiv);
    final int intDivider =
      (clkDivValue & Constants.SM0_CLKDIV_INT_BITS) >>>
      Constants.SM0_CLKDIV_INT_LSB;
    final int displayIntDivider = intDivider == 0 ? 0x10000 : intDivider;
    final int fracDivider =
      (clkDivValue & Constants.SM0_CLKDIV_FRAC_BITS) >>>
      Constants.SM0_CLKDIV_FRAC_LSB;
    final float divider = displayIntDivider + FRAC_MUL * fracDivider;
    console.printf("(pio%d:sm%d) int-divider=0x%05x, frac-divider=0x%02x%n",
                   pioNum, smNum, displayIntDivider, fracDivider);
    console.printf("           divider=%f%n", divider);
  }

  private void setIntDivider(final int pioNum, final int smNum,
                             final int intDivider)
    throws IOException
  {
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_CLKDIV);
    final int clkDiv = intDivider << Constants.SM0_CLKDIV_INT_LSB;
    final int mask = Constants.SM0_CLKDIV_INT_BITS;
    sdk.hwWriteMasked(address, clkDiv, mask);
    final int displayIntDivider = intDivider == 0 ? 0x10000 : intDivider;
    console.printf("(pio%d:sm%d) set int-divider=0x%05x%n",
                   pioNum, smNum, displayIntDivider);
  }

  private void setFracDivider(final int pioNum, final int smNum,
                              final int fracDivider)
    throws IOException
  {
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_CLKDIV);
    final int clkDiv = fracDivider << Constants.SM0_CLKDIV_FRAC_LSB;
    final int mask = Constants.SM0_CLKDIV_FRAC_BITS;
    sdk.hwWriteMasked(address, clkDiv, mask);
    console.printf("(pio%d:sm%d) set frac-divider=0x%02x%n",
                   pioNum, smNum, fracDivider);
  }

  private void restart(final int pioNum, final int smNum) throws IOException
  {
    final int address = PIORegisters.getAddress(pioNum, PIORegisters.Regs.CTRL);
    final int mask =
      (0x1 << (Constants.CTRL_SM_RESTART_LSB + smNum)) &
      Constants.CTRL_SM_RESTART_BITS;
    sdk.hwSetBits(address, mask);
    console.printf("(pio%d:sm%d) restarted clock%n", pioNum, smNum);
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
    final Integer optIntDividerValue = options.getValue(optIntDivider);
    final Integer optFracDividerValue = options.getValue(optFracDivider);
    final boolean optRestartValue = options.getValue(optRestart).isOn();
    final boolean haveModOp =
      (optIntDividerValue != null) || (optFracDividerValue != null) ||
      optRestartValue;
    if (!haveModOp) {
      displayDivider(pioNum, smNum);
    }
    if (optIntDividerValue != null) {
      setIntDivider(pioNum, smNum, optIntDividerValue);
    }
    if (optFracDividerValue != null) {
      setFracDivider(pioNum, smNum, optFracDividerValue);
    }
    if (optRestartValue) {
      restart(pioNum, smNum);
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
