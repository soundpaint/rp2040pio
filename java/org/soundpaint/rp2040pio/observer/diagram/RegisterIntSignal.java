/*
 * @(#)RegisterIntSignal.java 1.00 21/07/23
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
import java.util.Objects;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.sdk.SDK;

public class RegisterIntSignal extends ValuedSignal<Integer>
{
  private final SDK sdk;
  private final Supplier<Boolean> displayFilter;
  private final int address;
  private final int msb;
  private final int lsb;

  public RegisterIntSignal(final SDK sdk,
                           final String label,
                           final Supplier<Boolean> displayFilter,
                           final int address,
                           final int msb,
                           final int lsb)
  {
    this(sdk, label, displayFilter, address, msb, lsb, null);
  }

  /**
   * @param changeInfoGetter If set to &lt;code&gt;null&lt;/code&gt;,
   * then a change is assumed only when the updated value changes.
   */
  public RegisterIntSignal(final SDK sdk,
                           final String label,
                           final Supplier<Boolean> displayFilter,
                           final int address,
                           final int msb,
                           final int lsb,
                           final Supplier<Boolean> changeInfoGetter)
  {
    super(label, changeInfoGetter);
    Objects.requireNonNull(sdk);
    this.sdk = sdk;
    this.displayFilter = displayFilter;
    this.address = address;
    this.msb = msb;
    this.lsb = lsb;
  }

  public Supplier<Boolean> getDisplayFilter()
  {
    return displayFilter;
  }

  public int getAddress()
  {
    return address;
  }

  public int getMsb()
  {
    return msb;
  }

  public int getLsb()
  {
    return lsb;
  }

  @Override
  protected Integer sampleValue() throws IOException
  {
    if ((displayFilter != null) && (!displayFilter.get()))
      return null;
    return sdk.readAddress(address, msb, lsb);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
