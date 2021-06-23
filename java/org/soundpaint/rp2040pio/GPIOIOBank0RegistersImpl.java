/*
 * @(#)GPIOIOBank0RegistersImpl.java 1.00 21/03/20
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
 * Facade to the internal GPIO IO Bank 0 subsystem.  The layout of
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
public class GPIOIOBank0RegistersImpl extends GPIOIOBank0Registers
{
  public static final int GPIO_DATA_SIZE =
    Regs.GPIO1_STATUS.ordinal() - Regs.GPIO0_STATUS.ordinal();

  public static final int PROC_INT_DATA_SIZE =
    Regs.PROC1_INTE0.ordinal() - Regs.PROC0_INTE0.ordinal();

  private final GPIO gpio;

  public GPIOIOBank0RegistersImpl(final GPIO gpio)
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
    case GPIO0_STATUS:
    case GPIO1_STATUS:
    case GPIO2_STATUS:
    case GPIO3_STATUS:
    case GPIO4_STATUS:
    case GPIO5_STATUS:
    case GPIO6_STATUS:
    case GPIO7_STATUS:
    case GPIO8_STATUS:
    case GPIO9_STATUS:
    case GPIO10_STATUS:
    case GPIO11_STATUS:
    case GPIO12_STATUS:
    case GPIO13_STATUS:
    case GPIO14_STATUS:
    case GPIO15_STATUS:
    case GPIO16_STATUS:
    case GPIO17_STATUS:
    case GPIO18_STATUS:
    case GPIO19_STATUS:
    case GPIO20_STATUS:
    case GPIO21_STATUS:
    case GPIO22_STATUS:
    case GPIO23_STATUS:
    case GPIO24_STATUS:
    case GPIO25_STATUS:
    case GPIO26_STATUS:
    case GPIO27_STATUS:
    case GPIO28_STATUS:
    case GPIO29_STATUS:
      break; // read-only address
    case GPIO0_CTRL:
    case GPIO1_CTRL:
    case GPIO2_CTRL:
    case GPIO3_CTRL:
    case GPIO4_CTRL:
    case GPIO5_CTRL:
    case GPIO6_CTRL:
    case GPIO7_CTRL:
    case GPIO8_CTRL:
    case GPIO9_CTRL:
    case GPIO10_CTRL:
    case GPIO11_CTRL:
    case GPIO12_CTRL:
    case GPIO13_CTRL:
    case GPIO14_CTRL:
    case GPIO15_CTRL:
    case GPIO16_CTRL:
    case GPIO17_CTRL:
    case GPIO18_CTRL:
    case GPIO19_CTRL:
    case GPIO20_CTRL:
    case GPIO21_CTRL:
    case GPIO22_CTRL:
    case GPIO23_CTRL:
    case GPIO24_CTRL:
    case GPIO25_CTRL:
    case GPIO26_CTRL:
    case GPIO27_CTRL:
    case GPIO28_CTRL:
    case GPIO29_CTRL:
      gpio.setCTRL((regNum - Regs.GPIO0_CTRL.ordinal()) / GPIO_DATA_SIZE,
                   value, mask, xor);
      break;
    case INTR0:
    case INTR1:
    case INTR2:
    case INTR3:
      // TODO
      break;
    case PROC0_INTE0:
    case PROC0_INTE1:
    case PROC0_INTE2:
    case PROC0_INTE3:
    case PROC1_INTE0:
    case PROC1_INTE1:
    case PROC1_INTE2:
    case PROC1_INTE3:
      // TODO
      break;
    case PROC0_INTF0:
    case PROC0_INTF1:
    case PROC0_INTF2:
    case PROC0_INTF3:
    case PROC1_INTF0:
    case PROC1_INTF1:
    case PROC1_INTF2:
    case PROC1_INTF3:
      // TODO
      break;
    case PROC0_INTS0:
    case PROC0_INTS1:
    case PROC0_INTS2:
    case PROC0_INTS3:
    case PROC1_INTS0:
    case PROC1_INTS1:
    case PROC1_INTS2:
    case PROC1_INTS3:
      // TODO
      break;
    case DORMANT_WAKE_INTE0:
    case DORMANT_WAKE_INTE1:
    case DORMANT_WAKE_INTE2:
    case DORMANT_WAKE_INTE3:
      // TODO
      break;
    case DORMANT_WAKE_INTF0:
    case DORMANT_WAKE_INTF1:
    case DORMANT_WAKE_INTF2:
    case DORMANT_WAKE_INTF3:
      // TODO
      break;
    case DORMANT_WAKE_INTS0:
    case DORMANT_WAKE_INTS1:
    case DORMANT_WAKE_INTS2:
    case DORMANT_WAKE_INTS3:
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
    case GPIO0_STATUS:
    case GPIO1_STATUS:
    case GPIO2_STATUS:
    case GPIO3_STATUS:
    case GPIO4_STATUS:
    case GPIO5_STATUS:
    case GPIO6_STATUS:
    case GPIO7_STATUS:
    case GPIO8_STATUS:
    case GPIO9_STATUS:
    case GPIO10_STATUS:
    case GPIO11_STATUS:
    case GPIO12_STATUS:
    case GPIO13_STATUS:
    case GPIO14_STATUS:
    case GPIO15_STATUS:
    case GPIO16_STATUS:
    case GPIO17_STATUS:
    case GPIO18_STATUS:
    case GPIO19_STATUS:
    case GPIO20_STATUS:
    case GPIO21_STATUS:
    case GPIO22_STATUS:
    case GPIO23_STATUS:
    case GPIO24_STATUS:
    case GPIO25_STATUS:
    case GPIO26_STATUS:
    case GPIO27_STATUS:
    case GPIO28_STATUS:
    case GPIO29_STATUS:
      return
        gpio.getSTATUS((regNum - Regs.GPIO0_STATUS.ordinal()) / GPIO_DATA_SIZE);
    case GPIO0_CTRL:
    case GPIO1_CTRL:
    case GPIO2_CTRL:
    case GPIO3_CTRL:
    case GPIO4_CTRL:
    case GPIO5_CTRL:
    case GPIO6_CTRL:
    case GPIO7_CTRL:
    case GPIO8_CTRL:
    case GPIO9_CTRL:
    case GPIO10_CTRL:
    case GPIO11_CTRL:
    case GPIO12_CTRL:
    case GPIO13_CTRL:
    case GPIO14_CTRL:
    case GPIO15_CTRL:
    case GPIO16_CTRL:
    case GPIO17_CTRL:
    case GPIO18_CTRL:
    case GPIO19_CTRL:
    case GPIO20_CTRL:
    case GPIO21_CTRL:
    case GPIO22_CTRL:
    case GPIO23_CTRL:
    case GPIO24_CTRL:
    case GPIO25_CTRL:
    case GPIO26_CTRL:
    case GPIO27_CTRL:
    case GPIO28_CTRL:
    case GPIO29_CTRL:
      return
        gpio.getCTRL((regNum - Regs.GPIO0_CTRL.ordinal()) / GPIO_DATA_SIZE);
    case INTR0:
    case INTR1:
    case INTR2:
    case INTR3:
      return 0; // TODO
    case PROC0_INTE0:
    case PROC0_INTE1:
    case PROC0_INTE2:
    case PROC0_INTE3:
    case PROC1_INTE0:
    case PROC1_INTE1:
    case PROC1_INTE2:
    case PROC1_INTE3:
      return 0; // TODO
    case PROC0_INTF0:
    case PROC0_INTF1:
    case PROC0_INTF2:
    case PROC0_INTF3:
    case PROC1_INTF0:
    case PROC1_INTF1:
    case PROC1_INTF2:
    case PROC1_INTF3:
      return 0; // TODO
    case PROC0_INTS0:
    case PROC0_INTS1:
    case PROC0_INTS2:
    case PROC0_INTS3:
    case PROC1_INTS0:
    case PROC1_INTS1:
    case PROC1_INTS2:
    case PROC1_INTS3:
      return 0; // TODO
    case DORMANT_WAKE_INTE0:
    case DORMANT_WAKE_INTE1:
    case DORMANT_WAKE_INTE2:
    case DORMANT_WAKE_INTE3:
      return 0; // TODO
    case DORMANT_WAKE_INTF0:
    case DORMANT_WAKE_INTF1:
    case DORMANT_WAKE_INTF2:
    case DORMANT_WAKE_INTF3:
      return 0; // TODO
    case DORMANT_WAKE_INTS0:
    case DORMANT_WAKE_INTS1:
    case DORMANT_WAKE_INTS2:
    case DORMANT_WAKE_INTS3:
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
