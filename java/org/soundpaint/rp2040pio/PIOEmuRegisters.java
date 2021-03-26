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
public abstract class PIOEmuRegisters extends AbstractRegisters
  implements Constants
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
    INSTR_MEM0,
    INSTR_MEM1,
    INSTR_MEM2,
    INSTR_MEM3,
    INSTR_MEM4,
    INSTR_MEM5,
    INSTR_MEM6,
    INSTR_MEM7,
    INSTR_MEM8,
    INSTR_MEM9,
    INSTR_MEM10,
    INSTR_MEM11,
    INSTR_MEM12,
    INSTR_MEM13,
    INSTR_MEM14,
    INSTR_MEM15,
    INSTR_MEM16,
    INSTR_MEM17,
    INSTR_MEM18,
    INSTR_MEM19,
    INSTR_MEM20,
    INSTR_MEM21,
    INSTR_MEM22,
    INSTR_MEM23,
    INSTR_MEM24,
    INSTR_MEM25,
    INSTR_MEM26,
    INSTR_MEM27,
    INSTR_MEM28,
    INSTR_MEM29,
    INSTR_MEM30,
    INSTR_MEM31,
    GPIO_PINS,
    GPIO_PINDIRS;
  }

  protected static final Regs[] REGS = Regs.values();

  public static final int SM_SIZE =
    Regs.SM1_REGX.ordinal() - Regs.SM0_REGX.ordinal();

  public static String getLabelForRegister(final int regNum)
  {
    return REGS[regNum].toString();
  }

  public static int getAddress(final int pioNum,
                               final PIOEmuRegisters.Regs register)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    if (register == null) {
      throw new NullPointerException("register");
    }
    return Constants.getPIOEmuAddress(pioNum) + 0x4 * register.ordinal();
  }

  public static int getSMAddress(final int pioNum,
                                 final int smNum,
                                 final PIOEmuRegisters.Regs register)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
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
    return
      Constants.getPIOEmuAddress(pioNum) +
      0x4 * register.ordinal() + smNum * SM_SIZE;
  }

  public static int getFIFOMemAddress(final int pioNum, final int address)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkFIFOAddr(address, "FIFO address");
    return
      Constants.getPIOEmuAddress(pioNum) +
      0x4 * (Regs.SM0_FIFO_MEM0.ordinal() + address);
  }

  public static int getMemoryAddress(final int pioNum,
                                     final int memoryAddress)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmMemAddr(memoryAddress, "memory address");
    return
      Constants.getPIOEmuAddress(pioNum) +
      0x4 * (Regs.INSTR_MEM0.ordinal() + memoryAddress);
  }

  public PIOEmuRegisters(final MasterClock masterClock, final int pioNum)
  {
    super(masterClock, Constants.getPIOEmuAddress(pioNum), (short)REGS.length);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
