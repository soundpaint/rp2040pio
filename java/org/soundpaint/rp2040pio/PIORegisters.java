/*
 * @(#)PIORegisters.java 1.00 21/02/25
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
 * Facade to the internal subsystems of a PIO.  The layout of
 * registers follows the list of registers in Sect. 3.7 of the RP2040
 * datasheet.  The facade is in particular intended for use by the
 * SDK.
 */
public abstract class PIORegisters extends AbstractRegisters
  implements Constants
{
  public enum Regs {
    CTRL,
    FSTAT,
    FDEBUG,
    FLEVEL,
    TXF0,
    TXF1,
    TXF2,
    TXF3,
    RXF0,
    RXF1,
    RXF2,
    RXF3,
    IRQ,
    IRQ_FORCE,
    INPUT_SYNC_BYPASS,
    DBG_PADOUT,
    DBG_PADOE,
    DBG_CFGINFO,
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
    SM0_CLKDIV,
    SM0_EXECCTRL,
    SM0_SHIFTCTRL,
    SM0_ADDR,
    SM0_INSTR,
    SM0_PINCTRL,
    SM1_CLKDIV,
    SM1_EXECCTRL,
    SM1_SHIFTCTRL,
    SM1_ADDR,
    SM1_INSTR,
    SM1_PINCTRL,
    SM2_CLKDIV,
    SM2_EXECCTRL,
    SM2_SHIFTCTRL,
    SM2_ADDR,
    SM2_INSTR,
    SM2_PINCTRL,
    SM3_CLKDIV,
    SM3_EXECCTRL,
    SM3_SHIFTCTRL,
    SM3_ADDR,
    SM3_INSTR,
    SM3_PINCTRL,
    INTR,
    IRQ0_INTE,
    IRQ0_INTF,
    IRQ0_INTS,
    IRQ1_INTE,
    IRQ1_INTF,
    IRQ1_INTS;
  }

  protected static final Regs[] REGS = Regs.values();

  protected static final int SM_SIZE =
    Regs.SM1_CLKDIV.ordinal() - Regs.SM0_CLKDIV.ordinal();

  public static String getLabelForRegister(final int regNum)
  {
    return REGS[regNum].toString();
  }

  public static int getAddress(final int pioNum,
                               final PIORegisters.Regs register)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    if (register == null) {
      throw new NullPointerException("register");
    }
    return Constants.getPIOBaseAddress(pioNum) + 0x4 * register.ordinal();
  }

  public static int getSMAddress(final int pioNum,
                                 final int smNum,
                                 final PIORegisters.Regs register)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmNum(smNum);
    if (register == null) {
      throw new NullPointerException("register");
    }
    switch (register) {
    case SM0_CLKDIV:
    case SM0_EXECCTRL:
    case SM0_SHIFTCTRL:
    case SM0_ADDR:
    case SM0_INSTR:
    case SM0_PINCTRL:
      break; // ok
    default:
      throw new IllegalArgumentException("register not one of SM0_*: " +
                                         register);
    }
    return
      Constants.getPIOBaseAddress(pioNum) +
      0x4 * register.ordinal() + smNum * SM_SIZE;
  }

  public static int getMemoryAddress(final int pioNum,
                                     final int memoryAddress)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmMemAddr(memoryAddress, "memory address");
    return
      Constants.getPIOBaseAddress(pioNum) +
      0x4 * (Regs.INSTR_MEM0.ordinal() + memoryAddress);
  }

  public static int getTXFAddress(final int pioNum, final int smNum)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmNum(smNum);
    return
      Constants.getPIOBaseAddress(pioNum) +
      0x4 * (Regs.TXF0.ordinal() + smNum);
  }

  public static int getRXFAddress(final int pioNum, final int smNum)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    Constants.checkSmNum(smNum);
    return
      Constants.getPIOBaseAddress(pioNum) +
      0x4 * (Regs.RXF0.ordinal() + smNum);
  }

  public PIORegisters(final MasterClock masterClock, final int pioNum)
  {
    super(masterClock, Constants.getPIOBaseAddress(pioNum), (short)REGS.length);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
