/*
 * @(#)CycleRuler.java 1.00 21/06/28
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

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.List;

public class CycleRuler extends AbstractSignal<Void> implements Constants
{
  private static final int PADDING = 1;

  public CycleRuler()
  {
    this("cycle count");
  }

  public CycleRuler(final String label)
  {
    super(label);
  }

  @Override
  public void record()
  {
    record(null, false);
  }

  @Override
  public void paintCycle(final List<ToolTip> toolTips,
                         final Graphics2D g, final double zoom,
                         final double xStart, final double yBottom,
                         final int cycle,
                         final boolean firstCycle, final boolean lastCycle)
  {
    if (!next(cycle - 1)) return;
    final double tickYBottom = yBottom;
    final double tickYTop = yBottom - 0.3 * BIT_SIGNAL_HEIGHT;
    g.draw(new Line2D.Double(xStart, tickYBottom, xStart, tickYTop));
    if ((cycle % 5) == 0) {
      final double labelYBottom = yBottom - 0.5 * BIT_SIGNAL_HEIGHT;
      final String label = String.format("%d", cycle);
      g.setFont(DEFAULT_FONT);
      final FontMetrics fm = g.getFontMetrics(g.getFont());
      final int width = fm.stringWidth(label) - PADDING;
      g.drawString(label, (float)(xStart - 0.5 * width), (float)labelYBottom);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
