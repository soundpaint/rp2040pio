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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.sdk.SDK;

public class DiagramModel implements Iterable<Signal>
{
  private final PrintStream console;
  private final SDK sdk;
  private final HashMap<Integer, RegisterIntSignal> address2internalSignal;
  private final List<Signal> signals;
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
    address2internalSignal = new HashMap<Integer, RegisterIntSignal>();
    signals = new ArrayList<Signal>();
    wallClock = -1;
    signalSize = 0;
  }

  public Iterator<Signal> iterator()
  {
    return signals.iterator();
  }

  public Collection<RegisterIntSignal> getInternalSignals()
  {
    return address2internalSignal.values();
  }

  public Signal addInternalSignal(final Diagram diagram,
                                  final String label, final int address)
    throws IOException
  {
    final RegisterIntSignal signal =
      SignalFactory.createInternal(diagram, sdk, label, address);
    address2internalSignal.put(address, signal);
    return signal;
  }

  public RegisterIntSignal getInternalSignalByAddress(final int address)
  {
    return address2internalSignal.get(address);
  }

  public Signal addSignal(final Signal signal)
  {
    if (signal == null) {
      throw new NullPointerException("signal");
    }
    signals.add(signal);
    return signal;
  }

  public Signal addSignal(final Diagram diagram,
                          final String label, final int address,
                          final int msb, final int lsb,
                          final List<SignalFilter> displayFilters,
                          final int pioNum, final int smNum)
    throws IOException
  {
    final Signal signal =
      SignalFactory.
      createFromRegister(diagram, sdk, label, address, msb, lsb,
                         SignalRendering.Hex, displayFilters, pioNum, smNum);
    return addSignal(signal);
  }

  public Signal addSignal(final Diagram diagram,
                          final String label, final int address,
                          final int msb, final int lsb)
    throws IOException
  {
    return addSignal(diagram, label, address, msb, lsb, null, -1, -1);
  }

  public Signal addSignal(final Diagram diagram,
                          final String label, final int address, final int bit,
                          final List<SignalFilter> displayFilters,
                          final int pioNum, final int smNum)
    throws IOException
  {
    final RegisterBitSignal signal =
      SignalFactory.createFromRegister(diagram, sdk, label, address, bit,
                                       displayFilters, pioNum, smNum);
    return addSignal(signal);
  }

  public Signal addSignal(final Diagram diagram, final String label,
                          final int address, final int bit)
    throws IOException
  {
    return addSignal(diagram, label, address, bit, null, -1, -1);
  }

  public Signal addSignal(final Diagram diagram,
                          final String label, final int address)
    throws IOException
  {
    return addSignal(diagram, label, address, 31, 0);
  }

  public Signal addSignal(final Diagram diagram,
                          final String label, final int address,
                          final List<SignalFilter> displayFilters,
                          final int pioNum, final int smNum)
    throws IOException
  {
    return addSignal(diagram, label, address, 31, 0,
                     displayFilters, pioNum, smNum);
  }

  public Signal addSignal(final Diagram diagram, final int address)
    throws IOException
  {
    return addSignal(diagram, null, address);
  }

  public Signal addSignal(final Diagram diagram, final int address,
                          final List<SignalFilter> displayFilters,
                          final int pioNum, final int smNum)
    throws IOException
  {
    return addSignal(diagram, null, address, displayFilters, pioNum, smNum);
  }

  public void resetSignals()
  {
    for (final Signal signal : address2internalSignal.values()) {
      signal.reset();
    }
    for (final Signal signal : signals) {
      signal.reset();
    }
    signalSize = 0;
  }

  private void appendRecordToSignals() throws IOException
  {
    for (final Signal signal : address2internalSignal.values()) {
      signal.record();
    }
    for (final Signal signal : signals) {
      if (signal.getVisible()) {
        signal.record();
      }
    }
    signalSize++;
  }

  private void checkForUpdate() throws IOException
  {
    try {
      final long wallClock = sdk.getWallClock();
      if (wallClock == this.wallClock) {
        // nothing to update
      } else {
        if (wallClock != this.wallClock + 1) {
          // discontinuity in time => restart view
          resetSignals();
        }
        appendRecordToSignals();
      }
      this.wallClock = wallClock;
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
      sdk.triggerCyclePhase0(true);
      checkForUpdate();
      sdk.triggerCyclePhase1(true);
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
    resetSignals();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
