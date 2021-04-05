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

import java.io.IOException;
import java.io.PrintStream;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.monitor.Command;
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
    "Note that tracking memory allocation is not a feature of the%n" +
    "RP2040, but local to this monitor instance, just to avoid%n" +
    "accidentally overwriting your own PIO programs.  Other applications%n" +
    "that concurrently access the RP2040 will therefore ignore%n" +
    "this instance's allocation tracking and may arbitrarily%n" +
    "overwrite allocated PIO memory, using their own allocation scheme.%n" +
    "%n" +
    "Order of precedence in looking up the specified path:%n" +
    "The \"load\" command will first try to find a regular file with the%n" +
    "specified path interpreted as file path.  If no such file is%n" +
    "found, it will next interpret the path as a resource path and try%n" +
    "to find a resource that matches the specified path.  In particular,%n" +
    "under the resource path \"/examples\" within the jar file that%n" +
    "contains this application, there are some pre-installed example%n" +
    "programs available.%n" +
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
  private static final String defaultPath = "/examples/squarewave.hex";

  private final SDK sdk;

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.StringOptionDeclaration optFile =
    CmdOptions.createStringOption("PATH", false, 'f', "file", defaultPath,
                                  "path of hex dump file to load");
  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", null,
                                   "preferred program start address");

  public Load(final PrintStream console, final SDK sdk)
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
      final int pioNum = options.getValue(optPio);
      if ((pioNum < 0) || (pioNum > Constants.PIO_NUM - 1)) {
        throw new CmdOptions.
          ParseException("PIO number must be either 0 or 1");
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
    final Integer optAddressValue = options.getValue(optAddress);
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    final int assignedAddress =
      optAddressValue != null ?
      pioSdk.addProgramAtOffset(resourcePath, optAddressValue) :
      pioSdk.addProgram(resourcePath);
    console.printf("stored program at PIO %d, address 0x%02x%n",
                   pioNum, assignedAddress);
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
