/*
 * @(#)TimingDiagram.java 1.00 21/02/12
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
package org.soundpaint.rp2040pio;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.soundpaint.rp2040pio.sdk.Program;
import org.soundpaint.rp2040pio.sdk.ProgramParser;
import org.soundpaint.rp2040pio.sdk.PIOSDK;

/**
 * Framework for displaying a timing diagram resulting from an
 * emulation run.
 *
 * TODO: Ellipsis, see e.g. Fig. 55.
 *
 * TODO: Labelled external data via GPIO or DMA (e.g. data bits "D0",
 * "D1", "D2", ...).
 *
 * Syntax:
 * CLK=SIGNAL
 * DMA.SIGNAL_NAME=(SIGNAL|BIT)
 * SMx.SIGNAL_NAME=(SIGNAL|BIT)
 * GPIOx=(SIGNAL|BIT)
 */
public class TimingDiagram implements Constants
{
  private static final double TOP_MARGIN = 16.0;
  private static final double BOTTOM_MARGIN = 16.0;
  private static final double LEFT_MARGIN = 200.0;
  private static final double RIGHT_MARGIN = 16.0;
  private static final double CLOCK_CYCLE_WIDTH = 32.0;
  private static final double BIT_SIGNAL_HEIGHT = 16.0;
  private static final double BIT_LANE_HEIGHT = BIT_SIGNAL_HEIGHT + 16.0;
  private static final double VALUED_SIGNAL_HEIGHT = 24.0;
  private static final double VALUED_LANE_HEIGHT =
    VALUED_SIGNAL_HEIGHT + 16.0;
  private static final double SIGNAL_SETUP_X = 4.0;
  private static final double VALUE_LABEL_MARGIN_BOTTOM = 8.0;
  private static final double LEGEND_LABEL_MARGIN_BOTTOM = 4.0;
  private static final double LEGEND_LABEL_MARGIN_RIGHT = 10.0;

  private static final Font DEFAULT_FONT = new JPanel().getFont();
  private static final Font VALUE_FONT = DEFAULT_FONT.deriveFont(10.0f);

