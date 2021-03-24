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
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.GPIOPadsBank0Registers;
import org.soundpaint.rp2040pio.MasterClock;

/**
 * Minimal subset of GPIO SDK Interface, just enough to provide all
 * required GPIO-related functionality for PIO SDK.
 */
public class GPIOSDK implements Constants
{
  private final GPIOIOBank0Registers ioBank0Registers;
  private final GPIOPadsBank0Registers padsBank0Registers;

  public GPIOSDK(final MasterClock masterClock,
                 final GPIO gpio,
                 final int gpioIOBank0BaseAddress,
                 final int gpioPadsBank0BaseAddress)
  {
    this(new GPIOIOBank0Registers(masterClock, gpio, gpioIOBank0BaseAddress),
         new GPIOPadsBank0Registers(masterClock, gpio,
                                    gpioPadsBank0BaseAddress));
  }

  public GPIOSDK(final GPIOIOBank0Registers ioBank0Registers,
                 final GPIOPadsBank0Registers padsBank0Registers)
  {
    if (ioBank0Registers == null) {
      throw new NullPointerException("ioBank0Registers");
    }
    if (padsBank0Registers == null) {
      throw new NullPointerException("padsBank0Registers");
    }
    this.ioBank0Registers = ioBank0Registers;
    this.padsBank0Registers = padsBank0Registers;
  }

  public GPIOIOBank0Registers getIOBank0Registers()
  {
    return ioBank0Registers;
  }

  public GPIOPadsBank0Registers getPadsBank0Registers()
  {
    return padsBank0Registers;
  }

  public void setFunction(final int pin, final GPIO_Function fn)
  {
    Constants.checkGpioPin(pin, "GPIO pin number");

    final int padsGpioAddress = padsBank0Registers.getGPIOAddress(pin);
    final int padsValues = Bit.LOW.getValue() << PADS_BANK0_GPIO0_IE_LSB;
    final int padsWriteMask = PADS_BANK0_GPIO0_IE_BITS;
    padsBank0Registers.hwWriteMasked(padsGpioAddress,
                                     padsValues, padsWriteMask);

    final GPIOIOBank0Registers.Regs ioBank0Reg =
      GPIOIOBank0Registers.Regs.GPIO0_CTRL;
    final int ioGpioAddress = ioBank0Registers.getGPIOAddress(ioBank0Reg, pin);
    final int ioValues = fn.getValue() << IO_BANK0_GPIO0_CTRL_FUNCSEL_LSB;
    final int ioWriteMask = IO_BANK0_GPIO0_CTRL_FUNCSEL_BITS;
    ioBank0Registers.hwWriteMasked(ioGpioAddress, ioValues, ioWriteMask);
  }

  public String asBitArrayDisplay()
  {
    // return gpio.asBitArrayDisplay(); // TODO
    throw new InternalError("not yet implemented");
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
