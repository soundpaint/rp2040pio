/*
 * @(#)TelemetryPanel.java 1.00 21/06/27
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
package org.soundpaint.rp2040pio.observer.diagram;

import java.util.Objects;
import java.util.function.DoubleSupplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.soundpaint.rp2040pio.SwingUtils;

public class TelemetryPanel extends JPanel
{
  private static final long serialVersionUID = -2133401042831474028L;

  private final DiagramModel model;
  private final DoubleSupplier leftMostVisibleCycleGetter;
  private final JLabel lbCycles;
  private final JLabel lbPosition;

  private TelemetryPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public TelemetryPanel(final DiagramModel model,
                        final DoubleSupplier leftMostVisibleCycleGetter)
  {
    Objects.requireNonNull(model);
    this.model = model;
    Objects.requireNonNull(leftMostVisibleCycleGetter);
    this.leftMostVisibleCycleGetter = leftMostVisibleCycleGetter;
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    add(new JLabel("Cycles Recorded:"));
    add(Box.createHorizontalStrut(5));
    lbCycles = new JLabel();
    add(lbCycles);
    add(Box.createHorizontalStrut(15));
    add(new JLabel("Position:"));
    add(Box.createHorizontalStrut(5));
    lbPosition = new JLabel();
    add(lbPosition);
    add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(this);
  }

  public void modelChanged()
  {
    lbCycles.setText(String.format("%d", model.getSignalSize()));
    final double leftMostVisibleCycle =
      leftMostVisibleCycleGetter.getAsDouble();
    lbPosition.setText(String.format("%d", (int)leftMostVisibleCycle));
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
