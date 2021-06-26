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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * System Master Clock
 */
public class MasterClock implements Clock, Constants
{
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
    public DrivingGear()
    {
      super("Emulation Thread");
    }

    private void runSingleStep()
    {
      synchronized(this) {
        while ((mode == Mode.SINGLE_STEP) &&
               (phase == Phase.PHASE_1_STABLE)) {
          try {
            wait();
          } catch (final InterruptedException e) {
            // ignore here, since check in while condition
          }
          if (terminate) return;
        }
        if (phase == Phase.PHASE_0_IN_PROGRESS) {
          syncWithRealTime();
          cyclePhase0();
        }
        while ((mode == Mode.SINGLE_STEP) &&
               (phase == Phase.PHASE_0_STABLE)) {
          try {
            wait();
          } catch (final InterruptedException e) {
            // ignore here, since check in while condition
          }
          if (terminate) return;
        }
        if (phase == Phase.PHASE_1_IN_PROGRESS) {
          cyclePhase1();
        }
      }
    }

    private void runTargetFrequency()
    {
      syncWithRealTime();
      phase = Phase.PHASE_0_IN_PROGRESS;
      cyclePhase0();
      phase = Phase.PHASE_1_IN_PROGRESS;
      cyclePhase1();
    }

    @Override
    public void run()
    {
      while (true) {
        while (mode == Mode.SINGLE_STEP) {
          runSingleStep();
          if (terminate) return;
        }
        while (mode == Mode.TARGET_FREQUENCY) {
          runTargetFrequency();
          if (terminate) return;
        }
      }
    }
  }

  private final PrintStream console;

  /**
   * Insure access to the following set of variables is atomic:
   * (refWallClock, refRealTime, frequency, milliSecondsPerCycle).
   */
  private final Object accountingLock;

  /**
   * Emulator-wide lock for synchronizing register reads waiting for a
   * specific masked value to match.
   */
  private final Object registerWaitLock;

  private final DrivingGear drivingGear;
  private final List<TransitionListener> listeners;
  private long frequency;
  private double milliSecondsPerCycle;
  private Mode mode;
  private Phase phase;
  private long wallClock;
  private long refWallClock;
  private long refRealTime;
  private boolean terminate;

  private MasterClock()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public MasterClock(final PrintStream console)
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.console = console;
    accountingLock = new Object();
    registerWaitLock = new Object();
    drivingGear = new DrivingGear();
    listeners = new ArrayList<TransitionListener>();
    reset();
    start();
  }

  public void reset()
  {
    setFrequency(DEFAULT_FREQUENCY);
    setMode(Mode.SINGLE_STEP);
    phase = Phase.PHASE_1_STABLE;
    wallClock = 0;
  }

  private void start()
  {
    terminate = false;
    drivingGear.start();
  }

  public void terminate()
  {
    synchronized(drivingGear) {
      terminate = true;
      drivingGear.notify();
    }
  }

  private void resetRef()
  {
    synchronized(accountingLock) {
      refWallClock = wallClock;
      refRealTime = System.currentTimeMillis();
    }
  }

  private long getMilliSecondsAhead()
  {
    synchronized(accountingLock) {
      if (frequency == 0) {
        return 0;
      }
      final long wallTimeMillisSinceRef =
        Math.round(milliSecondsPerCycle * (wallClock - refWallClock));
      final long realTimeMillisSinceRef =
        System.currentTimeMillis() - refRealTime;
      final long milliSecondsAhead =
        wallTimeMillisSinceRef - realTimeMillisSinceRef;
      return milliSecondsAhead >= 0 ? milliSecondsAhead : 0;
    }
  }

  private void syncWithRealTime()
  {
    if (mode != Mode.TARGET_FREQUENCY) return;
    final long milliSecondsAhead = getMilliSecondsAhead();
    if (milliSecondsAhead > 0) {
      try {
        Thread.sleep(milliSecondsAhead);
      } catch (final InterruptedException e) {
        // ignore
      }
    }
  }

  private void setFrequency(final int frequency)
  {
    synchronized(accountingLock) {
      this.frequency = frequency & 0xffffffff;
      milliSecondsPerCycle =
        frequency != 0 ? 8000.0 / this.frequency : Double.POSITIVE_INFINITY;
      resetRef();
    }
  }

  public Object getRegisterWaitLock()
  {
    return registerWaitLock;
  }

  public void setMASTERCLK_FREQ(final int frequency)
  {
    synchronized(drivingGear) {
      setFrequency(frequency);
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
      resetRef();
    }
  }

  public Mode getMode() { return mode; }

  public void setMASTERCLK_MODE(final int value)
  {
    setMode(Mode.fromValue(value & 0x1));
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

  private void announceRisingEdge()
  {
    for (final TransitionListener listener : listeners) {
      listener.risingEdge(wallClock);
    }
  }

  private void announceFallingEdge()
  {
    for (final TransitionListener listener : listeners) {
      listener.fallingEdge(wallClock);
    }
  }

  public Phase getPhase() { return phase; }

  public void triggerPhase0()
  {
    synchronized(accountingLock) {
      if (mode != Mode.SINGLE_STEP) return;
      synchronized(drivingGear) {
        if (phase == Phase.PHASE_1_STABLE) {
          phase = Phase.PHASE_0_IN_PROGRESS;
          drivingGear.notify();
        }
      }
    }
  }

  private void cyclePhase0()
  {
    if (phase != Phase.PHASE_0_IN_PROGRESS) {
      console.println("warning: cyclePhase0: unexpected phase: " + phase);
      return;
    }
    announceRisingEdge();
    phase = Phase.PHASE_0_STABLE;
    synchronized(registerWaitLock) {
      registerWaitLock.notifyAll();
    }
  }

  public void triggerPhase1()
  {
    synchronized(accountingLock) {
      if (mode != Mode.SINGLE_STEP) return;
      synchronized(drivingGear) {
        if (phase == Phase.PHASE_0_STABLE) {
          phase = Phase.PHASE_1_IN_PROGRESS;
          drivingGear.notify();
        }
      }
    }
  }

  private void cyclePhase1()
  {
    if (phase != Phase.PHASE_1_IN_PROGRESS) {
      console.println("warning: cyclePhase1: unexpected phase: " + phase);
      return;
    }
    announceFallingEdge();
    wallClock++;
    phase = Phase.PHASE_1_STABLE;
    synchronized(registerWaitLock) {
      registerWaitLock.notifyAll();
    }
  }

  public void awaitPhaseChange() throws InterruptedException
  {
    synchronized(registerWaitLock) {
      registerWaitLock.wait();
    }
  }

  public void awaitPhaseChange(final long millisTimeout)
    throws InterruptedException
  {
    synchronized(registerWaitLock) {
      registerWaitLock.wait(millisTimeout);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
