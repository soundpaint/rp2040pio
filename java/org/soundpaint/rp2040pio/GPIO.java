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
public class GPIO
{
  public enum Direction {
    IN(0, "in"),
    OUT(1, "out");

    private final int value;
    private final String label;

    private Direction(final int value, final String label)
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

  private static Direction directionFromValue(final int value)
  {
    if (value == 0)
      return Direction.IN;
    else if (value == 1)
      return Direction.OUT;
    throw new IllegalArgumentException("value is neither 0 nor 1: " + value);
  }

  private static Bit bitFromValue(final int value)
  {
    if (value == 0)
      return Bit.LOW;
    else if (value == 1)
      return Bit.HIGH;
    throw new IllegalArgumentException("value is neither 0 nor 1: " + value);
  }

  private static class Terminal
  {
    private Direction direction;
    private Bit value;
  }

  private final Terminal[] terminals;

  public GPIO()
  {
    terminals = new Terminal[32];
    for (int port = 0; port < terminals.length; port++) {
      final Terminal terminal = new Terminal();
      terminal.direction = Direction.IN;
      terminal.value = Bit.LOW;
      terminals[port] = terminal;
    }
  }

  public void setBit(final int port, final Bit value)
  {
    if (value == null) {
      throw new NullPointerException("value");
    }
    if ((port < 0) || (port >= terminals.length)) {
      throw new IllegalArgumentException("port out of range: " + port);
    }
    terminals[port].value = value;
  }

  public Bit getBit(final int port)
  {
    if ((port < 0) || (port >= terminals.length)) {
      throw new IllegalArgumentException("port out of range: " + port);
    }
    return terminals[port].value;
  }

  public void setDirection(final int port, final Direction direction)
  {
    if (direction == null) {
      throw new NullPointerException("direction");
    }
    if ((port < 0) || (port >= terminals.length)) {
      throw new IllegalArgumentException("port out of range: " + port);
    }
    terminals[port].direction = direction;
  }

  public Direction getDirection(final int port)
  {
    if ((port < 0) || (port >= terminals.length)) {
      throw new IllegalArgumentException("port out of range: " + port);
    }
    return terminals[port].direction;
  }

  public int getPins(final int base, final int count)
  {
    if (base < 0) {
      throw new IllegalArgumentException("GPIO pin base < 0: " + base);
    }
    if (base > 31) {
      throw new IllegalArgumentException("GPIO pin base > 31: " + base);
    }
    if (count < 0) {
      throw new IllegalArgumentException("GPIO pin count < 0: " + count);
    }
    if (count > 31) {
      throw new IllegalArgumentException("GPIO pin count > 31: " + count);
    }
    int pins = 0;
    for (int pin = 0; pin < count; pin++) {
      pins = (pins << 0x1) |
        getBit((base + count) & 0x1f).getValue();
    }
    return pins;
  }

  public void setPins(final int pins, final int base, final int count)
  {
    if (base < 0) {
      throw new IllegalArgumentException("GPIO pin base < 0: " + base);
    }
    if (base > 31) {
      throw new IllegalArgumentException("GPIO pin base > 31: " + base);
    }
    if (count < 0) {
      throw new IllegalArgumentException("GPIO pin count < 0: " + count);
    }
    if (count > 31) {
      throw new IllegalArgumentException("GPIO pin count > 31: " + count);
    }
    for (int pin = 0; pin < count; pin++) {
      setBit((base + count) & 0x1f, bitFromValue((pins >>> count) & 0x1));
    }
  }

  public void setPinDirs(final int pinDirs, final int base, final int count)
  {
    if (base < 0) {
      throw new IllegalArgumentException("GPIO pin base < 0: " + base);
    }
    if (base > 31) {
      throw new IllegalArgumentException("GPIO pin base > 31: " + base);
    }
    if (count < 0) {
      throw new IllegalArgumentException("GPIO pin count < 0: " + count);
    }
    if (count > 31) {
      throw new IllegalArgumentException("GPIO pin count > 31: " + count);
    }
    for (int pin = 0; pin < count; pin++) {
      setDirection((base + count) & 0x1f,
                   directionFromValue((pinDirs >>> count) & 0x1));
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
