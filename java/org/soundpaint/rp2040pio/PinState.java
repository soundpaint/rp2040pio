/*
 * @(#)PinState.java 1.00 21/04/06
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
 * Representation of a single GPIO pin's state.
 */
public class PinState
{
  private Direction direction;
  private Bit level;

  public PinState()
  {
    reset();
  }

  public void reset()
  {
    direction = Direction.IN;
    level = Bit.LOW;
  }

  public Direction getDirection() { return direction; }

  public void setDirection(final Direction direction)
  {
    if (direction == null) {
      throw new NullPointerException("direction");
    }
    this.direction = direction;
  }

  public Bit getLevel() { return level; }

  public void setLevel(final Bit level)
  {
    if (level == null) {
      throw new NullPointerException("level");
    }
    this.level = level;
  }

  public static String toChar(final Direction direction, final Bit level)
  {
    return level.toChar(direction);
  }

  public String toChar()
  {
    return toChar(direction, level);
  }

  @Override
  public String toString()
  {
    return String.valueOf(toChar());
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
