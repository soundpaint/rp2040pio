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
    IN, OUT
  };

  public enum Bit {
    LOW(0, "0"),
    HIGH(1, "1");

    private final int value;
    private final String label;

    private Bit(final int value, final String label)
    {
      this.value = value;
      this.label = label;
    }

    @Override
    public String toString()
    {
      return label;
    }
  };

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
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
