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

import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.Emulator;
import org.soundpaint.rp2040pio.GPIO;
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.GPIOPadsBank0Registers;
import org.soundpaint.rp2040pio.MasterClock;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PIO;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.Registers;

public class LocalRegisters implements Constants, Registers
{
  private final Emulator emulator;
  private final List<Registers> registersList;

  public LocalRegisters(final Emulator emulator)
  {
    if (emulator == null) {
      throw new NullPointerException("emulator");
    }
    this.emulator = emulator;
    final MasterClock masterClock = emulator.getMasterClock();

    registersList = new ArrayList<Registers>();
    final PicoEmuRegisters picoEmuRegisters =
      new PicoEmuRegisters(emulator, EMULATOR_BASE);
    registersList.add(picoEmuRegisters);

    final GPIO gpio = emulator.getGPIO();
    final GPIOIOBank0Registers gpioIOBank0Registers =
      new GPIOIOBank0Registers(masterClock, gpio, IO_BANK0_BASE);
    registersList.add(gpioIOBank0Registers);
    final GPIOPadsBank0Registers gpioPadsBank0Registers =
      new GPIOPadsBank0Registers(masterClock, gpio, PADS_BANK0_BASE);
    registersList.add(gpioPadsBank0Registers);

    final PIO pio0 = emulator.getPIO0();
    final PIORegisters pio0Registers =
      new PIORegisters(masterClock, pio0, PIO0_BASE);
    registersList.add(pio0Registers);
    final PIOEmuRegisters pio0EmuRegisters =
      new PIOEmuRegisters(masterClock, pio0, PIO0_BASE + 0x0800);
    registersList.add(pio0EmuRegisters);

    final PIO pio1 = emulator.getPIO1();
    final PIORegisters pio1Registers =
      new PIORegisters(masterClock, pio1, PIO1_BASE);
    registersList.add(pio1Registers);
    final PIOEmuRegisters pio1EmuRegisters =
      new PIOEmuRegisters(masterClock, pio1, PIO1_BASE + 0x0800);
    registersList.add(pio1EmuRegisters);
  }

  private Registers getProvidingRegisters(final int address) throws IOException
  {
    for (final Registers registers : registersList) {
      if (registers.providesAddress(address)) {
        return registers;
      }
    }
    return null;
  }

  public int getBaseAddress() { return 0; }

  public boolean providesAddress(final int address) throws IOException
  {
    return getProvidingRegisters(address) != null;
  }

  public String getLabel(final int address) throws IOException
  {
    final Registers registers = getProvidingRegisters(address);
    return registers != null ? registers.getLabel(address) : null;
  }

  public void writeAddress(final int address, final int value)
    throws IOException
  {
    final Registers registers = getProvidingRegisters(address);
    if (registers != null) registers.writeAddress(address, value);
  }

  public int readAddress(final int address) throws IOException
  {
    final Registers registers = getProvidingRegisters(address);
    return registers != null ? registers.readAddress(address) : 0;
  }

  public int wait(final int address, final int expectedValue, final int mask,
                  final long cyclesTimeout, final long millisTimeout)
    throws IOException
  {
    final Registers registers = getProvidingRegisters(address);
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
