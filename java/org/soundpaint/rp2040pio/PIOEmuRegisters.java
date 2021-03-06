/*
 * @(#)PIOEmuRegisters.java 1.00 21/03/06
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

/**
 * Facade to additonal emulator properties of the internal subsystems
 * of a PIO that are not available via the PIORegisters facade.  This
 * facade is in particular intended for use by software that wants to
 * exploit the emulator's debug facilities.
 */
public class PIOEmuRegisters extends AbstractRegisters implements Constants
{
  public enum Regs {
    SM0_REGX,
    SM0_REGY,
    SM0_ISR,
    SM0_ISR_SHIFT_COUNT,
    SM0_OSR,
    SM0_OSR_SHIFT_COUNT,
    SM0_FIFO_MEM0,
    SM0_FIFO_MEM1,
    SM0_FIFO_MEM2,
    SM0_FIFO_MEM3,
    SM0_FIFO_MEM4,
    SM0_FIFO_MEM5,
    SM0_FIFO_MEM6,
    SM0_FIFO_MEM7,
    SM0_PENDING_DELAY,
    SM0_CLK_ENABLE,
    SM1_REGX,
    SM1_REGY,
    SM1_ISR,
    SM1_ISR_SHIFT_COUNT,
    SM1_OSR,
    SM1_OSR_SHIFT_COUNT,
    SM1_FIFO_MEM0,
    SM1_FIFO_MEM1,
    SM1_FIFO_MEM2,
    SM1_FIFO_MEM3,
    SM1_FIFO_MEM4,
    SM1_FIFO_MEM5,
    SM1_FIFO_MEM6,
    SM1_FIFO_MEM7,
    SM1_PENDING_DELAY,
    SM1_CLK_ENABLE,
    SM2_REGX,
    SM2_REGY,
    SM2_ISR,
    SM2_ISR_SHIFT_COUNT,
    SM2_OSR,
    SM2_OSR_SHIFT_COUNT,
    SM2_FIFO_MEM0,
    SM2_FIFO_MEM1,
    SM2_FIFO_MEM2,
    SM2_FIFO_MEM3,
    SM2_FIFO_MEM4,
    SM2_FIFO_MEM5,
    SM2_FIFO_MEM6,
    SM2_FIFO_MEM7,
    SM2_PENDING_DELAY,
    SM2_CLK_ENABLE,
    SM3_REGX,
    SM3_REGY,
    SM3_ISR,
    SM3_ISR_SHIFT_COUNT,
    SM3_OSR,
    SM3_OSR_SHIFT_COUNT,
    SM3_FIFO_MEM0,
    SM3_FIFO_MEM1,
    SM3_FIFO_MEM2,
    SM3_FIFO_MEM3,
    SM3_FIFO_MEM4,
    SM3_FIFO_MEM5,
    SM3_FIFO_MEM6,
    SM3_FIFO_MEM7,
    SM3_PENDING_DELAY,
    SM3_CLK_ENABLE,
    GPIO_PINS,
    GPIO_PINDIRS,
    MASTERCLK_FREQ,
    MASTERCLK_MODE,
    BREAKPOINTS;
  }

  final static Regs[] REGS = Regs.values();

  public static final int SM_SIZE =
    Regs.SM1_CLK_ENABLE.ordinal() - Regs.SM0_CLK_ENABLE.ordinal();

  private final PIO pio;

  public PIOEmuRegisters(final PIO pio, final int baseAddress)
  {
    super(baseAddress, (short)REGS.length);
    if (pio == null) {
      throw new NullPointerException("pio");
    }
    this.pio = pio;
  }

  public PIO getPIO() { return pio; }

  public int getPIOIndex()
  {
    return pio.getIndex();
  }

  public int getAddress(PIOEmuRegisters.Regs register)
  {
    if (register == null) {
      throw new NullPointerException("register");
    }
    return getBaseAddress() + 0x4 * register.ordinal();
  }

  public int getSMAddress(PIOEmuRegisters.Regs register, final int smNum)
  {
    Constants.checkSmNum(smNum);
    if (register == null) {
      throw new NullPointerException("register");
    }
    switch (register) {
    case SM0_REGX:
    case SM0_REGY:
    case SM0_ISR:
    case SM0_ISR_SHIFT_COUNT:
    case SM0_OSR:
    case SM0_OSR_SHIFT_COUNT:
    case SM0_FIFO_MEM0:
    case SM0_FIFO_MEM1:
    case SM0_FIFO_MEM2:
    case SM0_FIFO_MEM3:
    case SM0_FIFO_MEM4:
    case SM0_FIFO_MEM5:
    case SM0_FIFO_MEM6:
    case SM0_FIFO_MEM7:
    case SM0_PENDING_DELAY:
    case SM0_CLK_ENABLE:
      break; // ok
    default:
      throw new IllegalArgumentException("register not one of SM0_*: " +
                                         register);
    }
    return getBaseAddress() + 0x4 * register.ordinal() + smNum * SM_SIZE;
  }

