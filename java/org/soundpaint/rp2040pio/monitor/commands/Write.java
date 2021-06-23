/*
 * @(#)Write.java 1.00 21/03/30
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
 * Monitor command "write" provides low-level write access to a
 * register.
 */
public class Write extends Command
{
  private static final String fullName = "write";
  private static final String singleLineDescription =
    "low-level write access to a register";

  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", null,
                                   "address (0x00000000…0xffffffff) of the " +
                                   "register to access");
  private static final CmdOptions.IntegerOptionDeclaration optValue =
    CmdOptions.createIntegerOption("VALUE", false, 'v', "value", null,
                                   "value to write");

  private final SDK sdk;

  public Write(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription,
          new CmdOptions.OptionDeclaration<?>[] { optAddress, optValue });
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
      if (!options.isDefined(optAddress)) {
        throw new CmdOptions.
          ParseException("option not specified: " + optAddress);
      }
      if (!options.isDefined(optValue)) {
        throw new CmdOptions.
          ParseException("option not specified: " + optValue);
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
    final int address = options.getValue(optAddress);
    final int value = options.getValue(optValue);
    final boolean validAddress = sdk.providesAddress(address);
    if (!validAddress) {
      final String message =
        String.format("write to unsupported address: 0x%08x", address);
      throw new IOException(message);
    }
    sdk.writeAddress(address, value);
    final String label = sdk.getLabelForAddress(address);
    console.printf("wrote 0x%04x to %s (0x%08x)%n", value, label, address);
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
