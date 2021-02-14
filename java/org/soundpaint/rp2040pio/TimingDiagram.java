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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.FontMetrics;
import java.awt.TexturePaint;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Framework for displaying a timing diagram resulting from an
 * emulation run.
 */
public class TimingDiagram
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
  private final DiagramConfig diagramConfig;
  private final JFrame frame;
  private final JPanel panel;

  public TimingDiagram(final PIO pio)
  {
    this.pio = pio;
    diagramConfig = new DiagramConfig();
    diagramConfig.addSignal(new DiagramConfig.ClockSignal());
    frame = new JFrame("Timing Diagram");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(panel = new JPanel() {
        public void paint(final Graphics g)
        {
          super.paint(g);
          paintDiagram(this, (Graphics2D)g, getWidth(), getHeight());
        }
      });
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
                               final double xStart, final double yBottom)
  {
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
                                   final DiagramConfig.BitSignal signal)
  {
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
    final Object signalValue = signal.getRenderedPreviousValue();
    if (signalValue == null) {
      return;
    } else {
      final String label = String.valueOf(signalValue);
      final FontMetrics fm = panel.getFontMetrics(panel.getFont());
      final int width = fm.stringWidth(label);
      final double xLabelStart =
        xStart - 0.5 * (cycles * CLOCK_CYCLE_WIDTH - SIGNAL_SETUP_X + width);
      final double yTextBottom = yBottom - VALUE_LABEL_MARGIN_BOTTOM;
      g.setFont(VALUE_FONT);
      g.drawString(label, (float)xLabelStart, (float)yTextBottom);
    }
  }

  private void paintValuedSignalCycle(final JPanel panel, final Graphics2D g,
                                      final double xStart, final double yBottom,
                                      final DiagramConfig.ValuedSignal<?> signal,
                                      final boolean lastCycle)
  {
    final int notChangedSince = signal.notChangedSince();
    signal.update();
    final double yTop = yBottom - VALUED_SIGNAL_HEIGHT;
    final double xStable = xStart + SIGNAL_SETUP_X;
    final double xStop = xStart + CLOCK_CYCLE_WIDTH;
    if (xStart > LEFT_MARGIN) {
      if (signal.changed()) {
        paintValuedLabel(panel, g, xStart, yBottom, signal,
                         notChangedSince + 1);
      } else if (lastCycle) {
        paintValuedLabel(panel, g, xStart, yBottom, signal,
                         notChangedSince);
      }
    }
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
                                final boolean lastCycle)
  {
    if (signal instanceof DiagramConfig.ClockSignal) {
      paintClockCycle(panel, g, xStart, yBottom);
    } else if (signal instanceof DiagramConfig.BitSignal) {
      paintBitSignalCycle(panel, g, xStart, yBottom,
                          (DiagramConfig.BitSignal)signal);
    } else if (signal instanceof DiagramConfig.ValuedSignal<?>) {
      paintValuedSignalCycle(panel, g, xStart, yBottom,
                             (DiagramConfig.ValuedSignal<?>)signal, lastCycle);
    } else {
      throw new InternalError("unexpected signal type: " + signal);
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

  private void paintSignalsCycle(final JPanel panel, final Graphics2D g,
                                 final double x, final boolean lastCycle)
  {
    g.setColor(Color.BLACK);
    g.setStroke(PLAIN_STROKE);
    double y = TOP_MARGIN;
    for (final DiagramConfig.Signal signal : diagramConfig) {
      final double height =
        signal.isValued() ? VALUED_LANE_HEIGHT : BIT_LANE_HEIGHT;
      paintSignalCycle(panel, g, x, y += height, signal, lastCycle);
    }
  }

  private void resetEmulation()
  {
    pio.smRestartMask(PIO.SM_COUNT - 1);
    final GPIO gpio = pio.getGPIO();
    gpio.reset();
    for (final DiagramConfig.Signal signal : diagramConfig) {
      signal.reset();
    }
  }

  private void paintDiagram(final JPanel panel, final Graphics2D g,
                            final int width, final int height)
  {
    final MasterClock clock = MasterClock.getDefaultInstance();
    final SM sm0 = pio.getSM(0);
    g.setStroke(PLAIN_STROKE);
    paintLabels(panel, g);
    final int stopCycle =
      (int)((width - LEFT_MARGIN - RIGHT_MARGIN) / CLOCK_CYCLE_WIDTH);
    resetEmulation();
    pio.smSetEnabledMask(PIO.SM_COUNT - 1, true);
    for (int cycle = 0; cycle < stopCycle; cycle++) {
      clock.cycle();
      for (final Decoder.DecodeException e : pio.getExceptions()) {
        System.err.println(e);
      }
      final double x = LEFT_MARGIN + cycle * CLOCK_CYCLE_WIDTH;
      paintGridLine(g, x, height);
      final boolean lastCycle = cycle + 1 == stopCycle;
      paintSignalsCycle(panel, g, x, lastCycle);
    }
    paintGridLine(g, LEFT_MARGIN + stopCycle * CLOCK_CYCLE_WIDTH, height);
    pio.smSetEnabledMask(PIO.SM_COUNT - 1, false);
  }

  public void addProgram(final String programResourcePath)
    throws IOException
  {
    final Program program = Program.fromHexResource(programResourcePath);
    pio.addProgram(program);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
