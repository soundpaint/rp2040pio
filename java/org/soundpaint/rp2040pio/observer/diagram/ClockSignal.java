/*
 * @(#)ClockSignal.java 1.00 21/02/12
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

import java.awt.Graphics2D;
import java.awt.geom.Line2D;

public class ClockSignal extends AbstractSignal<Void>
{
  private static final double SIGNAL_HEIGHT = 16.0;

  public ClockSignal()
  {
    this("clock");
  }

  public ClockSignal(final String label)
  {
    super(new SignalRendering.SignalParams(label));
  }

  @Override
  public void record()
  {
    record(null, false);
  }

  @Override
  protected double getSignalHeight() { return SIGNAL_HEIGHT; }

  @Override
  public double getDisplayHeight()
  {
    return SIGNAL_HEIGHT + 16.0;
  }

  private void drawUpArrow(final Graphics2D g, final double x, final double y)
  {
    final double arrowWidth = 0.3 * SIGNAL_HEIGHT;
    final double arrowHeight = 0.3 * SIGNAL_HEIGHT;
    g.draw(new Line2D.Double(x, y, x - 0.5 * arrowWidth, y + arrowHeight));
    g.draw(new Line2D.Double(x, y, x + 0.5 * arrowWidth, y + arrowHeight));
  }

  @Override
  public void paintCycle(final Graphics2D g, final double zoom,
                         final double xStart, final double yBottom,
                         final int cycle,
                         final boolean firstCycle, final boolean lastCycle)
  {
    if (!next(cycle - 1)) return;
    final double xFallingEdge = xStart + 0.5 * zoom;
    final double xStop = xStart + zoom;
    final double yTop = yBottom - SIGNAL_HEIGHT;
    drawUpArrow(g, xStart, yTop);
    g.draw(new Line2D.Double(xStart, yBottom, xStart, yTop));
    g.draw(new Line2D.Double(xStart, yTop, xFallingEdge, yTop));
    g.draw(new Line2D.Double(xFallingEdge, yTop, xFallingEdge, yBottom));
    g.draw(new Line2D.Double(xFallingEdge, yBottom, xStop, yBottom));
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
