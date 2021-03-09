/*
 * @(#)Registers.java 1.00 21/03/03
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

public interface Registers
{
  static final int REG_ALIAS_RW_BITS = 0x0000;
  static final int REG_ALIAS_XOR_BITS = 0x1000;
  static final int REG_ALIAS_SET_BITS = 0x2000;
  static final int REG_ALIAS_CLR_BITS = 0x3000;

  int getBaseAddress();
  boolean providesAddress(final int address);
  String getLabel(final int address);
  void writeAddress(final int address, final int value);
  int readAddress(final int address);
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
