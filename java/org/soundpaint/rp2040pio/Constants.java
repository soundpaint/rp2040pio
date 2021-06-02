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
  public static final String PROGRAM_ID = "RP2040 PIO Emulator";
  public static final String VERSION_ID = "0.1";
  public static final String COPYRIGHT_TAG_LINE =
    String.format("Copyright © 2021 by Jürgen Reuter, Karlsruhe, Germany.%n");
  public static final String CMD_LINE_COPYRIGHT_NOTICE =
    String.format(COPYRIGHT_TAG_LINE +
                  "%n" +
                  "This software comes with ABSOLUTELY NO WARRANTY;%n" +
                  "for details please look at the license file that you%n" +
                  "should have received together with this software.%n" +
                  "This is free software, and you are welcome to%n" +
                  "redistribute it under certain conditions;%n" +
                  "for details please look at the license file that you%n" +
                  "should have received together with this software.%n");
  public static final String MONITOR_COPYRIGHT_NOTICE =
    String.format(COPYRIGHT_TAG_LINE +
                  "%n" +
                  "This software comes with ABSOLUTELY NO WARRANTY;%n" +
                  "for details please look at the license file that you%n" +
                  "should have received together with this software.%n" +
                  "This is free software, and you are welcome to%n" +
                  "redistribute it under certain conditions;%n" +
                  "for details please look at the license file that you%n" +
                  "should have received together with this software.%n");
  public static final String GUI_COPYRIGHT_NOTICE =
    String.format(COPYRIGHT_TAG_LINE +
                  "%n" +
                  "This software comes with ABSOLUTELY NO WARRANTY;%n" +
                  "for details type `Alt-L' in the main window.%n" +
                  "This is free software, and you are welcome to%n" +
                  "redistribute it under certain conditions;%n" +
                  "type `Alt-L' in the main window for details.%n");

  public static String getEmulatorId()
  {
    return PROGRAM_ID;
  }

  public static String getEmulatorVersion()
  {
    return VERSION_ID;
  }

  public static String getCmdLineCopyrightNotice()
  {
    return CMD_LINE_COPYRIGHT_NOTICE;
  }

  public static String getMonitorCopyrightNotice()
  {
    return MONITOR_COPYRIGHT_NOTICE;
  }

  public static String getGuiCopyrightNotice()
  {
    return GUI_COPYRIGHT_NOTICE;
  }

  public static String getEmulatorIdAndVersionWithOs()
  {
    return
      getEmulatorId() +
      " Version " + getEmulatorVersion() +
      " / jvm " + System.getProperty("java.version") +
      " / " + System.getProperty("java.class.version");
  }

  public static final int GPIO_NUM = 32;
  public static final int MEMORY_SIZE = 32;
  public static final int FIFO_DEPTH = 4;
  public static final int PIO_NUM = 2;
  public static final int SM_COUNT = 4;
  public static final int INTR_NUM = 4;
  public static final int DEFAULT_FREQUENCY = 1000000000;

  // address map
  public static final int IO_BANK0_BASE = 0x40014000;
  public static final int PADS_BANK0_BASE = 0x4001c000;
  public static final int PIO0_BASE = 0x50200000;
  public static final int PIO0_EMU_BASE = PIO0_BASE + 0x08000000;
  public static final int PIO1_BASE = 0x50300000;
  public static final int PIO1_EMU_BASE = PIO1_BASE + 0x08000000;
  public static final int EMULATOR_BASE = 0x58000000;

  public static int getPIOBaseAddress(final int pioNum)
  {
    checkPioNum(pioNum, "PIO index number");
    return pioNum == 0 ? PIO0_BASE : PIO1_BASE;
  }

  public static int getPIOEmuBaseAddress(final int pioNum)
  {
    checkPioNum(pioNum, "PIO index number");
    return pioNum == 0 ? PIO0_EMU_BASE : PIO1_EMU_BASE;
  }

  // Emulator registers addressing
  public static final int PICO_PWR_UP_VALUE = 0xa55a5aa5;

  // GPIO registers addressing
  public static final int IO_BANK0_GPIO0_CTRL_IRQOVER_LSB = 28;
  public static final int IO_BANK0_GPIO0_CTRL_IRQOVER_BITS = 0x30000000;
  public static final int IO_BANK0_GPIO0_CTRL_INOVER_LSB = 16;
  public static final int IO_BANK0_GPIO0_CTRL_INOVER_BITS = 0x00030000;
  public static final int IO_BANK0_GPIO0_CTRL_OEOVER_LSB = 12;
  public static final int IO_BANK0_GPIO0_CTRL_OEOVER_BITS = 0x00003000;
  public static final int IO_BANK0_GPIO0_CTRL_OUTOVER_LSB = 8;
  public static final int IO_BANK0_GPIO0_CTRL_OUTOVER_BITS = 0x00000300;
  public static final int IO_BANK0_GPIO0_CTRL_FUNCSEL_LSB = 0;
  public static final int IO_BANK0_GPIO0_CTRL_FUNCSEL_BITS = 0x0000001f;
  public static final int IO_BANK0_GPIO0_STATUS_IRQTOPROC_LSB = 26;
  public static final int IO_BANK0_GPIO0_STATUS_IRQTOPROC_BITS = 0x04000000;
  public static final int IO_BANK0_GPIO0_STATUS_IRQFROMPAD_LSB = 24;
  public static final int IO_BANK0_GPIO0_STATUS_IRQFROMPAD_BITS = 0x01000000;
  public static final int IO_BANK0_GPIO0_STATUS_INTOPERI_LSB = 19;
  public static final int IO_BANK0_GPIO0_STATUS_INTOPERI_BITS = 0x00080000;
  public static final int IO_BANK0_GPIO0_STATUS_INFROMPAD_LSB = 17;
  public static final int IO_BANK0_GPIO0_STATUS_INFROMPAD_BITS = 0x00020000;
  public static final int IO_BANK0_GPIO0_STATUS_OETOPAD_LSB = 13;
  public static final int IO_BANK0_GPIO0_STATUS_OETOPAD_BITS = 0x00002000;
  public static final int IO_BANK0_GPIO0_STATUS_OEFROMPERI_LSB = 12;
  public static final int IO_BANK0_GPIO0_STATUS_OEFROMPERI_BITS = 0x00001000;
  public static final int IO_BANK0_GPIO0_STATUS_OUTTOPAD_LSB = 9;
  public static final int IO_BANK0_GPIO0_STATUS_OUTTOPAD_BITS = 0x00000200;
  public static final int IO_BANK0_GPIO0_STATUS_OUTFROMPERI_LSB = 8;
  public static final int IO_BANK0_GPIO0_STATUS_OUTFROMPERI_BITS = 0x00000100;
  public static final int PADS_BANK0_GPIO0_IE_LSB = 6;
  public static final int PADS_BANK0_GPIO0_IE_BITS = 0x00000040;

  // PIO registers addressing
  public static final int CTRL_CLKDIV_RESTART_LSB = 8;
  public static final int CTRL_CLKDIV_RESTART_BITS = 0x00000f00;
  public static final int CTRL_SM_RESTART_LSB = 4;
  public static final int CTRL_SM_RESTART_BITS = 0x000000f0;
  public static final int CTRL_SM_ENABLE_LSB = 0;
  public static final int CTRL_SM_ENABLE_BITS = 0x0000000f;
  public static final int FSTAT_TXEMPTY_LSB = 24;
  public static final int FSTAT_TXEMPTY_BITS = 0x0f000000;
  public static final int FSTAT_TXFULL_LSB = 16;
  public static final int FSTAT_TXFULL_BITS = 0x000f0000;
  public static final int FSTAT_RXEMPTY_LSB = 8;
  public static final int FSTAT_RXEMPTY_BITS = 0x00000f00;
  public static final int FSTAT_RXFULL_LSB = 0;
  public static final int FSTAT_RXFULL_BITS = 0x0000000f;
  public static final int FDEBUG_TXSTALL_LSB = 24;
  public static final int FDEBUG_TXSTALL_BITS = 0x0f000000;
  public static final int FDEBUG_TXOVER_LSB = 16;
  public static final int FDEBUG_TXOVER_BITS = 0x000f0000;
  public static final int FDEBUG_RXUNDER_LSB = 8;
  public static final int FDEBUG_RXUNDER_BITS = 0x00000f00;
  public static final int FDEBUG_RXSTALL_LSB = 0;
  public static final int FDEBUG_RXSTALL_BITS = 0x0000000f;
  public static final int FLEVEL_RX1_LSB = 12;
  public static final int FLEVEL_RX1_BITS = 0x0000f000;
  public static final int FLEVEL_TX1_LSB = 8;
  public static final int FLEVEL_TX1_BITS = 0x00000f00;
  public static final int FLEVEL_RX0_LSB = 4;
  public static final int FLEVEL_RX0_BITS = 0x000000f0;
  public static final int FLEVEL_TX0_LSB = 0;
  public static final int FLEVEL_TX0_BITS = 0x0000000f;
  public static final int SM0_CLKDIV_INT_LSB = 16;
  public static final int SM0_CLKDIV_INT_BITS = 0xffff0000;
  public static final int SM0_CLKDIV_FRAC_LSB = 8;
  public static final int SM0_CLKDIV_FRAC_BITS = 0x0000ff00;
  public static final int SM0_EXECCTRL_EXEC_STALLED_LSB = 31;
  public static final int SM0_EXECCTRL_EXEC_STALLED_BITS = 0x80000000;
  public static final int SM0_EXECCTRL_SIDE_EN_LSB = 30;
  public static final int SM0_EXECCTRL_SIDE_EN_BITS = 0x40000000;
  public static final int SM0_EXECCTRL_SIDE_PINDIR_LSB = 29;
  public static final int SM0_EXECCTRL_SIDE_PINDIR_BITS = 0x20000000;
  public static final int SM0_EXECCTRL_JMP_PIN_LSB = 24;
  public static final int SM0_EXECCTRL_JMP_PIN_BITS = 0x1f000000;
  public static final int SM0_EXECCTRL_OUT_EN_SEL_LSB = 19;
  public static final int SM0_EXECCTRL_OUT_EN_SEL_BITS = 0x00f80000;
  public static final int SM0_EXECCTRL_INLINE_OUT_EN_LSB = 18;
  public static final int SM0_EXECCTRL_INLINE_OUT_EN_BITS = 0x00040000;
  public static final int SM0_EXECCTRL_OUT_STICKY_LSB = 17;
  public static final int SM0_EXECCTRL_OUT_STICKY_BITS = 0x00020000;
  public static final int SM0_EXECCTRL_WRAP_TOP_LSB = 12;
  public static final int SM0_EXECCTRL_WRAP_TOP_BITS = 0x0001f000;
  public static final int SM0_EXECCTRL_WRAP_BOTTOM_LSB = 7;
  public static final int SM0_EXECCTRL_WRAP_BOTTOM_BITS = 0x00000f80;
  public static final int SM0_EXECCTRL_STATUS_SEL_LSB = 4;
  public static final int SM0_EXECCTRL_STATUS_SEL_BITS = 0x00000010;
  public static final int SM0_EXECCTRL_STATUS_N_LSB = 0;
  public static final int SM0_EXECCTRL_STATUS_N_BITS = 0x0000000f;
  public static final int SM0_SHIFTCTRL_FJOIN_RX_LSB = 31;
  public static final int SM0_SHIFTCTRL_FJOIN_RX_BITS = 0x80000000;
  public static final int SM0_SHIFTCTRL_FJOIN_TX_LSB = 30;
  public static final int SM0_SHIFTCTRL_FJOIN_TX_BITS = 0x40000000;
  public static final int SM0_SHIFTCTRL_PULL_THRESH_LSB = 25;
  public static final int SM0_SHIFTCTRL_PULL_THRESH_BITS = 0x3e000000;
  public static final int SM0_SHIFTCTRL_PUSH_THRESH_LSB = 20;
  public static final int SM0_SHIFTCTRL_PUSH_THRESH_BITS = 0x01f00000;
  public static final int SM0_SHIFTCTRL_OUT_SHIFTDIR_LSB = 19;
  public static final int SM0_SHIFTCTRL_OUT_SHIFTDIR_BITS = 0x00080000;
  public static final int SM0_SHIFTCTRL_IN_SHIFTDIR_LSB = 18;
  public static final int SM0_SHIFTCTRL_IN_SHIFTDIR_BITS = 0x00040000;
  public static final int SM0_SHIFTCTRL_AUTOPULL_LSB = 17;
  public static final int SM0_SHIFTCTRL_AUTOPULL_BITS = 0x00020000;
  public static final int SM0_SHIFTCTRL_AUTOPUSH_LSB = 16;
  public static final int SM0_SHIFTCTRL_AUTOPUSH_BITS = 0x00010000;
  public static final int SM0_PINCTRL_SIDESET_COUNT_LSB = 29;
  public static final int SM0_PINCTRL_SIDESET_COUNT_BITS = 0xe0000000;
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

  public static final int REGISTER_SERVER_DEFAULT_PORT_NUMBER = 2040;

  // Instruction Origin
  public static final int INSTR_ORIGIN_UNKNOWN = -3;
  public static final int INSTR_ORIGIN_EXECD = -2;
  public static final int INSTR_ORIGIN_FORCED = -1;
  public static final int INSTR_ORIGIN_MEMORY = 0;

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

    private static final GPIO_Function[] values = GPIO_Function.values();

    private final int value;
    private final String label;

    private GPIO_Function(final int value, final String label)
    {
      this.value = value;
      this.label = label;
    }

    public int getValue() { return value; }

    public static GPIO_Function fromValue(final int value)
    {
      if ((value >= 0) && (value <= 9)) return values[value];
      if (value == 15) return NULL;
      throw new IllegalArgumentException("value: " + value);
    }

    public static GPIO_Function fromValue(final int value,
                                          final GPIO_Function defaultValue)
    {
      try {
        return fromValue(value);
      } catch (final IllegalArgumentException e) {
        return defaultValue;
      }
    }

    @Override
    public String toString()
    {
      return label;
    }
  };

  public static void checkBit(final int bit)
  {
    if (bit < 0) {
      throw new IllegalArgumentException("bit < 0: " + bit);
    }
    if (bit > 31) {
      throw new IllegalArgumentException("bit > 31: " + bit);
    }
  }

  public static void checkMSBLSB(final int msb, final int lsb)
  {
    if (lsb < 0) {
      throw new IllegalArgumentException("lsb < 0: " + lsb);
    }
    if (msb > 31) {
      throw new IllegalArgumentException("msb > 31: " + msb);
    }
    if (lsb > msb) {
      throw new IllegalArgumentException("lsb > msb: " + lsb + " > " + msb);
    }
  }

  public static void checkFIFOAddr(final int address, final String label)
  {
    if (address < 0) {
      throw new IllegalArgumentException(label + " < 0" + address);
    }
    if (address > (2 * FIFO_DEPTH) - 1) {
      throw new IllegalArgumentException(label + " > " +
                                         ((2 * FIFO_DEPTH) - 1) + ":" +
                                         address);
    }
  }

  public static void checkGpioPin(final int pin, final String label)
  {
    if (pin < 0) {
      throw new IllegalArgumentException(label + " < 0: " + pin);
    }
    if (pin > GPIO_NUM - 1) {
      throw new IllegalArgumentException(label + " > " + (GPIO_NUM - 1) + ": " +
                                         pin);
    }
  }

  public static void checkGpioPinsCount(final int count, final String label)
  {
    if (count < 0) {
      throw new IllegalArgumentException(label + " < 0: " + count);
    }
    if (count > GPIO_NUM) {
      throw new IllegalArgumentException(label + " > " + GPIO_NUM + ": " +
                                         count);
    }
  }

  public static void checkPioNum(final int pioNum, final String label)
  {
    if (pioNum < 0) {
      throw new IllegalArgumentException(label + " < 0: " + pioNum);
    }
    if (pioNum > PIO_NUM - 1) {
      throw new IllegalArgumentException(label + " > " + (PIO_NUM - 1) + ": " +
                                         pioNum);
    }
  }

  public static void checkSmMemAddr(final int address, final String label)
  {
    if (address < 0) {
      throw new IllegalArgumentException(label + " < 0: " + address);
    }
    if (address > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException(label + " > " +
                                         (MEMORY_SIZE - 1) + ": " +
                                         address);
    }
  }

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

  public static void checkIntrNum(final int intrNum, final String label)
  {
    if (intrNum < 0) {
      throw new IllegalArgumentException(label + " < 0: " + intrNum);
    }
    if (intrNum > INTR_NUM - 1) {
      throw new IllegalArgumentException(label + " > " + (INTR_NUM - 1) + ": " +
                                         intrNum);
    }
  }

  public static int checkBitCount(final int bitCount, final String label)
  {
    if (bitCount < 0) {
      throw new IllegalArgumentException(label + " < 0: " + bitCount);
    }
    if (bitCount > 31) {
      throw new IllegalArgumentException(label + " > 31: " + bitCount);
    }
    return bitCount > 0 ? bitCount : 32;
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

  public static int hwSetBits(final int oldBits, final int newBits,
                              final int mask, final boolean xor)
  {
    return (mask & (xor ? oldBits ^ newBits : newBits)) | (~mask & oldBits);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
