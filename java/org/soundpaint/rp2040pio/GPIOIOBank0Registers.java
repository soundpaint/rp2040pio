/*
 * @(#)GPIOIOBank0Registers.java 1.00 21/03/20
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.soundpaint.rp2040pio.doctool.RegistersDocs;

/**
 * Facade to the internal GPIO IO Bank 0 subsystem.  The layout of
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
public abstract class GPIOIOBank0Registers extends RegisterSet
{
  public enum Regs implements RegistersDocs<Regs>
  {
    GPIO0_STATUS("GPIO status.",
                 new BitsInfo[] {
                   new BitsInfo(null, 31, 27, null, BitsType.RESERVED, null),
                   new BitsInfo("IRQTOPROC", 26, 26, null, BitsType.RO, 0),
                   new BitsInfo(null, 25, 25, null, BitsType.RESERVED, null),
                   new BitsInfo("IRQFROMPAD", 24, 24, null, BitsType.RO, 0),
                   new BitsInfo(null, 23, 20, null, BitsType.RESERVED, null),
                   new BitsInfo("INTOPERI", 19, 19, null, BitsType.RO, 0),
                   new BitsInfo(null, 18, 18, null, BitsType.RESERVED, null),
                   new BitsInfo("INFROMPAD", 17, 17, null, BitsType.RO, 0),
                   new BitsInfo(null, 16, 14, null, BitsType.RESERVED, null),
                   new BitsInfo("OETOPAD", 13, 13, null, BitsType.RO, 0),
                   new BitsInfo("OEFROMPERI", 12, 12, null, BitsType.RO, 0),
                   new BitsInfo(null, 11, 10, null, BitsType.RESERVED, null),
                   new BitsInfo("OUTTOPAD", 9, 9, null, BitsType.RO, 0),
                   new BitsInfo("OUTFROMPERI", 8, 8, null, BitsType.RO, 0),
                   new BitsInfo(null, 7, 0, null, BitsType.RESERVED, null)
                 }),
    GPIO0_CTRL("GPIO control.",
               new BitsInfo[] {
                 new BitsInfo(null, 31, 30, null, BitsType.RESERVED, null),
                 new BitsInfo("IRQOVER", 29, 28, null, BitsType.RW, 0),
                 new BitsInfo(null, 27, 18, null, BitsType.RESERVED, null),
                 new BitsInfo("INOVER", 17, 16, null, BitsType.RW, 0),
                 new BitsInfo(null, 15, 14, null, BitsType.RESERVED, null),
                 new BitsInfo("OEOVER", 13, 12, null, BitsType.RW, 0),
                 new BitsInfo(null, 11, 10, null, BitsType.RESERVED, null),
                 new BitsInfo("OUTOVER", 9, 8, null, BitsType.RW, 0),
                 new BitsInfo(null, 7, 5, null, BitsType.RESERVED, null),
                 new BitsInfo("FUNCSEL", 4, 0, null, BitsType.RW, 0x1f)
               }),
    GPIO1_STATUS(Regs.GPIO0_STATUS),
    GPIO1_CTRL(Regs.GPIO0_CTRL),
    GPIO2_STATUS(Regs.GPIO0_STATUS),
    GPIO2_CTRL(Regs.GPIO0_CTRL),
    GPIO3_STATUS(Regs.GPIO0_STATUS),
    GPIO3_CTRL(Regs.GPIO0_CTRL),
    GPIO4_STATUS(Regs.GPIO0_STATUS),
    GPIO4_CTRL(Regs.GPIO0_CTRL),
    GPIO5_STATUS(Regs.GPIO0_STATUS),
    GPIO5_CTRL(Regs.GPIO0_CTRL),
    GPIO6_STATUS(Regs.GPIO0_STATUS),
    GPIO6_CTRL(Regs.GPIO0_CTRL),
    GPIO7_STATUS(Regs.GPIO0_STATUS),
    GPIO7_CTRL(Regs.GPIO0_CTRL),
    GPIO8_STATUS(Regs.GPIO0_STATUS),
    GPIO8_CTRL(Regs.GPIO0_CTRL),
    GPIO9_STATUS(Regs.GPIO0_STATUS),
    GPIO9_CTRL(Regs.GPIO0_CTRL),
    GPIO10_STATUS(Regs.GPIO0_STATUS),
    GPIO10_CTRL(Regs.GPIO0_CTRL),
    GPIO11_STATUS(Regs.GPIO0_STATUS),
    GPIO11_CTRL(Regs.GPIO0_CTRL),
    GPIO12_STATUS(Regs.GPIO0_STATUS),
    GPIO12_CTRL(Regs.GPIO0_CTRL),
    GPIO13_STATUS(Regs.GPIO0_STATUS),
    GPIO13_CTRL(Regs.GPIO0_CTRL),
    GPIO14_STATUS(Regs.GPIO0_STATUS),
    GPIO14_CTRL(Regs.GPIO0_CTRL),
    GPIO15_STATUS(Regs.GPIO0_STATUS),
    GPIO15_CTRL(Regs.GPIO0_CTRL),
    GPIO16_STATUS(Regs.GPIO0_STATUS),
    GPIO16_CTRL(Regs.GPIO0_CTRL),
    GPIO17_STATUS(Regs.GPIO0_STATUS),
    GPIO17_CTRL(Regs.GPIO0_CTRL),
    GPIO18_STATUS(Regs.GPIO0_STATUS),
    GPIO18_CTRL(Regs.GPIO0_CTRL),
    GPIO19_STATUS(Regs.GPIO0_STATUS),
    GPIO19_CTRL(Regs.GPIO0_CTRL),
    GPIO20_STATUS(Regs.GPIO0_STATUS),
    GPIO20_CTRL(Regs.GPIO0_CTRL),
    GPIO21_STATUS(Regs.GPIO0_STATUS),
    GPIO21_CTRL(Regs.GPIO0_CTRL),
    GPIO22_STATUS(Regs.GPIO0_STATUS),
    GPIO22_CTRL(Regs.GPIO0_CTRL),
    GPIO23_STATUS(Regs.GPIO0_STATUS),
    GPIO23_CTRL(Regs.GPIO0_CTRL),
    GPIO24_STATUS(Regs.GPIO0_STATUS),
    GPIO24_CTRL(Regs.GPIO0_CTRL),
    GPIO25_STATUS(Regs.GPIO0_STATUS),
    GPIO25_CTRL(Regs.GPIO0_CTRL),
    GPIO26_STATUS(Regs.GPIO0_STATUS),
    GPIO26_CTRL(Regs.GPIO0_CTRL),
    GPIO27_STATUS(Regs.GPIO0_STATUS),
    GPIO27_CTRL(Regs.GPIO0_CTRL),
    GPIO28_STATUS(Regs.GPIO0_STATUS),
    GPIO28_CTRL(Regs.GPIO0_CTRL),
    GPIO29_STATUS(Regs.GPIO0_STATUS),
    GPIO29_CTRL(Regs.GPIO0_CTRL),
    INTR0("Raw interrupts.", createBitsInfo(0, null)),
    INTR1("Raw interrupts.", createBitsInfo(1, null)),
    INTR2("Raw interrupts.", createBitsInfo(2, null)),
    INTR3("Raw interrupts.", createBitsInfo(3, null)),
    PROC0_INTE0("Interrupt enable for proc0.", createBitsInfo(0, BitsType.RW)),
    PROC0_INTE1("Interrupt enable for proc0.", createBitsInfo(1, BitsType.RW)),
    PROC0_INTE2("Interrupt enable for proc0.", createBitsInfo(2, BitsType.RW)),
    PROC0_INTE3("Interrupt enable for proc0.", createBitsInfo(3, BitsType.RW)),
    PROC0_INTF0("Interrupt force for proc0.", createBitsInfo(0, BitsType.RW)),
    PROC0_INTF1("Interrupt force for proc0.", createBitsInfo(1, BitsType.RW)),
    PROC0_INTF2("Interrupt force for proc0.", createBitsInfo(2, BitsType.RW)),
    PROC0_INTF3("Interrupt force for proc0.", createBitsInfo(3, BitsType.RW)),
    PROC0_INTS0("Interrupt status for proc0.", createBitsInfo(0, BitsType.RO)),
    PROC0_INTS1("Interrupt status for proc0.", createBitsInfo(1, BitsType.RO)),
    PROC0_INTS2("Interrupt status for proc0.", createBitsInfo(2, BitsType.RO)),
    PROC0_INTS3("Interrupt status for proc0.", createBitsInfo(3, BitsType.RO)),
    PROC1_INTE0("Interrupt enable for proc1.", createBitsInfo(0, BitsType.RW)),
    PROC1_INTE1("Interrupt enable for proc1.", createBitsInfo(1, BitsType.RW)),
    PROC1_INTE2("Interrupt enable for proc1.", createBitsInfo(2, BitsType.RW)),
    PROC1_INTE3("Interrupt enable for proc1.", createBitsInfo(3, BitsType.RW)),
    PROC1_INTF0("Interrupt force for proc1.", createBitsInfo(0, BitsType.RW)),
    PROC1_INTF1("Interrupt force for proc1.", createBitsInfo(1, BitsType.RW)),
    PROC1_INTF2("Interrupt force for proc1.", createBitsInfo(2, BitsType.RW)),
    PROC1_INTF3("Interrupt force for proc1.", createBitsInfo(3, BitsType.RW)),
    PROC1_INTS0("Interrupt status for proc1.", createBitsInfo(0, BitsType.RO)),
    PROC1_INTS1("Interrupt status for proc1.", createBitsInfo(1, BitsType.RO)),
    PROC1_INTS2("Interrupt status for proc1.", createBitsInfo(2, BitsType.RO)),
    PROC1_INTS3("Interrupt status for proc1.", createBitsInfo(3, BitsType.RO)),
    DORMANT_WAKE_INTE0("Interrupt enable for dormant wake.",
                       createBitsInfo(0, BitsType.RW)),
    DORMANT_WAKE_INTE1("Interrupt enable for dormant wake.",
                       createBitsInfo(1, BitsType.RW)),
    DORMANT_WAKE_INTE2("Interrupt enable for dormant wake.",
                       createBitsInfo(2, BitsType.RW)),
    DORMANT_WAKE_INTE3("Interrupt enable for dormant wake.",
                       createBitsInfo(3, BitsType.RW)),
    DORMANT_WAKE_INTF0("Interrupt force for dormant wake.",
                       createBitsInfo(0, BitsType.RW)),
    DORMANT_WAKE_INTF1("Interrupt force for dormant wake.",
                       createBitsInfo(1, BitsType.RW)),
    DORMANT_WAKE_INTF2("Interrupt force for dormant wake.",
                       createBitsInfo(2, BitsType.RW)),
    DORMANT_WAKE_INTF3("Interrupt force for dormant wake.",
                       createBitsInfo(3, BitsType.RW)),
    DORMANT_WAKE_INTS0("Interrupt status for dormant wake.",
                       createBitsInfo(0, BitsType.RO)),
    DORMANT_WAKE_INTS1("Interrupt status for dormant wake.",
                       createBitsInfo(1, BitsType.RO)),
    DORMANT_WAKE_INTS2("Interrupt status for dormant wake.",
                       createBitsInfo(2, BitsType.RO)),
    DORMANT_WAKE_INTS3("Interrupt status for dormant wake.",
                       createBitsInfo(3, BitsType.RO));

    private static List<BitsInfo> createBitsInfo(final int octett,
                                                 final BitsType forcedBitsType)
    {
      if (octett < 0) {
        throw new IllegalArgumentException("octett < 0: " + octett);
      }
      if (octett > 3) {
        throw new IllegalArgumentException("octett > 3: " + octett);
      }
      final int gpioMin = octett << 3;
      final int gpioCount = octett < 3 ? 8 : 6;
      final int gpioMax = gpioCount << 2;
      final List<BitsInfo> bitsInfo =
        IntStream.rangeClosed(0, gpioMax - 1).boxed()
        .map(n -> new BitsInfo("GPIO" + (((gpioMax - 1 - n) >> 2) + gpioMin) +
                               "_" + ((n & 2) == 0 ? "EDGE" : "LEVEL") +
                               "_" + ((n & 1) == 0 ? "HIGH" : "LOW"),
                               gpioMax - 1 - n, gpioMax - 1 - n, null,
                               forcedBitsType != null ? forcedBitsType :
                               ((n & 2) == 0 ? BitsType.WC : BitsType.RO), 0))
        .collect(Collectors.toList());
      if (octett < 3) {
        return bitsInfo;
      } else {
        final List<BitsInfo> paddedBitsInfo = new ArrayList<BitsInfo>();
        paddedBitsInfo.add(new BitsInfo(null, 31, 24, null,
                                        BitsType.RESERVED, null));
        paddedBitsInfo.addAll(bitsInfo);
        return paddedBitsInfo;
      }
    }

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

  protected static final int GPIO_DATA_SIZE =
    Regs.GPIO1_STATUS.ordinal() - Regs.GPIO0_STATUS.ordinal();

  protected static final int PROC_INT_DATA_SIZE =
    Regs.PROC1_INTE0.ordinal() - Regs.PROC0_INTE0.ordinal();

  public static int getAddress(final GPIOIOBank0Registers.Regs register)
  {
    if (register == null) {
      throw new NullPointerException("register");
    }
    return IO_BANK0_BASE + 0x4 * register.ordinal();
  }

  public static int getGPIOAddress(final int gpioNum,
                                   final GPIOIOBank0Registers.Regs register)
  {
    Constants.checkGpioPin(gpioNum, "GPIO pin number");
    if (register == null) {
      throw new NullPointerException("register");
    }
    switch (register) {
    case GPIO0_STATUS:
    case GPIO0_CTRL:
      break; // ok
    default:
      throw new IllegalArgumentException("register not one of GPIO0_*: " +
                                         register);
    }
    return
      IO_BANK0_BASE + 0x4 * (register.ordinal() + gpioNum * GPIO_DATA_SIZE);
  }

  public static int getIntr(final int intrNum)
  {
    Constants.checkIntrNum(intrNum, "INTR number");
    return IO_BANK0_BASE + 0x4 * (Regs.INTR0.ordinal() + intrNum);
  }

  public static int getProcIntE(final int pioNum, final int inteNum)
  {
    Constants.checkPioNum(pioNum, "PIO number");
    Constants.checkIntrNum(inteNum, "INTE number");
    final int regsOffs = pioNum * PROC_INT_DATA_SIZE + inteNum;
    return IO_BANK0_BASE + 0x4 * (Regs.PROC0_INTE0.ordinal() + regsOffs);
  }

  public static int getProcIntF(final int pioNum, final int intfNum)
  {
    Constants.checkPioNum(pioNum, "PIO number");
    Constants.checkIntrNum(intfNum, "INTF number");
    final int regsOffs = pioNum * PROC_INT_DATA_SIZE + intfNum;
    return IO_BANK0_BASE + 0x4 * (Regs.PROC0_INTF0.ordinal() + regsOffs);
  }

  public static int getProcIntS(final int pioNum, final int intsNum)
  {
    Constants.checkPioNum(pioNum, "PIO number");
    Constants.checkIntrNum(intsNum, "INTS number");
    final int regsOffs = pioNum * PROC_INT_DATA_SIZE + intsNum;
    return IO_BANK0_BASE + 0x4 * (Regs.PROC0_INTS0.ordinal() + regsOffs);
  }

  public static int getDormantWakeIntE(final int inteNum)
  {
    Constants.checkIntrNum(inteNum, "Dormant Wake INTE number");
    return IO_BANK0_BASE + 0x4 * (Regs.DORMANT_WAKE_INTE0.ordinal() + inteNum);
  }

  public static int getDormantWakeIntF(final int intfNum)
  {
    Constants.checkIntrNum(intfNum, "Dormant Wake INTF number");
    return IO_BANK0_BASE + 0x4 * (Regs.DORMANT_WAKE_INTF0.ordinal() + intfNum);
  }

  public static int getDormantWakeIntS(final int intsNum)
  {
    Constants.checkIntrNum(intsNum, "Dormant Wake INTS number");
    return IO_BANK0_BASE + 0x4 * (Regs.DORMANT_WAKE_INTS0.ordinal() + intsNum);
  }

  public GPIOIOBank0Registers()
  {
    super("GPIOIOBank0", IO_BANK0_BASE);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
