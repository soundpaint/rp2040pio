/*
 * @(#)PIOGPIO.java 1.00 21/03/19
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
public class PIOGPIO implements Constants
{
  private static class State
  {
    private Direction direction;
    private Bit level;

    public void reset()
    {
      direction = Direction.IN;
      level = Bit.LOW;
    }

    public char toChar()
    {
      if (direction == Direction.IN) {
        return level == Bit.HIGH ? '¹' : '⁰';
      } else {
        return level == Bit.HIGH ? '₁' : '₀';
      }
    }
  }

  private final GPIO gpio;
  private final State[] states;

  private PIOGPIO()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public PIOGPIO(final GPIO gpio)
  {
    if (gpio == null) {
      throw new NullPointerException("gpio");
    }
    this.gpio = gpio;
    states = new State[GPIO_NUM];
    for (int port = 0; port < states.length; port++) {
      states[port] = new State();
    }
    reset();
  }

  public void reset()
  {
    for (int port = 0; port < states.length; port++) {
      states[port].reset();
    }
  }

  public GPIO getGPIO() { return gpio; }

  public void setLevel(final int port, final Bit level)
  {
    if (level == null) {
      throw new NullPointerException("level");
    }
    Constants.checkGpioPin(port, "GPIO port");
    states[port].level = level;
  }

  public Bit getLevel(final int port)
  {
    // TODO: Clarify what happens when reading from a GPIO with pin
    // direction set to OUT.
    Constants.checkGpioPin(port, "GPIO port");
    return states[port].level;
  }

  public void setDirection(final int port, final Direction direction)
  {
    if (direction == null) {
      throw new NullPointerException("direction");
    }
    Constants.checkGpioPin(port, "GPIO port");
    states[port].direction = direction;
  }

  public Direction getDirection(final int port)
  {
    Constants.checkGpioPin(port, "GPIO port");
    return states[port].direction;
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
      setLevel((base + pin) & 0x1f, Bit.fromValue((pins >>> pin) & 0x1));
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

  public Bit getOutToPad(final int gpio)
  {
    final Bit beforeRegisterOverride = getOutFromPeripheral(gpio);
    // TODO: Check if we need to implement register override.
    final Bit afterRegisterOverride = beforeRegisterOverride;
    return afterRegisterOverride;
  }

  public Bit getOutFromPeripheral(final int gpio)
  {
    return getLevel(gpio);
  }

  public String asBitArrayDisplay()
  {
    final StringBuffer s = new StringBuffer();
    for (final State state : states) {
      s.append(state.toChar());
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
