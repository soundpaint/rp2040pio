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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.soundpaint.rp2040pio.doctool.RegistersDocs;

/**
 * Facade to additonal emulator properties of the internal subsystems
 * of a PIO that are not available via the PIORegisters facade.  This
 * facade is in particular intended for use by software that wants to
 * exploit the emulator's debug facilities.
 */
public abstract class PicoEmuRegisters extends RegisterSet
{
  public enum Regs implements RegistersDocs<Regs>
  {
    PWR_UP("Writing the value 0xa55a5aa5 to this address will fully reset%n" +
           "the emulator.  Writing any other value will have no effect.",
             new BitsInfo[] {
               new BitsInfo(null, 31, 0, null, BitsType.WF, 0)
             }),
    MASTERCLK_FREQ("Unsigned integer value that represents the%n" +
                   "target frequency of the emulation in 1/8Hz.%n" +
                   "That is, a value of 1 represents a frequency of%n" +
                   "0.125 Hz, and the maximum value of 2^32 - 1 =%n" +
                   "4294967295 represents a frequency of%n" +
                   "536.870911875MHz.%n" +
                   "%n" +
                   "A value of 0 indicates that the emulation should%n" +
                   "execute as fast as possible.%n" +
                   "%n" +
                   "Note that there is no guarantee at all to run at%n" +
                   "the specified frequency.  Instead, the value is%n" +
                   "just the frequency that the emulation tries to%n" +
                   "catch up with as close as possible.  The reset%n" +
                   "value corresponds to a target frequency of 125MHz.",
                   new BitsInfo[] {
                     new BitsInfo(null, 31, 0, null, BitsType.RW,
                                  DEFAULT_FREQUENCY)
                   }),
    MASTERCLK_MODE("Selects the clock mode.",
                   new BitsInfo[] {
                     new BitsInfo(null, 31, 1, null, BitsType.RESERVED, null),
                     new BitsInfo(null, 0, 0,
                                  "Bit 0 = 0: Target frequency mode.%n" +
                                  "Bit 0 = 1: Single step mode.",
                                  BitsType.RW, 0)
                   }),
    MASTERCLK_TRIGGER_PHASE0("When master clock is in single step%n" +
                             "mode, writing any value to this address%n" +
                             "will trigger the emulator to execute phase%n" +
                             "0 of the next clock cycle.  In phase%n" +
                             "0, the emulator fetches and decodes the%n" +
                             "next instruction.  When already in phase%n" +
                             "0, writing once more to this address will%n" +
                             "have no effect.  When master clock is in%n" +
                             "target frequency mode, writing to this%n" +
                             "address will have no effect.  Upon reset,%n" +
                             "the system is in phase 1.%n" +
                             "Reading from this register will return value%n" +
                             "0x1 if and only if the emulator is in phase%n" +
                             "0 *and* phase 0 is settled (i.e. the emulator%n" +
                             "has completed all operations to be performed%n" +
                             "during this phase), and 0x0 otherwise.",
                             new BitsInfo[] {
                               new BitsInfo(null, 31, 0, null,
                                            BitsType.WF, null)
                             }),
    MASTERCLK_TRIGGER_PHASE1("When master clock is in single step%n" +
                             "mode, writing any value to this address%n" +
                             "will trigger the emulator to execute phase%n" +
                             "1 of the current clock cycle.  In phase%n" +
                             "1, the emulator will execute the%n" +
                             "instruction previously decoded in%n" +
                             "phase 0.  When already in phase%n" +
                             "1, writing once more to this address will%n" +
                             "have no effect. When master clock is in%n" +
                             "target frequency mode, writing to this%n" +
                             "address will have no effect.  Upon reset,%n" +
                             "the system is in phase 1.%n" +
                             "Reading from this register will return value%n" +
                             "0x1 if and only if the emulator is in phase%n" +
                             "1 *and* phase 1 is settled (i.e. the emulator%n" +
                             "has completed all operations to be performed%n" +
                             "during this phase), and 0x0 otherwise.",
                             new BitsInfo[] {
                               new BitsInfo(null, 31, 0, null,
                                            BitsType.WF, null)
                             }),
    WALLCLOCK_LSB("LSB value (lower 32 bits) of wall clock.  The%n" +
                  "wall clock is a 64 bit counter that is initialized%n" +
                  "to 0 and incremented whenever the master clock has%n" +
                  "completed a cycle.",
                  new BitsInfo[] {
                    new BitsInfo(null, 31, 0, null, BitsType.RO, null)
                  }),
    WALLCLOCK_MSB("MSB value (upper 32 bits) of wall clock.  The%n" +
                  "wall clock is a 64 bit counter that is initialized%n" +
                  "to 0 and incremented whenever the master clock has%n" +
                  "completed a cycle.",
                  new BitsInfo[] {
                    new BitsInfo(null, 31, 0, null, BitsType.RO, null)
                  }),
    GPIO_PADIN("Each bit of this value represents the corresponding%n" +
               "pad input state of the 32 GPIO pins, virtually provided%n" +
               "from some external source.",
               IntStream.rangeClosed(0, 31).boxed()
               .map(n -> new BitsInfo("INFROMPAD_GPIO" + (31 - n),
                                      31 - n, 31 - n,
                                      "signal value 0x0 or 0x1, as%n" +
                                      "provided by some external source.",
                                      BitsType.RW, 0))
               .collect(Collectors.toList()));

    public static String getRegisterSetLabel()
    {
      return "Emulator Global Registers";
    }

    public static String getRegisterSetDescription()
    {
      return
        "The PIO emulator provides global registers, hereafter%n" +
        "called *Emulator Global Registers*, that are used to inspect%n" +
        "and control the emulator as a whole (rather than just%n" +
        "referring to a specifc PIO) and that are accessible through%n" +
        "this registers facade and provided in addition to the%n" +
        "registers of the original RP2040 hardware.%n" +
        "Base address for the emulator global register set is%n" +
        String.format("0x%08x.%n", EMULATOR_BASE);
    }

    private final RegisterDetails registerDetails;

    private Regs()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private Regs(final Regs ref)
    {
      this(ref.registerDetails);
    }

    private Regs(final Regs ref, final int smNum)
    {
      this(ref.registerDetails.createCopyForDifferentSm(smNum));
    }

    private Regs(final String info, final BitsInfo[] bitsInfos)
    {
      this(new RegisterDetails(info, bitsInfos));
    }

    private Regs(final String info, final List<BitsInfo> bitsInfos)
    {
      this(new RegisterDetails(info, bitsInfos));
    }

    private Regs(final String info, final int smNum,
                 final BitsInfo[] bitsInfos)
    {
      this(new RegisterDetails(info, smNum, bitsInfos));
    }

    private Regs(final String info, final int smNum,
                 final List<BitsInfo> bitsInfos)
    {
      this(new RegisterDetails(info, smNum, bitsInfos));
    }

    private Regs(final RegisterDetails registerDetails)
    {
      this.registerDetails = registerDetails;
    }

    @Override
    public String getInfo()
    {
      return registerDetails.getInfo();
    }

    @Override
    public RegisterDetails getRegisterDetails()
    {
      return registerDetails;
    }
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

  public PicoEmuRegisters()
  {
    super("PicoEmu", EMULATOR_BASE);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
