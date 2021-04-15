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
public enum PinState
{
  IN_LOW(Direction.IN, Bit.LOW),
  IN_HIGH(Direction.IN, Bit.HIGH),
  OUT_LOW(Direction.OUT, Bit.LOW),
  OUT_HIGH(Direction.OUT, Bit.HIGH);

  private final Direction direction;
  private final Bit level;

  private PinState(final Direction direction, final Bit level)
  {
    if (direction == null) {
      throw new NullPointerException("direction");
    }
    if (level == null) {
      throw new NullPointerException("level");
    }
    this.direction = direction;
    this.level = level;
  }

  public Direction getDirection() { return direction; }

  public Bit getLevel() { return level; }

  public static PinState fromValues(final Direction direction, final Bit level)
  {
    if (direction == null) {
      throw new NullPointerException("direction");
    }
    if (level == null) {
      throw new NullPointerException("level");
    }
    switch (direction) {
    case IN:
      switch (level) {
      case LOW: return IN_LOW;
      case HIGH: return IN_HIGH;
      default: throw new InternalError("unexpected case fall-through");
      }
    case OUT:
      switch (level) {
      case LOW: return OUT_LOW;
      case HIGH: return OUT_HIGH;
      default: throw new InternalError("unexpected case fall-through");
      }
    default: throw new InternalError("unexpected case fall-through");
    }
  }

  @Override
  public String toString()
  {
    return String.format("PinState[level=%s, direction=%s]", level, direction);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
