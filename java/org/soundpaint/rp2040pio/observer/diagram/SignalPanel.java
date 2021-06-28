/*
 * @(#)SignalPanel.java 1.00 21/04/07
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/**
 * Panel for drawing the view of the signals.
 */
public class SignalPanel extends JComponent implements Constants
{
  private static final long serialVersionUID = 6327282160532117231L;

  private final DiagramModel model;
  private final List<ToolTip> toolTips;
  private final Dimension preferredSize;
  private double zoom;
  private Rectangle clipBounds;

  private SignalPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public SignalPanel(final DiagramModel model) throws IOException
  {
    if (model == null) {
      throw new NullPointerException("model");
    }
    this.model = model;
    toolTips = new ArrayList<ToolTip>();
    setToolTipText("");
    preferredSize = new Dimension();
    zoom = ZOOM_DEFAULT;
    clipBounds = new Rectangle();
    updatePreferredSize();
  }

  @Override
  public String getToolTipText(final MouseEvent event) {
    return getDiagramToolTipText(event);
  }

  private void addToolTip(final int x0, final int y0,
                          final int x1, final int y1,
                          final String text)
  {
    toolTips.add(new ToolTip(x0, y0, x1, y1, text));
  }

  private String getDiagramToolTipText(final MouseEvent event)
  {
    final Point p = event.getPoint();
    for (final ToolTip toolTip: toolTips) {
      if ((p.x >= toolTip.x0) && (p.x <= toolTip.x1) &&
          (p.y >= toolTip.y0) && (p.y <= toolTip.y1)) {
        return toolTip.text;
      }
    }
    return null;
  }

  private void updatePreferredHeight()
  {
    double preferredHeight = TOP_MARGIN + BOTTOM_MARGIN;
    for (final Signal signal : model) {
      if (signal.getVisible()) {
        preferredHeight +=
          signal.isValued() ? VALUED_LANE_HEIGHT : BIT_LANE_HEIGHT;
      }
    }
    preferredSize.setSize((int)preferredSize.getWidth(), (int)preferredHeight);
  }

  private void updatePreferredWidth()
  {
    final double preferredWidth =
      Math.round(LEFT_MARGIN + zoom * model.getSignalSize() + RIGHT_MARGIN);
    preferredSize.setSize((int)preferredWidth, (int)preferredSize.getHeight());
  }

  public void updatePreferredSize()
  {
    updatePreferredHeight();
    updatePreferredWidth();
    revalidate();
  }

  @Override
  public Dimension getPreferredSize()
  {
    return preferredSize;
  }

  public void setZoom(final int zoom)
  {
    this.zoom =
      zoom < ZOOM_MIN ? ZOOM_MIN : (zoom > ZOOM_MAX ? ZOOM_MAX : zoom);
    updatePreferredWidth();
    revalidate();
  }

  public double getZoom()
  {
    return zoom;
  }

  private void paintGridLine(final Graphics2D g, final double x,
                             final double height)
  {
    g.setColor(Color.LIGHT_GRAY);
    g.setStroke(DOTTED_STROKE);
    g.draw(new Line2D.Double(x, TOP_MARGIN, x, height - BOTTOM_MARGIN));
  }

  private void paintSignalsCycle(final Graphics2D g,
                                 final double xStart, final boolean firstCycle,
                                 final boolean lastCycle)
  {
    g.setColor(Color.BLACK);
    g.setStroke(PLAIN_STROKE);
    double y = TOP_MARGIN;
    for (final Signal signal : model) {
      if (signal.getVisible()) {
        final double height =
          signal.isValued() ? VALUED_LANE_HEIGHT : BIT_LANE_HEIGHT;
        signal.paintCycle(toolTips, g, zoom, xStart, y += height,
                          firstCycle, lastCycle);
      }
    }
  }

  public double x2cycle(final double x)
  {
    return (x - LEFT_MARGIN) / zoom;
  }

  public double cycle2x(final double cycle)
  {
    return cycle * zoom + LEFT_MARGIN;
  }

  private void paintDiagram(final Graphics2D g,
                            final int width, final int height)
    throws IOException
  {
    toolTips.clear();
    g.setStroke(PLAIN_STROKE);
    g.getClipBounds(clipBounds);
    final int cycles = model.getSignalSize();
    final int leftMostCycle = (int)x2cycle(clipBounds.x);
    final int rightMostCycle =
      Math.min(model.getSignalSize(),
               ((int)x2cycle(clipBounds.x + clipBounds.width - 1)) + 1);
    for (final Signal signal : model) {
      if (signal.getVisible()) {
        signal.rewind(leftMostCycle);
      }
    }
    for (int cycle = leftMostCycle; cycle < rightMostCycle; cycle++) {
      final double x = LEFT_MARGIN + cycle * zoom;
      final boolean firstCycle = cycle == 0;
      final boolean lastCycle = cycle == cycles - 1;
      paintGridLine(g, x, height);
      paintSignalsCycle(g, x, firstCycle, lastCycle);
    }
    paintGridLine(g, LEFT_MARGIN + rightMostCycle * zoom, height);
  }

  private void paintError(final Graphics2D g,
                          final int width, final int height,
                          final IOException exception)
  {
    g.setStroke(PLAIN_STROKE);
    g.setFont(VALUE_FONT);
    g.drawString(exception.getMessage(), 10.0f, 10.0f);
  }

  @Override
  public void paintComponent(final Graphics g)
  {
    super.paintComponent(g);
    try {
      paintDiagram((Graphics2D)g, getWidth(), getHeight());
    } catch (final IOException e) {
      paintError((Graphics2D)g, getWidth(), getHeight(), e);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
