/*
 * @(#)GPIOIOBank0Registers.java 1.00 21/03/20
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
public abstract class GPIOIOBank0Registers extends AbstractRegisters
  implements Constants
{
  public enum Regs {
    GPIO0_STATUS,
    GPIO0_CTRL,
    GPIO1_STATUS,
    GPIO1_CTRL,
    GPIO2_STATUS,
    GPIO2_CTRL,
    GPIO3_STATUS,
    GPIO3_CTRL,
    GPIO4_STATUS,
    GPIO4_CTRL,
    GPIO5_STATUS,
    GPIO5_CTRL,
    GPIO6_STATUS,
    GPIO6_CTRL,
    GPIO7_STATUS,
    GPIO7_CTRL,
    GPIO8_STATUS,
    GPIO8_CTRL,
    GPIO9_STATUS,
    GPIO9_CTRL,
    GPIO10_STATUS,
    GPIO10_CTRL,
    GPIO11_STATUS,
    GPIO11_CTRL,
    GPIO12_STATUS,
    GPIO12_CTRL,
    GPIO13_STATUS,
    GPIO13_CTRL,
    GPIO14_STATUS,
    GPIO14_CTRL,
    GPIO15_STATUS,
    GPIO15_CTRL,
    GPIO16_STATUS,
    GPIO16_CTRL,
    GPIO17_STATUS,
    GPIO17_CTRL,
    GPIO18_STATUS,
    GPIO18_CTRL,
    GPIO19_STATUS,
    GPIO19_CTRL,
    GPIO20_STATUS,
    GPIO20_CTRL,
    GPIO21_STATUS,
    GPIO21_CTRL,
    GPIO22_STATUS,
    GPIO22_CTRL,
    GPIO23_STATUS,
    GPIO23_CTRL,
    GPIO24_STATUS,
    GPIO24_CTRL,
    GPIO25_STATUS,
    GPIO25_CTRL,
    GPIO26_STATUS,
    GPIO26_CTRL,
    GPIO27_STATUS,
    GPIO27_CTRL,
    GPIO28_STATUS,
    GPIO28_CTRL,
    GPIO29_STATUS,
    GPIO29_CTRL,
    INTR0,
    INTR1,
    INTR2,
    INTR3,
    PROC0_INTE0,
    PROC0_INTE1,
    PROC0_INTE2,
    PROC0_INTE3,
    PROC0_INTF0,
    PROC0_INTF1,
    PROC0_INTF2,
    PROC0_INTF3,
    PROC0_INTS0,
    PROC0_INTS1,
    PROC0_INTS2,
    PROC0_INTS3,
    PROC1_INTE0,
    PROC1_INTE1,
    PROC1_INTE2,
    PROC1_INTE3,
    PROC1_INTF0,
    PROC1_INTF1,
    PROC1_INTF2,
    PROC1_INTF3,
    PROC1_INTS0,
    PROC1_INTS1,
    PROC1_INTS2,
    PROC1_INTS3,
    DORMANT_WAKE_INTE0,
    DORMANT_WAKE_INTE1,
    DORMANT_WAKE_INTE2,
    DORMANT_WAKE_INTE3,
    DORMANT_WAKE_INTF0,
    DORMANT_WAKE_INTF1,
    DORMANT_WAKE_INTF2,
    DORMANT_WAKE_INTF3,
    DORMANT_WAKE_INTS0,
    DORMANT_WAKE_INTS1,
    DORMANT_WAKE_INTS2,
    DORMANT_WAKE_INTS3;
  }

  protected static final Regs[] REGS = Regs.values();

  @Override
  @SuppressWarnings("unchecked")
  protected <T extends Enum<T>> T[] getRegs() { return (T[])REGS; }

  protected static final int GPIO_DATA_SIZE =
    Regs.GPIO1_STATUS.ordinal() - Regs.GPIO0_STATUS.ordinal();

  protected static final int PROC_INT_DATA_SIZE =
    Regs.PROC1_INTE0.ordinal() - Regs.PROC0_INTE0.ordinal();

  public static int getAddress(final GPIOIOBank0Registers.Regs register)
  {
    if (register == null) {
      throw new NullPointerException("register");
    }
    return IO_BANK0_BASE + 0x4 * register.ordinal();
  }

  public static int getGPIOAddress(final int gpioNum,
                                   final GPIOIOBank0Registers.Regs register)
  {
    Constants.checkGpioPin(gpioNum, "GPIO pin number");
    if (register == null) {
      throw new NullPointerException("register");
    }
    switch (register) {
    case GPIO0_STATUS:
    case GPIO0_CTRL:
      break; // ok
    default:
      throw new IllegalArgumentException("register not one of GPIO0_*: " +
                                         register);
    }
    return
      IO_BANK0_BASE + 0x4 * (register.ordinal() + gpioNum * GPIO_DATA_SIZE);
  }

  public static int getIntr(final int intrNum)
  {
    Constants.checkIntrNum(intrNum, "INTR number");
    return IO_BANK0_BASE + 0x4 * (Regs.INTR0.ordinal() + intrNum);
  }

  public static int getProcIntE(final int pioNum, final int inteNum)
  {
    Constants.checkPioNum(pioNum, "PIO number");
    Constants.checkIntrNum(inteNum, "INTE number");
    final int regsOffs = pioNum * PROC_INT_DATA_SIZE + inteNum;
    return IO_BANK0_BASE + 0x4 * (Regs.PROC0_INTE0.ordinal() + regsOffs);
  }

  public static int getProcIntF(final int pioNum, final int intfNum)
  {
    Constants.checkPioNum(pioNum, "PIO number");
    Constants.checkIntrNum(intfNum, "INTF number");
    final int regsOffs = pioNum * PROC_INT_DATA_SIZE + intfNum;
    return IO_BANK0_BASE + 0x4 * (Regs.PROC0_INTF0.ordinal() + regsOffs);
  }

  public static int getProcIntS(final int pioNum, final int intsNum)
  {
    Constants.checkPioNum(pioNum, "PIO number");
    Constants.checkIntrNum(intsNum, "INTS number");
    final int regsOffs = pioNum * PROC_INT_DATA_SIZE + intsNum;
    return IO_BANK0_BASE + 0x4 * (Regs.PROC0_INTS0.ordinal() + regsOffs);
  }

  public static int getDormantWakeIntE(final int inteNum)
  {
    Constants.checkIntrNum(inteNum, "Dormant Wake INTE number");
    return IO_BANK0_BASE + 0x4 * (Regs.DORMANT_WAKE_INTE0.ordinal() + inteNum);
  }

  public static int getDormantWakeIntF(final int intfNum)
  {
    Constants.checkIntrNum(intfNum, "Dormant Wake INTF number");
    return IO_BANK0_BASE + 0x4 * (Regs.DORMANT_WAKE_INTF0.ordinal() + intfNum);
  }

  public static int getDormantWakeIntS(final int intsNum)
  {
    Constants.checkIntrNum(intsNum, "Dormant Wake INTS number");
    return IO_BANK0_BASE + 0x4 * (Regs.DORMANT_WAKE_INTS0.ordinal() + intsNum);
  }

  public GPIOIOBank0Registers()
  {
    super(IO_BANK0_BASE, (short)REGS.length);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
