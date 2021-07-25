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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
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
  private static final double LEFT_MARGIN = 2.0; // for clock arrow
  private static final double RIGHT_MARGIN = 0.0;
  private static final Stroke PLAIN_STROKE =
    new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f);
  private static final Stroke DOTTED_STROKE =
    new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0.0f,
                    new float[]{2.0f}, 0.0f);

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
  public String getToolTipText(final MouseEvent event)
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
        preferredHeight += signal.getDisplayHeight();
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
                                 final double xStart, final int cycle,
                                 final boolean firstCycle,
                                 final boolean lastCycle)
  {
    g.setColor(Color.BLACK);
    g.setStroke(PLAIN_STROKE);
    double y = TOP_MARGIN;
    for (final Signal signal : model) {
      if (signal.getVisible()) {
        final double height = signal.getDisplayHeight();
        signal.paintCycle(g, zoom, xStart, y += height, cycle,
                          firstCycle, lastCycle);
      }
    }
  }

  public int x2cycle(final double x)
  {
    return (int)((x - LEFT_MARGIN) / zoom);
  }

  public double cycle2x(final double cycle)
  {
    return cycle * zoom + LEFT_MARGIN;
  }

  private void paintDiagram(final Graphics2D g,
                            final int width, final int height)
    throws IOException
  {
    g.setStroke(PLAIN_STROKE);
    g.getClipBounds(clipBounds);
    final int cycles = model.getSignalSize();
    final int leftMostCycle =
      Math.min(model.getSignalSize(),
               x2cycle(clipBounds.x));
    final int rightMostCycle =
      Math.min(model.getSignalSize(),
               x2cycle(clipBounds.x + clipBounds.width - 1) + 1);
    for (int cycle = leftMostCycle; cycle < rightMostCycle; cycle++) {
      final double x = LEFT_MARGIN + cycle * zoom;
      final boolean firstCycle = cycle == 0;
      final boolean lastCycle = cycle == cycles - 1;
      paintGridLine(g, x, height);
      paintSignalsCycle(g, x, cycle, firstCycle, lastCycle);
    }
    paintGridLine(g, LEFT_MARGIN + rightMostCycle * zoom, height);
  }

  private void paintError(final Graphics2D g,
                          final int width, final int height,
                          final IOException exception)
  {
    g.setStroke(PLAIN_STROKE);
    g.setFont(LABEL_FONT);
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

  private final void createToolTips(final int cycle,
                                    final boolean firstCycle,
                                    final boolean lastCycle,
                                    final double xStart)
  {
    double y = TOP_MARGIN;
    for (final Signal signal : model) {
      if (signal.getVisible()) {
        final double height = signal.getDisplayHeight();
        signal.createToolTip(toolTips, cycle, firstCycle, lastCycle,
                             zoom, xStart, y += height);
      }
    }
  }

  public void rebuildToolTips(final Rectangle clipBounds)
  {
    toolTips.clear();
    final int cycles = model.getSignalSize();
    final int leftMostCycle =
      Math.min(model.getSignalSize(),
               x2cycle(clipBounds.x));
    final int rightMostCycle =
      Math.min(model.getSignalSize(),
               x2cycle(clipBounds.x + clipBounds.width - 1) + 1);
    for (int cycle = leftMostCycle; cycle <= rightMostCycle; cycle++) {
      final double x = LEFT_MARGIN + cycle * zoom;
      final boolean firstCycle = cycle == 0;
      final boolean lastCycle = cycle == cycles - 1;
      createToolTips(cycle, firstCycle, lastCycle, x);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
