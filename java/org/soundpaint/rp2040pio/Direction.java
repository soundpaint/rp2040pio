/*
 * @(#)Direction.java 1.00 21/03/19
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
 * Representation of a single GPIO pin's direction.
 */
public enum Direction
{
  IN(0, "in"),
  OUT(1, "out");

  private final int value;
  private final String label;

  private Direction(final int value, final String label)
  {
    this.value = value;
    this.label = label;
  }

  public int getValue() { return value; }

  public static Direction fromValue(final int value)
  {
    if (value == IN.value) return IN;
    if (value == OUT.value) return OUT;
    throw new IllegalArgumentException("value not a direction: " + value);
  }

  public Direction inverse()
  {
    return this == IN ? OUT : IN;
  }

  @Override
  public String toString()
  {
    return label;
  }
};

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
