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

    public char toChar()
    {
      final Bit level = getLevel(num);
      final Direction direction = getDirection(num);
      return level != null ? level.toChar(direction) : '?';
    }
  }

  private final Terminal[] terminals;
  private int regINPUT_SYNC_BYPASS; // bits 0..31 of INPUT_SYNC_BYPASS
                                    // (contents currently ignored)

  public GPIO()
  {
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
    final int oldValue =
      getFunction(gpio).ordinal() << IO_BANK0_GPIO0_CTRL_FUNCSEL_LSB;
    final int newValue =
      Constants.hwSetBits(oldValue, value, mask, xor);
    setFunction(gpio,
                GPIO_Function.fromValue((newValue &
                                         IO_BANK0_GPIO0_CTRL_FUNCSEL_BITS) >>
                                        IO_BANK0_GPIO0_CTRL_FUNCSEL_LSB));
  }

  public void setLevel(final int port, final Bit level)
  {
    // TODO: Clarify what happens when writing to a GPIO with pin
    // direction set to IN.
    if (level == null) {
      throw new NullPointerException("value");
    }
    Constants.checkGpioPin(port, "GPIO port");
    // TODO: GPIO Mapping: Ouput priority.
    //terminals[port].level = level;
  }

  public Bit getLevel(final int port)
  {
    // TODO: Clarify what happens when reading from a GPIO with pin
    // direction set to OUT.
    Constants.checkGpioPin(port, "GPIO port");
    // TODO: GPIO Mapping: Ouput priority.
    return Bit.LOW; //terminals[port].level;
  }

  public void setDirection(final int port, final Direction direction)
  {
    if (direction == null) {
      throw new NullPointerException("direction");
    }
    Constants.checkGpioPin(port, "GPIO port");
    // TODO: GPIO Mapping: Ouput priority.
    //terminals[port].direction = direction;
  }

  public Direction getDirection(final int port)
  {
    Constants.checkGpioPin(port, "GPIO port");
    // TODO: GPIO Mapping: Ouput priority.
    return Direction.IN; //terminals[port].direction;
  }

  public int getPins(final int base, final int count)
  {
    Constants.checkGpioPin(base, "GPIO pin base");
    Constants.checkGpioPinsCount(count, "GPIO pin count");
    int pins = 0;
    for (int pin = 0; pin < count; pin++) {
      pins = (pins << 0x1) | getLevel((base - pin - 1) & 0x1f).getValue();
    }
    return pins;
  }

  public void setPins(final int pins, final int base, final int count)
  {
    Constants.checkGpioPin(base, "GPIO pin base");
    Constants.checkGpioPinsCount(count, "GPIO pin count");
    for (int pin = 0; pin < count; pin++) {
      setLevel((base + pin) & 0x1f,  Bit.fromValue((pins >>> pin) & 0x1));
    }
  }

  public int getPinDirs(final int base, final int count)
  {
    Constants.checkGpioPin(base, "GPIO pin base");
    Constants.checkGpioPinsCount(count, "GPIO pin count");
    int pinDirs = 0;
    for (int pin = 0; pin < count; pin++) {
      pinDirs =
        (pinDirs << 0x1) | getDirection((base - pin - 1) & 0x1f).getValue();
    }
    return pinDirs;
  }

  public void setPinDirs(final int pinDirs, final int base, final int count)
  {
    Constants.checkGpioPin(base, "GPIO pin base");
    Constants.checkGpioPinsCount(count, "GPIO pin count");
    for (int pin = 0; pin < count; pin++) {
      setDirection((base + pin) & 0x1f,
                   Direction.fromValue((pinDirs >>> pin) & 0x1));
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

  public String asBitArrayDisplay()
  {
    final StringBuffer s = new StringBuffer();
    for (final Terminal terminal : terminals) {
      s.append(terminal.toChar());
      if ((s.length() + 1) % 9 == 0) s.append(' ');
    }
    return s.toString();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
