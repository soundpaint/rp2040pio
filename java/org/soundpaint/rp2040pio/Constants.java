/*
 * @(#)Constants.java 1.00 21/02/27
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

public interface Constants
{
  public static final int GPIO_NUM = 32;
  public static final int MEMORY_SIZE = 32;
  public static final int FIFO_DEPTH = 4;
  public static final int SM_COUNT = 4;

  // address map
  public static final int PIO0_BASE = 0x50200000;
  public static final int PIO1_BASE = 0x50300000;

  // PIO registers addressing
  public static final int CTRL_CLKDIV_RESTART_LSB = 8;
  public static final int CTRL_CLKDIV_RESTART_BITS = 0x00000f00;
  public static final int CTRL_SM_RESTART_LSB = 4;
  public static final int CTRL_SM_RESTART_BITS = 0x000000f0;
  public static final int CTRL_SM_ENABLE_LSB = 0;
  public static final int CTRL_SM_ENABLE_BITS = 0x0000000f;
  public static final int FSTAT_TXEMPTY_LSB = 24;
  public static final int FSTAT_TXFULL_LSB = 16;
  public static final int FSTAT_RXEMPTY_LSB = 8;
  public static final int FSTAT_RXFULL_LSB = 0;
  public static final int FDEBUG_TXSTALL_LSB = 24;
  public static final int FDEBUG_TXOVER_LSB = 16;
  public static final int FDEBUG_RXUNDER_LSB = 8;
  public static final int FDEBUG_RXSTALL_LSB = 0;
  public static final int FLEVEL_RX1_LSB = 12;
  public static final int FLEVEL_RX1_BITS = 0x0000f000;
  public static final int FLEVEL_TX1_LSB = 8;
  public static final int FLEVEL_TX1_BITS = 0x00000f00;
  public static final int FLEVEL_RX0_LSB = 4;
  public static final int FLEVEL_RX0_BITS = 0x000000f0;
  public static final int FLEVEL_TX0_LSB = 0;
  public static final int FLEVEL_TX0_BITS = 0x0000000f;
  public static final int SM0_CLKDIV_INT_LSB = 16;
  public static final int SM0_CLKDIV_FRAC_LSB = 8;
  public static final int SM0_EXECCTRL_EXEC_STALLED_BITS = 0x80000000;
  public static final int SM0_EXECCTRL_WRAP_TOP_LSB = 12;
  public static final int SM0_EXECCTRL_WRAP_TOP_BITS = 0x0001f000;
  public static final int SM0_EXECCTRL_WRAP_BOTTOM_LSB = 7;
  public static final int SM0_EXECCTRL_WRAP_BOTTOM_BITS = 0x00000f80;
  public static final int SM0_SHIFTCTRL_FJOIN_RX_BITS = 0x80000000;
  public static final int SM0_SHIFTCTRL_FJOIN_TX_BITS = 0x40000000;
  public static final int SM0_SHIFTCTRL_AUTOPULL_BITS = 0x00020000;
  public static final int SM0_PINCTRL_SET_COUNT_LSB = 26;
  public static final int SM0_PINCTRL_SET_COUNT_BITS = 0x1c000000;
  public static final int SM0_PINCTRL_OUT_COUNT_LSB = 20;
  public static final int SM0_PINCTRL_OUT_COUNT_BITS = 0x03f00000;
  public static final int SM0_PINCTRL_IN_BASE_LSB = 15;
  public static final int SM0_PINCTRL_IN_BASE_BITS = 0x000f8000;
  public static final int SM0_PINCTRL_SIDESET_BASE_LSB = 10;
  public static final int SM0_PINCTRL_SIDESET_BASE_BITS = 0x00007c00;
  public static final int SM0_PINCTRL_SET_BASE_LSB = 5;
  public static final int SM0_PINCTRL_SET_BASE_BITS = 0x000003e0;
  public static final int SM0_PINCTRL_OUT_BASE_LSB = 0;
  public static final int SM0_PINCTRL_OUT_BASE_BITS = 0x0000001f;

  public enum GPIO_Function {
    XIP(0, "xip"),
    SPI(1, "spi"),
    UART(2, "uart"),
    I2C(3, "i2c"),
    PWM(4, "pwm"),
    SIO(5, "sio"),
    PIO0(6, "pio0"),
    PIO1(7, "pio1"),
    GPCK(8, "gpck"),
    USB(9, "usb"),
    NULL(15, "null");

    private final int value;
    private final String label;

    private GPIO_Function(final int value, final String label)
    {
      this.value = value;
      this.label = label;
    }

    private int getValue() { return value; }

    @Override
    public String toString()
    {
      return label;
    }
  };

  public static void checkSmNum(final int smNum)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("smNum < 0: " + smNum);
    }
    if (smNum > SM_COUNT - 1) {
      throw new IllegalArgumentException("smNum > " + (SM_COUNT - 1) + ": " +
                                         smNum);
    }
  }

  /**
   * Functionally equivalent replacement for GNU GCC's built-in
   * function __builtin_ctz().
   *
   * @return The number of trailing 0-bits in x, starting at
   * the least significant bit position. If x is 0, the result is
   * undefined.
   */
  public static int ctz(final int x)
  {
    int count = 0;
    int shifted = x;
    while (((shifted & 0x1) == 0x0) && (count < 32)) {
      shifted >>>= 1;
      count++;
    }
    return count;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