  public int getFIFOMemAddress(final int address)
  {
    if (address < 0) {
      throw new IllegalArgumentException("address < 0" + address);
    }
    if (address > (2 * FIFO_DEPTH) - 1) {
      throw new IllegalArgumentException("address > " +
                                         ((2 * FIFO_DEPTH) - 1) + ":" +
                                         address);
    }
    return getBaseAddress() + 0x4 * (Regs.SM0_FIFO_MEM0.ordinal() + address);
  }

  @Override
  public void writeRegister(final int regNum, final int value, final int mask,
                            final boolean xor)
  {
    if ((regNum < 0) || (regNum >= REGS.length)) {
      throw new InternalError("regNum out of bounds: " + regNum);
    }
    final Regs register = REGS[regNum];
    switch (register) {
    case SM0_REGX:
    case SM1_REGX:
    case SM2_REGX:
    case SM3_REGX:
      break; // (for now) read-only address
    case SM0_REGY:
    case SM1_REGY:
    case SM2_REGY:
    case SM3_REGY:
      break; // (for now) read-only address
    case SM0_ISR:
    case SM1_ISR:
    case SM2_ISR:
    case SM3_ISR:
      break; // (for now) read-only address
    case SM0_ISR_SHIFT_COUNT:
    case SM1_ISR_SHIFT_COUNT:
    case SM2_ISR_SHIFT_COUNT:
    case SM3_ISR_SHIFT_COUNT:
      break; // (for now) read-only address
    case SM0_OSR:
    case SM1_OSR:
    case SM2_OSR:
    case SM3_OSR:
      break; // (for now) read-only address
    case SM0_OSR_SHIFT_COUNT:
    case SM1_OSR_SHIFT_COUNT:
    case SM2_OSR_SHIFT_COUNT:
    case SM3_OSR_SHIFT_COUNT:
      break; // (for now) read-only address
    case SM0_PENDING_DELAY:
    case SM1_PENDING_DELAY:
    case SM2_PENDING_DELAY:
    case SM3_PENDING_DELAY:
      break; // (for now) read-only address
    case SM0_CLK_ENABLE:
    case SM1_CLK_ENABLE:
    case SM2_CLK_ENABLE:
    case SM3_CLK_ENABLE:
      break; // (for now) read-only address
    case SM0_FIFO_MEM0:
    case SM0_FIFO_MEM1:
    case SM0_FIFO_MEM2:
    case SM0_FIFO_MEM3:
    case SM0_FIFO_MEM4:
    case SM0_FIFO_MEM5:
    case SM0_FIFO_MEM6:
    case SM0_FIFO_MEM7:
    case SM1_FIFO_MEM0:
    case SM1_FIFO_MEM1:
    case SM1_FIFO_MEM2:
    case SM1_FIFO_MEM3:
    case SM1_FIFO_MEM4:
    case SM1_FIFO_MEM5:
    case SM1_FIFO_MEM6:
    case SM1_FIFO_MEM7:
    case SM2_FIFO_MEM0:
    case SM2_FIFO_MEM1:
    case SM2_FIFO_MEM2:
    case SM2_FIFO_MEM3:
    case SM2_FIFO_MEM4:
    case SM2_FIFO_MEM5:
    case SM2_FIFO_MEM6:
    case SM2_FIFO_MEM7:
    case SM3_FIFO_MEM0:
    case SM3_FIFO_MEM1:
    case SM3_FIFO_MEM2:
    case SM3_FIFO_MEM3:
    case SM3_FIFO_MEM4:
    case SM3_FIFO_MEM5:
    case SM3_FIFO_MEM6:
    case SM3_FIFO_MEM7:
      break; // (for now) read-only address
    case GPIO_PINS:
      break; // (for now) read-only address
    case GPIO_PINDIRS:
      break; // (for now) read-only address
    case MASTERCLK_FREQ:
      break; // (for now) read-only address
    case MASTERCLK_MODE:
      break; // (for now) read-only address
    case BREAKPOINTS:
      break; // (for now) read-only address
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }

  private int getClockEnable(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final boolean clockEnable = pio.getSM(smNum).getPLL().getClockEnable();
    return clockEnable ? 0x1 : 0x0;
  }

  private int getFIFOMemValue(final int regsOffset)
  {
    final int smNum = regsOffset / SM_SIZE;
    final int address = regsOffset - SM_SIZE * smNum;
    Constants.checkSmNum(smNum);
    return pio.getSM(smNum).getFIFO().getMemValue(address);
  }

  @Override
  public synchronized int readRegister(final int regNum)
  {
    if ((regNum < 0) || (regNum >= REGS.length)) {
      throw new InternalError("regNum out of bounds: " + regNum);
    }
    final Regs register = REGS[regNum];
    switch (register) {
    case SM0_REGX:
    case SM1_REGX:
    case SM2_REGX:
    case SM3_REGX:
      return
        pio.getSM((regNum - Regs.SM0_REGX.ordinal()) / SM_SIZE).getX();
    case SM0_REGY:
    case SM1_REGY:
    case SM2_REGY:
    case SM3_REGY:
      return
        pio.getSM((regNum - Regs.SM0_REGY.ordinal()) / SM_SIZE).getY();
    case SM0_ISR:
    case SM1_ISR:
    case SM2_ISR:
    case SM3_ISR:
      return
        pio.getSM((regNum - Regs.SM0_ISR.ordinal()) / SM_SIZE).getISRValue();
    case SM0_ISR_SHIFT_COUNT:
    case SM1_ISR_SHIFT_COUNT:
    case SM2_ISR_SHIFT_COUNT:
    case SM3_ISR_SHIFT_COUNT:
      return
        pio.getSM((regNum - Regs.SM0_ISR_SHIFT_COUNT.ordinal()) / SM_SIZE).
        getISRShiftCount();
    case SM0_OSR:
    case SM1_OSR:
    case SM2_OSR:
    case SM3_OSR:
      return
        pio.getSM((regNum - Regs.SM0_OSR.ordinal()) / SM_SIZE).getOSRValue();
    case SM0_OSR_SHIFT_COUNT:
    case SM1_OSR_SHIFT_COUNT:
    case SM2_OSR_SHIFT_COUNT:
    case SM3_OSR_SHIFT_COUNT:
      return
        pio.getSM((regNum - Regs.SM0_OSR_SHIFT_COUNT.ordinal()) / SM_SIZE).
        getOSRShiftCount();
    case SM0_PENDING_DELAY:
    case SM1_PENDING_DELAY:
    case SM2_PENDING_DELAY:
    case SM3_PENDING_DELAY:
      return
        pio.getSM((regNum - Regs.SM0_PENDING_DELAY.ordinal()) / SM_SIZE).
        getPendingDelay();
    case SM0_CLK_ENABLE:
    case SM1_CLK_ENABLE:
    case SM2_CLK_ENABLE:
    case SM3_CLK_ENABLE:
      return getClockEnable((regNum - Regs.SM0_CLK_ENABLE.ordinal()) / SM_SIZE);
    case SM0_FIFO_MEM0:
    case SM0_FIFO_MEM1:
    case SM0_FIFO_MEM2:
    case SM0_FIFO_MEM3:
    case SM0_FIFO_MEM4:
    case SM0_FIFO_MEM5:
    case SM0_FIFO_MEM6:
    case SM0_FIFO_MEM7:
    case SM1_FIFO_MEM0:
    case SM1_FIFO_MEM1:
    case SM1_FIFO_MEM2:
    case SM1_FIFO_MEM3:
    case SM1_FIFO_MEM4:
    case SM1_FIFO_MEM5:
    case SM1_FIFO_MEM6:
    case SM1_FIFO_MEM7:
    case SM2_FIFO_MEM0:
    case SM2_FIFO_MEM1:
    case SM2_FIFO_MEM2:
    case SM2_FIFO_MEM3:
    case SM2_FIFO_MEM4:
    case SM2_FIFO_MEM5:
    case SM2_FIFO_MEM6:
    case SM2_FIFO_MEM7:
    case SM3_FIFO_MEM0:
    case SM3_FIFO_MEM1:
    case SM3_FIFO_MEM2:
    case SM3_FIFO_MEM3:
    case SM3_FIFO_MEM4:
    case SM3_FIFO_MEM5:
    case SM3_FIFO_MEM6:
    case SM3_FIFO_MEM7:
      return getFIFOMemValue(regNum - Regs.SM0_FIFO_MEM0.ordinal());
    case GPIO_PINS:
      return pio.getGPIO().getPins(0, GPIO_NUM);
    case GPIO_PINDIRS:
      return pio.getGPIO().getPinDirs(0, GPIO_NUM);
    case MASTERCLK_FREQ:
      return 0; // TODO
    case MASTERCLK_MODE:
      return 0; // TODO
    case BREAKPOINTS:
      return 0; // TODO
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