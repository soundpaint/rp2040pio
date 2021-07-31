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
import java.awt.Color;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.RemoteAddressSpaceClient;
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

  static
  {
    UIManager.put("OptionPane.okButtonMnemonic", "79"); // 'O' as mnemonic
    UIManager.put("OptionPane.cancelButtonMnemonic", "67"); // 'C' as mnemonic
  }

  private final String appTitle;
  private final String appFullName;
  private final PrintStream console;
  private final ConnectDialog connectDialog;
  private final JLabel lbStatus;
  private final CmdOptions options;
  private final SDK sdk;
  private final RemoteAddressSpaceClient sdkClient;
  private final RemoteAddressSpaceClient updateLoopClient;
  private final ActionPanel<? extends GUIObserver> actionPanel;

  private GUIObserver()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public GUIObserver(final String appTitle, final String appFullName,
                     final PrintStream console, final String[] argv)
    throws IOException
  {
    super(appTitle);
    Objects.requireNonNull(appTitle);
    Objects.requireNonNull(console);
    Objects.requireNonNull(argv);
    this.appTitle = appTitle;
    this.appFullName =
      appFullName != null ? appFullName : DEFAULT_APP_FULL_NAME;
    this.console = console;
    options = parseArgs(argv);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    connectDialog = new ConnectDialog(this, getPort());
    lbStatus = new JLabel();
    sdkClient = createRemoteAddressSpace("GUI event thread");
    sdk = new SDK(console, sdkClient);
    updateLoopClient = createRemoteAddressSpace("update loop thread");
    connect(null, getPort());
    add(actionPanel = createActionPanel(), BorderLayout.NORTH);
    add(createStatusLine(), BorderLayout.SOUTH);
    setJMenuBar(createMenuBar());
    printAbout();
  }

  private Box createStatusLine()
  {
    final Box hBox = new Box(BoxLayout.LINE_AXIS);
    hBox.add(lbStatus);
    hBox.add(Box.createHorizontalGlue());
    return hBox;
  }

  protected void setStatus(final String status)
  {
    setStatus(status, false);
  }

  protected void setStatus(final String status, final boolean alert)
  {
    lbStatus.setText(status);
    lbStatus.setForeground(alert ? Color.RED : Color.BLACK);
  }

  /**
   * Override this method to provide a custom action panel.  The
   * default implementation of this method uses the default
   * ActionPanel implementation in the java package of this class.
   */
  protected ActionPanel<? extends GUIObserver> createActionPanel()
  {
    return new ActionPanel<GUIObserver>(this);
  }

  /**
   * Override this method to provide a custom menu bar.  The default
   * implementation of this method uses the default MenuBar
   * implementation in the java package of this class.
   */
  protected MenuBar<? extends GUIObserver> createMenuBar()
  {
    return new MenuBar<GUIObserver>(this);
  }

  public String getAppTitle()
  {
    return appTitle;
  }

  public String getAppFullName()
  {
    return appFullName;
  }

  public PrintStream getConsole()
  {
    return console;
  }

  protected SDK getSDK()
  {
    return sdk;
  }

  protected ActionPanel<? extends GUIObserver> getActionPanel()
  {
    return actionPanel;
  }

  protected int getPort()
  {
    return options.getValue(optPort);
  }

  /**
   * Override this method to add additional option declarations.  The
   * default implementation returns &lt;code&gt;null&lt;/code&gt;.
   */
  protected List<CmdOptions.OptionDeclaration<?>>
    getAdditionalOptionDeclarations()
  {
    return null;
  }

  private List<CmdOptions.OptionDeclaration<?>> collectOptionDeclarations()
  {
    final List<CmdOptions.OptionDeclaration<?>> additionalOptionDeclarations =
      getAdditionalOptionDeclarations();
    if (additionalOptionDeclarations != null) {
      optionDeclarations.addAll(additionalOptionDeclarations);
    }
    return optionDeclarations;
  }

  private CmdOptions parseArgs(final String argv[])
  {
    final CmdOptions options;
    try {
      options = new CmdOptions(appTitle, appFullName, null,
                               collectOptionDeclarations());
      options.parse(argv);
      checkValidity0(options);
    } catch (final CmdOptions.ParseException e) {
      console.printf("parsing command line args failed: %s%n", e.getMessage());
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

  /**
   * Override this method to add additional checks for command-line
   * option values.  If an error is spotted, the code should throw a
   * &lt;code&gt;CmdOptions.ParseException&lt;/code&gt; with detailed
   * error reason as message text.  The default implementation is
   * empty.
   */
  protected void checkValidity(final CmdOptions options)
    throws CmdOptions.ParseException
  {
  }

  private void checkValidity0(final CmdOptions options)
    throws CmdOptions.ParseException
  {
    final int port = options.getValue(optPort);
    if ((port <= 0) || (port > 65535)) {
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
    console.printf("%s%n%s for%n%s%n%s",
                   appTitle, appFullName,
                   Constants.getEmulatorIdAndVersionWithOs(),
                   Constants.getGuiCopyrightNotice());
  }

  private RemoteAddressSpaceClient
    createRemoteAddressSpace(final String threadName)
  {
    final int port = getPort();
    try {
      console.printf("%s: connecting to emulation server at port %d…%n",
                     threadName, port);
      return new RemoteAddressSpaceClient(console);
    } catch (final IOException e) {
      console.println("failed to connect to emulation server: " +
                      e.getMessage());
      console.println("check that emulation server runs at port address " +
                      port);
      System.exit(-1);
      throw new InternalError();
    }
  }

  public void openConnectDialog()
  {
    connectDialog.makeVisible();
  }

  public void connect() throws IOException
  {
    connect(sdkClient.getPort());
  }

  public void connect(final int port) throws IOException
  {
    connect(sdkClient.getHost(), port);
  }

  public void connect(final String host, final int port) throws IOException
  {
    sdkClient.connect(host, port);
    updateLoopClient.connect(host, port);
    final String status =
      String.format("Connected to emulation server at port %d.", port);
    setStatus(status);
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
   * should check if the RP2040 Emulator's data, that it displays,
   * has changed, and if so, properly update its view.
   */
  protected abstract void updateView();

  private void updateLoop()
  {
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
    final int millisTimeoutPhase0 = refresh / 2;
    final int millisTimeoutPhase1 = refresh - millisTimeoutPhase0;
    while (true) {
      try {
        while (true) {
          updateLoopClient.waitAddress(addressPhase1, expectedValue, mask,
                                       cyclesTimeout, millisTimeoutPhase1);
          updateView();
          SwingUtilities.invokeLater(() -> repaint());
          updateLoopClient.waitAddress(addressPhase0, expectedValue, mask,
                                       cyclesTimeout, millisTimeoutPhase0);
        }
      } catch (final IOException e) {
        final String message = String.format("Error: %s", e.getMessage());
        setStatus(message, true);
        console.println(message);
        boolean recovered = false;
        while (!recovered) {
          try {
            Thread.sleep(1000); // limit CPU load in case of
                                // persisting error
          } catch (final InterruptedException e2) {
            // ignore
          }
          try {
            connect();
            recovered = true;
          } catch (final IOException e3) {
            // inherited error; ignore
          }
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
