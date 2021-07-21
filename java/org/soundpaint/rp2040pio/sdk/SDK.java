/*
 * @(#)SDK.java 1.00 21/02/02
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
package org.soundpaint.rp2040pio.sdk;

import java.io.IOException;
import java.io.PrintStream;
import org.soundpaint.rp2040pio.AddressSpace;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.Emulator;
import org.soundpaint.rp2040pio.PicoEmuRegisters;

public class SDK implements Constants
{
  private final PrintStream console;
  private final AddressSpace memory;

  /*
   * TODO: There is only a single GPIO, but each of the two PIOs has
   * its own GPIO input / output latches. Therefore, some GPIO
   * functionality is shared between both PIOs, while other GPIO
   * functionality is instantiated per PIO.  This difference should be
   * made more explicit in the overall architecture.
   */
  private final GPIOSDK gpioSdk;
  private final PIOSDK pio0Sdk;
  private final PIOSDK pio1Sdk;

  private SDK()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public SDK(final PrintStream console, final AddressSpace memory)
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.console = console;
    if (memory == null) {
      throw new NullPointerException("memory");
    }
    this.memory = memory;
    gpioSdk = new GPIOSDK(memory);
    pio0Sdk = new PIOSDK(0, memory, gpioSdk);
    pio1Sdk = new PIOSDK(1, memory, gpioSdk);
  }

  public PrintStream getConsole() { return console; }
  public GPIOSDK getGPIOSDK() { return gpioSdk; }
  public PIOSDK getPIO0SDK() { return pio0Sdk; }
  public PIOSDK getPIO1SDK() { return pio1Sdk; }

  public int readAddress(final int address) throws IOException
  {
    return memory.readAddress(address);
  }

  public int readAddress(final int address, final int msb, final int lsb)
    throws IOException
  {
    Constants.checkMSBLSB(msb, lsb);
    final int value = readAddress(address);
    return
      (msb - lsb == 31) ?
      value :
      (value >>> lsb) & ((0x1 << (msb - lsb + 1)) - 1);
  }

  public void writeAddress(final int address, final int value)
    throws IOException
  {
    memory.writeAddress(address, value);
  }

  public void writeAddressMasked(final int address, final int bits,
                                 final int mask, final boolean xor)
    throws IOException
  {
    memory.writeAddressMasked(address, bits, mask, xor);
  }

  public int wait(final int address, final int expectedValue)
    throws IOException
  {
    return wait(address, expectedValue, 0xffffffff);
  }

  public int wait(final int address, final int expectedValue, final int mask)
    throws IOException
  {
    return wait(address, expectedValue, mask, 0x0);
  }

  public int wait(final int address, final int expectedValue, final int mask,
                  final long cyclesTimeout)
    throws IOException
  {
    return wait(address, expectedValue, mask, cyclesTimeout, 0x0);
  }

  public int wait(final int address, final int expectedValue, final int mask,
                  final long cyclesTimeout, final long millisTimeout)
    throws IOException
  {
    return
      memory.waitAddress(address, expectedValue, mask,
                         cyclesTimeout, millisTimeout);
  }

  public void awaitNextCycle() throws IOException
  {
    memory.waitAddress(EMULATOR_BASE, 0xffffffff, 0x0, 1, 0);
  }

  public void hwSetBits(final int address, final int mask) throws IOException
  {
    memory.hwSetBits(address, mask);
  }

  public void hwClearBits(final int address, final int mask) throws IOException
  {
    memory.hwClearBits(address, mask);
  }

  public void hwXorBits(final int address, final int mask) throws IOException
  {
    memory.hwXorBits(address, mask);
  }

  public void hwWriteMasked(final int address, final int values,
                            final int writeMask)
    throws IOException
  {
    memory.hwWriteMasked(address, values, writeMask);
  }

  // -------- address helpers --------

  public String getEmulatorInfo() throws IOException
  {
    return memory.getEmulatorInfo();
  }

  public boolean providesAddress(final int address) throws IOException
  {
    return memory.providesAddress(address);
  }

  public String getRegisterSetId(final int address) throws IOException
  {
    return memory.getRegisterSetId(address);
  }

  public String getLabelForAddress(final int address) throws IOException
  {
    return memory.getAddressLabel(address);
  }

  public String getFullLabelForAddress(final int address) throws IOException
  {
    return getRegisterSetId(address) + "_" + getLabelForAddress(address);
  }

  // -------- PicoEmuRegisters convenience methods --------

  public void reset() throws IOException
  {
    final int address =
      PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.PWR_UP);
    synchronized(memory) {
      memory.writeAddress(address, PICO_PWR_UP_VALUE);
      pio0Sdk.reset();
      pio1Sdk.reset();
    }
  }

  private void triggerCyclePhaseX(final PicoEmuRegisters.Regs trigger,
                                  final boolean await)
    throws IOException
  {
    final int triggerAddress = PicoEmuRegisters.getAddress(trigger);
    synchronized(memory) {
      memory.writeAddress(triggerAddress, 0);
      while (await && (memory.readAddress(triggerAddress) == 0)) {
        Thread.yield();
      }
    }
  }

  public void triggerCyclePhase0(final boolean await) throws IOException
  {
    triggerCyclePhaseX(PicoEmuRegisters.Regs.MASTERCLK_TRIGGER_PHASE0, await);
  }

  public void triggerCyclePhase1(final boolean await) throws IOException
  {
    triggerCyclePhaseX(PicoEmuRegisters.Regs.MASTERCLK_TRIGGER_PHASE1, await);
  }

  public long getWallClock() throws IOException
  {
    final int addressWallClockLsb =
      PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.WALLCLOCK_LSB);
    final int addressWallClockMsb =
      PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.WALLCLOCK_MSB);
    final int wallClockLsb = memory.readAddress(addressWallClockLsb);
    final int wallClockMsb = memory.readAddress(addressWallClockMsb);
    return ((long)wallClockMsb << 32) | wallClockLsb;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
