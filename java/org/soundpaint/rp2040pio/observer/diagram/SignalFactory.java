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
import java.util.function.BiFunction;
import java.util.function.Supplier;
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
                                          final int address, final int bit)
    throws IOException
  {
    return
      (label != null) ? label : sdk.getLabelForAddress(address) + "_" + bit;
  }

  private static String createSignalLabel(final SDK sdk, final String label,
                                          final int address,
                                          final int msb, final int lsb)
    throws IOException
  {
    if (label != null) return label;
    return
      sdk.getLabelForAddress(address) +
      ((lsb == 0) && (msb == 31) ? "" :
       ("_" + msb +
        (lsb != msb ? ":" + lsb : "")));
  }

  public static BitSignal createFromRegister(final SDK sdk, final String label,
                                             final int address, final int bit)
    throws IOException
  {
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    Constants.checkBit(bit);
    final String signalLabel = createSignalLabel(sdk, label, address, bit);
    final Supplier<Bit> supplier = () -> {
      try {
        return Bit.fromValue(sdk.readAddress(address, bit, bit));
      } catch (final IOException e) {
        // TODO: console.println(e.getMessage());
        return null;
      }
    };
    return new BitSignal(signalLabel, supplier);
  }

  public static ValuedSignal<Integer>
    createInternal(final SDK sdk, final String label, final int address)
    throws IOException
  {
    return
      new ValuedSignal<Integer>(label,
                                () -> {
                                  try {
                                    return sdk.readAddress(address);
                                  } catch (final IOException e) {
                                    // TODO: console.println(e.getMessage());
                                    return null;
                                  }});
  }

  public static ValuedSignal<Integer>
    createFromRegister(final SDK sdk, final String label,
                       final int address)
    throws IOException
  {
    return createFromRegister(sdk, label, address, 31, 0);
  }

  public static ValuedSignal<Integer>
    createFromRegister(final SDK sdk, final String label,
                       final int address, final int msb, final int lsb)
    throws IOException
  {
    return createFromRegister(sdk, label, address, msb, lsb, null);
  }

  public static ValuedSignal<Integer>
    createFromRegister(final SDK sdk, final String label,
                       final int address, final int msb, final int lsb,
                       final Supplier<Boolean> displayFilter)
    throws IOException
  {
    return createFromRegister(sdk, label, address, msb, lsb,
                              (cycle, value) -> String.format("%x", value),
                              (cycle, value) -> String.format("0x%x", value),
                              displayFilter);
  }

  public static ValuedSignal<Integer>
    createFromRegister(final SDK sdk, final String label,
                       final int address, final int msb, final int lsb,
                       final BiFunction<Integer, Integer, String> valueRenderer,
                       final BiFunction<Integer, Integer, String> toolTipRenderer,
                       final Supplier<Boolean> displayFilter)
    throws IOException
  {
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    Constants.checkMSBLSB(msb, lsb);
    final String signalLabel = createSignalLabel(sdk, label, address, msb, lsb);
    final Supplier<Integer> supplier = () -> {
      if ((displayFilter != null) && (!displayFilter.get()))
        return null;
      try {
        return sdk.readAddress(address, msb, lsb);
      } catch (final IOException e) {
        // TODO: console.println(e.getMessage());
        return null;
      }
    };
    final ValuedSignal<Integer> intSignal =
      new ValuedSignal<Integer>(signalLabel, supplier);
    intSignal.setRenderer((cycle, intValue) ->
                          valueRenderer.apply(cycle, intValue));
    if (toolTipRenderer != null) {
      intSignal.setToolTipTexter((cycle, intValue) ->
                                 toolTipRenderer.apply(cycle, intValue));
    }
    return intSignal;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
