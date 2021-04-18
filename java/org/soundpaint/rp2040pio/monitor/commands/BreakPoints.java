/*
 * @(#)BreakPoints.java 1.00 21/04/04
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
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "breakpoints" manages breakpoints.  Breakpoints are
 * not a feature of the RP2040 itself, but have been added to the PIO
 * emulator for advanced debugging.
 */
public class BreakPoints extends Command
{
  private static final String fullName = "breakpoints";
  private static final String singleLineDescription =
    "change breakpoints";
  private static final String notes =
    "For displaying breakpoints, use the \"unassemble\" command.";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optAdd =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "add", null,
                                   "add breakpoint at specified address " +
                                   "(0x00…0x1f)");
  private static final CmdOptions.IntegerOptionDeclaration optDelete =
    CmdOptions.createIntegerOption("ADDRESS", false, 'd', "delete", null,
                                   "remove breakpoint from specified " +
                                   "address (0x00…0x1f)");

  private final SDK sdk;

  public BreakPoints(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optAdd, optDelete });
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
      if (!options.isDefined(optAdd) && !options.isDefined(optDelete)) {
        throw new CmdOptions.
          ParseException("at least one of options -a and -d must be specified");
      }
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
    final Integer optAddValue = options.getValue(optAdd);
    final Integer optDeleteValue = options.getValue(optDelete);
    final int address =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_BREAKPOINTS);
    if (optAddValue != null) {
      sdk.hwSetBits(address, 0x1 << optAddValue);
    }
    if (optDeleteValue != null) {
      sdk.hwClearBits(address, 0x1 << optDeleteValue);
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
