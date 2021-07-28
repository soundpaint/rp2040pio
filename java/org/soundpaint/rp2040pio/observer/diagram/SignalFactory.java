/*
 * @(#)SignalFactory.java 1.00 21/02/12
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
import java.util.List;
import java.util.Objects;
import org.soundpaint.rp2040pio.Bit;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Configuration of a timing diagram.
 */
public class SignalFactory
{
  public static CycleRuler createRuler(final String label)
  {
    return new CycleRuler(label);
  }

  public static ClockSignal createClockSignal(final String label)
  {
    return new ClockSignal(label);
  }

  private static String createSignalLabel(final SDK sdk, final String label,
                                          final int address,
                                          final String bitRange)
    throws IOException
  {
    return
      (label != null) ? label : sdk.getLabelForAddress(address) + bitRange;
  }

  private static String createSignalLabel(final SDK sdk, final String label,
                                          final int address, final int bit)
    throws IOException
  {
    return createSignalLabel(sdk, label, address, "_" + bit);
  }

  private static String createSignalLabel(final SDK sdk, final String label,
                                          final int address,
                                          final int msb, final int lsb)
    throws IOException
  {
    return createSignalLabel(sdk, label, address,
                             ((lsb == 0) && (msb == 31) ? "" :
                              ("_" + msb +
                               (lsb != msb ? ":" + lsb : ""))));

  }

  public static RegisterBitSignal
    createFromRegister(final Diagram diagram, final SDK sdk, final String label,
                       final int address, final int bit,
                       final List<SignalFilter> displayFilters,
                       final int pioNum, final int smNum)
    throws IOException
  {
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(sdk);
    Objects.requireNonNull(label);
    Constants.checkBit(bit);
    final String signalLabel = createSignalLabel(sdk, label, address, bit);
    final SignalRendering.SignalParams signalParams =
      new SignalRendering.SignalParams(diagram, sdk, label, address,
                                       bit, bit, displayFilters, pioNum, smNum);
    return new RegisterBitSignal(signalParams);
  }

  public static RegisterIntSignal
    createInternal(final Diagram diagram, final SDK sdk,
                   final String label, final int address)
    throws IOException
  {
    final SignalRendering.SignalParams signalParams =
      new SignalRendering.SignalParams(diagram, sdk, label, address, 31, 0,
                                       null, -1, -1);
    return new RegisterIntSignal(SignalRendering.Unsigned, signalParams);
  }

  public static RegisterIntSignal
    createFromRegister(final Diagram diagram, final SDK sdk, final String label,
                       final int address, final int msb, final int lsb,
                       final SignalRendering valueRendering,
                       final List<SignalFilter> displayFilters,
                       final int pioNum, final int smNum)
    throws IOException
  {
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(sdk);
    Objects.requireNonNull(label);
    final String signalLabel = createSignalLabel(sdk, label, address, msb, lsb);
    final SignalRendering.SignalParams signalParams =
      new SignalRendering.SignalParams(diagram, sdk, signalLabel, address,
                                       msb, lsb, displayFilters, pioNum, smNum);
    return new RegisterIntSignal(valueRendering, signalParams);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
