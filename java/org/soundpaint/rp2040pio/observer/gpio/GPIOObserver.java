/*
 * @(#)GPIOObserver.java 1.00 21/04/10
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
package org.soundpaint.rp2040pio.observer.gpio;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.RegisterClient;
import org.soundpaint.rp2040pio.Registers;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Emulation GPIO Status Observation
 */
public class GPIOObserver extends JFrame
{
  private static final long serialVersionUID = -4777618004050203269L;

  private static final String PRG_NAME = "GPIO Observer";
  private static final String PRG_ID_AND_VERSION =
    "Emulation GPIO Observer Version 0.1 for " +
    Constants.getProgramAndVersion();
  private static final CmdOptions.FlagOptionDeclaration optVersion =
    CmdOptions.createFlagOption(false, 'V', "version", CmdOptions.Flag.OFF,
                                "display version information and exit");
  private static final CmdOptions.FlagOptionDeclaration optHelp =
    CmdOptions.createFlagOption(false, 'h', "help", CmdOptions.Flag.OFF,
                                "display this help text and exit");
  private static final CmdOptions.IntegerOptionDeclaration optPort =
    CmdOptions.createIntegerOption("PORT", false, 'p', "port",
                                   Constants.
                                   REGISTER_SERVER_DEFAULT_PORT_NUMBER,
                                   "use PORT as server port number");
  private static final CmdOptions.IntegerOptionDeclaration optRefresh =
    CmdOptions.createIntegerOption("TIME", false, 'r', "refresh", 100,
                                   "autorefresh after <TIME> millis or no " +
                                   "autorefresh, if 0");
  private static final List<CmdOptions.OptionDeclaration<?>>
    optionDeclarations =
    Arrays.asList(new CmdOptions.OptionDeclaration<?>[]
                  { optVersion, optHelp, optPort, optRefresh });

  private final PrintStream console;
  private final CmdOptions options;
  private final SDK sdk;

  private GPIOObserver()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public GPIOObserver(final PrintStream console, final String[] argv)
  {
    super("GPIO Observer");
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.console = console;
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    options = parseArgs(argv);
    printAbout();
    final Registers registers = connect();
    sdk = new SDK(console, registers);
  }

  private CmdOptions parseArgs(final String argv[])
  {
    final CmdOptions options;
    try {
      options = new CmdOptions(PRG_NAME, PRG_ID_AND_VERSION, null,
                               optionDeclarations);
      options.parse(argv);
      checkValidity(options);
    } catch (final CmdOptions.ParseException e) {
      console.printf("parsing command line args failed: %s%n", e.getMessage());
      System.exit(-1);
      throw new InternalError();
    }
    if (options.getValue(optVersion) == CmdOptions.Flag.ON) {
      console.println(PRG_ID_AND_VERSION);
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
        ParseException("PORT must be in the range 0…65535", optPort);
    }
    final int refresh = options.getValue(optRefresh);
    if (refresh < 0) {
      throw new CmdOptions.
        ParseException("TIME must be a non-negative value", optRefresh);
    }
  }

  private void printAbout()
  {
    console.println("GPIO Observer App");
    console.println(Constants.getAbout());
  }

  private Registers connect()
  {
    final int port = options.getValue(optPort);
    try {
      console.printf("connecting to emulation server at port %d…%n", port);
      return new RegisterClient(console, port);
    } catch (final IOException e) {
      console.println("failed to connect to emulation server: " +
                      e.getMessage());
      console.println("check that emulation server runs at port address " +
                      port);
      System.exit(-1);
      throw new InternalError();
    }
  }

  public void close()
  {
    final WindowEvent closeEvent =
      new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
    dispatchEvent(closeEvent);
  }

  private int run()
  {
    final int refresh = options.getValue(optRefresh);
    try {
      getContentPane().add(new GPIOArrayPanel(console, sdk, refresh));
      getContentPane().add(new ActionPanel(this), BorderLayout.SOUTH);
      pack();
      setVisible(true);
      return 0;
    } catch (final IOException e) {
      console.printf("initialization failed: %s%n", e.getMessage());
      return -1;
    }
  }

  public static void main(final String argv[])
  {
    new GPIOObserver(System.out, argv).run();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
