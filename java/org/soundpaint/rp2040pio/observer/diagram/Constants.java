/*
 * @(#)Constants.java 1.00 21/04/07
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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

public interface Constants
{
  static final int ZOOM_MIN = 16;
  static final int ZOOM_MAX = 112;
  static final int ZOOM_DEFAULT = 32;

  static final double TOP_MARGIN = 0.0;
  static final double BOTTOM_MARGIN = 16.0;
  static final double BIT_SIGNAL_HEIGHT = 16.0;
  static final double BIT_LANE_HEIGHT = BIT_SIGNAL_HEIGHT + 16.0;
  static final double VALUED_SIGNAL_HEIGHT = 24.0;
  static final double VALUED_LANE_HEIGHT = VALUED_SIGNAL_HEIGHT + 16.0;
  static final double LEFT_MARGIN = 2.0; // for clock arrow
  static final double RIGHT_MARGIN = 0.0;
  static final double SIGNAL_SETUP_X = 4.0;
  static final double VALUE_LABEL_MARGIN_BOTTOM = 8.0;

  static final Font DEFAULT_FONT = Font.decode(null);
  static final Font VALUE_FONT = DEFAULT_FONT.deriveFont(10.0f);
  static final Stroke PLAIN_STROKE =
    new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f);
  static final Stroke DOTTED_STROKE =
    new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0.0f,
                    new float[]{2.0f}, 0.0f);
  static final BufferedImage FILL_IMAGE =
    ((Supplier<BufferedImage>)(() -> {
        final BufferedImage image =
          new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = (Graphics2D)image.getGraphics();
        for (int x = -12; x < 12; x += 2) {
          g.drawLine(x, 12, x + 12, 0);
        }
        return image;
      })).get();

  static final TexturePaint FILL_PAINT =
    new TexturePaint(FILL_IMAGE, new Rectangle2D.Double(0.0, 0.0, 12.0, 12.0));
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
