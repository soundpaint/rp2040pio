/*
 * @(#)ValuedSignal.java 1.00 21/02/12
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
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Supplier;

public class ValuedSignal<T> extends AbstractSignal<T> implements Constants
{
  private Supplier<T> valueGetter;
  private Supplier<Boolean> changeInfoGetter;

  public ValuedSignal(final String label,
                      final Supplier<T> valueGetter)
  {
    this(label, valueGetter, null);
  }

  /**
   * @param changeInfoGetter If set to &lt;code&gt;null&lt;/code&gt;,
   * then a change is assumed only when the updated value changes.
   */
  public ValuedSignal(final String label,
                      final Supplier<T> valueGetter,
                      final Supplier<Boolean> changeInfoGetter)
  {
    super(label);
    if (valueGetter == null) {
      throw new NullPointerException("valueGetter");
    }
    this.valueGetter = valueGetter;
    this.changeInfoGetter = changeInfoGetter;
  }

  @Override
  public void record()
  {
    final boolean enforceChanged =
      changeInfoGetter != null ? changeInfoGetter.get() : false;
    record(valueGetter.get(), enforceChanged);
  }

  private static void addToolTip(final List<ToolTip> toolTips,
                                 final int x0, final int y0,
                                 final int x1, final int y1,
                                 final String text)
  {
    if (text != null) {
      toolTips.add(new ToolTip(x0, y0, x1, y1, text));
    }
  }

  @Override
  public void createToolTip(final List<ToolTip> toolTips,
                            final int cycle,
                            final boolean isFirstCycle,
                            final boolean isLastCycle,
                            final double zoom,
                            final double xStart,
                            final double yBottom)
  {
    final String previousToolTipText = getToolTipText(cycle - 1);
    final int previousCycles = getNotChangedSince(cycle - 1) + 1;
    addToolTip(toolTips,
               (int)(xStart - previousCycles * zoom),
               (int)(yBottom - VALUED_SIGNAL_HEIGHT),
               (int)xStart - 1, (int)yBottom,
               previousToolTipText);
    if (isLastCycle) {
      // print label as preview for not yet finished value
      final String toolTipText = getToolTipText(cycle);
      final int cycles = getNotChangedSince(cycle) - 1;
      addToolTip(toolTips,
                 (int)(xStart - cycles * zoom),
                 (int)(yBottom - VALUED_SIGNAL_HEIGHT),
                 (int)xStart - 1, (int)yBottom,
                 toolTipText);
    }
  }

  private static void paintValuedLabel(final List<ToolTip> toolTips,
                                       final Graphics2D g,
                                       final double zoom,
                                       final double xStart,
                                       final double yBottom,
                                       final String label,
                                       final String toolTipText,
                                       final int cycles)
  {
    if (label != null) {
      g.setFont(VALUE_FONT);
      final FontMetrics fm = g.getFontMetrics(g.getFont());
      final int width = fm.stringWidth(label);
      final double xLabelStart =
        xStart - 0.5 * (cycles * zoom - SIGNAL_SETUP_X + width);

      final double yTextBottom = yBottom - VALUE_LABEL_MARGIN_BOTTOM;
      g.drawString(label, (float)xLabelStart, (float)yTextBottom);
    }
  }

  @Override
  public void paintCycle(final List<ToolTip> toolTips,
                         final Graphics2D g, final double zoom,
                         final double xStart, final double yBottom,
                         final int cycle,
                         final boolean firstCycle, final boolean lastCycle)
  {
    // Draw previous value only if finished, since current value may
    // be still ongoing such that centered display of text is not yet
    // reached.  However, if this is the last cycle for that a value
    // has been recorded, then draw it anyway, since we can not forsee
    // the future signal and thus print the current state.
    if (!next(cycle) && !lastCycle) return;

    if (changed(cycle) && !firstCycle) {
      // signal changed => print label of previous, now finished
      // value; but exclude first cycle, as it will be handled on next
      // turn
      paintValuedLabel(toolTips, g, zoom, xStart, yBottom,
                       getRenderedValue(cycle - 1), getToolTipText(cycle - 1),
                       getNotChangedSince(cycle - 1) + 1);
    }

    // draw lines for current value
    final double yTop = yBottom - VALUED_SIGNAL_HEIGHT;
    final double xStable = xStart + SIGNAL_SETUP_X;
    final double xStop = xStart + zoom;
    if (changed(cycle) && !firstCycle) {
      g.draw(new Line2D.Double(xStart, yTop, xStable, yBottom));
      g.draw(new Line2D.Double(xStart, yBottom, xStable, yTop));
    } else {
      g.draw(new Line2D.Double(xStart, yBottom, xStable, yBottom));
      g.draw(new Line2D.Double(xStart, yTop, xStable, yTop));
    }
    g.draw(new Line2D.Double(xStable, yTop, xStop, yTop));
    g.draw(new Line2D.Double(xStable, yBottom, xStop, yBottom));
    if (getValue(cycle) == null) {
      final double xPatternStart = changed(cycle) ? xStable : xStart;
      final Graphics2D fillG = (Graphics2D)g.create();
      final Rectangle2D.Double rectangle =
        new Rectangle2D.Double(xPatternStart, yTop + 1,
                               xStop - xPatternStart + 1, yBottom - yTop - 1);
      fillG.setPaint(FILL_PAINT);
      fillG.fill(rectangle);
    }

    if (lastCycle) {
      // print label as preview for not yet finished value
      paintValuedLabel(toolTips, g, zoom, xStart, yBottom,
                       getRenderedValue(cycle), getToolTipText(cycle),
                       getNotChangedSince(cycle) - 1);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
