/*
 * @(#)Unload.java 1.00 21/04/03
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
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "unload" removes a program from a PIO's memory.
 */
public class Unload extends Command
{
  private static final String fullName = "unload";
  private static final String singleLineDescription =
    "zero PIO memory area for the specified program and unmark it as allocated";
  private static final String notes =
    "The \"unload\" command reads in again the specified program in order%n" +
    "to determine its program length and, if appropriate, origin address.%n" +
    "The identified memory area that is associated with the specified PIO%n" +
    "program will be zeroed, and any memory allocation marks found in this%n" +
    "memory area will be removed.%n" +
    "%n" +
    "Note that tracking memory allocation is not a feature of the%n" +
    "RP2040, but local to this monitor instance, just to avoid%n" +
    "accidentally overwriting your own PIO programs.  Other applications%n" +
    "that concurrently access the RP2040 will therefore ignore%n" +
    "this instance's allocation tracking and may arbitrarily%n" +
    "overwrite allocated PIO memory, using their own allocation scheme.%n" +
    "%n" +
    "For information about order of precedence in looking up the specified%n" +
    "path and expected file format, enter the command \"load -h\" to view%n" +
    "the help information of the \"load\" command.%n";
  private static final String defaultPath = "/examples/squarewave.hex";

  private final SDK sdk;

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.StringOptionDeclaration optFile =
    CmdOptions.createStringOption("STRING", false, 'f', "file", defaultPath,
                                  "path of hex dump file to unload");
  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", null,
                                   "start address of the area to free");

  public Unload(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optFile, optAddress });
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
      final int pio = options.getValue(optPio);
      if ((pio < 0) || (pio > 1)) {
        throw new CmdOptions.
          ParseException("PIO number must be either 0 or 1");
      }
      if (!options.isDefined(optAddress)) {
        throw new CmdOptions.
          ParseException("option not specified: " + optAddress);
      }
      if (!options.isDefined(optFile)) {
        throw new CmdOptions.
          ParseException("option not specified: " + optFile);
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
    final String resourcePath = options.getValue(optFile);
    final int address = options.getValue(optAddress);
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    pioSdk.removeProgram(resourcePath, address);
    console.printf("removed program from PIO %d, address 0x%02x%n",
                   pioNum, address);
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
