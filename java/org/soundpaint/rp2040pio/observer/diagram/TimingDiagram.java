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
package org.soundpaint.rp2040pio.observer.diagram;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.sdk.SDK;

public class TimingDiagram
{
  private final PrintStream console;
  private final SDK sdk;
  private final DiagramConfig diagramConfig;
  private final Object wallClockLock;
  private long wallClock;

  private TimingDiagram()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public TimingDiagram(final PrintStream console, final SDK sdk,
                       final DiagramConfig diagramConfig)
    throws IOException
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    if (diagramConfig == null) {
      throw new NullPointerException("diagramConfig");
    }
    this.console = console;
    this.sdk = sdk;
    this.diagramConfig = diagramConfig;
    wallClockLock = new Object();
    wallClock = 0;
  }

  public DiagramConfig.Signal addSignal(final DiagramConfig.Signal signal)
  {
    if (signal == null) {
      throw new NullPointerException("signal");
    }
    diagramConfig.addSignal(signal);
    return signal;
  }

  public DiagramConfig.Signal addSignal(final String label, final int address,
                                        final int msb, final int lsb,
                                        final Supplier<Boolean> displayFilter)
    throws IOException
  {
    final DiagramConfig.ValuedSignal<Integer> signal =
      DiagramConfig.createFromRegister(sdk, label, address, msb, lsb,
                                       displayFilter);
    return addSignal(signal);
  }

  public DiagramConfig.Signal addSignal(final String label, final int address,
                                        final int msb, final int lsb)
    throws IOException
  {
    return addSignal(label, address, msb, lsb, null);
  }

  public DiagramConfig.Signal addSignal(final String label, final int address,
                                        final int bit)
    throws IOException
  {
    final DiagramConfig.BitSignal signal =
      DiagramConfig.createFromRegister(sdk, label, address, bit);
    return addSignal(signal);
  }

  public DiagramConfig.Signal addSignal(final int address, final int bit)
    throws IOException
  {
    return addSignal(null, address, bit);
  }

  public DiagramConfig.Signal addSignal(final String label, final int address)
    throws IOException
  {
    return addSignal(label, address, 31, 0);
  }

  public DiagramConfig.Signal addSignal(final String label, final int address,
                                        final Supplier<Boolean> displayFilter)
    throws IOException
  {
    return addSignal(label, address, 31, 0, displayFilter);
  }

  public DiagramConfig.Signal addSignal(final int address) throws IOException
  {
    return addSignal(null, address);
  }

  public DiagramConfig.Signal addSignal(final int address,
                                        final Supplier<Boolean> displayFilter)
    throws IOException
  {
    return addSignal(null, address, displayFilter);
  }

  public void clear()
  {
    for (final DiagramConfig.Signal signal : diagramConfig) {
      signal.reset();
    }
  }

  public void createRecord()
  {
    for (final DiagramConfig.Signal signal : diagramConfig) {
      if (signal.getVisible()) {
        signal.record();
      }
    }
  }

  public void checkForUpdate()
  {
    try {
      synchronized(wallClockLock) {
        final long wallClock = sdk.getWallClock();
        if (wallClock == this.wallClock) {
          // nothing to update
        } else if (wallClock == this.wallClock + 1) {
          createRecord();
        } else {
          // discontinuity in time => restart view
          clear();
        }
        this.wallClock = wallClock;
      }
    } catch (final IOException e) {
      console.println("error: failed reading wall clock: " + e.getMessage());
    }
  }

  public void applyCycles(final int count) throws IOException
  {
    for (int cycle = 0; cycle < count; cycle++) {
      sdk.triggerCyclePhase1(true);
      checkForUpdate();
      sdk.triggerCyclePhase0(true);
    }
  }

  public void fillInCurrentSignals(final List<DiagramConfig.Signal> signals)
  {
    signals.clear();
    for (final DiagramConfig.Signal signal : diagramConfig) {
      signals.add(signal);
    }
  }

  public void updateListOfSignals(final List<DiagramConfig.Signal> signals)
  {
    diagramConfig.clear();
    for (final DiagramConfig.Signal signal : signals) {
      diagramConfig.addSignal(signal);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
