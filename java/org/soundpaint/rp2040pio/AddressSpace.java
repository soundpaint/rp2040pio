/*
 * @(#)AddressSpace.java 1.00 21/03/03
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

public abstract class AddressSpace
{
  static final int REG_ALIAS_RW_BITS = 0x0000;
  static final int REG_ALIAS_XOR_BITS = 0x1000;
  static final int REG_ALIAS_SET_BITS = 0x2000;
  static final int REG_ALIAS_CLR_BITS = 0x3000;

  private enum AccessMethod {
    NORMAL_RW, ATOMIC_XOR, ATOMIC_SET, ATOMIC_CLEAR;
  };

  private static AccessMethod[] ACCESS_METHODS = AccessMethod.values();

  protected static void checkAddressAligned(final int address)
  {
    if ((address & 0x3) != 0x0) {
      throw new IllegalArgumentException("address not word-aligned: " +
                                         String.format("0x%08x", address));
    }
  }

  private static void checkAddressNormalRWSpace(final int address)
  {
    if ((address & 0x3000) != 0x0) {
      throw new IllegalArgumentException("address is not in the space of " +
                                         "normal read / write access: " +
                                         String.format("0x%08x", address));
    }
  }

  public abstract String getEmulatorInfo() throws IOException;

  public abstract boolean providesAddress(final int address)
    throws IOException;

  public abstract String getRegisterSetId(final int address) throws IOException;

  public abstract String getAddressLabel(final int address) throws IOException;

  public abstract int readAddress(final int address) throws IOException;

  public abstract void writeAddressMasked(final int address, final int bits,
                                          final int mask, final boolean xor)
    throws IOException;

  public abstract int waitAddress(final int address, final int expectedValue,
                                  final int mask,
                                  final long cyclesTimeout,
                                  final long millisTimeout)
    throws IOException;

  public void writeAddress(final int address, final int value)
    throws IOException
  {
    checkAddressAligned(address);
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
      bits = value;
      break;
    case ATOMIC_SET:
      mask = value;
      bits = value;
      break;
    case ATOMIC_CLEAR:
      mask = value;
      bits = 0x0;
      break;
    default:
      throw new InternalError("unexpected case fall-through");
    }
    writeAddressMasked(address & ~0x3000, bits, mask,
                       accessMethod == AccessMethod.ATOMIC_XOR);
  }

  public void hwSetBits(final int address, final int mask) throws IOException
  {
    checkAddressNormalRWSpace(address);
    writeAddress(address | REG_ALIAS_SET_BITS, mask);
  }

  public void hwClearBits(final int address, final int mask) throws IOException
  {
    checkAddressNormalRWSpace(address);
    writeAddress(address | REG_ALIAS_CLR_BITS, mask);
  }

  public void hwXorBits(final int address, final int mask) throws IOException
  {
    checkAddressNormalRWSpace(address);
    writeAddress(address | REG_ALIAS_XOR_BITS, mask);
  }

  public void hwWriteMasked(final int address, final int values,
                            final int writeMask)
    throws IOException
  {
    checkAddressNormalRWSpace(address);
    writeAddressMasked(address, values, writeMask, false);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
