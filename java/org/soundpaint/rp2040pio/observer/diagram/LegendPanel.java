/*
 * @(#)LegendPanel.java 1.00 21/06/26
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

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import org.soundpaint.rp2040pio.sdk.SDK;

public class LegendPanel extends JComponent
{
  private static final long serialVersionUID = 6634499718877657461L;

  private static final int LEGEND_WIDTH = 200;
  private static final double LABEL_MARGIN_BOTTOM = 4.0;
  private static final double LABEL_MARGIN_RIGHT = 10.0;

  private final DiagramModel model;
  private final Dimension preferredSize;

  private LegendPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public LegendPanel(final DiagramModel model)
  {
    Objects.requireNonNull(model);
    this.model = model;
    setMinimumSize(new Dimension(LEGEND_WIDTH, 0));
    setMaximumSize(new Dimension(LEGEND_WIDTH, Integer.MAX_VALUE));
    preferredSize = new Dimension(LEGEND_WIDTH, 0);
    updatePreferredSize();
  }

  private void updatePreferredHeight()
  {
    double preferredHeight = Constants.TOP_MARGIN + Constants.BOTTOM_MARGIN;
    for (final Signal signal : model) {
      if (signal.getVisible()) {
        preferredHeight += signal.getDisplayHeight();
      }
    }
    preferredSize.setSize(preferredSize.getWidth(), (int)preferredHeight);
  }

  public void updatePreferredSize()
  {
    updatePreferredHeight();
    revalidate();
  }

  @Override
  public Dimension getPreferredSize()
  {
    return preferredSize;
  }

  private void paintLabel(final Graphics2D g,
                          final double xStart, final double yBottom,
                          final String label)
  {
    final FontMetrics fm = g.getFontMetrics(g.getFont());
    final int width = fm.stringWidth(label);
    g.drawString(label,
                 (float)(xStart - width - LABEL_MARGIN_RIGHT),
                 (float)(yBottom - LABEL_MARGIN_BOTTOM));
  }

  private void paintLabels(final Graphics2D g)
  {
    g.setFont(Constants.DEFAULT_FONT);
    double y = Constants.TOP_MARGIN;
    for (final Signal signal : model) {
      if (signal.getVisible()) {
        final String label = signal.getLabel();
        final double height = signal.getDisplayHeight();
        paintLabel(g, LEGEND_WIDTH, y += height, label);
      }
    }
  }

  @Override
  public void paintComponent(final Graphics g)
  {
    super.paintComponent(g);
    paintLabels((Graphics2D)g);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
