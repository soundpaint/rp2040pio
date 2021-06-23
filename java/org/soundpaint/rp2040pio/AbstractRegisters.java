/*
 * @(#)AbstractRegisters.java 1.00 21/03/05
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

import java.io.IOException;

public abstract class AbstractRegisters
{
  private final int baseAddress;
  private final short size;

  private AbstractRegisters()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  /**
   * @param baseAddress The base address of the set of registers.
   * @param size Number of words provided by this registers interface.
   * The maximum allowed address computes as &lt;code&gt;baseAddress +
   * (size - 1) * 0x4&lt;/code&gt;.
   */
  protected AbstractRegisters(final int baseAddress)
  {
    if ((baseAddress & 0x3fff) != 0x0) {
      throw new IllegalArgumentException("base address not conforming to " +
                                         "model of register access methods: " +
                                         String.format("0x%08x", baseAddress));
    }
    this.baseAddress = baseAddress;
    size = (short)getRegs().length;
    if (size * 0x4 > 0x1000) {
      throw new IllegalArgumentException(String.format("size * 0x4 > 0x1000: " +
                                                       "0x%08x" + size * 0x4));
    }
  }

  public int getBaseAddress() { return baseAddress; }

  public int getSize() { return size; }

  protected void checkRegNum(final int regNum)
  {
    if ((regNum < 0) || (regNum >= size)) {
      final String message =
        String.format("regNum out of bounds: 0x%08x", regNum);
      throw new InternalError(message);
    }
  }

  /**
   * Returns all instance values of the subclass's REGS enum.
   */
  protected abstract <T extends Enum<T>> T[] getRegs();

  public <T extends Enum<T>> String getRegisterLabel(final int regNum)
    throws IOException
  {
    if (regNum < 0) {
      throw new IllegalArgumentException("regNum < 0: " + regNum);
    }
    if (regNum > 0xfff) {
      throw new IllegalArgumentException("regNum > 0xfff: " +
                                         String.format("%08x", regNum));
    }
    final T[] regs = getRegs();
    return regNum < regs.length ? regs[regNum].toString() : null;
  }

  public abstract void writeRegister(final int regNum,
                                     final int bits, final int mask,
                                     final boolean xor)
    throws IOException;

  public abstract int readRegister(final int regNum) throws IOException;

  @Override
  public String toString()
  {
    return
      String.format("%s@%08x, size=%08x", super.toString(), baseAddress, size);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
