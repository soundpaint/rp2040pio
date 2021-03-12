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
  public static final long DEFAULT_FREQUENCY = 1000000000;

  private static final MasterClock DEFAULT_INSTANCE = new MasterClock();

  public enum Mode {
    TARGET_FREQUENCY,
    SINGLE_STEP;

    public static Mode fromValue(final int value) {
      if ((value < 0) || (value >= MODES.length)) {
        throw new IllegalArgumentException("value: " + value);
      }
      return MODES[value];
    }
  };

  private static final Mode[] MODES = Mode.values();

  private class DrivingGear extends Thread
  {
    @Override
    public void run()
    {
      while (true) {
        synchronized(this) {
          while ((mode == Mode.SINGLE_STEP) && (trigger == Phase.PHASE_1)) {
            try {
              wait();
            } catch (final InterruptedException e) {
              // ignore here, since check in while condition
            }
          }
          if (phase == Phase.PHASE_1) {
            cyclePhase0();
          }
          while ((mode == Mode.SINGLE_STEP) && (trigger == Phase.PHASE_0)) {
            try {
              wait();
            } catch (final InterruptedException e) {
              // ignore here, since check in while condition
            }
          }
          if (phase == Phase.PHASE_0) {
            cyclePhase1();
          }
        }
      }
    }
  }

  private final DrivingGear drivingGear;
  private final List<TransitionListener> listeners;
  private long frequency;
  private Mode mode;
  private Phase phase;
  private Phase trigger;
  private long wallClock;

  public static MasterClock getDefaultInstance()
  {
    return DEFAULT_INSTANCE;
  }

  public MasterClock()
  {
    drivingGear = new DrivingGear();
    listeners = new ArrayList<TransitionListener>();
    reset();
    drivingGear.start();
  }

  public void reset()
  {
    frequency = DEFAULT_FREQUENCY;
    mode = Mode.SINGLE_STEP;
    phase = Phase.PHASE_1;
    trigger = Phase.PHASE_1;
    wallClock = -1;
  }

  public void setMASTERCLK_FREQ(final int frequency)
  {
    synchronized(drivingGear) {
      this.frequency = frequency & 0xffffffff;
      drivingGear.notify();
    }
  }

  public int getMASTERCLK_FREQ()
  {
    return (int)frequency;
  }

  public void setMode(final Mode mode)
  {
    synchronized(drivingGear) {
      this.mode = mode;
      drivingGear.notify();
    }
  }

  public Mode getMode() { return mode; }

  public void setMASTERCLK_MODE(final int value)
  {
    synchronized(drivingGear) {
      mode = Mode.fromValue(value & 0x1);
      drivingGear.notify();
    }
  }

  public int getMASTERCLK_MODE()
  {
    return mode.ordinal();
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

  public Phase getPhase() { return phase; }

  public Phase getLatestTrigger() { return trigger; }

  public void triggerPhase0()
  {
    synchronized(drivingGear) {
      trigger = Phase.PHASE_0;
      drivingGear.notify();
    }
  }

  private void cyclePhase0()
  {
    if (mode != Mode.SINGLE_STEP) return;
    if (phase == Phase.PHASE_0) return;
    phase = Phase.PHASE_0;
    announceRaisingEdge();
  }

  public void triggerPhase1()
  {
    synchronized(drivingGear) {
      trigger = Phase.PHASE_1;
      drivingGear.notify();
    }
  }

  private void cyclePhase1()
  {
    if (mode != Mode.SINGLE_STEP) return;
    if (phase == Phase.PHASE_1) return;
    phase = Phase.PHASE_1;
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
