/*
 * @(#)GPIOPadsBank0Registers.java 1.00 21/03/20
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
package org.soundpaint.rp2040pio;

/**
 * Facade to the internal GPIO Pads Bank 0 subsystem.  The layout of
 * registers follows the list of registers in Sect. 2.19.6 of the
 * RP2040 datasheet.  The facade is in particular intended for use by
 * the SDK.
 *
 * Note: This class implements only a subset of the RP2040 GPIO set of
 * registers, focussing on those registers that are relevant for PIO
 * simulation.  All GPIO registers specified by the RP2040 datasheet
 * are addressable, but writing to non-relevant registers or register
 * bits will have no effect, and reading from non-relevant registers
 * or register bits will return a constant value of 0.
 */
public abstract class GPIOPadsBank0Registers extends AbstractRegisters
  implements Constants
{
  public enum Regs {
    VOLTAGE_SELECT,
    GPIO0,
    GPIO1,
    GPIO2,
    GPIO3,
    GPIO4,
    GPIO5,
    GPIO6,
    GPIO7,
    GPIO8,
    GPIO9,
    GPIO10,
    GPIO11,
    GPIO12,
    GPIO13,
    GPIO14,
    GPIO15,
    GPIO16,
    GPIO17,
    GPIO18,
    GPIO19,
    GPIO20,
    GPIO21,
    GPIO22,
    GPIO23,
    GPIO24,
    GPIO25,
    GPIO26,
    GPIO27,
    GPIO28,
    GPIO29,
    SWCLK,
    SWD;
  }

  protected static final Regs[] REGS = Regs.values();

  public static String getLabelForRegister(final int regNum)
  {
    return REGS[regNum].toString();
  }

  public static int getAddress(final GPIOPadsBank0Registers.Regs register)
  {
    if (register == null) {
      throw new NullPointerException("register");
    }
    return PADS_BANK0_BASE + 0x4 * register.ordinal();
  }

  public static int getGPIOAddress(final int gpioNum)
  {
    Constants.checkGpioPin(gpioNum, "GPIO pin number");
    return PADS_BANK0_BASE + 0x4 * (Regs.GPIO0.ordinal() + gpioNum);
  }

  public GPIOPadsBank0Registers()
  {
    super(PADS_BANK0_BASE, (short)REGS.length);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
