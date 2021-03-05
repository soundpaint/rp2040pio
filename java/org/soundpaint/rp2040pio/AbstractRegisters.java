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

abstract class AbstractRegisters implements Registers
{
  private final int baseAddress;
  private final short size;
  private final int addrMin;
  private final int addrMax;

  private AbstractRegisters()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  /**
   * @param baseAddress The base address of the set of registers.
   * @param size Number of words provided by this registers interface.
   * The maximum allowed address computes as &lt;code&gt;baseAddress +
   * size * 0x4&lt;/code&gt;.
   */
  protected AbstractRegisters(final int baseAddress, final short size)
  {
    if ((baseAddress & 0x3) != 0x0) {
      throw new IllegalArgumentException("base address not word-aligned: " +
                                         String.format("%08x", baseAddress));
    }
    if ((baseAddress & 0x3000) != 0x0) {
      throw new IllegalArgumentException("base address not conforming to " +
                                         "model of register access methods: " +
                                         String.format("%08x", baseAddress));
    }
    if (size < 0) {
      throw new IllegalArgumentException("size < 0: " + size);
    }
    if (size * 0x4 > 0x1000) {
      throw new IllegalArgumentException(String.format("size * 0x4 > 0x1000: " +
                                                       "0x%08x" + size * 0x4));
    }
    this.baseAddress = baseAddress;
    this.size = size;
    addrMin = baseAddress;
    addrMax = baseAddress + size * 0x4;
  }

  public int getBaseAddress() { return baseAddress; }

  public int getSize() { return size; }

  public boolean providesAddress(final int address)
  {
    if ((address & 0x3) != 0x0) {
      throw new IllegalArgumentException("address not word-aligned: " +
                                         String.format("%08x", address));
    }
    /*
     * Work around signed vs. unsigned int / wrap issues by comparing
     * address offset against address range rather than comparing
     * absolute addresses.
     */
    final int offset = address - baseAddress;
    return (offset >= 0) && (offset < addrMax - addrMin);
  }

  private enum AccessMethod {
    NORMAL_RW, ATOMIC_XOR, ATOMIC_SET, ATOMIC_CLEAR;
  };

  private static AccessMethod[] ACCESS_METHODS = AccessMethod.values();

  abstract protected void writeRegister(final int regNum,
                                        final int bits, final int mask);

  public synchronized void writeAddress(final int address, final int value)
  {
    if ((address & 0x3) != 0x0) {
      throw new IllegalArgumentException("address not word-aligned: " +
                                         String.format("%04x", address));
    }
    final AccessMethod accessMethod = ACCESS_METHODS[((address >> 12) & 0x3)];
    final int mask;
    final int bits;
    switch (accessMethod) {
    case NORMAL_RW:
      mask = ~0x0;
      bits = value;
      break;
    case ATOMIC_XOR:
      mask = value;
      bits = ~readAddress(address) ^ value;
      break;
    case ATOMIC_SET:
      mask = value;
      bits = ~0x0;
      break;
    case ATOMIC_CLEAR:
      mask = value;
      bits = 0x0;
      break;
    default:
      throw new InternalError("unexpected case fall-through");
    }
    writeRegister(((address - baseAddress) & ~0x3000) >>> 2, bits, mask);
  }

  abstract protected int readRegister(final int regNum);

  public synchronized int readAddress(final int address)
  {
    if ((address & 0x3) != 0x0) {
      throw new IllegalArgumentException("address not word-aligned: " +
                                         String.format("%04x", address));
    }
    return readRegister(((address - baseAddress) & ~0x3000) >>> 2);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
