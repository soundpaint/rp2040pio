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
  private final GPIO gpio;
  private final PinState[] states;

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
    states = new PinState[GPIO_NUM];
    reset();
  }

  public void reset()
  {
    for (int gpioNum = 0; gpioNum < states.length; gpioNum++) {
      states[gpioNum] = PinState.IN_LOW;
    }
  }

  public GPIO getGPIO() { return gpio; }

  public void setLevel(final int gpioNum, final Bit level)
  {
    if (level == null) {
      throw new NullPointerException("level");
    }
    Constants.checkGpioPin(gpioNum, "GPIO pin number");
    final PinState pinState = states[gpioNum];
    states[gpioNum] = PinState.fromValues(pinState.getDirection(), level);
  }

  public Bit getLevel(final int gpioNum)
  {
    // TODO: Clarify what happens when reading from a GPIO with pin
    // direction set to OUT.
    Constants.checkGpioPin(gpioNum, "GPIO pin number");
    return states[gpioNum].getLevel();
  }

  public void setDirection(final int gpioNum, final Direction direction)
  {
    if (direction == null) {
      throw new NullPointerException("direction");
    }
    Constants.checkGpioPin(gpioNum, "GPIO pin number");
    final PinState pinState = states[gpioNum];
    states[gpioNum] = PinState.fromValues(direction, pinState.getLevel());
  }

  public Direction getDirection(final int gpioNum)
  {
    Constants.checkGpioPin(gpioNum, "GPIO pin number");
    return states[gpioNum].getDirection();
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

  public void setPinsMask(final int pins, final int mask, final boolean xor)
  {
    for (int gpioNum = 0; gpioNum < GPIO_NUM; gpioNum++) {
      final int oldLevel = getLevel(gpioNum).getValue();
      final int pin = pins >>> gpioNum & 0x1;
      final int maskBit = mask >>> gpioNum & 0x1;
      final int newLevel = Constants.hwSetBits(oldLevel, pin, maskBit, xor);
      setLevel(gpioNum, Bit.fromValue(newLevel));
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

  public void setPinDirsMask(final int pinDirs, final int mask,
                             final boolean xor)
  {
    for (int gpioNum = 0; gpioNum < GPIO_NUM; gpioNum++) {
      final int oldDirection = getDirection(gpioNum).getValue();
      final int pinDir = pinDirs >>> gpioNum & 0x1;
      final int maskBit = mask >>> gpioNum & 0x1;
      final int newDirection = Constants.hwSetBits(oldDirection, pinDir,
                                                   maskBit, xor);
      setDirection(gpioNum, Direction.fromValue(newDirection));
    }
  }

  public Direction getOeToPad(final int gpioNum)
  {
    final Direction beforeRegisterOverride = getOeFromPeripheral(gpioNum);
    // TODO: Check if we need to implement register override.
    final Direction afterRegisterOverride = beforeRegisterOverride;
    return afterRegisterOverride;
  }

  public Direction getOeFromPeripheral(final int gpioNum)
  {
    return getDirection(gpioNum);
  }

  public Bit getOutToPad(final int gpioNum)
  {
    final Bit beforeRegisterOverride = getOutFromPeripheral(gpioNum);
    // TODO: Check if we need to implement register override.
    final Bit afterRegisterOverride = beforeRegisterOverride;
    return afterRegisterOverride;
  }

  public Bit getOutFromPeripheral(final int gpioNum)
  {
    return getLevel(gpioNum);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
