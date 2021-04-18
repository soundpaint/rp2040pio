/*
 * @(#)GPIO.java 1.00 21/01/31
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

import java.io.PrintStream;

/**
 * General-Purpose Set of 32 Peripheral I/O Terminals
 */
public class GPIO implements Constants
{
  private class Terminal
  {
    private int num;
    private GPIO_Function function;

    private Terminal()
    {
    }

    private Terminal(final int num)
    {
      Constants.checkGpioPin(num, "GPIO port");
      this.num = num;
    }

    public void reset()
    {
      function = GPIO_Function.NULL;
    }
  }

  private final PrintStream console;
  private final PIO pio0;
  private final PIO pio1;
  private final Terminal[] terminals;
  private int regINPUT_SYNC_BYPASS; // bits 0…31 of INPUT_SYNC_BYPASS
                                    // (contents currently ignored)

  private GPIO()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public GPIO(final PrintStream console, final MasterClock masterClock)
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    if (masterClock == null) {
      throw new NullPointerException("masterClock");
    }
    this.console = console;
    pio0 = new PIO(0, console, masterClock, this);
    pio1 = new PIO(1, console, masterClock, this);
    terminals = new Terminal[GPIO_NUM];
    for (int port = 0; port < terminals.length; port++) {
      terminals[port] = new Terminal(port);
    }
    reset();
  }

  public void reset()
  {
    for (int port = 0; port < terminals.length; port++) {
      terminals[port].reset();
    }
  }

  public PIO getPIO0() { return pio0; }

  public PIO getPIO1() { return pio1; }

  /**
   * Set GPIOx_CTRL_FUNCSEL to 6 (for PIO0) or 7 (for PIO1), see
   * Sect. 2.19.2. "Function Select" of RP2040 datasheet for details.
   */
  public void setFunction(final int gpio, final GPIO_Function fn)
  {
    Constants.checkGpioPin(gpio, "GPIO port");
    if (fn == null) {
      throw new NullPointerException("fn");
    }
    terminals[gpio].function = fn;
  }

  private GPIO_Function getFunction(final int gpio)
  {
    Constants.checkGpioPin(gpio, "GPIO port");
    return terminals[gpio].function;
  }

  public void setCTRL(final int gpio, final int value,
                      final int mask, final boolean xor)
  {
    final int oldValue = getCTRL(gpio);
    final int newValue =
      Constants.hwSetBits(oldValue, value, mask, xor);
    setFunction(gpio,
                GPIO_Function.fromValue((newValue &
                                         IO_BANK0_GPIO0_CTRL_FUNCSEL_BITS) >>
                                        IO_BANK0_GPIO0_CTRL_FUNCSEL_LSB,
                                        GPIO_Function.NULL));
  }

  public int getCTRL(final int gpio)
  {
    return getFunction(gpio).getValue() << IO_BANK0_GPIO0_CTRL_FUNCSEL_LSB;
  }

  public int getSTATUS(final int gpio)
  {
    return
      (getIrqToProc(gpio).getValue() << IO_BANK0_GPIO0_STATUS_IRQTOPROC_LSB) |
      (getIrqFromPad(gpio).getValue() <<
       IO_BANK0_GPIO0_STATUS_IRQFROMPAD_LSB) |
      (getInToPeri(gpio).getValue() << IO_BANK0_GPIO0_STATUS_INTOPERI_LSB) |
      (getInFromPad(gpio).getValue() << IO_BANK0_GPIO0_STATUS_INFROMPAD_LSB) |
      (getOeToPad(gpio).getValue() << IO_BANK0_GPIO0_STATUS_OETOPAD_LSB) |
      (getOeFromPeripheral(gpio).getValue() <<
       IO_BANK0_GPIO0_STATUS_OEFROMPERI_LSB) |
      (getOutToPad(gpio).getValue() << IO_BANK0_GPIO0_STATUS_OUTTOPAD_LSB) |
      (getOutFromPeripheral(gpio).getValue() <<
       IO_BANK0_GPIO0_STATUS_OUTFROMPERI_LSB);
  }

  public Bit getIrqToProc(final int gpio)
  {
    // not implemented by this emulator
    return Bit.LOW;
  }

  public Bit getIrqFromPad(final int gpio)
  {
    // not implemented by this emulator
    return Bit.LOW;
  }

  public Bit getInToPeri(final int gpio)
  {
    // not implemented by this emulator
    return Bit.LOW;
  }

  public Bit getInFromPad(final int gpio)
  {
    // not implemented by this emulator
    return Bit.LOW;
  }

  public Direction getOeToPad(final int gpio)
  {
    switch (getFunction(gpio)) {
    case XIP:
    case SPI:
    case UART:
    case I2C:
    case PWM:
    case SIO:
      // not implemented by this emulator
      return Direction.IN;
    case PIO0:
      return pio0.getOeToPad(gpio);
    case PIO1:
      return pio1.getOeToPad(gpio);
    case GPCK:
    case USB:
      // not implemented by this emulator
      return Direction.IN;
    case NULL:
      return Direction.IN;
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }

  public Direction getOeFromPeripheral(final int gpio)
  {
    switch (getFunction(gpio)) {
    case XIP:
    case SPI:
    case UART:
    case I2C:
    case PWM:
    case SIO:
      // not implemented by this emulator
      return Direction.IN;
    case PIO0:
      return pio0.getOeFromPeripheral(gpio);
    case PIO1:
      return pio1.getOeFromPeripheral(gpio);
    case GPCK:
    case USB:
      // not implemented by this emulator
      return Direction.IN;
    case NULL:
      return Direction.IN;
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }

  public Bit getOutToPad(final int gpio)
  {
    switch (getFunction(gpio)) {
    case XIP:
    case SPI:
    case UART:
    case I2C:
    case PWM:
    case SIO:
      // not implemented by this emulator
      return Bit.LOW;
    case PIO0:
      return pio0.getOutToPad(gpio);
    case PIO1:
      return pio1.getOutToPad(gpio);
    case GPCK:
    case USB:
      // not implemented by this emulator
      return Bit.LOW;
    case NULL:
      return Bit.LOW;
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }

  public Bit getOutFromPeripheral(final int gpio)
  {
    switch (getFunction(gpio)) {
    case XIP:
    case SPI:
    case UART:
    case I2C:
    case PWM:
    case SIO:
      // not implemented by this emulator
      return Bit.LOW;
    case PIO0:
      return pio0.getOutFromPeripheral(gpio);
    case PIO1:
      return pio1.getOutFromPeripheral(gpio);
    case GPCK:
    case USB:
      // not implemented by this emulator
      return Bit.LOW;
    case NULL:
      return Bit.LOW;
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }

  public void setInputSyncByPass(final int bits, final int mask,
                                 final boolean xor)
  {
    regINPUT_SYNC_BYPASS =
      (mask & (xor ? regINPUT_SYNC_BYPASS ^ bits : bits)) |
      (~mask & regINPUT_SYNC_BYPASS);
  }

  public int getInputSyncByPass()
  {
    return regINPUT_SYNC_BYPASS;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
