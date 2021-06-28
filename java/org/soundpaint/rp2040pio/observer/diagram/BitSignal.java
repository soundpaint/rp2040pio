/*
 * @(#)BitSignal.java 1.00 21/02/12
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
import org.soundpaint.rp2040pio.Bit;

public class BitSignal extends ValuedSignal<Bit>
{
  public BitSignal(final String label,
                   final Supplier<Bit> valueGetter)
  {
    super(label, valueGetter);
  }

  public BitSignal(final String label,
                   final Supplier<Bit> valueGetter,
                   final Supplier<Boolean> changeInfoGetter)
  {
    super(label, valueGetter, changeInfoGetter);
  }

  @Override
  public boolean isBinary() { return true; }

  public Boolean asBoolean()
  {
    final Bit value = getValue();
    return value != null ? (value == Bit.HIGH) : null;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
