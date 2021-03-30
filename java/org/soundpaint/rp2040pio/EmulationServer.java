/*
 * @(#)EmulationServer.java 1.00 21/03/27
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
package org.soundpaint.rp2040pio;

import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.sdk.LocalRegisters;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

public class EmulationServer
{
  private static class Flags {
    private static final String PRG_NAME = "EmulationServer";
    private static final String PRG_ID_AND_VERSION =
      "Emulation Server Version 0.1 for " + Constants.getProgramAndVersion();

    private static final CmdOptions.FlagOptionDeclaration optVersion =
      CmdOptions.createFlagOption(false, 'V', "version", CmdOptions.Flag.OFF,
                                  "display version information and exit");
    private static final CmdOptions.FlagOptionDeclaration optHelp =
      CmdOptions.createFlagOption(false, 'h', "help", CmdOptions.Flag.OFF,
                                  "display this help text and exit");
    private static final CmdOptions.FlagOptionDeclaration optSilent =
      CmdOptions.createFlagOption(false, 's', "silent", CmdOptions.Flag.OFF,
                                  "print no information at all");
    private static final CmdOptions.FlagOptionDeclaration optVerbose =
      CmdOptions.createFlagOption(false, 'v', "verbose", CmdOptions.Flag.OFF,
                                  "print verbose information");
    private static final CmdOptions.IntegerOptionDeclaration optPort =
      CmdOptions.createIntegerOption("PORT", false, 'p', "port", 1088,
                                     "use PORT as server port number");

    private final PrintStream console;
    private final CmdOptions options;

    private Flags()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private Flags(final PrintStream console, final String argv[])
      throws CmdOptions.ParseException
    {
      this.console = console;
      options = new CmdOptions(PRG_NAME, PRG_ID_AND_VERSION,
                               optVersion, optHelp, optSilent,
                               optVerbose, optPort);
      options.parse(argv);
      checkValidity();
      if (options.getValue(optVersion) == CmdOptions.Flag.ON) {
        console.println(PRG_ID_AND_VERSION);
        System.exit(0);
      }
      if (options.getValue(optHelp) == CmdOptions.Flag.ON) {
        console.println(options.getFullInfo());
        System.exit(0);
      }
    }

    private void checkValidity() throws CmdOptions.ParseException
    {
      final Integer portNumber = options.getValue(optPort);
      if (portNumber != null) {
        if ((portNumber < 0) || (portNumber > 65535)) {
          throw new CmdOptions.
            ParseException("PORT must be in the range 0..65535");
        }
      }
      if ((options.getValue(optSilent) == CmdOptions.Flag.ON) &&
          (options.getValue(optVerbose) == CmdOptions.Flag.ON)) {
        throw new CmdOptions.
          ParseException("either 'silent' or 'verbose' can be activated");
      }
    }
  }

  private static void startServer(final PrintStream console, final Flags flags)
    throws IOException
  {
    final Emulator emulator = new Emulator(console);
    final LocalRegisters registers = new LocalRegisters(emulator);
    final SDK sdk = new SDK(console, registers);
    final Integer port = flags.options.getValue(Flags.optPort);
    final RegisterServer server =
      port != null ? new RegisterServer(sdk, port) : new RegisterServer(sdk);
    try {
      Thread.sleep(1000); // wait for server thread starting up
    } catch (final InterruptedException e) {
      throw new IOException("failed starting server: " + e.getMessage());
    }
  }

  public static void main(final String argv[])
  {
    try {
      final PrintStream console = System.out;
      final Flags flags = new Flags(console, argv);
      startServer(console, flags);
      if (flags.options.getValue(Flags.optSilent) != CmdOptions.Flag.ON) {
        final Integer port = flags.options.getValue(Flags.optPort);
        final int portNumber =
          port != null ? port : Constants.REGISTER_SERVER_DEFAULT_PORT_NUMBER;
        console.println("started emulation server at port " + portNumber);
      }
    } catch (final CmdOptions.ParseException e) {
      System.err.println("failed parsing command line arguments: " +
                         e.getMessage());
      System.exit(-1);
    } catch (final IOException e) {
      System.err.println("failed starting emulation server: " +
                         e.getMessage());
      System.exit(-1);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
