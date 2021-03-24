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
import java.util.ArrayList;
import java.util.List;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.Emulator;
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.GPIOPadsBank0Registers;
import org.soundpaint.rp2040pio.MasterClock;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PIO;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.Registers;

public class SDK implements Constants
{
  private final PrintStream console;
  private final Emulator emulator;
  private final PicoEmuRegisters picoEmuRegisters;

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

  /*
   * TODO: Really should replace this simple-minded list approach with
   * either a responsibility chain or a composite design pattern, as
   * soon as the number of registers interfaces grows.
   */
  private final List<Registers> registersList;

  public SDK(final PrintStream console)
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.console = console;
    emulator = new Emulator(console);
    final MasterClock masterClock = emulator.getMasterClock();

    registersList = new ArrayList<Registers>();
    picoEmuRegisters = new PicoEmuRegisters(emulator, EMULATOR_BASE);
    registersList.add(picoEmuRegisters);

    gpioSdk = new GPIOSDK(masterClock, emulator.getGPIO(),
                          IO_BANK0_BASE, PADS_BANK0_BASE);
    final GPIOIOBank0Registers gpioIOBank0Registers =
      gpioSdk.getIOBank0Registers();
    registersList.add(gpioIOBank0Registers);
    final GPIOPadsBank0Registers gpioPadsBank0Registers =
      gpioSdk.getPadsBank0Registers();
    registersList.add(gpioPadsBank0Registers);

    pio0Sdk = new PIOSDK(gpioSdk, masterClock, emulator.getPIO0(), PIO0_BASE);
    final PIORegisters pio0Registers = pio0Sdk.getRegisters();
    registersList.add(pio0Registers);
    final PIOEmuRegisters pio0EmuRegisters = pio0Sdk.getEmuRegisters();
    registersList.add(pio0EmuRegisters);

    pio1Sdk = new PIOSDK(gpioSdk, masterClock, emulator.getPIO1(), PIO1_BASE);
    final PIORegisters pio1Registers = pio1Sdk.getRegisters();
    registersList.add(pio1Registers);
    final PIOEmuRegisters pio1EmuRegisters = pio1Sdk.getEmuRegisters();
    registersList.add(pio1EmuRegisters);
  }

  public String getProgramAndVersion()
  {
    return emulator.getProgramAndVersion();
  }

  public String getAbout()
  {
    return emulator.getAbout();
  }

  public PrintStream getConsole() { return console; }
  public GPIOSDK getGPIOSDK() { return gpioSdk; }
  public PIOSDK getPIO0SDK() { return pio0Sdk; }
  public PIOSDK getPIO1SDK() { return pio1Sdk; }

  public int readAddress(final int address) throws IOException
  {
    final Registers registers = getProvidingRegisters(address);
    return registers != null ? registers.readAddress(address) : 0;
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
    final Registers registers = getProvidingRegisters(address);
    if (registers != null) registers.writeAddress(address, value);
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
    final Registers registers = getProvidingRegisters(address);
    if (registers != null) {
      return registers.wait(address, expectedValue, mask,
                            cyclesTimeout, millisTimeout);
    }
    return 0;
  }

  // -------- address helpers --------

  private Registers getProvidingRegisters(final int address) throws IOException
  {
    for (final Registers registers : registersList) {
      if (registers.providesAddress(address)) {
        return registers;
      }
    }
    return null;
  }

  public boolean matchesProvidingRegisters(final int address) throws IOException
  {
    return getProvidingRegisters(address) != null;
  }

  public String getLabelForAddress(final int address) throws IOException
  {
    final Registers registers = getProvidingRegisters(address);
    return registers != null ? registers.getLabel(address) : null;
  }

  public int getGPIOAddress(final GPIOIOBank0Registers.Regs register)
  {
    return gpioSdk.getIOBank0Registers().getAddress(register);
  }

  public int getGPIOAddress(final GPIOPadsBank0Registers.Regs register)
  {
    return gpioSdk.getPadsBank0Registers().getAddress(register);
  }

  public int getPIO0Address(final PIORegisters.Regs register)
  {
    return pio0Sdk.getRegisters().getAddress(register);
  }

  public int getPIO1Address(final PIORegisters.Regs register)
  {
    return pio1Sdk.getRegisters().getAddress(register);
  }

  public int getPIO0Address(final PIOEmuRegisters.Regs register)
  {
    return pio0Sdk.getEmuRegisters().getAddress(register);
  }

  public int getPIO1Address(final PIOEmuRegisters.Regs register)
  {
    return pio1Sdk.getEmuRegisters().getAddress(register);
  }

  // -------- PicoEmuRegisters convenience methods --------

  public void reset()
  {
    final int address =
      picoEmuRegisters.getAddress(PicoEmuRegisters.Regs.PWR_UP);
    picoEmuRegisters.writeAddress(address, PICO_PWR_UP_VALUE);
  }

  private void triggerCyclePhaseX(final PicoEmuRegisters.Regs trigger,
                                  final boolean await)
  {
    final int triggerAddress = picoEmuRegisters.getAddress(trigger);
    synchronized(picoEmuRegisters) {
      picoEmuRegisters.writeAddress(triggerAddress, 0);
      while (await && (picoEmuRegisters.readAddress(triggerAddress) == 0)) {
        Thread.yield();
      }
    }
  }

  public void triggerCyclePhase0(final boolean await)
  {
    triggerCyclePhaseX(PicoEmuRegisters.Regs.MASTERCLK_TRIGGER_PHASE0, await);
  }

  public void triggerCyclePhase1(final boolean await)
  {
    triggerCyclePhaseX(PicoEmuRegisters.Regs.MASTERCLK_TRIGGER_PHASE1, await);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
