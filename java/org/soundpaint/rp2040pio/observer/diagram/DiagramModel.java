/*
 * @(#)DiagramModel.java 1.00 21/02/12
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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.sdk.SDK;

public class DiagramModel implements Iterable<Signal>
{
  private final PrintStream console;
  private final SDK sdk;
  private final List<Signal> signals;
  private final Object wallClockLock;
  private long wallClock;
  private int signalSize;

  private DiagramModel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public DiagramModel(final PrintStream console, final SDK sdk)
    throws IOException
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    this.console = console;
    this.sdk = sdk;
    signals = new ArrayList<Signal>();
    wallClockLock = new Object();
    wallClock = 0;
    signalSize = 0;
  }

  public Iterator<Signal> iterator()
  {
    return signals.iterator();
  }

  public Signal addSignal(final Signal signal)
  {
    if (signal == null) {
      throw new NullPointerException("signal");
    }
    signals.add(signal);
    return signal;
  }

  public Signal addSignal(final String label, final int address,
                          final int msb, final int lsb,
                          final Supplier<Boolean> displayFilter)
    throws IOException
  {
    final SignalFactory.ValuedSignal<Integer> signal =
      SignalFactory.createFromRegister(sdk, label, address, msb, lsb,
                                       displayFilter);
    return addSignal(signal);
  }

  public Signal addSignal(final String label, final int address,
                          final int msb, final int lsb)
    throws IOException
  {
    return addSignal(label, address, msb, lsb, null);
  }

  public Signal addSignal(final String label, final int address, final int bit)
    throws IOException
  {
    final SignalFactory.BitSignal signal =
      SignalFactory.createFromRegister(sdk, label, address, bit);
    return addSignal(signal);
  }

  public Signal addSignal(final int address, final int bit) throws IOException
  {
    return addSignal(null, address, bit);
  }

  public Signal addSignal(final String label, final int address)
    throws IOException
  {
    return addSignal(label, address, 31, 0);
  }

  public Signal addSignal(final String label, final int address,
                          final Supplier<Boolean> displayFilter)
    throws IOException
  {
    return addSignal(label, address, 31, 0, displayFilter);
  }

  public Signal addSignal(final int address) throws IOException
  {
    return addSignal(null, address);
  }

  public Signal addSignal(final int address,
                          final Supplier<Boolean> displayFilter)
    throws IOException
  {
    return addSignal(null, address, displayFilter);
  }

  public void resetSignals()
  {
    for (final Signal signal : signals) {
      signal.reset();
    }
    signalSize = 0;
  }

  private void appendRecordToSignals()
  {
    for (final Signal signal : signals) {
      if (signal.getVisible()) {
        signal.record();
      }
    }
    signalSize++;
  }

  public void checkForUpdate()
  {
    try {
      synchronized(wallClockLock) {
        final long wallClock = sdk.getWallClock();
        if (wallClock == this.wallClock) {
          // nothing to update
        } else if (wallClock == this.wallClock + 1) {
          appendRecordToSignals();
        } else {
          // discontinuity in time => restart view
          resetSignals();
        }
        this.wallClock = wallClock;
      }
    } catch (final IOException e) {
      console.println("error: failed reading wall clock: " + e.getMessage());
    }
  }

  public void applyCycles(final int count) throws IOException
  {
    if (count < 0) {
      throw new IllegalArgumentException("count < 0: " + count);
    }
    for (int cycle = 0; cycle < count; cycle++) {
      sdk.triggerCyclePhase1(true);
      checkForUpdate();
      sdk.triggerCyclePhase0(true);
    }
  }

  public int getSignalSize()
  {
    return signalSize;
  }

  public void pullSignals(final List<Signal> targetSignals)
  {
    targetSignals.clear();
    for (final Signal signal : signals) {
      targetSignals.add(signal);
    }
  }

  public void pushSignals(final List<Signal> newSignals)
  {
    signals.clear();
    for (final Signal signal : newSignals) {
      signals.add(signal);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
