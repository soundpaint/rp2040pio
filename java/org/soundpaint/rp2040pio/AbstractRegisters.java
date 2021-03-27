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

public abstract class AbstractRegisters implements Registers
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
                                         String.format("0x%08x", baseAddress));
    }
    if ((baseAddress & 0x3fff) != 0x0) {
      throw new IllegalArgumentException("base address not conforming to " +
                                         "model of register access methods: " +
                                         String.format("0x%08x", baseAddress));
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
    addrMax = baseAddress | 0x3fff;
  }

  @Override
  public int getBaseAddress() { return baseAddress; }

  public int getSize() { return size; }

  @Override
  public boolean providesAddress(final int address) throws IOException
  {
    if ((address & 0x3) != 0x0) {
      throw new IllegalArgumentException("address not word-aligned: " +
                                         String.format("0x%08x", address));
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

  protected abstract String getRegisterLabel(final int regNum);

  @Override
  public String getAddressLabel(final int address) throws IOException
  {
    if (!providesAddress(address)) {
      return null;
    }
    return getRegisterLabel(((address - baseAddress) & ~0x3000) >>> 2);
  }

  private static void checkAddressAligned(final int address)
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

  abstract protected void writeRegister(final int regNum,
                                        final int bits, final int mask,
                                        final boolean xor)
    throws IOException;

  @Override
  public synchronized void writeAddress(final int address, final int value)
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
    writeRegister(((address - baseAddress) & ~0x3000) >>> 2, bits, mask,
                  accessMethod == AccessMethod.ATOMIC_XOR);
  }

  @Override
  public void hwSetBits(final int address, final int mask) throws IOException
  {
    checkAddressNormalRWSpace(address);
    writeAddress(address | REG_ALIAS_SET_BITS, mask);
  }

  @Override
  public void hwClearBits(final int address, final int mask) throws IOException
  {
    checkAddressNormalRWSpace(address);
    writeAddress(address | REG_ALIAS_CLR_BITS, mask);
  }

  @Override
  public void hwXorBits(final int address, final int mask) throws IOException
  {
    checkAddressNormalRWSpace(address);
    writeAddress(address | REG_ALIAS_XOR_BITS, mask);
  }

  @Override
  public void hwWriteMasked(final int address, final int values,
                            final int writeMask)
    throws IOException
  {
    hwXorBits(address, (readAddress(address) ^ values) & writeMask);
  }

  abstract protected int readRegister(final int regNum) throws IOException;

  @Override
  public synchronized int readAddress(final int address) throws IOException
  {
    checkAddressAligned(address);
    return readRegister(((address - baseAddress) & ~0x3000) >>> 2);
  }

  private boolean timedOut(final long startWallClock,
                           final long stopWallClock,
                           final long wallClock)
  {
    return
      (startWallClock < stopWallClock) ?
      (wallClock < startWallClock) || (wallClock >= stopWallClock) :
      (wallClock < startWallClock) && (wallClock >= stopWallClock);
  }

  private long getWallClock() throws IOException
  {
    final int addressLSB =
      PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.WALLCLOCK_LSB);
    final int addressMSB =
      PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.WALLCLOCK_LSB);
    final int wallClockLSB = readAddress(addressLSB);
    final int wallClockMSB = readAddress(addressMSB);
    return (wallClockMSB << 32) | wallClockLSB;
  }

  @Override
  public int wait(final int address, final int expectedValue, final int mask,
                  final long cyclesTimeout, final long millisTimeout)
    throws IOException
  {
    checkAddressAligned(address);
    if (cyclesTimeout < 0) {
      throw new IllegalArgumentException("cyclesTimeout < 0: " + cyclesTimeout);
    }
    if (millisTimeout < 0) {
      throw new IllegalArgumentException("millisTimeout < 0: " + millisTimeout);
    }
    final int regNum = ((address - baseAddress) & ~0x3000) >>> 2;
    final long startWallClock = getWallClock();
    final long stopWallClock = startWallClock + cyclesTimeout;
    final long startTime = System.currentTimeMillis();
    final long stopTime = startTime + millisTimeout;
    synchronized(this) {
      int receivedValue;
      while (((receivedValue = readRegister(regNum) & mask) != expectedValue)) {
        final long wallClock = getWallClock();
        if (timedOut(startWallClock, stopWallClock, wallClock)) break;
        try {
          if (millisTimeout != 0) {
            final long time = System.currentTimeMillis();
            if (timedOut(startTime, stopTime, time)) break;
            wait(stopTime - time);
          } else {
            wait();
          }
        } catch (final InterruptedException e) {
          // ignore here, since check in while condition
        }
      }
      return receivedValue;
    }
  }

  public String toString()
  {
    return
      String.format("%s@%08x, size=%08x, addrMin=%08x, addrMax=%08x",
                    getClass().getName(), baseAddress, size, addrMin, addrMax);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
