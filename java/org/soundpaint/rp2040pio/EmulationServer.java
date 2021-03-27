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
    private static final String PRG_DESCRIPTION =
      "Emulation Server Version 0.1 for " + Constants.getProgramAndVersion();
    private static final CmdOptions.OptionDeclaration optVersion =
      new CmdOptions.OptionDeclaration(CmdOptions.Type.FLAG, null, false,
                                       'V', "version",
                                       CmdOptions.FlagOptionDefinition.OFF,
                                       "display version information and exit");
    private static final CmdOptions.OptionDeclaration optHelp =
      new CmdOptions.OptionDeclaration(CmdOptions.Type.FLAG, null, false,
                                       'h', "help",
                                       CmdOptions.FlagOptionDefinition.OFF,
                                       "display this help text and exit");
    private static final CmdOptions.OptionDeclaration optSilent =
      new CmdOptions.OptionDeclaration(CmdOptions.Type.FLAG, null, false,
                                       's', "silent",
                                       CmdOptions.FlagOptionDefinition.OFF,
                                       "print verbose information");
    private static final CmdOptions.OptionDeclaration optVerbose =
      new CmdOptions.OptionDeclaration(CmdOptions.Type.FLAG, null, false,
                                       'v', "verbose",
                                       CmdOptions.FlagOptionDefinition.OFF,
                                       "print verbose information");
    private static final CmdOptions.OptionDeclaration optPort =
      new CmdOptions.OptionDeclaration(CmdOptions.Type.INTEGER, "PORT", false,
                                       'p', "port",
                                       null,
                                       "use PORT as server port number");

    private static final CmdOptions.OptionDeclaration[] OPTION_DECLARATIONS =
      new CmdOptions.OptionDeclaration[] {
      optVersion, optHelp, optVerbose, optPort
    };

    private CmdOptions.FlagOptionDefinition version;
    private CmdOptions.FlagOptionDefinition help;
    private CmdOptions.FlagOptionDefinition silent;
    private CmdOptions.FlagOptionDefinition verbose;
    private CmdOptions.IntegerOptionDefinition port;

    private final static CmdOptions options;

    static {
      try {
        options = new CmdOptions(PRG_NAME, PRG_DESCRIPTION,
                                 OPTION_DECLARATIONS);
      } catch (final CmdOptions.ParseException ex) {
        throw new RuntimeException("bad option declaration in class Recorder",
                                   ex);
      }
    }

    private final PrintStream console;

    private Flags()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private Flags(final PrintStream console, final String argv[])
      throws CmdOptions.ParseException
    {
      this.console = console;
      options.parse(argv);
      version = (CmdOptions.FlagOptionDefinition)options.
        <Boolean>findDefinitionForDeclaration(optVersion);
      help = (CmdOptions.FlagOptionDefinition)options.
        <Boolean>findDefinitionForDeclaration(optHelp);
      silent = (CmdOptions.FlagOptionDefinition)options.
        <Boolean>findDefinitionForDeclaration(optSilent);
      verbose = (CmdOptions.FlagOptionDefinition)options.
        <Boolean>findDefinitionForDeclaration(optVerbose);
      port = (CmdOptions.IntegerOptionDefinition)options.
        <Integer>findDefinitionForDeclaration(optPort);
    }

    public boolean checkValidity()
    {
      final Integer portNumber = port.getValue();
      if (portNumber != null) {
        if ((portNumber < 0) || (portNumber > 65535)) {
          console.println("port number must be in the range 0..65535");
          return false;
        }
      }
      if (silent.isTrue() && verbose.isTrue()) {
        console.println("either 'silent' or 'verbose' can be activated");
      }
      return true;
    }
  }

  private static void startServer(final PrintStream console, final Flags flags)
    throws IOException
  {
    final Emulator emulator = new Emulator(console);
    final LocalRegisters registers = new LocalRegisters(emulator);
    final SDK sdk = new SDK(console, registers);
    final Integer portNumber = flags.port.getValue();
    final RegisterServer server =
      portNumber != null ?
      new RegisterServer(sdk, portNumber) :
      new RegisterServer(sdk);
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
      if ((flags.silent == null) || !flags.silent.isTrue()) {
        final Integer port = flags.port.getValue();
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
