/*
 * @(#)Save.java 1.00 21/04/05
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.Instant;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "save" saves a selected range of a PIO's
 * instruction memory to a file.
 */
public class Save extends Command
{
  private static final String fullName = "save";
  private static final String singleLineDescription =
    "save a selected range of a PIO's instruction memory to a file";
  private static final String notes =
    "The file is written as a text file, with each instruction%n" +
    "added as a line consisting of its operation code represented%n" +
    "as hexadecimal 32 bit integer value (without \"0x\" prefix).%n" +
    "%n" +
    "If the specified stop address is lower than start address, then%n" +
    "the program is assumed to wrap from the highest memory address to%n" +
    "the first memory address.  Any configuration of a SM specific wrap%n" +
    "or wrap target is ignored.%n" +
    "%n" +
    "If the file is specified to be not relocatable, a proper%n" +
    "\".origin\" directive will be added as a comment line.%n" +
    "%n" +
    "If a program name is provided, it will be added as a%n" +
    "\".program\" directive in a separate comment line.%n" +
    "%n" +
    "Comment lines start with the hash symbol \"#\".";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optStart =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "start", null,
                                   "first address (0x00…0x1f) of the program");
  private static final CmdOptions.IntegerOptionDeclaration optStop =
    CmdOptions.createIntegerOption("ADDRESS", false, 's', "stop", null,
                                   "last address (0x00…0x1f, inclusive) of "+
                                   "the program");
  private static final CmdOptions.StringOptionDeclaration optFile =
    CmdOptions.createStringOption("PATH", false, 'f', "file", null,
                                  "path of file to write");
  private static final CmdOptions.StringOptionDeclaration optName =
    CmdOptions.createStringOption("NAME", false, 'n', "name", null,
                                  "program name to be added as \".program\"" +
                                  "directive");
  private static final CmdOptions.BooleanOptionDeclaration optOverWrite =
    CmdOptions.createBooleanOption(false, 'o', "overwrite", false,
                                   "overwrite if file already exists");
  private static final CmdOptions.BooleanOptionDeclaration optRelocatable =
    CmdOptions.createBooleanOption(false, 'r', "relocatable", true,
                                   "true, if the PIO program may be loaded " +
                                   "anywhere into instruction memory");

  private final SDK sdk;

  public Save(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optStart, optStop, optFile, optName,
              optOverWrite, optRelocatable });
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
      final Integer optPioValue = options.getValue(optPio);
      if (optPioValue != null) {
        final int pioNum = optPioValue;
        if ((pioNum < 0) || (pioNum > Constants.PIO_NUM - 1)) {
          throw new CmdOptions.
            ParseException("PIO number must be either 0 or 1, if defined");
        }
      }
      if (!options.isDefined(optStart)) {
        throw new CmdOptions.
          ParseException("option not specified: " + optStart);
      }
      if (!options.isDefined(optStop)) {
        throw new CmdOptions.
          ParseException("option not specified: " + optStop);
      }
      if (!options.isDefined(optFile)) {
        throw new CmdOptions.
          ParseException("option not specified: " + optFile);
      }
    }
  }

  private void writeProgram(final int pioNum,
                            final int startAddress, final int stopAddress,
                            final PrintWriter out, final String name,
                            final boolean relocatable)
    throws IOException
  {
    out.printf("# ; PIO Program Hexdump%n");
    out.printf("# ; automatically created on %s%n", Instant.now());
    out.printf("# ; by Monitor Control Program%n");
    out.printf("# ; %s%n", Constants.getEmulatorIdAndVersionWithOs());
    if (name != null) {
      // TODO: Escape program name ("\r", "\n", "\"", …)?
      out.printf("# .program %s%n", name);
    }
    if (!relocatable) {
      out.printf("# .origin %d%n", startAddress);
    }
    int address = startAddress;
    do {
      final int instrAddress =
        PIOEmuRegisters.getMemoryAddress(pioNum, address);
      final int opCode = sdk.readAddress(instrAddress) & 0xffff;
      out.printf("%04x%n", opCode);
      address = (address + 1) & (Constants.MEMORY_SIZE - 1);
    } while (address != stopAddress);
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final int pioNum = options.getValue(optPio);
    final int startAddress =
      options.getValue(optStart) & (Constants.MEMORY_SIZE - 1);
    final int stopAddress =
      (options.getValue(optStop) + 1) & (Constants.MEMORY_SIZE - 1);
    final String filePath = options.getValue(optFile);
    final String name = options.getValue(optName);
    final boolean overWrite = options.getValue(optOverWrite);
    final boolean relocatable = options.getValue(optRelocatable);
    final File file = new File(filePath);
    if (file.exists() & !overWrite) {
      console.println("file already exists: " + filePath);
      return false;
    }
    try {
      final PrintWriter out = new PrintWriter(filePath);
      writeProgram(pioNum, startAddress, stopAddress, out, name, relocatable);
      out.close();
      console.printf("(pio%d:sm*) saved 0x%02x instruction words to file %s%n",
                     pioNum, (stopAddress - startAddress) & 0x1f, filePath);
      return true;
    } catch (final IOException e) {
      console.println("failed saving to file: " + e.getMessage());
      return false;
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
