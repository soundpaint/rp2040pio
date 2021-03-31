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
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor "load" load a program from a file and stores it in the
 * PIO's memory.
 */
public class Load extends Command
{
  private static final String fullName = "load";
  private static final String singleLineDescription = "load program from file";

  private final SDK sdk;

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.StringOptionDeclaration optFile =
    CmdOptions.createStringOption("STRING", false, 'f', "file", "program.hex",
                                  "path of hex dump file to load");
  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", null,
                                   "preferred program start address");

  public Load(final PrintStream out, final SDK sdk)
  {
    super(out, fullName, singleLineDescription,
          new CmdOptions.OptionDeclaration<?>[] { optPio, optFile, optAddress });
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
    final int pio = options.getValue(optPio);
    final String resourcePath = options.getValue(optFile);
    final Integer preferredAddress = options.getValue(optAddress);
    final PIOSDK pioSdk = pio == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    final int assignedAddress =
      preferredAddress != null ?
      pioSdk.addProgramAtOffset(resourcePath, preferredAddress) :
      pioSdk.addProgram(resourcePath);
    out.printf("stored program at address 0x%02x%n", assignedAddress);
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
