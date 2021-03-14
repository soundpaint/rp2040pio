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

import java.util.ArrayList;
import java.util.List;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PIO;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.Registers;

public class SDK
{
  private static final SDK DEFAULT_INSTANCE = new SDK();
  public static SDK getDefaultInstance() { return DEFAULT_INSTANCE; }

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

  private SDK()
  {
    registersList = new ArrayList<Registers>();
    picoEmuRegisters = new PicoEmuRegisters(PIO.MASTER_CLOCK,
                                            PIO.MASTER_CLOCK_BASE);
    registersList.add(picoEmuRegisters);

    gpioSdk = new GPIOSDK(PIO.GPIO);
    // TODO: Implement registers for GPIO SDK.

    pio0Sdk = new PIOSDK(gpioSdk, PIO.PIO0, PIO.PIO0_BASE);
    final PIORegisters pio0Registers = pio0Sdk.getRegisters();
    registersList.add(pio0Registers);
    final PIOEmuRegisters pio0EmuRegisters = pio0Sdk.getEmuRegisters();
    registersList.add(pio0EmuRegisters);

    pio1Sdk = new PIOSDK(gpioSdk, PIO.PIO1, PIO.PIO1_BASE);
    final PIORegisters pio1Registers = pio1Sdk.getRegisters();
    registersList.add(pio1Registers);
    final PIOEmuRegisters pio1EmuRegisters = pio1Sdk.getEmuRegisters();
    registersList.add(pio1EmuRegisters);

  }

  public GPIOSDK getGPIOSDK() { return gpioSdk; }
  public PIOSDK getPIO0SDK() { return pio0Sdk; }
  public PIOSDK getPIO1SDK() { return pio1Sdk; }

  public Registers getProvidingRegisters(final int address)
  {
    for (final Registers registers : registersList) {
      if (registers.providesAddress(address)) {
        return registers;
      }
    }
    return null;
  }

  public int readAddress(final int address)
  {
    final Registers registers = getProvidingRegisters(address);
    return registers != null ? registers.readAddress(address) : 0;
  }

  public int readAddress(final int address, final int msb, final int lsb)
  {
    Constants.checkMSBLSB(msb, lsb);
    final int value = readAddress(address);
    return
      (msb - lsb == 31) ?
      value :
      (value >>> lsb) & ((0x1 << (msb - lsb + 1)) - 1);
  }

  public void writeAddress(final int address, final int value)
  {
    final Registers registers = getProvidingRegisters(address);
    if (registers != null) registers.writeAddress(address, value);
  }

  public void irqWaitAddress(final int address)
  {
    final Registers registers = getProvidingRegisters(address);
    if (registers != null) registers.irqWaitAddress(address);
  }

  // -------- address helpers --------

  public String getLabelForAddress(final int address)
  {
    final Registers registers = getProvidingRegisters(address);
    return registers != null ? registers.getLabel(address) : null;
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
