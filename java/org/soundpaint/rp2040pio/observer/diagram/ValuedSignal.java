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
  protected static final double SIGNAL_HEIGHT = 24.0;
  protected static final BufferedImage FILL_IMAGE =
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

  private final SDK sdk;
  private final List<SignalFilter> displayFilters;
  private final int pioNum;
  private final int smNum;
  private final Supplier<Boolean> changeInfoGetter;

  /**
   * @param changeInfoGetter If set to &lt;code&gt;null&lt;/code&gt;,
   * then a change is assumed only when the updated value changes.
   */
  public ValuedSignal(final SDK sdk,
                      final String label,
                      final List<SignalFilter> displayFilters,
                      final int pioNum,
                      final int smNum,
                      final Supplier<Boolean> changeInfoGetter)
  {
    super(label);
    Objects.requireNonNull(sdk);
    this.sdk = sdk;
    this.displayFilters = displayFilters;
    this.pioNum = pioNum;
    this.smNum = smNum;
    this.changeInfoGetter = changeInfoGetter;
  }

  protected SDK getSDK() { return sdk; }

  public List<SignalFilter> getDisplayFilters()
  {
    return displayFilters;
  }

  abstract protected T sampleValue() throws IOException;

  private boolean passAllFilters() throws IOException
  {
    for (final SignalFilter filter : displayFilters) {
      if (!filter.acceptCurrentSignalValue(sdk, pioNum, smNum))
        return false;
    }
    return true;
  }

  @Override
  public void record() throws IOException
  {
    final boolean enforceChanged =
      changeInfoGetter != null ? changeInfoGetter.get() : false;
    final boolean pass;
    if (displayFilters != null) {
      pass = passAllFilters();
    } else {
      pass = true;
    }
    record(pass ? sampleValue() : null, enforceChanged);
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
               (int)(yBottom - SIGNAL_HEIGHT),
               (int)xStart - 1, (int)yBottom,
               previousToolTipText);
    if (isLastCycle) {
      // print label as preview for not yet finished value
      final String toolTipText = getToolTipText(cycle);
      final int cycles = getNotChangedSince(cycle) - 1;
      addToolTip(toolTips,
                 (int)(xStart - cycles * zoom),
                 (int)(yBottom - SIGNAL_HEIGHT),
                 (int)xStart - 1, (int)yBottom,
                 toolTipText);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
