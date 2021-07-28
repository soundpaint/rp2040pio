/*
 * @(#)RegisterIntSignal.java 1.00 21/07/23
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
import java.awt.TexturePaint;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.sdk.SDK;

public class RegisterIntSignal extends ValuedSignal<Integer>
{
  private static final double VALUE_LABEL_MARGIN_BOTTOM = 8.0;

  public RegisterIntSignal(final SignalRendering valueRendering,
                           final SignalRendering.SignalParams signalParams)
  {
    this(valueRendering, signalParams, null);
  }

  public RegisterIntSignal(final SignalRendering valueRendering,
                           final SignalRendering.SignalParams signalParams,
                           final Supplier<Boolean> changeInfoGetter)
  {
    super(valueRendering, signalParams, changeInfoGetter);
    Objects.requireNonNull(valueRendering.getToolTipRenderer());
    if (valueRendering == SignalRendering.Bit) {
      final String message = "this class does not support bit rendering";
      throw new IllegalArgumentException(message);
    }
  }

  @Override
  public double getDisplayHeight()
  {
    return SIGNAL_HEIGHT + 16.0;
  }

  @Override
  protected Integer sampleValue() throws IOException
  {
    final SignalRendering.SignalParams signalParams = getSignalParams();
    final SDK sdk = signalParams.getSDK();
    final int address = signalParams.getAddress();
    final int msb = signalParams.getMsb();
    final int lsb = signalParams.getLsb();
    return sdk.readAddress(address, msb, lsb);
  }

  @Override
  public String getToolTipText(final int cycle)
  {
    final Integer value = getValue(cycle);
    if (value == null)
      return null;
    final SignalRendering.ValueRenderer toolTipRenderer =
      getValueRendering().getToolTipRenderer();
    final SignalRendering.SignalParams signalParams = getSignalParams();
    return
      toolTipRenderer != null ?
      toolTipRenderer.renderValue(signalParams, cycle, value) : null;
  }

  private String getRenderedValue(final int cycle)
  {
    final Integer value = getValue(cycle);
    if (value == null)
      return null;
    final SignalRendering.ValueRenderer lifeLineRenderer =
      getValueRendering().getLifeLineRenderer();
    return
      lifeLineRenderer != null ?
      lifeLineRenderer.renderValue(getSignalParams(), cycle, value) :
      String.valueOf(value);
  }

  private static void paintValuedLabel(final Graphics2D g,
                                       final double zoom,
                                       final double xStart,
                                       final double yBottom,
                                       final String label,
                                       final int cycles)
  {
    if (label != null) {
      g.setFont(Constants.LABEL_FONT);
      final FontMetrics fm = g.getFontMetrics(g.getFont());
      final int width = fm.stringWidth(label);
      final double xLabelStart =
        xStart - 0.5 * (cycles * zoom - Constants.SIGNAL_SETUP_X + width);

      final double yTextBottom = yBottom - VALUE_LABEL_MARGIN_BOTTOM;
      g.drawString(label, (float)xLabelStart, (float)yTextBottom);
    }
  }

  @Override
  public void paintCycle(final Graphics2D g, final double zoom,
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
      paintValuedLabel(g, zoom, xStart, yBottom,
                       getRenderedValue(cycle - 1),
                       getNotChangedSince(cycle - 1) + 1);
    }

    // draw lines for current value
    final double yTop = yBottom - SIGNAL_HEIGHT;
    final double xStable = xStart + Constants.SIGNAL_SETUP_X;
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
      paintValuedLabel(g, zoom, xStart, yBottom,
                       getRenderedValue(cycle), getNotChangedSince(cycle) - 1);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
