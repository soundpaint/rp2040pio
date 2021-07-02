/*
 * @(#)GPIOArrayPanel.java 1.00 21/04/10
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
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PinState;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.GPIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

public class GPIOArrayPanel extends JPanel
{
  private static final long serialVersionUID = -2035403823264488596L;

  private final PrintStream console;
  private final SDK sdk;
  private final GPIOPanel[] panels;
  private GPIOSDK.Override override;

  private GPIOArrayPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public GPIOArrayPanel(final PrintStream console, final SDK sdk)
    throws IOException
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    this.console = console;
    this.sdk = sdk;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

    final Box overrideSelection = new Box(BoxLayout.LINE_AXIS);
    add(overrideSelection);
    final JLabel lbOverride = new JLabel("Override");
    overrideSelection.add(lbOverride);
    overrideSelection.add(Box.createHorizontalStrut(15));
    addOverrideButtons(overrideSelection);
    overrideSelection.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(overrideSelection);

    final Box box = new Box(BoxLayout.LINE_AXIS);
    add(box);
    box.add(Box.createHorizontalStrut(15));
    panels = new GPIOPanel[Constants.GPIO_NUM];
    for (int gpioNum = 0; gpioNum < Constants.GPIO_NUM; gpioNum++) {
      final GPIOPanel panel = new GPIOPanel(console, sdk, gpioNum);
      panels[gpioNum] = panel;
      box.add(panel);
      box.add(Box.createHorizontalStrut((gpioNum & 0x7) == 0x7 ? 15 : 5));
    }
    box.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(box);
    add(Box.createVerticalStrut(5));
    add(Box.createVerticalGlue());
  }

  private void addOverrideButtons(final Box overrideSelection)
  {
    final ButtonGroup bgOverride = new ButtonGroup();
    final JRadioButton rbBefore = new JRadioButton("Before");
    rbBefore.addActionListener((event) -> overrideChanged(GPIOSDK.Override.BEFORE));
    bgOverride.add(rbBefore);
    overrideSelection.add(rbBefore);
    overrideSelection.add(Box.createHorizontalStrut(10));
    final JRadioButton rbAfter = new JRadioButton("After");
    rbAfter.setSelected(true);
    rbAfter.addActionListener((event) -> overrideChanged(GPIOSDK.Override.AFTER));
    bgOverride.add(rbAfter);
    overrideSelection.add(rbAfter);
    override = GPIOSDK.Override.AFTER;
  }

  public void updateStatus() throws IOException
  {
    final GPIOSDK gpioSdk = sdk.getGPIOSDK();
    final PinState[] pinStates = gpioSdk.getPinStates(override);
    for (int gpioNum = 0; gpioNum < Constants.GPIO_NUM; gpioNum++) {
      final PinState pinState = pinStates[gpioNum];
      final GPIOPanel panel = panels[gpioNum];
      panel.updateStatus(pinState.getDirection(), pinState.getLevel());
    }
  }

  public void checkedUpdate()
  {
    try {
      updateStatus();
    } catch (final IOException e) {
      for (int gpioNum = 0; gpioNum < Constants.GPIO_NUM; gpioNum++) {
        panels[gpioNum].markAsUnknown();
      }
    }
  }

  private void overrideChanged(final GPIOSDK.Override override)
  {
    this.override = override;
    checkedUpdate();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
