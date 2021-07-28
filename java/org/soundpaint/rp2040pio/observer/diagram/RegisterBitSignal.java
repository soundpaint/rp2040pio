/*
 * @(#)RegisterBitSignal.java 1.00 21/02/12
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
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.Bit;
import org.soundpaint.rp2040pio.sdk.SDK;

public class RegisterBitSignal extends ValuedSignal<Bit>
{
  private static final double SIGNAL_HEIGHT = 16.0;

  public RegisterBitSignal(final SignalRendering.SignalParams signalParams)
  {
    this(signalParams, null);
  }

  public RegisterBitSignal(final SignalRendering.SignalParams signalParams,
                           final Supplier<Boolean> changeInfoGetter)
  {
    super(SignalRendering.Bit, signalParams, changeInfoGetter);
    final int msb = signalParams.getMsb();
    final int lsb = signalParams.getLsb();
    if (msb != lsb) {
      final String message =
        String.format("RegisterBitSignal: msb != lsb: %d != %d",
                      msb, lsb);
      throw new IllegalArgumentException(message);
    }
  }

  public int getBit()
  {
    return getSignalParams().getMsb();
  }

  @Override
  public double getDisplayHeight()
  {
    return SIGNAL_HEIGHT + 16.0;
  }

  @Override
  protected Bit sampleValue() throws IOException
  {
    final SignalRendering.SignalParams signalParams = getSignalParams();
    final SDK sdk = signalParams.getSDK();
    final int address = signalParams.getAddress();
    final int bit = getBit();
    return Bit.fromValue(sdk.readAddress(address, bit, bit));
  }

  @Override
  public String getToolTipText(final int cycle)
  {
    final Bit bit = getValue(cycle);
    return bit != null ? bit.getLevel() : null;
  }

  @Override
  public void paintCycle(final Graphics2D g, final double zoom,
                         final double xStart, final double yBottom,
                         final int cycle,
                         final boolean firstCycle, final boolean lastCycle)
  {
    if (!next(cycle - 1)) return;
    final double xStable = xStart + Constants.SIGNAL_SETUP_X;
    final double xStop = xStart + zoom;

    final Bit prevBit = getValue(cycle - 1);
    final Bit bit = getValue(cycle);
    if (bit != null) {
      final double yStable =
        yBottom - (bit == Bit.HIGH ? SIGNAL_HEIGHT : 0.0);
      g.draw(new Line2D.Double(xStable, yStable, xStop, yStable));
      if (prevBit != null) {
        final double yPrev =
          firstCycle ?
          yStable :
          yBottom - (prevBit == Bit.HIGH ? SIGNAL_HEIGHT : 0.0);
        g.draw(new Line2D.Double(xStart, yPrev, xStable, yStable));
      }
    } else {
      final double xPatternStart = changed(cycle) ? xStable : xStart;
      final Graphics2D fillG = (Graphics2D)g.create();
      final double yTop = yBottom - SIGNAL_HEIGHT;
      final Rectangle2D.Double rectangle =
        new Rectangle2D.Double(xPatternStart, yTop + 1,
                               xStop - xPatternStart + 1, yBottom - yTop - 1);
      fillG.setPaint(FILL_PAINT);
      fillG.fill(rectangle);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
