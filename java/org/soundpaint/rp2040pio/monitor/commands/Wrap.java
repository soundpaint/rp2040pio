/*
 * @(#)Wrap.java 1.00 21/04/04
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
 * Monitor command "wrap" provides functionality comparable to the
 * PIOASM directives ".wrap_target" and ".wrap".
 */
public class Wrap extends Command
{
  private static final String fullName = "wrap";
  private static final String singleLineDescription =
    "display or control a state machine's wrap and wrap target configuration";
  private static final String notes =
    "Options -p and -s select the state machine that this command%n" +
    "applies to.  Default is PIO0 and SM0.%n" +
    "%n" +
    "If none of the options -w, -t is specified, the currently%n" +
    "configured wrap and wrap target of the selected state machine will be%n" +
    "displayed.  If at least one of the options -w, -t is%n" +
    "specified, the corresponding settings will be adjusted, while for%n" +
    "those not specified the corresponding settings will keep unmodified.";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optWrap =
    CmdOptions.createIntegerOption("ADDRESS", false, 'w', "wrap", null,
                                   "wrap (WRAP_TOP) address (0x00…0x1f)");
  private static final CmdOptions.IntegerOptionDeclaration optWrapTarget =
    CmdOptions.createIntegerOption("ADDRESS", false, 't', "target", null,
                                   "wrap target (WRAP_BOTTOM) address " +
                                   "(0x00…0x1f)");

  private final SDK sdk;

  public Wrap(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optWrap, optWrapTarget });
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
      final Integer optWrapValue = options.getValue(optWrap);
      if (optWrapValue != null) {
        final int wrap = optWrapValue;
        if ((wrap < 0) || (wrap > Constants.MEMORY_SIZE - 1)) {
          final String message =
            String.format("wrap address must be in the range 0x00…0x%02x",
                          Constants.MEMORY_SIZE - 1);
          throw new CmdOptions.ParseException(message);
        }
      }
      final Integer optWrapTargetValue = options.getValue(optWrapTarget);
      if (optWrapTargetValue != null) {
        final int wrapTarget = optWrapTargetValue;
        if ((wrapTarget < 0) || (wrapTarget > Constants.MEMORY_SIZE - 1)) {
          final String message =
            String.format("wrap target address must be in the range " +
                          "0x00…0x%02x",
                          Constants.MEMORY_SIZE - 1);
          throw new CmdOptions.ParseException(message);
        }
      }
    }
  }

  private void displayWrap(final int pioNum, final int smNum,
                           final SDK sdk)
    throws IOException
  {
    final int execCtrlAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final int execCtrl = sdk.readAddress(execCtrlAddress);
    final int wrap =
      (execCtrl & Constants.SM0_EXECCTRL_WRAP_TOP_BITS) >>>
      Constants.SM0_EXECCTRL_WRAP_TOP_LSB;
    final int wrapTarget =
      (execCtrl & Constants.SM0_EXECCTRL_WRAP_BOTTOM_BITS) >>>
      Constants.SM0_EXECCTRL_WRAP_BOTTOM_LSB;
    console.printf("(pio%d:sm%d) wrap=%d, wrap_target=%d%n",
                   pioNum, smNum, wrap, wrapTarget);
  }

  private void setWrap(final int pioNum, final int smNum,
                       final SDK sdk, final int wrap)
    throws IOException
  {
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final int writeMask = Constants.SM0_EXECCTRL_WRAP_TOP_BITS;
    final int values = wrap << Constants.SM0_EXECCTRL_WRAP_TOP_LSB;
    sdk.hwWriteMasked(address, values, writeMask);
    console.printf("(pio%d:sm%d) set wrap=%d%n", pioNum, smNum, wrap);
  }

  private void setWrapTarget(final int pioNum, final int smNum,
                             final SDK sdk, final int wrapTarget)
    throws IOException
  {
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final int writeMask = Constants.SM0_EXECCTRL_WRAP_BOTTOM_BITS;
    final int values = wrapTarget << Constants.SM0_EXECCTRL_WRAP_BOTTOM_LSB;
    sdk.hwWriteMasked(address, values, writeMask);
    console.printf("(pio%d:sm%d) set wrap_target=%d%n",
                   pioNum, smNum, wrapTarget);
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
    final Integer optWrapValue = options.getValue(optWrap);
    final Integer optWrapTargetValue = options.getValue(optWrapTarget);
    if ((optWrapValue == null) && (optWrapTargetValue == null)) {
      displayWrap(pioNum, smNum, sdk);
    } else {
      if (optWrapValue != null) {
        setWrap(pioNum, smNum, sdk, optWrapValue);
      }
      if (optWrapTargetValue != null) {
        setWrapTarget(pioNum, smNum, sdk, optWrapTargetValue);
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
