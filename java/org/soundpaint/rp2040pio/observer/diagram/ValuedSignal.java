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

import java.awt.Graphics2D;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.sdk.SDK;

public abstract class ValuedSignal<T> extends AbstractSignal<T>
{
  private static final BufferedImage FILL_IMAGE =
    ((Supplier<BufferedImage>)(() -> {
        final BufferedImage image =
          new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = (Graphics2D)image.getGraphics();
        for (int x = -12; x < 12; x += 2) {
          g.drawLine(x, 12, x + 12, 0);
        }
        return image;
      })).get();
  protected static final TexturePaint FILL_PAINT =
    new TexturePaint(FILL_IMAGE, new Rectangle2D.Double(0.0, 0.0, 12.0, 12.0));
  protected static final double SIGNAL_HEIGHT = 24.0;

  private final SignalRendering valueRendering;
  private final Supplier<Boolean> changeInfoGetter;

  /**
   * @param changeInfoGetter If set to &lt;code&gt;null&lt;/code&gt;,
   * then a change is assumed only when the updated value changes.
   */
  protected ValuedSignal(final SignalRendering valueRendering,
                         final SignalRendering.SignalParams signalParams,
                         final Supplier<Boolean> changeInfoGetter)
  {
    super(signalParams);
    Objects.requireNonNull(signalParams);
    Objects.requireNonNull(signalParams.getDiagram());
    Objects.requireNonNull(signalParams.getSDK());
    this.valueRendering = valueRendering;
    this.changeInfoGetter = changeInfoGetter;
  }

  public SignalRendering getValueRendering()
  {
    return valueRendering;
  }

  @Override
  protected double getSignalHeight() { return SIGNAL_HEIGHT; }

  abstract protected T sampleValue() throws IOException;

  private boolean passesAllFilters(final List<SignalFilter> displayFilters)
    throws IOException
  {
    final SignalRendering.SignalParams signalParams = getSignalParams();
    final SDK sdk = signalParams.getSDK();
    final int pioNum = signalParams.getPioNum();
    final int smNum = signalParams.getSmNum();
    for (final SignalFilter displayFilter : displayFilters) {
      if (!displayFilter.acceptCurrentSignalValue(sdk, pioNum, smNum))
        return false;
    }
    return true;
  }

  @Override
  public void record() throws IOException
  {
    final boolean enforceChanged =
      changeInfoGetter != null ? changeInfoGetter.get() : false;
    final SignalRendering.SignalParams signalParams = getSignalParams();
    final List<SignalFilter> displayFilters = signalParams.getDisplayFilters();
    final boolean passes;
    if (displayFilters != null) {
      passes = passesAllFilters(displayFilters);
    } else {
      passes = true;
    }
    record(passes ? sampleValue() : null, enforceChanged);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
