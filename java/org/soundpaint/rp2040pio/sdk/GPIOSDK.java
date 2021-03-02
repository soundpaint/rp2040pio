/*
 * @(#)GPIOSDK.java 1.00 21/03/02
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
package org.soundpaint.rp2040pio.sdk;

import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.Bit;
import org.soundpaint.rp2040pio.GPIO;

/**
 * Minimal subset of GPIO SDK Interface, just enough to provide all
 * required GPIO-related functionality for PIO SDK.
 *
 * TODO: GPIOSDK should, just as PIOSDK does, use a memory-mapped I/O
 * interface (see class Registers) rather than directly storing and
 * accessing GPIO subsystem.
 */
public class GPIOSDK implements Constants
{
  private final GPIO gpio;

  public GPIOSDK(final GPIO gpio)
  {
    if (gpio == null) {
      throw new NullPointerException("gpio");
    }
    this.gpio = gpio;
  }

  public void setFunction(final int pin, final GPIO_Function fn)
  {
    if (pin < 0) {
      throw new IllegalArgumentException("gpio pin < 0: " + pin);
    }
    if (pin > 31) {
      throw new IllegalArgumentException("gpio pin > 31: " + pin);
    }
    synchronized(gpio) {
      gpio.setFunction(pin, fn);
      gpio.setBit(pin, Bit.LOW);
      // TODO: Also clear the input/output/irq override bits.
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
