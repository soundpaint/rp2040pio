/*
 * @(#)Label.java 1.00 21/03/29
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
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "label" displays a register's name, if available.
 */
public class Label extends Command
{
  private static final String fullName = "label";
  private static final String singleLineDescription =
    "display a register's label";

  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", null,
                                   "address (0x00000000…0xffffffff) of the " +
                                   "register to display");

  private final SDK sdk;

  public Label(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription,
          new CmdOptions.OptionDeclaration<?>[] { optAddress });
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    this.sdk = sdk;
  }

  @Override
  protected void checkValidity(final CmdOptions options)
    throws CmdOptions.ParseException
  {
    if ((options.getValue(optHelp) != CmdOptions.Flag.ON) &&
        (!options.isDefined(optAddress))) {
      throw new CmdOptions.
        ParseException("option not specified: " + optAddress);
    }
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final int address = options.getValue(optAddress);
    final boolean validAddress = sdk.providesAddress(address);
    if (!validAddress) {
      final String message =
        String.format("unsupported address: 0x%08x", address);
      throw new IOException(message);
    }
    final String label = sdk.getLabelForAddress(address);
    console.printf("%s (0x%08x)%n" , label, address);
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
