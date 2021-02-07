/*
 * @(#)MasterClock.java 1.00 21/02/05
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
 * System Master Clock
 */
public class MasterClock implements Clock
{
  private final List<TransitionListener> listeners;
  private long wallClock;

  private static final MasterClock DEFAULT_INSTANCE = new MasterClock();

  public static MasterClock getDefaultInstance()
  {
    return DEFAULT_INSTANCE;
  }

  public MasterClock()
  {
    listeners = new ArrayList<TransitionListener>();
    reset();
  }

  public void reset()
  {
    wallClock = -1;
  }

  @Override
  public void addTransitionListener(final TransitionListener listener)
  {
    listeners.add(listener);
  }

  @Override
  public boolean removeTransitionListener(final TransitionListener listener)
  {
    return listeners.remove(listener);
  }

  @Override
  public long getWallClock()
  {
    return wallClock;
  }

  private void announceRaisingEdge()
  {
    for (final TransitionListener listener : listeners) {
      listener.raisingEdge(wallClock);
    }
  }

  private void announceFallingEdge()
  {
    for (final TransitionListener listener : listeners) {
      listener.fallingEdge(wallClock);
    }
  }

  public void cycle()
  {
    announceRaisingEdge();
    announceFallingEdge();
    wallClock++;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
