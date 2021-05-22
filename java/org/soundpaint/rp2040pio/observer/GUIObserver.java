/*
 * @(#)GUIObserver.java 1.00 21/04/01
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.RegisterClient;
import org.soundpaint.rp2040pio.Registers;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Common abstract super class for all Swing-GUI based observer
 * implementations.
 */
public abstract class GUIObserver extends JFrame
{
  private static final long serialVersionUID = 3771005056052179959L;

  private static final String DEFAULT_APP_FULL_NAME =
    "Emulation Observer Version 0.1";
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
    CmdOptions.createIntegerOption("TIME", false, 'r', "refresh", 1000,
                                   "autorefresh after <TIME> millis or " +
                                   "no autorefresh, if 0");
  private static final List<CmdOptions.OptionDeclaration<?>>
    optionDeclarations =
    Arrays.asList(new CmdOptions.OptionDeclaration<?>[]
                  { optVersion, optHelp, optPort, optRefresh });

  private final String appTitle;
  private final String appFullName;
  private final PrintStream console;
  private final CmdOptions options;
  private final SDK sdk;

  private GUIObserver()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public GUIObserver(final String appTitle, final String appFullName,
                     final PrintStream console, final String[] argv)
    throws IOException
  {
    super(appTitle);
    if (appTitle == null) {
      throw new NullPointerException("appTitle");
    }
    if (console == null) {
      throw new NullPointerException("console");
    }
    if (argv == null) {
      throw new NullPointerException("argv");
    }
    this.appTitle = appTitle;
    this.appFullName =
      appFullName != null ? appFullName : DEFAULT_APP_FULL_NAME;
    this.console = console;
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    add(new ActionPanel(this), BorderLayout.SOUTH);
    setJMenuBar(new MenuBar(this, console));
    options = parseArgs(argv);
    printAbout();
    sdk = new SDK(console, createRegisters("GUI event thread"));
  }

  public String getAppTitle()
  {
    return appTitle;
  }

  public String getAppFullName()
  {
    return appFullName;
  }

  protected SDK getSDK()
  {
    return sdk;
  }

  protected int getPort()
  {
    return options.getValue(optPort);
  }

  private CmdOptions parseArgs(final String argv[])
  {
    final CmdOptions options;
    try {
      options = new CmdOptions(appTitle, appFullName, null, optionDeclarations);
      options.parse(argv);
      checkValidity0(options);
    } catch (final CmdOptions.ParseException e) {
      console.printf("parsing command line args failed: %s%n", e.getMessage());
      System.exit(-1);
      throw new InternalError();
    }
    if (options.getValue(optVersion) == CmdOptions.Flag.ON) {
      console.println(appFullName);
      console.println(Constants.getEmulatorAbout());
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

  /**
   * Override this method to add additional checks for command-line
   * option values.  If an error is spotted, the code should throw a
   * &lt;code&gt;CmdOptions.ParseException&lt;/code&gt; with detailed
   * error reason as message text.  The default implementation does
   * nothing.
   */
  public void checkValidity(final CmdOptions options)
    throws CmdOptions.ParseException
  {
    // empty default implementation
  }

  private void checkValidity0(final CmdOptions options)
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
    checkValidity(options);
  }

  private void printAbout()
  {
    console.println(appFullName);
    console.println(Constants.getEmulatorAbout());
  }

  private Registers createRegisters(final String threadName)
  {
    final int port = getPort();
    try {
      console.printf("%s: connecting to emulation server at port %d…%n",
                     threadName, port);
      return new RegisterClient(console, null, port);
    } catch (final IOException e) {
      console.println("failed to connect to emulation server: " +
                      e.getMessage());
      console.println("check that emulation server runs at port address " +
                      port);
      System.exit(-1);
      throw new InternalError();
    }
  }

  /**
   * Convenience method.  Call this method, if the application is to
   * be closed in response to user action, e.g. when clicking on a
   * "Close" button for closing the application.
   */
  public void close()
  {
    final WindowEvent closeEvent =
      new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
    dispatchEvent(closeEvent);
  }

  protected void startUpdating()
  {
    new Thread(() -> updateLoop()).start();
  }

  /**
   * This method is regularly called.  The observer implementation
   * should update its view.
   */
  protected abstract void updateView();

  private void updateLoop()
  {
    final Registers registers = createRegisters("update loop thread");
    final int addressPhase0 =
      PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.
                                  MASTERCLK_TRIGGER_PHASE0);
    final int addressPhase1 =
      PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.
                                  MASTERCLK_TRIGGER_PHASE1);
    final int expectedValue = 0x1; // update upon stable cycle phase 1
    final int mask = 0xffffffff;
    final int cyclesTimeout = 0;
    final int refresh = options.getValue(optRefresh);
    final int millisTimeout1 = refresh / 2;
    final int millisTimeout2 = refresh - millisTimeout1;
    while (true) {
      try {
        while (true) {
          registers.wait(addressPhase1, expectedValue, mask,
                         cyclesTimeout, millisTimeout1);
          updateView();
          registers.wait(addressPhase0, expectedValue, mask,
                         cyclesTimeout, millisTimeout2);
        }
      } catch (final IOException e) {
        console.printf("update loop: %s%n", e.getMessage());
        try {
          Thread.sleep(1000); // limit CPU load in case of persisting
                              // error
        } catch (final InterruptedException e2) {
          // ignore
        }
      }
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
