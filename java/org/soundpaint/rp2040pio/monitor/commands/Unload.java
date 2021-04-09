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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.IOUtils;
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
    "Just like with command load, hex dumps can be loaded from either a%n" +
    "regular file specified with option \"-f\", or an example hex dump%n" +
    "specified with option \"-e\".  For getting a list of available%n" +
    "example hex dump names, use the \"load\" command.%n" +
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

  private final SDK sdk;

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.StringOptionDeclaration optExample =
    CmdOptions.createStringOption("NAME", false, 'e', "example", null,
                                  "name of example hex dump to unload");
  private static final CmdOptions.StringOptionDeclaration optFile =
    CmdOptions.createStringOption("STRING", false, 'f', "file", null,
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
    final int pioNum = options.getValue(optPio);
    if ((pioNum < 0) || (pioNum > Constants.PIO_NUM - 1)) {
      throw new CmdOptions.
        ParseException("PIO number must be either 0 or 1");
    }
    final String optExampleValue = options.getValue(optExample);
    final String optFileValue = options.getValue(optFile);
    int count = 0;
    if (optExampleValue != null) count++;
    if (optFileValue != null) count++;
    if (options.getValue(optHelp) != CmdOptions.Flag.ON) {
      if (count == 0) {
        throw new CmdOptions.
          ParseException("at least one of options \"-l\", \"-e\" and \"-f\" " +
                         "must be specified");
      }
    }
    if (count > 1) {
      throw new CmdOptions.
        ParseException("at most one of options \"-l\", \"-e\" and \"-f\" " +
                       "may be specified at the same time");
    }
    if (options.getValue(optHelp) != CmdOptions.Flag.ON) {
      if (!options.isDefined(optAddress)) {
        throw new CmdOptions.
          ParseException("option not specified: " + optAddress);
      }
    }
  }

  private boolean unloadHexDump(final int pioNum,
                                final BufferedReader reader,
                                final String hexDumpId,
                                final Integer loadedOffset)
    throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    pioSdk.removeProgram(hexDumpId, reader, loadedOffset);
    console.printf("removed program %s from PIO %d, address 0x%02x%n",
                   pioNum, hexDumpId, loadedOffset);
    return true;
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final int pioNum = options.getValue(optPio);
    final String optExampleValue = options.getValue(optExample);
    final String optFileValue = options.getValue(optFile);
    final int address = options.getValue(optAddress);
    if (optExampleValue != null) {
      final String resourcePath =
        String.format("/examples/%s.hex", optExampleValue);
      final LineNumberReader reader =
        IOUtils.getReaderForResourcePath(resourcePath);
      return unloadHexDump(pioNum, reader, optExampleValue, address);
    } else if (optFileValue != null) {
      final LineNumberReader reader =
        IOUtils.getReaderForResourcePath(optFileValue);
      return unloadHexDump(pioNum, reader, optFileValue, address);
    }
    return false;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */