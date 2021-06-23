/*
 * @(#)Observer.java 1.00 21/04/01
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
package org.soundpaint.rp2040pio.observer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import org.soundpaint.rp2040pio.AddressSpace;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.RemoteAddressSpaceClient;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Emulation Register Status Observation (Command-Line Version)
 */
public class Observer
{
  private static final String APP_TITLE = "Observer";
  private static final String APP_FULL_NAME = "Emulation Observer Version 0.1";

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
  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", null,
                                   "address of the register to observe");
  private static final CmdOptions.IntegerOptionDeclaration optMask =
    CmdOptions.createIntegerOption("MASK", false, 'm', "mask", 0xffffffff,
                                   "bit mask to select bits to observe");
  private static final CmdOptions.IntegerOptionDeclaration optRefresh =
    CmdOptions.createIntegerOption("TIME", false, 'r', "refresh", 10000,
                                   "autorefresh after <TIME> millis or no autorefresh, if 0");
  private static final List<CmdOptions.OptionDeclaration<?>>
    optionDeclarations =
    Arrays.asList(new CmdOptions.OptionDeclaration<?>[]
                  { optVersion, optHelp, optPort, optAddress,
                    optMask, optRefresh });

  private final PrintStream console;
  private final CmdOptions options;
  private final SDK sdk;

  private Observer()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public Observer(final PrintStream console, final String[] argv)
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.console = console;
    options = parseArgs(argv);
    printAbout();
    final AddressSpace memory = connect();
    sdk = new SDK(console, memory);
  }

  private CmdOptions parseArgs(final String argv[])
  {
    final CmdOptions options;
    try {
      options = new CmdOptions(APP_TITLE, APP_FULL_NAME, null,
                               optionDeclarations);
      options.parse(argv);
      checkValidity(options);
    } catch (final CmdOptions.ParseException e) {
      console.println(e.getMessage());
      System.exit(-1);
      throw new InternalError();
    }
    if (options.getValue(optVersion) == CmdOptions.Flag.ON) {
      printAbout();
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
    if (options.getValue(optHelp) != CmdOptions.Flag.ON) {
      if (!options.isDefined(optAddress)) {
        throw new CmdOptions.
          ParseException("option not specified", optAddress);
      }
    }
    final int refresh = options.getValue(optRefresh);
    if (refresh < 0) {
      throw new CmdOptions.
        ParseException("TIME must be a non-negative value", optRefresh);
    }
  }

  private void printAbout()
  {
    console.printf("%s for%n%s%n%s%n",
                   APP_FULL_NAME,
                   Constants.getEmulatorIdAndVersionWithOs(),
                   Constants.getCmdLineCopyrightNotice());
  }

  private AddressSpace connect()
  {
    final int port = options.getValue(optPort);
    try {
      console.printf("connecting to emulation server at port %d…%n", port);
      return new RemoteAddressSpaceClient(console, null, port);
    } catch (final IOException e) {
      console.println("failed to connect to emulation server: " +
                      e.getMessage());
      console.println("check that emulation server runs at port address " +
                      port);
      System.exit(-1);
      throw new InternalError();
    }
  }

  private void run()
  {
    try {
      final int address = options.getValue(optAddress);
      final boolean validAddress = sdk.providesAddress(address);
      if (!validAddress) {
        final String message =
          String.format("unsupported address: 0x%08x", address);
        console.println(message);
        System.exit(-1);
        throw new InternalError();
      }
      final int mask = options.getValue(optMask);
      final String label = sdk.getLabelForAddress(address);
      final int refresh = options.getValue(optRefresh);
      update(address, label, sdk.readAddress(address));
      while (true) {
        update(address, label, sdk.wait(address, 0xffffffff, 0x0, 1, refresh));
        update(address, label, sdk.readAddress(address));
      }
    } catch (final IOException e) {
      console.println(e.getMessage());
      System.exit(-1);
    }
    System.exit(0);
  }

  private void update(final int address, final String label, final int value)
  {
    console.printf("wait on %s (0x%08x) returned 0x%08x%n",
                   label, address, value);
  }

  public static void main(final String argv[])
  {
    new Observer(System.out, argv).run();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
