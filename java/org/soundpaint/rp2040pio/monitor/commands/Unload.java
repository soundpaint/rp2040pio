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
import org.soundpaint.rp2040pio.monitor.MonitorUtils;
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
    "The \"unload\" command first reads in the specified hex dump in order%n" +
    "to determine the program length of the corresponding PIO program.%n" +
    "Then, the identified instruction memory area that is associated with%n" +
    "the PIO program in the specified PIO will be zeroed, and any memory%n" +
    "allocation marks found in this memory area will be removed.%n" +
    "%n" +
    "Built-in example hex dumps are available that can be listed with%n" +
    "the \"-l\" option.  To select any of the example hex dumps, use the%n" +
    "\"-e\" option and pass to this option the hex dump's name as shown%n" +
    "in the list of available built-in hex dumps.  To view a built-in hex%n" +
    "dump prior to unloading it, use the \"-s\" option.%n" +
    "For user-provided hex dumps, use the \"-f\" option to specify the%n" +
    "file path of the hex dump, including the \".hex\" file name suffix." +
    "%n" +
    "Note that tracking memory allocation is not a feature of the%n" +
    "RP2040, but local to this monitor instance, just to avoid%n" +
    "accidentally overwriting your own PIO programs.  Other applications%n" +
    "that concurrently access the RP2040 will therefore ignore%n" +
    "this instance's allocation tracking and may arbitrarily%n" +
    "overwrite allocated PIO memory, using their own allocation scheme.%n" +
    "%n" +
    "For information about the expected file format, enter the command%n" +
    "\"load -h\" to view the help information of the \"load\" command.";

  private final SDK sdk;

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.FlagOptionDeclaration optList =
    CmdOptions.createFlagOption(false, 'l', "list", CmdOptions.Flag.OFF,
                                "list names of available example hex dumps");
  private static final CmdOptions.StringOptionDeclaration optShow =
    CmdOptions.createStringOption("NAME", false, 's', "show", null,
                                  "name of example hex dump to show");
  private static final CmdOptions.StringOptionDeclaration optExample =
    CmdOptions.createStringOption("NAME", false, 'e', "example", null,
                                  "name of example hex dump to unload");
  private static final CmdOptions.StringOptionDeclaration optFile =
    CmdOptions.createStringOption("STRING", false, 'f', "file", null,
                                  "path of hex dump file to unload");
  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", null,
                                   "start address (0x00…0x1f) of the area " +
                                   "to free");

  public Unload(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optList, optShow, optExample, optFile, optAddress });
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
    final boolean optListValue =
      options.getValue(optList) == CmdOptions.Flag.ON;
    final String optShowValue = options.getValue(optShow);
    final String optExampleValue = options.getValue(optExample);
    final String optFileValue = options.getValue(optFile);
    int count = 0;
    if (optListValue) count++;
    if (optShowValue != null) count++;
    if (optExampleValue != null) count++;
    if (optFileValue != null) count++;
    if (options.getValue(optHelp) != CmdOptions.Flag.ON) {
      if (count == 0) {
        throw new CmdOptions.
          ParseException("at least one of options \"-l\", \"-s\", \"-e\" " +
                         "and \"-f\" must be specified");
      }
    }
    if (count > 1) {
      throw new CmdOptions.
        ParseException("at most one of options \"-l\", \"-s\", \"-e\" " +
                       "and \"-f\" may be specified at the same time");
    }
    if (options.getValue(optHelp) != CmdOptions.Flag.ON) {
      if (options.isDefined(optExample) ||
          options.isDefined(optFile)) {
        if (!options.isDefined(optAddress)) {
          throw new CmdOptions.
            ParseException("option not specified: " + optAddress);
        }
      }
    }
  }

  private boolean unloadHexDump(final int pioNum,
                                final BufferedReader reader,
                                final String hexDumpId,
                                final int loadedOffset)
    throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    pioSdk.removeProgram(hexDumpId, reader, loadedOffset);
    console.printf("removed program %s from PIO %d, address 0x%02x%n",
                   hexDumpId, pioNum, loadedOffset);
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
    final boolean optListValue =
      options.getValue(optList) == CmdOptions.Flag.ON;
    final String optShowValue = options.getValue(optShow);
    final String optExampleValue = options.getValue(optExample);
    final String optFileValue = options.getValue(optFile);
    final Integer optAddressValue = options.getValue(optAddress);
    if (optListValue) {
      return MonitorUtils.listExampleHexDumps(console);
    } else if (optShowValue != null) {
      return MonitorUtils.showExampleHexDump(console, optShowValue);
    } else if (optExampleValue != null) {
      final String resourcePath =
        String.format("/examples/%s.hex", optExampleValue);
      final LineNumberReader reader =
        IOUtils.getReaderForResourcePath(resourcePath);
      return unloadHexDump(pioNum, reader, optExampleValue, optAddressValue);
    } else if (optFileValue != null) {
      final LineNumberReader reader =
        IOUtils.getReaderForResourcePath(optFileValue);
      return unloadHexDump(pioNum, reader, optFileValue, optAddressValue);
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
