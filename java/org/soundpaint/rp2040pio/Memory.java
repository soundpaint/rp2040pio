/*
 * @(#)Memory.java 1.00 21/01/31
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
 * 32 32-Bit Words of Shared Instruction Memory
 */
public class Memory implements Constants
{
  public final Object FETCH_LOCK;
  private final short[] code;

  public Memory()
  {
    FETCH_LOCK = new Object();
    code = new short[MEMORY_SIZE];
  }

  public void reset()
  {
    synchronized(FETCH_LOCK) {
      for (int address = 0; address < MEMORY_SIZE; address++) {
        set(address, (short)0);
      }
    }
  }

  public void set(final int address, final int value,
                  final int mask, final boolean xor)
  {
    set(address, (short)Constants.hwSetBits(get(address), value, mask, xor));
  }

  private void set(final int address, final short value)
  {
    Constants.checkSmMemAddr(address, "write address");
    code[address] = value;
  }

  public short get(final int address)
  {
    Constants.checkSmMemAddr(address, "read address");
    return code[address];
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
