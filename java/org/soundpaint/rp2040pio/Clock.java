/*
 * @(#)Clock.java 1.00 21/02/05
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

import java.util.ArrayList;
import java.util.List;

/**
 * Clock Signal Provider
 */
public interface Clock
{
  public enum Phase
  {
    PHASE_0_IN_PROGRESS,
    PHASE_0_STABLE,
    PHASE_1_IN_PROGRESS,
    PHASE_1_STABLE
  };

  public static interface TransitionListener
  {
    void risingEdge(final long wallClock);
    void fallingEdge(final long wallClock);
  }

  void addTransitionListener(final TransitionListener listener);
  boolean removeTransitionListener(final TransitionListener listener);
  long getWallClock();
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
