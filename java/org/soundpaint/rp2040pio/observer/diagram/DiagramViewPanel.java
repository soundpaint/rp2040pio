/*
 * @(#)DiagramViewPanel.java 1.00 21/06/26
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
import java.awt.Rectangle;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class DiagramViewPanel extends JPanel implements Scrollable
{
  private static final long serialVersionUID = 6691239292598254146L;

  public static final double TOP_MARGIN = 16.0;
  public static final double BOTTOM_MARGIN = 16.0;
  public static final double BIT_SIGNAL_HEIGHT = 16.0;
  public static final double BIT_LANE_HEIGHT = BIT_SIGNAL_HEIGHT + 16.0;
  public static final double VALUED_SIGNAL_HEIGHT = 24.0;
  public static final double VALUED_LANE_HEIGHT =
    VALUED_SIGNAL_HEIGHT + 16.0;

  private final DiagramModel model;
  private final LegendPanel legendPanel;
  private final SignalPanel signalPanel;
  private final Dimension preferredViewportSize;

  private DiagramViewPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public DiagramViewPanel(final DiagramModel model)
    throws IOException
  {
    Objects.requireNonNull(model);
    this.model = model;
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    legendPanel = new LegendPanel(model);
    add(legendPanel);
    add(Box.createHorizontalStrut(5));
    signalPanel = new SignalPanel(model);
    add(new JScrollPane(signalPanel,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    preferredViewportSize = new Dimension(320, 240);
  }

  @Override
  public Dimension getPreferredScrollableViewportSize()
  {
    return preferredViewportSize;
  }

  @Override
  public int getScrollableBlockIncrement(final Rectangle visibleRect,
                                         final int orientation,
                                         final int direction)
  {
    return 1;
  }

  @Override
  public int getScrollableUnitIncrement(final Rectangle visibleRect,
                                        final int orientation,
                                        final int direction)
  {
    return 1;
  }

  @Override
  public boolean getScrollableTracksViewportWidth()
  {
    return true;
  }

  @Override
  public boolean getScrollableTracksViewportHeight()
  {
    return false;
  }

  public void updatePreferredSize()
  {
    legendPanel.updatePreferredSize();
    signalPanel.updatePreferredSize();
  }

  public void setZoom(final int zoom)
  {
    signalPanel.setZoom(zoom);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
