/*
 * @(#)PicoEmuRegisters.java 1.00 21/03/12
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

import java.util.function.LongSupplier;

/**
 * Facade to additonal emulator properties of the internal subsystems
 * of a PIO that are not available via the PIORegisters facade.  This
 * facade is in particular intended for use by software that wants to
 * exploit the emulator's debug facilities.
 */
public abstract class PicoEmuRegisters extends AbstractRegisters
  implements Constants
{
  public enum Regs {
    /**
     * W/C address.
     *
     * Writing the value 0xa55a5aa5 to this address will fully reset
     * the emulator.  Writing any other value will have no effect.
     */
    PWR_UP,
    /**
     * R/W address.  Reset value: 1000000000.
     *
     * Unsigned integer value that represents the target frequency of
     * the emulation in 1/8Hz.  That is, a value of 1 represents a
     * frequency of 0.125 Hz, and the maximum value of 2^32 - 1 =
     * 4294967295 represents a frequency of 536.870911875MHz.
     *
     * A value of 0 indicates that the emulation should execute as
     * fast as possible.
     *
     * Note that there is no guarantee at all to run at the specified
     * frequency.  Instead, the value is just the frequency that the
     * emulation tries to catch up with as close as possible.  The
     * reset value corresponds to a target frequency of 125MHz.
     */
    MASTERCLK_FREQ,
    /**
     * R/W address.  Reset value: 0.
     *
     * Bit 0 = 0: Target frequency mode.
     * Bit 0 = 1: Single step mode.
     *
     * Bits 1..31: Reserved.
     */
    MASTERCLK_MODE,
    /**
     * W/C address.
     *
     * When master clock is in single step mode, writing any value to
     * this address will trigger the emulator to execute phase 0 of
     * the next clock cycle.  In phase 0, the emulator fetches and
     * decodes the next instruction.  When already in phase 0, writing
     * once more to this address will have no effect.  When master
     * clock is in target frequency mode, writing to this address will
     * have no effect.  Upon reset, the system is in phase 1.
     */
    MASTERCLK_TRIGGER_PHASE0,
    /**
     * W/C address.
     *
     * When master clock is in single step mode, writing any value to
     * this address will trigger the emulator to execute phase 1 of
     * the current clock cycle.  In phase 1, the emulator will execute
     * the instruction previously decoded in phase 0.  When already in
     * phase 1, writing once more to this address will have no effect.
     * When master clock is in target frequency mode, writing to this
     * address will have no effect.  Upon reset, the system is in
     * phase 1.
     */
    MASTERCLK_TRIGGER_PHASE1,
    /**
     * R/O address.
     *
     * LSB value (lower 32 bits) of wall clock.  The wall clock is a
     * 64 bit counter that is initialized to 0 and incremented
     * whenever the master clock has completed a cycle.
     */
    WALLCLOCK_LSB,
    /**
     * R/O address.
     *
     * MSB value (upper 32 bits) of wall clock.  The wall clock is a
     * 64 bit counter that is initialized to 0 and incremented
     * whenever the master clock has completed a cycle.
     */
    WALLCLOCK_MSB;
  }

  protected static final Regs[] REGS = Regs.values();

  @Override
  @SuppressWarnings("unchecked")
  protected <T extends Enum<T>> T[] getRegs() { return (T[])REGS; }

  public static int getAddress(final PicoEmuRegisters.Regs register)
  {
    if (register == null) {
      throw new NullPointerException("register");
    }
    return EMULATOR_BASE + 0x4 * register.ordinal();
  }

  public PicoEmuRegisters(final LongSupplier wallClockSupplier)
  {
    super(EMULATOR_BASE, (short)REGS.length, wallClockSupplier);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
