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
import java.util.function.LongSupplier;
import org.soundpaint.rp2040pio.AbstractRegisters;
import org.soundpaint.rp2040pio.Emulator;
import org.soundpaint.rp2040pio.GPIO;
import org.soundpaint.rp2040pio.GPIOIOBank0RegistersImpl;
import org.soundpaint.rp2040pio.GPIOPadsBank0RegistersImpl;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PicoEmuRegistersImpl;
import org.soundpaint.rp2040pio.PIO;
import org.soundpaint.rp2040pio.PIORegistersImpl;
import org.soundpaint.rp2040pio.PIOEmuRegistersImpl;

public class LocalRegisters extends AbstractRegisters
{
  private final Emulator emulator;
  private final PicoEmuRegistersImpl picoEmuRegisters;
  private final GPIOIOBank0RegistersImpl gpioIOBank0Registers;
  private final GPIOPadsBank0RegistersImpl gpioPadsBank0Registers;
  private final PIORegistersImpl pio0Registers;
  private final PIOEmuRegistersImpl pio0EmuRegisters;
  private final PIORegistersImpl pio1Registers;
  private final PIOEmuRegistersImpl pio1EmuRegisters;
  private final LongSupplier wallClockSupplier;

  /*
   * TODO: Really should replace this simple-minded list approach with
   * either a responsibility chain or a composite design pattern, as
   * soon as the number of registers interfaces grows.
   */
  private final List<AbstractRegisters> registersList;

  public LocalRegisters(final Emulator emulator)
  {
    super(0x0, (short)0x0, null/* TODO */);
    this.emulator = emulator;

    wallClockSupplier = () -> getWallClock();

    registersList = new ArrayList<AbstractRegisters>();
    picoEmuRegisters = new PicoEmuRegistersImpl(emulator, wallClockSupplier);
    registersList.add(picoEmuRegisters);

    final GPIO gpio = emulator.getGPIO();
    gpioIOBank0Registers =
      new GPIOIOBank0RegistersImpl(gpio, wallClockSupplier);
    registersList.add(gpioIOBank0Registers);
    gpioPadsBank0Registers =
      new GPIOPadsBank0RegistersImpl(gpio, wallClockSupplier);
    registersList.add(gpioPadsBank0Registers);

    final PIO pio0 = emulator.getPIO0();
    pio0Registers = new PIORegistersImpl(pio0, wallClockSupplier);
    registersList.add(pio0Registers);
    pio0EmuRegisters = new PIOEmuRegistersImpl(pio0, wallClockSupplier);
    registersList.add(pio0EmuRegisters);

    final PIO pio1 = emulator.getPIO1();
    pio1Registers = new PIORegistersImpl(pio1, wallClockSupplier);
    registersList.add(pio1Registers);
    pio1EmuRegisters = new PIOEmuRegistersImpl(pio1, wallClockSupplier);
    registersList.add(pio1EmuRegisters);
  }

  @Override
  public LongSupplier getWallClockSupplier() { return wallClockSupplier; }

  private Long getWallClock()
  {
    try {
      final int addressLSB =
        PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.WALLCLOCK_LSB);
      final int addressMSB =
        PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.WALLCLOCK_LSB);
      final int wallClockLSB = picoEmuRegisters.readAddress(addressLSB);
      final int wallClockMSB = picoEmuRegisters.readAddress(addressMSB);
      return (((long)wallClockMSB) << 32) | wallClockLSB;
    } catch (final IOException e) {
      emulator.getConsole().println(e.getMessage());
      return null;
    }
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

  private AbstractRegisters getProvidingRegisters(final int address)
    throws IOException
  {
    for (final AbstractRegisters registers : registersList) {
      if (registers.providesAddress(address)) {
        return registers;
      }
    }
    return null;
  }

  @Override
  public int getBaseAddress() { return 0; }

  @Override
  public boolean providesAddress(final int address) throws IOException
  {
    return getProvidingRegisters(address) != null;
  }

  @Override
  protected <T extends Enum<T>> T[] getRegs() {
    throw new InternalError("method not applicable for this class");
  }

  @Override
  public String getAddressLabel(final int address) throws IOException
  {
    final AbstractRegisters registers = getProvidingRegisters(address);
    return registers != null ? registers.getAddressLabel(address) : null;
  }

  @Override
  protected void writeRegister(final int regNum,
                               final int bits, final int mask,
                               final boolean xor)
  {
    throw new InternalError("method not applicable for this class");
  }

  @Override
  public void writeAddress(final int address, final int value)
    throws IOException
  {
    final AbstractRegisters registers = getProvidingRegisters(address);
    if (registers != null) {
      registers.writeAddress(address, value);
    } else {
      emulator.getConsole().
        println("warning: write ignored for unsupported address: " +
                String.format("%08x", address));
    }
  }

  @Override
  protected int readRegister(final int regNum)
  {
    throw new InternalError("method not applicable for this class");
  }

  @Override
  public int readAddress(final int address) throws IOException
  {
    final AbstractRegisters registers = getProvidingRegisters(address);
    if (registers != null) {
      return registers.readAddress(address);
    } else {
      emulator.getConsole().
        println("warning: returning 0 for read from unsupported address: " +
                String.format("%08x", address));
      return 0;
    }
  }

  @Override
  public int wait(final int address, final int expectedValue, final int mask,
                  final long cyclesTimeout, final long millisTimeout)
    throws IOException
  {
    final AbstractRegisters registers = getProvidingRegisters(address);
    if (registers != null) {
      return registers.wait(address, expectedValue, mask,
                            cyclesTimeout, millisTimeout);
    }
    return 0;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
