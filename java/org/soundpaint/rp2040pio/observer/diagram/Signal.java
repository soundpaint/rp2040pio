/*
 * @(#)Signal.java 1.00 21/02/12
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
import java.io.IOException;
import java.util.List;

public interface Signal
{
  void reset();
  String getLabel();
  double getDisplayHeight();
  int size();
  String getToolTipText(final int cycle);
  void record() throws IOException;
  int getNotChangedSince(final int cycle);
  void setVisible(final boolean visible);
  boolean getVisible();
  void paintCycle(final Graphics2D g, final double zoom,
                  final double xStart, final double yBottom,
                  final int cycle,
                  final boolean isFirstCycle, final boolean isLastCycle);
  void createToolTip(final List<ToolTip> toolTips,
                     final int cycle,
                     final boolean isFirstCycle,
                     final boolean isLastCycle,
                     final double zoom,
                     final double xStart,
                     final double yBottom);
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
