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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import org.soundpaint.rp2040pio.sdk.SDK;

public class DiagramViewPanel extends JPanel implements Scrollable
{
  private static final long serialVersionUID = 6691239292598254146L;

  private final DiagramModel model;
  private final LegendPanel legendPanel;
  private final SignalPanel signalPanel;
  private final JScrollPane scrollPane;
  private final JViewport viewport;
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
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    legendPanel = new LegendPanel(model);
    add(legendPanel);
    add(Box.createHorizontalStrut(5));
    signalPanel = new SignalPanel(model);
    scrollPane =
      new JScrollPane(signalPanel,
                      ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    viewport = scrollPane.getViewport();
    viewport.addChangeListener((event) -> rebuildToolTips());
    add(scrollPane);
    preferredViewportSize = new Dimension(720, 360);
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

  public void modelChanged()
  {
    legendPanel.updatePreferredSize();
    signalPanel.updatePreferredSize();
    SwingUtilities.invokeLater(() -> signalPanel.repaint());
  }

  public void ensureCycleIsVisible(final int cycle)
  {
    scrollPane.validate(); // ensure scrollbar max value is up to date
    final double leftMostVisibleCycle = getLeftMostVisibleCycle();
    final double rightMostVisibleCycle = getRightMostVisibleCycle();
    if ((cycle >= leftMostVisibleCycle) &&
        (cycle < (int)rightMostVisibleCycle))
      return;
    final double newLeftMostVisibleCycle;
    if (cycle < leftMostVisibleCycle) {
      newLeftMostVisibleCycle = cycle;
    } else /* (cycle >= (int)rightMostVisibleCycle) */ {
      newLeftMostVisibleCycle =
        cycle + leftMostVisibleCycle - rightMostVisibleCycle + 1;
    }
    setLeftMostVisibleCycle(newLeftMostVisibleCycle);
  }

  public double getRightMostVisibleCycle()
  {
    final JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
    final int scrollBarValue = scrollBar.getValue();
    final int viewPortWidth = scrollPane.getViewportBorderBounds().width;
    return signalPanel.x2cycle(scrollBarValue + viewPortWidth);
  }

  public double getLeftMostVisibleCycle()
  {
    final JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
    final int scrollBarValue = scrollBar.getValue();
    return signalPanel.x2cycle(scrollBarValue);
  }

  private void setLeftMostVisibleCycle(final double cycle)
  {
    final JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
    final double scrollBarValue = signalPanel.cycle2x(cycle);
    scrollBar.setValue((int)Math.round(scrollBarValue));
    SwingUtilities.invokeLater(() -> {
        rebuildToolTips();
        signalPanel.repaint();
      });
  }

  public void setZoom(final int zoom)
  {
    final double leftMostCycle = getLeftMostVisibleCycle();
    signalPanel.setZoom(zoom);
    setLeftMostVisibleCycle(leftMostCycle);
  }

  /**
   * While redraw of viewport is done via paintComponent() methods,
   * rebuild of tooltips is triggered separately and executed via this
   * method, such that partial repaint (e.g. when a displayed tooltip
   * disappears) does not retrigger a complete rebuild of all
   * tooltips.
   */
  public void rebuildToolTips()
  {
    final Rectangle viewRect = viewport.getViewRect();
    signalPanel.rebuildToolTips(viewRect);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
