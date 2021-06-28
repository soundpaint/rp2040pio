/*
 * @(#)ValuedSignal.java 1.00 21/02/12
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

import java.util.function.Supplier;

public class ValuedSignal<T> extends AbstractSignal<T>
{
  private Supplier<T> valueGetter;
  private Supplier<Boolean> changeInfoGetter;

  public ValuedSignal(final String label,
                      final Supplier<T> valueGetter)
  {
    this(label, valueGetter, null);
  }

  /**
   * @param changeInfoGetter If set to &lt;code&gt;null&lt;/code&gt;,
   * then a change is assumed only when the updated value changes.
   */
  public ValuedSignal(final String label,
                      final Supplier<T> valueGetter,
                      final Supplier<Boolean> changeInfoGetter)
  {
    super(label);
    if (valueGetter == null) {
      throw new NullPointerException("valueGetter");
    }
    this.valueGetter = valueGetter;
    this.changeInfoGetter = changeInfoGetter;
  }

  @Override
  public void record()
  {
    final boolean enforceChanged =
      changeInfoGetter != null ? changeInfoGetter.get() : false;
    record(valueGetter.get(), enforceChanged);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