  private static final Stroke PLAIN_STROKE =
    new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f);
  private static final Stroke DOTTED_STROKE =
    new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0.0f,
                    new float[]{2.0f}, 0.0f);

  private static final BufferedImage FILL_IMAGE =
    new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
  static {
    final Graphics2D g = (Graphics2D)FILL_IMAGE.getGraphics();
    for (int x = -12; x < 12; x += 2) {
      g.drawLine(x, 12, x + 12, 0);
    }
  }
  private static final TexturePaint FILL_PAINT =
    new TexturePaint(FILL_IMAGE, new Rectangle2D.Double(0.0, 0.0, 12.0, 12.0));

  private final PIO pio;
  private final PIOSDK pioSdk;
  private final DiagramConfig diagramConfig;
  private final JFrame frame;
  private final JPanel panel;
  private final List<ToolTip> toolTips;

  private static class ToolTip
  {
    private final int x0;
    private final int y0;
    private final int x1;
    private final int y1;
    private final String text;

    private ToolTip()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    public ToolTip(final int x0, final int y0, final int x1, final int y1,
                   final String text)
    {
      if (text == null) {
        throw new NullPointerException("text");
      }
      this.x0 = x0;
      this.y0 = y0;
      this.x1 = x1;
      this.y1 = y1;
      this.text = text;
    }
  }

  private TimingDiagram()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public TimingDiagram(final PIO pio)
  {
    this.pio = pio;
    pioSdk = new PIOSDK(new Registers(pio));
    diagramConfig = new DiagramConfig();
    frame = new JFrame("Timing Diagram");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(panel = new JPanel() {
        @Override
        public void paint(final Graphics g)
        {
          super.paint(g);
          paintDiagram(this, (Graphics2D)g, getWidth(), getHeight());
        }
        @Override
        public String getToolTipText(final MouseEvent event) {
          return getDiagramToolTipText(event);
        }
      });
    panel.setToolTipText("");
    toolTips = new ArrayList<ToolTip>();
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

  public void addSignal(final DiagramConfig.Signal signal)
  {
    if (signal == null) {
      throw new NullPointerException("signal");
    }
    diagramConfig.addSignal(signal);
  }

  private int getPreferredHeight()
  {
    double y = TOP_MARGIN;
    for (final DiagramConfig.Signal signal : diagramConfig) {
      y += signal.isValued() ? VALUED_LANE_HEIGHT : BIT_LANE_HEIGHT;
    }
    return (int)(y + BOTTOM_MARGIN);
  }

  public void create()
  {
    panel.setPreferredSize(new Dimension(640, getPreferredHeight()));
    frame.pack();
    frame.setVisible(true);
  }

  private void paintGridLine(final Graphics2D g, final double x,
                             final double height)
  {
    g.setColor(Color.LIGHT_GRAY);
    g.setStroke(DOTTED_STROKE);
    g.draw(new Line2D.Double(x, TOP_MARGIN, x, height - BOTTOM_MARGIN));
  }

  private void drawUpArrow(final Graphics2D g, final double x, final double y)
  {
    final double arrowWidth = 0.3 * BIT_SIGNAL_HEIGHT;
    final double arrowHeight = 0.3 * BIT_SIGNAL_HEIGHT;
    g.draw(new Line2D.Double(x, y, x - 0.5 * arrowWidth, y + arrowHeight));
    g.draw(new Line2D.Double(x, y, x + 0.5 * arrowWidth, y + arrowHeight));
  }

  private void paintClockCycle(final JPanel panel, final Graphics2D g,
                               final double xStart, final double yBottom,
                               final boolean rightBorder)
  {
    if (rightBorder) return;
    final double xFallingEdge = xStart + 0.5 * CLOCK_CYCLE_WIDTH;
    final double xStop = xStart + CLOCK_CYCLE_WIDTH;
    final double yTop = yBottom - BIT_SIGNAL_HEIGHT;
    drawUpArrow(g, xStart, yTop);
    g.draw(new Line2D.Double(xStart, yBottom, xStart, yTop));
    g.draw(new Line2D.Double(xStart, yTop, xFallingEdge, yTop));
    g.draw(new Line2D.Double(xFallingEdge, yTop, xFallingEdge, yBottom));
    g.draw(new Line2D.Double(xFallingEdge, yBottom, xStop, yBottom));
  }

  private void paintBitSignalCycle(final JPanel panel, final Graphics2D g,
                                   final double xStart, final double yBottom,
                                   final DiagramConfig.BitSignal signal,
                                   final boolean rightBorder)
  {
    if (rightBorder) return;
    signal.update();
    final double y =
      yBottom - (signal.asBoolean() ? BIT_SIGNAL_HEIGHT : 0.0);
    final double xStable = xStart + SIGNAL_SETUP_X;
    final double xStop = xStart + CLOCK_CYCLE_WIDTH;
    final double yPrev;
    if (signal.changed()) {
      yPrev =
        yBottom - (signal.previousAsBoolean() ? BIT_SIGNAL_HEIGHT : 0.0);
    } else {
      yPrev = y;
    }
    g.draw(new Line2D.Double(xStart, yPrev, xStable, y));
    g.draw(new Line2D.Double(xStable, y, xStop, y));
  }

  private void paintValuedLabel(final JPanel panel, final Graphics2D g,
                                final double xStart, final double yBottom,
                                final DiagramConfig.ValuedSignal<?> signal,
                                final int cycles)
  {
    final String label = signal.getPreviousRenderedValue();
    if (label != null) {
      final FontMetrics fm = panel.getFontMetrics(panel.getFont());
      final int width = fm.stringWidth(label);
      final double xLabelStart =
        xStart - 0.5 * (cycles * CLOCK_CYCLE_WIDTH - SIGNAL_SETUP_X + width);
      final double yTextBottom = yBottom - VALUE_LABEL_MARGIN_BOTTOM;
      g.setFont(VALUE_FONT);
      g.drawString(label, (float)xLabelStart, (float)yTextBottom);
    }
    final String toolTipText = signal.getPreviousToolTipText();
    if (toolTipText != null) {
      addToolTip((int)(xStart - cycles * CLOCK_CYCLE_WIDTH),
                 (int)(yBottom - VALUED_SIGNAL_HEIGHT),
                 (int)xStart - 1, (int)yBottom,
                 toolTipText);
    }
  }

  private void paintValuedSignalCycle(final JPanel panel, final Graphics2D g,
                                      final double xStart, final double yBottom,
                                      final DiagramConfig.ValuedSignal<?> signal,
                                      final boolean leftBorder,
                                      final boolean rightBorder)
  {
    // draw only previous event if completed, since current event may
    // be still ongoing such that centered display of text is not yet
    // reached
    final int notChangedSince =
      signal.notChangedSince(); // safe prior to signal update
    signal.update();

    if (!leftBorder && (signal.changed() || rightBorder)) {
      // signal changed => go for printing label of completed value;
      // right border => print label as preview for incomplete value
      paintValuedLabel(panel, g, xStart, yBottom, signal,
                       notChangedSince + 1);
    }

    // draw lines for current value
    if (rightBorder) return;
    final double yTop = yBottom - VALUED_SIGNAL_HEIGHT;
    final double xStable = xStart + SIGNAL_SETUP_X;
    final double xStop = xStart + CLOCK_CYCLE_WIDTH;
    if (signal.changed()) {
      g.draw(new Line2D.Double(xStart, yTop, xStable, yBottom));
      g.draw(new Line2D.Double(xStart, yBottom, xStable, yTop));
    } else {
      g.draw(new Line2D.Double(xStart, yBottom, xStable, yBottom));
      g.draw(new Line2D.Double(xStart, yTop, xStable, yTop));
    }
    g.draw(new Line2D.Double(xStable, yTop, xStop, yTop));
    g.draw(new Line2D.Double(xStable, yBottom, xStop, yBottom));
    if (signal.getValue() == null) {
      final double xPatternStart = signal.changed() ? xStable : xStart;
      final Graphics2D fillG = (Graphics2D)g.create();
      final Rectangle2D.Double rectangle =
        new Rectangle2D.Double(xPatternStart, yTop + 1,
                               xStop - xPatternStart + 1, yBottom - yTop - 1);
      fillG.setPaint(FILL_PAINT);
      fillG.fill(rectangle);
    }
  }

  private void paintSignalCycle(final JPanel panel, final Graphics2D g,
                                final double xStart, final double yBottom,
                                final DiagramConfig.Signal signal,
                                final boolean leftBorder,
                                final boolean rightBorder)
  {
    if (signal instanceof DiagramConfig.ClockSignal) {
      paintClockCycle(panel, g, xStart, yBottom, rightBorder);
    } else if (signal instanceof DiagramConfig.BitSignal) {
      paintBitSignalCycle(panel, g, xStart, yBottom,
                          (DiagramConfig.BitSignal)signal, rightBorder);
    } else if (signal instanceof DiagramConfig.ValuedSignal<?>) {
      paintValuedSignalCycle(panel, g, xStart, yBottom,
                             (DiagramConfig.ValuedSignal<?>)signal,
                             leftBorder, rightBorder);
    } else {
      throw new InternalError("unexpected signal type: " + signal);
    }
  }

  private void paintSignalsCycle(final JPanel panel, final Graphics2D g,
                                 final double xStart, final boolean leftBorder,
                                 final boolean rightBorder)
  {
    g.setColor(Color.BLACK);
    g.setStroke(PLAIN_STROKE);
    double y = TOP_MARGIN;
    for (final DiagramConfig.Signal signal : diagramConfig) {
      final double height =
        signal.isValued() ? VALUED_LANE_HEIGHT : BIT_LANE_HEIGHT;
      paintSignalCycle(panel, g, xStart, y += height, signal,
                       leftBorder, rightBorder);
    }
  }

  private void paintLabel(final JPanel panel, final Graphics2D g,
                          final double xStart, final double yBottom,
                          final String label)
  {
    final FontMetrics fm = panel.getFontMetrics(panel.getFont());
    final int width = fm.stringWidth(label);
    g.drawString(label,
                 (float)(xStart - width - LEGEND_LABEL_MARGIN_RIGHT),
                 (float)(yBottom - LEGEND_LABEL_MARGIN_BOTTOM));
  }

  private void paintLabels(final JPanel panel, final Graphics2D g)
  {
    double y = TOP_MARGIN;
    for (final DiagramConfig.Signal signal : diagramConfig) {
      final String label = signal.getLabel();
      final double height =
        signal.isValued() ? VALUED_LANE_HEIGHT : BIT_LANE_HEIGHT;
      paintLabel(panel, g, LEFT_MARGIN, y += height, label);
    }
  }

  private void resetEmulation()
  {
    pioSdk.setSmMaskEnabled((1 << SM_COUNT) - 1, false);
    pioSdk.restartSmMask(SM_COUNT - 1);
    final GPIO gpio = pio.getGPIO();
    gpio.reset();
    for (final DiagramConfig.Signal signal : diagramConfig) {
      signal.reset();
    }
  }

  private void paintDiagram(final JPanel panel, final Graphics2D g,
                            final int width, final int height)
  {
    toolTips.clear();
    final MasterClock clock = MasterClock.getDefaultInstance();
    final SM sm0 = pio.getSM(0);
    g.setStroke(PLAIN_STROKE);
    paintLabels(panel, g);
    final int stopCycle =
      (int)((width - LEFT_MARGIN - RIGHT_MARGIN) / CLOCK_CYCLE_WIDTH + 1);
    resetEmulation();
    // TODO: Enabling SM should be part of configuration and
    // replayed, whenever the simulation is restarted.
    pioSdk.setSmMaskEnabled(1, true);
    for (int cycle = 0; cycle < stopCycle; cycle++) {
      clock.cyclePhase0();
      for (final Decoder.DecodeException e : pio.getExceptions()) {
        System.err.println(e);
      }
      final double x = LEFT_MARGIN + cycle * CLOCK_CYCLE_WIDTH;
      final boolean leftBorder = cycle == 0;
      final boolean rightBorder = cycle + 1 == stopCycle;
      paintGridLine(g, x, height);
      paintSignalsCycle(panel, g, x, leftBorder, rightBorder);
      clock.cyclePhase1();
    }
    paintGridLine(g, LEFT_MARGIN + stopCycle * CLOCK_CYCLE_WIDTH, height);
  }

  public void addProgram(final String programResourcePath)
    throws IOException
  {
    final Program program = ProgramParser.parse(programResourcePath);
    pioSdk.addProgram(program);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
