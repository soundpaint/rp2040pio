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
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PinState;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

public class PIOGPIOArrayPanel extends JPanel
{
  private static final long serialVersionUID = 7074300168406892457L;

  private final PrintStream console;
  private final SDK sdk;
  private final PIOGPIOPanel[] panels;
  private int pioNum;

  private PIOGPIOArrayPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public PIOGPIOArrayPanel(final PrintStream console, final SDK sdk)
    throws IOException
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    this.console = console;
    this.sdk = sdk;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

    final Box pioSelection = new Box(BoxLayout.LINE_AXIS);
    add(pioSelection);
    final JLabel lbPio = new JLabel("PIO");
    pioSelection.add(lbPio);
    pioSelection.add(Box.createHorizontalStrut(15));
    addPioButtons(pioSelection);
    pioSelection.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(pioSelection);

    final Box box = new Box(BoxLayout.LINE_AXIS);
    add(box);
    box.add(Box.createHorizontalStrut(15));
    panels = new PIOGPIOPanel[Constants.GPIO_NUM];
    for (int gpioNum = 0; gpioNum < Constants.GPIO_NUM; gpioNum++) {
      final PIOGPIOPanel panel = new PIOGPIOPanel(console, gpioNum);
      panels[gpioNum] = panel;
      box.add(panel);
      box.add(Box.createHorizontalStrut((gpioNum & 0x7) == 0x7 ? 15 : 5));
    }
    box.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(box);
    add(Box.createVerticalStrut(5));
    add(Box.createVerticalGlue());
  }

  private void addPioButtons(final Box pioSelection)
  {
    final ButtonGroup bgPio = new ButtonGroup();
    for (int pioNum = 0; pioNum < Constants.PIO_NUM; pioNum++) {
      if (pioNum != 0) pioSelection.add(Box.createHorizontalStrut(10));
      final JRadioButton rbPio = new JRadioButton(String.valueOf(pioNum));
      rbPio.setSelected(pioNum == 0);
      final int finalPioNum = pioNum;
      rbPio.addActionListener((event) -> pioChanged(finalPioNum));
      bgPio.add(rbPio);
      pioSelection.add(rbPio);
    }
  }

  public void updateStatus() throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    final PinState[] pinStates = pioSdk.getPinStates();
    for (int gpioNum = 0; gpioNum < Constants.GPIO_NUM; gpioNum++) {
      final PinState pinState = pinStates[gpioNum];
      final PIOGPIOPanel panel = panels[gpioNum];
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

  private void pioChanged(final int pioNum)
  {
    this.pioNum = pioNum;
    checkedUpdate();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
