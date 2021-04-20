/*
 * @(#)Load.java 1.00 21/03/31
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
 * Monitor command "load" loads a program from a file and stores it in
 * a PIO's memory.
 */
public class Load extends Command
{
  private static final String fullName = "load";
  private static final String singleLineDescription =
    "load program from file and mark affected PIO memory area as allocated";
  private static final String notes =
    "The \"load\" command reads in the specified hex dump and stores it%n" +
    "as a PIO program in one of the two PIOs' instruction memory.%n" +
    "By convention, hex dump files have \".hex\" file name suffix.%n" +
    "%n" +
    "Built-in example hex dumps are available that can be listed with%n" +
    "the \"-l\" option.  To select any of the example hex dumps, use the%n" +
    "\"-e\" option and pass to this option the hex dump's name as shown%n" +
    "in the list of available built-in hex dumps.  To view a built-in hex%n" +
    "dump prior to loading it, use the \"-s\" option.%n" +
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
    "Expected file format:%n" +
    "The program file to be loaded must be a regular text file with%n" +
    "either \"\\n\" or \"\\r\\n\" line endings and UTF-8 encoding.%n" +
    "Other encodings may also work for the core instruction opcodes, but%n" +
    "may give unexpected results if meta information is relevant.%n" +
    "Empty lines are ignored.  Each non-empty line of the text file%n" +
    "must contain either meta information or an opcode.  A meta%n" +
    "information line starts with a leading '#' character and may%n" +
    "contain either a special directive, or,  if the '#' is followed by%n" +
    "a ';' character, an arbitrary user comment.  Each non-empty line that%n" +
    "is not a meta information line must contain a single opcode.  Each%n" +
    "opcode is a 32 bits integer and represented as a plain four-digit%n" +
    "hexadecimal value without leading \"0x\".  The maximum allowed number%n" +
    "of opcodes is 32.";

  private final SDK sdk;

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.FlagOptionDeclaration optList =
    CmdOptions.createFlagOption(false, 'l', "list", CmdOptions.Flag.OFF,
                                "list names of available example hex dumps");
  private static final CmdOptions.StringOptionDeclaration optShow =
    CmdOptions.createStringOption("NAME", false, 's', "show", null,
                                  "name of built-in example hex dump to show");
  private static final CmdOptions.StringOptionDeclaration optExample =
    CmdOptions.createStringOption("NAME", false, 'e', "example", null,
                                  "name of built-in example hex dump to load");
  private static final CmdOptions.StringOptionDeclaration optFile =
    CmdOptions.createStringOption("PATH", false, 'f', "file", null,
                                  "path of hex dump file to load");
  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", null,
                                   "preferred program start address " +
                                   "(0x00…0x1f)");

  public Load(final PrintStream console, final SDK sdk)
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
  }

  private boolean loadHexDump(final int pioNum,
                              final BufferedReader reader,
                              final String hexDumpId,
                              final Integer address)
    throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    final int assignedAddress =
      address != null ?
      pioSdk.addProgramAtOffset(hexDumpId, reader, address) :
      pioSdk.addProgram(hexDumpId, reader);
    console.printf("(pio%d:sm*) loaded program %s at address 0x%02x%n",
                   pioNum, hexDumpId, assignedAddress);
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
      return loadHexDump(pioNum, reader, optExampleValue, optAddressValue);
    } else if (optFileValue != null) {
      final LineNumberReader reader =
        IOUtils.getReaderForResourcePath(optFileValue);
      return loadHexDump(pioNum, reader, optFileValue, optAddressValue);
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
