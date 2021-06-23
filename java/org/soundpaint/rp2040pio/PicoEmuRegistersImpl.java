/*
 * @(#)PicoEmuRegistersImpl.java 1.00 21/03/12
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

import org.soundpaint.rp2040pio.Clock;

/**
 * Facade to additonal emulator properties of the internal subsystems
 * of a PIO that are not available via the PIORegisters facade.  This
 * facade is in particular intended for use by software that wants to
 * exploit the emulator's debug facilities.
 */
public class PicoEmuRegistersImpl extends PicoEmuRegisters
{
  private final Emulator emulator;

  public PicoEmuRegistersImpl(final Emulator emulator)
  {
    this.emulator = emulator;
  }

  public Emulator getEmulator() { return emulator; }

  @Override
  public void writeRegister(final int regNum, final int value,
                            final int mask, final boolean xor)
  {
    checkRegNum(regNum);
    final Regs register = REGS[regNum];
    switch (register) {
    case PWR_UP:
      if (value == PICO_PWR_UP_VALUE) emulator.reset();
      break;
    case MASTERCLK_FREQ:
      emulator.getMasterClock().setMASTERCLK_FREQ(value);
      break;
    case MASTERCLK_MODE:
      emulator.getMasterClock().setMASTERCLK_MODE(value);
      break;
    case MASTERCLK_TRIGGER_PHASE0:
      emulator.getMasterClock().triggerPhase0();
      break;
    case MASTERCLK_TRIGGER_PHASE1:
      emulator.getMasterClock().triggerPhase1();
      break;
    case WALLCLOCK_LSB:
    case WALLCLOCK_MSB:
      break; // read-only address
    case GPIO_PADIN:
      emulator.getGPIO().setGPIO_PADIN(value, mask, xor);
      break;
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }

  @Override
  public synchronized int readRegister(final int regNum)
  {
    checkRegNum(regNum);
    final Regs register = REGS[regNum];
    switch (register) {
    case PWR_UP:
      return 0; // write-only address
    case MASTERCLK_FREQ:
      return emulator.getMasterClock().getMASTERCLK_FREQ();
    case MASTERCLK_MODE:
      return emulator.getMasterClock().getMASTERCLK_MODE();
    case MASTERCLK_TRIGGER_PHASE0:
      return
        emulator.getMasterClock().getPhase() == Clock.Phase.PHASE_0_STABLE ?
        0x1 : 0x0;
    case MASTERCLK_TRIGGER_PHASE1:
      return
        emulator.getMasterClock().getPhase() == Clock.Phase.PHASE_1_STABLE ?
        0x1 : 0x0;
    case WALLCLOCK_LSB:
      return (int)emulator.getMasterClock().getWallClock();
    case WALLCLOCK_MSB:
      return (int)(emulator.getMasterClock().getWallClock() >>> 32);
    case GPIO_PADIN:
      return emulator.getGPIO().getGPIO_PADIN();
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
