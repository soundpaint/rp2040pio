/*
 * @(#)PIOGPIOPanel.java 1.00 21/05/16
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

import java.io.PrintStream;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.soundpaint.rp2040pio.Bit;
import org.soundpaint.rp2040pio.Direction;

public class PIOGPIOPanel extends JPanel
{
  private static final long serialVersionUID = -4710787733361262765L;

  private final PrintStream console;
  private final int gpioNum;
  private final JLabel lbStatus;

  private PIOGPIOPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public PIOGPIOPanel(final PrintStream console, final int gpioNum)
  {
    Objects.requireNonNull(console);
    this.console = console;
    this.gpioNum = gpioNum;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    add(Box.createVerticalStrut(5));
    final Box gpioNumBox = new Box(BoxLayout.LINE_AXIS);
    gpioNumBox.add(Box.createHorizontalGlue());
    gpioNumBox.add(new JLabel(String.format("%d", gpioNum)));
    gpioNumBox.add(Box.createHorizontalGlue());
    add(gpioNumBox);
    add(Box.createVerticalStrut(5));
    final Box ledBox = new Box(BoxLayout.LINE_AXIS);
    ledBox.add(Box.createHorizontalGlue());
    ledBox.add(lbStatus = new JLabel(GPIOViewPanel.ledInLow));
    ledBox.add(Box.createHorizontalGlue());
    add(ledBox);
    add(Box.createVerticalGlue());
    setMaximumSize(getPreferredSize());
  }

  public void updateStatus(final Direction direction, final Bit level)
  {
    final ImageIcon icon =
      direction == Direction.IN ?
      (level == Bit.HIGH ? GPIOViewPanel.ledInHigh : GPIOViewPanel.ledInLow) :
      (level == Bit.HIGH ? GPIOViewPanel.ledOutHigh : GPIOViewPanel.ledOutLow);
    if (icon != lbStatus.getIcon()) {
      lbStatus.setIcon(icon);
    }
  }

  public void markAsUnknown()
  {
    final ImageIcon icon = GPIOViewPanel.ledUnknown;
    if (icon != lbStatus.getIcon()) {
      lbStatus.setIcon(icon);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
