/*
 * @(#)GPIOPadsBank0RegistersImpl.java 1.00 21/03/20
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
public class GPIOPadsBank0RegistersImpl extends GPIOPadsBank0Registers
{
  private final GPIO gpio;

  public GPIOPadsBank0RegistersImpl(final GPIO gpio)
  {
    if (gpio == null) {
      throw new NullPointerException("gpio");
    }
    this.gpio = gpio;
  }

  public GPIO getGPIO() { return gpio; }

  @Override
  public void writeRegister(final int regNum, final int value,
                            final int mask, final boolean xor)
  {
    checkRegNum(regNum);
    final Regs register = REGS[regNum];
    switch (register) {
    case VOLTAGE_SELECT:
      // TODO
      break;
    case GPIO0:
    case GPIO1:
    case GPIO2:
    case GPIO3:
    case GPIO4:
    case GPIO5:
    case GPIO6:
    case GPIO7:
    case GPIO8:
    case GPIO9:
    case GPIO10:
    case GPIO11:
    case GPIO12:
    case GPIO13:
    case GPIO14:
    case GPIO15:
    case GPIO16:
    case GPIO17:
    case GPIO18:
    case GPIO19:
    case GPIO20:
    case GPIO21:
    case GPIO22:
    case GPIO23:
    case GPIO24:
    case GPIO25:
    case GPIO26:
    case GPIO27:
    case GPIO28:
    case GPIO29:
      // TODO
      break;
    case SWCLK:
      // TODO
      break;
    case SWD:
      // TODO
      break;
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }

  @Override
  public synchronized int readRegister(final int regNum)
  {
    checkRegNum(regNum);
    final Regs register = REGS[regNum];
    switch (register) {
    case VOLTAGE_SELECT:
      return 0; // TODO
    case GPIO0:
    case GPIO1:
    case GPIO2:
    case GPIO3:
    case GPIO4:
    case GPIO5:
    case GPIO6:
    case GPIO7:
    case GPIO8:
    case GPIO9:
    case GPIO10:
    case GPIO11:
    case GPIO12:
    case GPIO13:
    case GPIO14:
    case GPIO15:
    case GPIO16:
    case GPIO17:
    case GPIO18:
    case GPIO19:
    case GPIO20:
    case GPIO21:
    case GPIO22:
    case GPIO23:
    case GPIO24:
    case GPIO25:
    case GPIO26:
    case GPIO27:
    case GPIO28:
    case GPIO29:
      return 0; // TODO
    case SWCLK:
      return 0; // TODO
    case SWD:
      return 0; // TODO
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
