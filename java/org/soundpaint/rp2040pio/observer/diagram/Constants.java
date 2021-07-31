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

import java.awt.Dimension;
import java.awt.Font;

public interface Constants
{
  static final String TOOLTIP_TEXT_LOAD =
    "load and run script for setting up emulation…";
  static final String TOOLTIP_TEXT_CYCLES =
    "number of cycles to emulate in one go";
  static final String TOOLTIP_TEXT_EMULATE =
    "emulate specified number of cycles in one go";
  static final String TOOLTIP_TEXT_CLEAR =
    "clear recorded cycles data";
  static final String TOOLTIP_TEXT_ZOOM =
    "change horizontal display scale";
  static final int ZOOM_MIN = 16;
  static final int ZOOM_MAX = 112;
  static final int ZOOM_DEFAULT = 32;
  static final double TOP_MARGIN = 0.0;
  static final double BOTTOM_MARGIN = 16.0;
  static final double SIGNAL_SETUP_X = 4.0;
  static final Font DEFAULT_FONT = Font.decode(null);
  static final Font LABEL_FONT = DEFAULT_FONT.deriveFont(10.0f);
  static final Dimension PREFERRED_LABEL_SIZE = new Dimension(120, 32);
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
