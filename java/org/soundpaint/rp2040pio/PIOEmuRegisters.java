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
    SM0_PC,
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
    SM0_DELAY,
    SM0_DELAY_CYCLE,
    SM0_PENDING_DELAY,
    SM0_CLK_ENABLE,
    /**
     * R/W address.  Reset value: 0.
     *
     * Each bit of this values corresponds to each of the 32 memory
     * locations of the PIO instruction memory (with the LSB of the
     * word corresponding to the lowest memory address).  Setting a
     * bit to 1 marks the corresponding memory address as location of
     * a breakpoint.  Setting a bit to 0 removes the breakpoint.
     *
     * As soon as the program counter of the state machine reaches an
     * address that is marked as a breakpoint, master clock
     * MASTERCLK_MODE will be automatically set to single step mode.
     */
    SM0_BREAKPOINTS,
    /**
     * R/W address.  Reset value: 0.
     *
     * Tracepoints work like breakpoints with the difference that
     * master clock MASTERCLK_MODE it not automatically set to single
     * step mode, but instead a message is printed to console output.
     * The message contains the state machine's number and
     * disassembled instruction with prefixed instruction memory
     * address.  Tracepoints work in all master clock MASTERCLK_MODE
     * modes.
     */
    SM0_TRACEPOINTS,
    SM1_REGX,
    SM1_REGY,
    SM1_PC,
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
    SM1_DELAY,
    SM1_DELAY_CYCLE,
    SM1_PENDING_DELAY,
    SM1_CLK_ENABLE,
    SM1_BREAKPOINTS,
    SM1_TRACEPOINTS,
    SM2_REGX,
    SM2_REGY,
    SM2_PC,
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
    SM2_DELAY,
    SM2_DELAY_CYCLE,
    SM2_PENDING_DELAY,
    SM2_CLK_ENABLE,
    SM2_BREAKPOINTS,
    SM2_TRACEPOINTS,
    SM3_REGX,
    SM3_REGY,
    SM3_PC,
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
    SM3_DELAY,
    SM3_DELAY_CYCLE,
    SM3_PENDING_DELAY,
    SM3_CLK_ENABLE,
    SM3_BREAKPOINTS,
    SM3_TRACEPOINTS,
    GPIO_PINS,
    GPIO_PINDIRS;
  }

  final static Regs[] REGS = Regs.values();

  public static final int SM_SIZE =
    Regs.SM1_REGX.ordinal() - Regs.SM0_REGX.ordinal();

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

  @Override
  protected String getLabelForRegister(final int regNum)
  {
    return REGS[regNum].toString();
  }

  public int getAddress(final PIOEmuRegisters.Regs register)
  {
    if (register == null) {
      throw new NullPointerException("register");
    }
    return getBaseAddress() + 0x4 * register.ordinal();
  }

  public int getSMAddress(final PIOEmuRegisters.Regs register, final int smNum)
  {
    Constants.checkSmNum(smNum);
    if (register == null) {
      throw new NullPointerException("register");
    }
    switch (register) {
    case SM0_REGX:
    case SM0_REGY:
    case SM0_PC:
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
    case SM0_DELAY:
    case SM0_DELAY_CYCLE:
    case SM0_PENDING_DELAY:
    case SM0_CLK_ENABLE:
    case SM0_BREAKPOINTS:
    case SM0_TRACEPOINTS:
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
  protected void writeRegister(final int regNum, final int value,
                               final int mask, final boolean xor)
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
    case SM0_PC:
    case SM1_PC:
    case SM2_PC:
    case SM3_PC:
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
    case SM0_DELAY:
    case SM1_DELAY:
    case SM2_DELAY:
    case SM3_DELAY:
      break; // (for now) read-only address
    case SM0_DELAY_CYCLE:
    case SM1_DELAY_CYCLE:
    case SM2_DELAY_CYCLE:
    case SM3_DELAY_CYCLE:
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
    case SM0_BREAKPOINTS:
    case SM1_BREAKPOINTS:
    case SM2_BREAKPOINTS:
    case SM3_BREAKPOINTS:
      pio.getSM((regNum - Regs.SM0_BREAKPOINTS.ordinal()) / SM_SIZE).
        setBreakPoints(value, mask, xor);
      break;
    case SM0_TRACEPOINTS:
    case SM1_TRACEPOINTS:
    case SM2_TRACEPOINTS:
    case SM3_TRACEPOINTS:
      pio.getSM((regNum - Regs.SM0_TRACEPOINTS.ordinal()) / SM_SIZE).
        setTracePoints(value, mask, xor);
      break;
    case GPIO_PINS:
      break; // (for now) read-only address
    case GPIO_PINDIRS:
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
  protected synchronized int readRegister(final int regNum)
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
    case SM0_PC:
    case SM1_PC:
    case SM2_PC:
    case SM3_PC:
      return
        pio.getSM((regNum - Regs.SM0_PC.ordinal()) / SM_SIZE).getPC();
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
    case SM0_DELAY:
    case SM1_DELAY:
    case SM2_DELAY:
    case SM3_DELAY:
      return
        pio.getSM((regNum - Regs.SM0_DELAY.ordinal()) / SM_SIZE).
        getDelay();
    case SM0_DELAY_CYCLE:
    case SM1_DELAY_CYCLE:
    case SM2_DELAY_CYCLE:
    case SM3_DELAY_CYCLE:
      return
        pio.getSM((regNum - Regs.SM0_DELAY_CYCLE.ordinal()) / SM_SIZE).
        isDelayCycle() ? 0x1 : 0x0;
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
    case SM0_BREAKPOINTS:
    case SM1_BREAKPOINTS:
    case SM2_BREAKPOINTS:
    case SM3_BREAKPOINTS:
      return
        pio.getSM((regNum - Regs.SM0_BREAKPOINTS.ordinal()) / SM_SIZE).
        getBreakPoints();
    case SM0_TRACEPOINTS:
    case SM1_TRACEPOINTS:
    case SM2_TRACEPOINTS:
    case SM3_TRACEPOINTS:
      return
        pio.getSM((regNum - Regs.SM0_TRACEPOINTS.ordinal()) / SM_SIZE).
        getTracePoints();
    case GPIO_PINS:
      return pio.getGPIO().getPins(0, GPIO_NUM);
    case GPIO_PINDIRS:
      return pio.getGPIO().getPinDirs(0, GPIO_NUM);
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
