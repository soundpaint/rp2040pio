/*
 * @(#)GPIOPadsBank0Registers.java 1.00 21/03/20
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
import org.soundpaint.rp2040pio.doctool.RegistersDocs;

/**
 * Facade to the internal GPIO Pads Bank 0 subsystem.  The layout of
 * registers follows the list of registers in Sect. 2.19.6 of the
 * RP2040 datasheet.  The facade is in particular intended for use by
 * the SDK.
 *
 * Note: This class implements only a subset of the RP2040 GPIO set of
 * registers, focussing on those registers that are relevant for PIO
 * simulation.  All GPIO registers specified by the RP2040 datasheet
 * are addressable, but writing to non-relevant registers or register
 * bits will have no effect, and reading from non-relevant registers
 * or register bits will return a constant value of 0.
 */
public abstract class GPIOPadsBank0Registers extends RegisterSet
{
  public enum Regs implements RegistersDocs<Regs>
  {
    VOLTAGE_SELECT("Voltage select.",
                   new BitsInfo[] {
                     new BitsInfo(null, 31, 1, null, BitsType.RESERVED, null),
                     new BitsInfo(null, 0, 0,  "3.3V / 1.8V", BitsType.RW, null)
                   }),
    GPIO0("Pad control register.",
          new BitsInfo[] {
            new BitsInfo(null, 31, 8, null, BitsType.RESERVED, null),
            new BitsInfo("OD", 7, 7, "Output disable.", BitsType.RW, 0),
            new BitsInfo("IE", 6, 6, "Input enable.", BitsType.RW, 1),
            new BitsInfo("DRIVE", 5, 4, "Drive strength.", BitsType.RW, 1),
            new BitsInfo("PUE", 3, 3, "Pull up enable.", BitsType.RW, 0),
            new BitsInfo("PDE", 2, 2, "Pull down enable.", BitsType.RW, 1),
            new BitsInfo("SCHMITT", 1, 1, "Enable schmitt trigger",
                         BitsType.RW, 1),
            new BitsInfo("SLEWFAST", 0, 0, "Slew rate control.",
                         BitsType.RW, 0)
          }),
    GPIO1(Regs.GPIO0),
    GPIO2(Regs.GPIO0),
    GPIO3(Regs.GPIO0),
    GPIO4(Regs.GPIO0),
    GPIO5(Regs.GPIO0),
    GPIO6(Regs.GPIO0),
    GPIO7(Regs.GPIO0),
    GPIO8(Regs.GPIO0),
    GPIO9(Regs.GPIO0),
    GPIO10(Regs.GPIO0),
    GPIO11(Regs.GPIO0),
    GPIO12(Regs.GPIO0),
    GPIO13(Regs.GPIO0),
    GPIO14(Regs.GPIO0),
    GPIO15(Regs.GPIO0),
    GPIO16(Regs.GPIO0),
    GPIO17(Regs.GPIO0),
    GPIO18(Regs.GPIO0),
    GPIO19(Regs.GPIO0),
    GPIO20(Regs.GPIO0),
    GPIO21(Regs.GPIO0),
    GPIO22(Regs.GPIO0),
    GPIO23(Regs.GPIO0),
    GPIO24(Regs.GPIO0),
    GPIO25(Regs.GPIO0),
    GPIO26(Regs.GPIO0),
    GPIO27(Regs.GPIO0),
    GPIO28(Regs.GPIO0),
    GPIO29(Regs.GPIO0),
    SWCLK("Pad control register.",
          new BitsInfo[] {
            new BitsInfo(null, 31, 8, null, BitsType.RESERVED, null),
            new BitsInfo("OD", 7, 7, "Output disable.", BitsType.RW, 1),
            new BitsInfo("IE", 6, 6, "Input enable.", BitsType.RW, 1),
            new BitsInfo("DRIVE", 5, 4, "Drive strength.", BitsType.RW, 1),
            new BitsInfo("PUE", 3, 3, "Pull up enable.", BitsType.RW, 1),
            new BitsInfo("PDE", 2, 2, "Pull down enable.", BitsType.RW, 0),
            new BitsInfo("SCHMITT", 1, 1, "Enable schmitt trigger",
                         BitsType.RW, 1),
            new BitsInfo("SLEWFAST", 0, 0, "Slew rate control.",
                         BitsType.RW, 0)
          }),
    SWD("Pad control register.",
          new BitsInfo[] {
            new BitsInfo(null, 31, 8, null, BitsType.RESERVED, null),
            new BitsInfo("OD", 7, 7, "Output disable.", BitsType.RW, 0),
            new BitsInfo("IE", 6, 6, "Input enable.", BitsType.RW, 1),
            new BitsInfo("DRIVE", 5, 4, "Drive strength.", BitsType.RW, 1),
            new BitsInfo("PUE", 3, 3, "Pull up enable.", BitsType.RW, 1),
            new BitsInfo("PDE", 2, 2, "Pull down enable.", BitsType.RW, 0),
            new BitsInfo("SCHMITT", 1, 1, "Enable schmitt trigger",
                         BitsType.RW, 1),
            new BitsInfo("SLEWFAST", 0, 0, "Slew rate control.",
                         BitsType.RW, 0)
          });

    public static String getRegisterSetLabel()
    {
      return "GPIO User Bank Pad Control Registers";
    }

    public static String getRegisterSetDescription()
    {
      return
        "The GPIO user bank pad control registers as described in%n" +
        "Sect. 2.19.6.3 of the RP2040 datasheet.%n" +
        "Base address for this register set is%n" +
        String.format("0x%08x.%n", PADS_BANK0_BASE);
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

  public static int getAddress(final GPIOPadsBank0Registers.Regs register)
  {
    if (register == null) {
      throw new NullPointerException("register");
    }
    return PADS_BANK0_BASE + 0x4 * register.ordinal();
  }

  public static int getGPIOAddress(final int gpioNum)
  {
    Constants.checkGpioPin(gpioNum, "GPIO pin number");
    return PADS_BANK0_BASE + 0x4 * (Regs.GPIO0.ordinal() + gpioNum);
  }

  public GPIOPadsBank0Registers()
  {
    super("GPIOPadsBank0", PADS_BANK0_BASE);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
