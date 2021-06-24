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
import java.util.Arrays;
import java.util.List;

public class EmulationServer
{
  private static final String PRG_TITLE = "EmulationServer";
  private static final String PRG_FULL_NAME = "Emulation Server Version 0.1";

  private static final CmdOptions.FlagOptionDeclaration optVersion =
    CmdOptions.createFlagOption(false, 'V', "version", CmdOptions.Flag.OFF,
                                "display version information and exit");
  private static final CmdOptions.FlagOptionDeclaration optHelp =
    CmdOptions.createFlagOption(false, 'h', "help", CmdOptions.Flag.OFF,
                                "display this help text and exit");
  private static final CmdOptions.FlagOptionDeclaration optSilent =
    CmdOptions.createFlagOption(false, 's', "silent", CmdOptions.Flag.OFF,
                                "print no info at all except fatal errors");
  private static final CmdOptions.FlagOptionDeclaration optVerbose =
    CmdOptions.createFlagOption(false, 'v', "verbose", CmdOptions.Flag.OFF,
                                "print verbose information");
  private static final CmdOptions.IntegerOptionDeclaration optPort =
    CmdOptions.createIntegerOption("PORT", false, 'p', "port",
                                   Constants.
                                   REGISTER_SERVER_DEFAULT_PORT_NUMBER,
                                   "use PORT as server port number");
  private static final List<CmdOptions.OptionDeclaration<?>>
    optionDeclarations =
    Arrays.asList(new CmdOptions.OptionDeclaration<?>[]
                  { optVersion, optHelp, optSilent, optVerbose, optPort });

  private final PrintStream console;
  private final CmdOptions options;

  private EmulationServer(final PrintStream console, final String[] argv)
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.console = console;
    options = parseArgs(argv);
    if (options.getValue(optSilent) != CmdOptions.Flag.ON) {
      printAbout();
    }
  }

  private CmdOptions parseArgs(final String argv[])
  {
    final CmdOptions options;
    try {
      options = new CmdOptions(PRG_TITLE, PRG_FULL_NAME, null,
                               optionDeclarations);
      options.parse(argv);
      checkValidity(options);
    } catch (final CmdOptions.ParseException e) {
      console.println(e.getMessage());
      System.exit(-1);
      throw new InternalError();
    }
    if (options.getValue(optVersion) == CmdOptions.Flag.ON) {
      console.println(PRG_FULL_NAME);
      console.println(Constants.getEmulatorIdAndVersionWithOs());
      System.exit(0);
      throw new InternalError();
    }
    if (options.getValue(optHelp) == CmdOptions.Flag.ON) {
      console.println(options.getFullInfo());
      System.exit(0);
      throw new InternalError();
    }
    return options;
  }

  private void checkValidity(final CmdOptions options)
    throws CmdOptions.ParseException
  {
    final int port = options.getValue(optPort);
    if ((port < 0) || (port > 65535)) {
      throw new CmdOptions.
        ParseException("PORT must be in the range 0…65535");
    }
    if ((options.getValue(optSilent) == CmdOptions.Flag.ON) &&
        (options.getValue(optVerbose) == CmdOptions.Flag.ON)) {
      throw new CmdOptions.
        ParseException("either 'silent' or 'verbose' can be activated");
    }
  }

  private void printAbout()
  {
    console.printf("%s%n%s%n%s%n",
                   "Emulation Server Daemon",
                   Constants.getEmulatorIdAndVersionWithOs(),
                   Constants.getCmdLineCopyrightNotice());
  }

  private void run()
  {
    try {
      final Emulator emulator = new Emulator(console);
      final LocalAddressSpace memory = new LocalAddressSpace(emulator);
      final int port = options.getValue(optPort);
      final RemoteAddressSpaceServer server =
        new RemoteAddressSpaceServer(console, memory, port);
      if (options.getValue(optSilent) != CmdOptions.Flag.ON) {
        console.println("started emulation server at port " + port);
      }
    } catch (final IOException e) {
      console.println("failed starting emulation server: " +
                      e.getMessage());
      System.exit(-1);
    }
  }

  public static void main(final String argv[])
  {
    new EmulationServer(System.out, argv).run();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
