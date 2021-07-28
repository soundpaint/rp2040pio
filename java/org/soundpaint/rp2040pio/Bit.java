/*
 * @(#)Bit.java 1.00 21/02/06
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
 * Representation of a single bit value.
 */
public enum Bit
{
  LOW(0, '0', "low", "\u001b[30;42m0\u001b[0m", "\u001b[37;41m0\u001b[0m"),
  HIGH(1, '1', "high", "\u001b[30;42m1\u001b[0m", "\u001b[37;41m1\u001b[0m");

  private final int value;
  private final char charLabel;
  private final String level;
  private final String superScriptLabel;
  private final String subScriptLabel;
  private final String stringLabel;

  private Bit(final int value, final char charLabel, final String level,
              final String superScriptLabel, final String subScriptLabel)
  {
    this.value = value;
    this.charLabel = charLabel;
    this.level = level;
    this.superScriptLabel = superScriptLabel;
    this.subScriptLabel = subScriptLabel;
    this.stringLabel = String.valueOf(charLabel);
  }

  public int getValue() { return value; }

  public String getLevel() { return level; }

  public static Bit fromValue(final boolean value)
  {
    return value ? HIGH : LOW;
  }

  public static Bit fromValue(final int value)
  {
    if (value == LOW.value) return LOW;
    if (value == HIGH.value) return HIGH;
    throw new IllegalArgumentException("value not a bit: " + value);
  }

  public static Bit fromValue(final int value, final Bit defaultValue)
  {
    if (value == LOW.value) return LOW;
    if (value == HIGH.value) return HIGH;
    return defaultValue;
  }

  public String toChar(final Direction direction)
  {
    if (direction == null) return String.valueOf(charLabel);
    return direction == Direction.IN ? superScriptLabel : subScriptLabel;
  }

  public Bit inverse()
  {
    return this == LOW ? HIGH : LOW;
  }

  @Override
  public String toString()
  {
    return stringLabel;
  }
};

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
