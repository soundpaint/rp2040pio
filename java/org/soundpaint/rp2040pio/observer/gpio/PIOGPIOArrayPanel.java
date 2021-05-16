/*
 * @(#)PIOGPIOArrayPanel.java 1.00 21/05/16
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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PinState;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.PIOSDK;

public class PIOGPIOArrayPanel extends JPanel
{
  private static final long serialVersionUID = 961030586932800213L;

  private final PrintStream console;
  private final PIOSDK pioSdk;
  private final int refresh;
  private final ImageIcon ledGreenOff;
  private final ImageIcon ledGreenOn;
  private final ImageIcon ledRedOff;
  private final ImageIcon ledRedOn;
  private final PIOGPIOPanel[] panels;

  private PIOGPIOArrayPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public PIOGPIOArrayPanel(final PrintStream console, final PIOSDK pioSdk,
                           final int refresh)
    throws IOException
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(pioSdk);
    this.console = console;
    this.pioSdk = pioSdk;
    this.refresh = refresh;
    ledGreenOff = SwingUtils.createImageIcon("led-green-off16x16.png", "off");
    ledGreenOn = SwingUtils.createImageIcon("led-green-on16x16.png", "on");
    ledRedOff = SwingUtils.createImageIcon("led-red-off16x16.png", "off");
    ledRedOn = SwingUtils.createImageIcon("led-red-on16x16.png", "on");
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    final String title = String.format("PIO%d GPIO Pins", pioSdk.getIndex());
    setBorder(BorderFactory.createTitledBorder(title));
    final Box box = new Box(BoxLayout.X_AXIS);
    add(box);
    box.add(Box.createHorizontalStrut(15));
    panels = new PIOGPIOPanel[Constants.GPIO_NUM];
    for (int gpioNum = 0; gpioNum < Constants.GPIO_NUM; gpioNum++) {
      final PIOGPIOPanel panel =
        new PIOGPIOPanel(console, refresh, gpioNum,
                         ledGreenOff, ledGreenOn, ledRedOff, ledRedOn);
      panels[gpioNum] = panel;
      box.add(panel);
      box.add(Box.createHorizontalStrut((gpioNum & 0x7) == 0x7 ? 15 : 5));
    }
    box.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(box);
    add(Box.createVerticalGlue());
    new Thread(() -> updateStatus()).start();
  }

  public void updateStatus()
  {
    while (true) {
      try {
        while (true) {
          final PinState[] pinStates = pioSdk.getPinStates();
          for (int gpioNum = 0; gpioNum < Constants.GPIO_NUM; gpioNum++) {
            final PinState pinState = pinStates[gpioNum];
            final PIOGPIOPanel panel = panels[gpioNum];
            panel.updateStatus(pinState.getDirection(), pinState.getLevel());
          }
          try {
            Thread.sleep(refresh);
          } catch (final InterruptedException e) {
            // ignore
          }
        }
      } catch (final IOException e) {
        console.println(e.getMessage());
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
