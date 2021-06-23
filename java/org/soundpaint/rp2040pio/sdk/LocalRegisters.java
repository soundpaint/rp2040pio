/*
 * @(#)LocalRegisters.java 1.00 21/03/25
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
import java.util.ArrayList;
import java.util.List;
import org.soundpaint.rp2040pio.AbstractRegisters;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.Emulator;
import org.soundpaint.rp2040pio.GPIO;
import org.soundpaint.rp2040pio.GPIOIOBank0RegistersImpl;
import org.soundpaint.rp2040pio.GPIOPadsBank0RegistersImpl;
import org.soundpaint.rp2040pio.MasterClock;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PicoEmuRegistersImpl;
import org.soundpaint.rp2040pio.PIO;
import org.soundpaint.rp2040pio.PIORegistersImpl;
import org.soundpaint.rp2040pio.PIOEmuRegistersImpl;
import org.soundpaint.rp2040pio.Registers;

public class LocalRegisters extends Registers
{
  private final Emulator emulator;
  private final PicoEmuRegistersImpl picoEmuRegisters;
  private final GPIOIOBank0RegistersImpl gpioIOBank0Registers;
  private final GPIOPadsBank0RegistersImpl gpioPadsBank0Registers;
  private final PIORegistersImpl pio0Registers;
  private final PIOEmuRegistersImpl pio0EmuRegisters;
  private final PIORegistersImpl pio1Registers;
  private final PIOEmuRegistersImpl pio1EmuRegisters;

  /*
   * TODO: Really should replace this simple-minded list approach with
   * either a responsibility chain or a composite design pattern, as
   * soon as the number of registers interfaces grows.
   */
  private final List<AbstractRegisters> registersList;

  public LocalRegisters(final Emulator emulator)
  {
    this.emulator = emulator;

    registersList = new ArrayList<AbstractRegisters>();
    picoEmuRegisters = new PicoEmuRegistersImpl(emulator);
    registersList.add(picoEmuRegisters);

    final GPIO gpio = emulator.getGPIO();
    gpioIOBank0Registers = new GPIOIOBank0RegistersImpl(gpio);
    registersList.add(gpioIOBank0Registers);
    gpioPadsBank0Registers = new GPIOPadsBank0RegistersImpl(gpio);
    registersList.add(gpioPadsBank0Registers);

    final PIO pio0 = emulator.getPIO0();
    pio0Registers = new PIORegistersImpl(pio0);
    registersList.add(pio0Registers);
    pio0EmuRegisters = new PIOEmuRegistersImpl(pio0);
    registersList.add(pio0EmuRegisters);

    final PIO pio1 = emulator.getPIO1();
    pio1Registers = new PIORegistersImpl(pio1);
    registersList.add(pio1Registers);
    pio1EmuRegisters = new PIOEmuRegistersImpl(pio1);
    registersList.add(pio1EmuRegisters);
  }

  @Override
  public String getEmulatorInfo() throws IOException
  {
    return Constants.getEmulatorIdAndVersionWithOs();
  }

  public int getGPIOAddress(final GPIOIOBank0RegistersImpl.Regs register)
  {
    return GPIOIOBank0RegistersImpl.getAddress(register);
  }

  public int getGPIOAddress(final GPIOPadsBank0RegistersImpl.Regs register)
  {
    return GPIOPadsBank0RegistersImpl.getAddress(register);
  }

  public int getPIO0Address(final PIORegistersImpl.Regs register)
  {
    return pio0Registers.getAddress(register);
  }

  public int getPIO1Address(final PIORegistersImpl.Regs register)
  {
    return pio1Registers.getAddress(register);
  }

  public int getPIO0Address(final PIOEmuRegistersImpl.Regs register)
  {
    return pio0EmuRegisters.getAddress(register);
  }

  public int getPIO1Address(final PIOEmuRegistersImpl.Regs register)
  {
    return pio1EmuRegisters.getAddress(register);
  }

  private static int address2register(final AbstractRegisters registers,
                                      final int address)
  {
    checkAddressAligned(address);
    return ((address - registers.getBaseAddress()) & ~0x3000) >>> 2;
  }

  private AbstractRegisters getProvidingRegisters(final int address)
    throws IOException
  {
    for (final AbstractRegisters registers : registersList) {
      final int regNum = address2register(registers, address);
      if (regNum < registers.getSize()) {
        return registers;
      }
    }
    return null;
  }

  @Override
  public boolean providesAddress(final int address) throws IOException
  {
    return getProvidingRegisters(address) != null;
  }

  @Override
  public String getAddressLabel(final int address) throws IOException
  {
    final AbstractRegisters registers = getProvidingRegisters(address);
    if (registers != null) {
      final int regNum = address2register(registers, address);
      return registers.getRegisterLabel(regNum);
    }
    final String message =
      String.format("requesting label for unsupported address: %08x",
                    address);
    throw new IOException(message);
  }

  @Override
  public synchronized void writeAddressMasked(final int address, final int bits,
                                              final int mask, final boolean xor)
    throws IOException
  {
    if ((address & 0x3000) != 0x0000) {
      final String message =
        String.format("writeAddressMasked(): " +
                      "address not in base address range: 0x%8x", address);
      throw new IOException(message);
    }
    final AbstractRegisters registers = getProvidingRegisters(address);
    if (registers != null) {
      final int regNum = address2register(registers, address);
      try {
        registers.writeRegister(regNum, bits, mask, xor);
      } catch (final Throwable t) {
        final String message = t.getMessage();
        emulator.getConsole().
          printf("warning: internal error occurred: %s%n", message);
        t.printStackTrace(emulator.getConsole());
        throw new IOException(message);
      }
      return;
    }
    final String message =
      String.format("write on unsupported address: %08x", address);
    throw new IOException(message);
  }

  @Override
  public synchronized int readAddress(final int address) throws IOException
  {
    final AbstractRegisters registers = getProvidingRegisters(address);
    if (registers != null) {
      final int regNum = address2register(registers, address);
      try {
        return registers.readRegister(regNum);
      } catch (final Throwable t) {
        final String message = t.getMessage();
        emulator.getConsole().
          printf("warning: internal error occurred: %s%n", message);
        t.printStackTrace(emulator.getConsole());
        throw new IOException(message);
      }
    }
    final String message =
      String.format("read from unsupported address: %08x", address);
    throw new IOException(message);
  }

  private static boolean timedOut(final long startWallClock,
                                  final long stopWallClock,
                                  final long wallClock)
  {
    return
      (startWallClock < stopWallClock) ?
      (wallClock < startWallClock) || (wallClock >= stopWallClock) :
      (wallClock < startWallClock) && (wallClock >= stopWallClock);
  }

  @Override
  public int waitAddress(final int address, final int expectedValue,
                         final int mask,
                         final long cyclesTimeout, final long millisTimeout)
    throws IOException
  {
    if (cyclesTimeout < 0) {
      throw new IllegalArgumentException("cyclesTimeout < 0: " + cyclesTimeout);
    }
    if (millisTimeout < 0) {
      throw new IllegalArgumentException("millisTimeout < 0: " + millisTimeout);
    }
    final MasterClock masterClock = emulator.getMasterClock();
    final long startWallClock = masterClock.getWallClock();
    final long stopWallClock = startWallClock + cyclesTimeout;
    final long startTime = System.currentTimeMillis();
    final long stopTime = startTime + millisTimeout;
    int receivedValue;
    while (((receivedValue = readAddress(address) & mask) != expectedValue)) {
      final long wallClock = masterClock.getWallClock();
      if (timedOut(startWallClock, stopWallClock, wallClock)) break;
      try {
        if (millisTimeout != 0) {
          final long time = System.currentTimeMillis();
          if (timedOut(startTime, stopTime, time)) break;
          masterClock.awaitPhaseChange(stopTime - time);
        } else {
          masterClock.awaitPhaseChange();
        }
      } catch (final InterruptedException e) {
        // ignore here, since check in while condition
      }
    }
    return receivedValue;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
