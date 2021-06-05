/*
 * @(#)GPIOViewPanel.java 1.00 21/05/17
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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import org.soundpaint.rp2040pio.CollapsiblePanel;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class GPIOViewPanel extends JPanel
{
  private static final long serialVersionUID = -14152871236331492L;

  public static final ImageIcon ledInLow;
  public static final ImageIcon ledInHigh;
  public static final ImageIcon ledOutLow;
  public static final ImageIcon ledOutHigh;
  public static final ImageIcon ledUnknown;

  static {
    try {
      ledInLow = SwingUtils.createImageIcon("led-green-off16x16.png", "in: 0");
      ledInHigh = SwingUtils.createImageIcon("led-green-on16x16.png", "in: 1");
      ledOutLow = SwingUtils.createImageIcon("led-red-off16x16.png", "out: 0");
      ledOutHigh = SwingUtils.createImageIcon("led-red-on16x16.png", "out: 1");
      ledUnknown = SwingUtils.createImageIcon("led-gray16x16.png", "unknown");
    } catch (final IOException e) {
      final String message =
        String.format("failed loading icon: %s", e.getMessage());
      System.out.println(message);
      throw new InternalError(message, e);
    }
  }

  private final PrintStream console;
  private final SDK sdk;
  private final PIOGPIOArrayPanel pioGpioArrayPanel;
  private final GPIOArrayPanel gpioArrayPanel;

  private GPIOViewPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public GPIOViewPanel(final PrintStream console, final SDK sdk)
    throws IOException
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    this.console = console;
    this.sdk = sdk;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    pioGpioArrayPanel = new PIOGPIOArrayPanel(console, sdk);
    SwingUtils.setPreferredHeightAsMaximum(pioGpioArrayPanel);
    add(new CollapsiblePanel(pioGpioArrayPanel, "PIO View of GPIO Pins", true));

    gpioArrayPanel = new GPIOArrayPanel(console, sdk);
    SwingUtils.setPreferredHeightAsMaximum(gpioArrayPanel);
    add(new CollapsiblePanel(gpioArrayPanel, "GPIO View of GPIO Pins", true));

    add(Box.createVerticalGlue());
  }

  public void updateView()
  {
    pioGpioArrayPanel.checkedUpdate();
    gpioArrayPanel.checkedUpdate();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
