/*
 * @(#)PIOEmuRegistersImpl.java 1.00 21/03/06
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
public class PIOEmuRegistersImpl extends PIOEmuRegisters
{
  private final PIO pio;

  public PIOEmuRegistersImpl(final PIO pio)
  {
    super("PIOEmu" + pio.getIndex(),
          Constants.getPIOEmuBaseAddress(pio.getIndex()));
    this.pio = pio;
  }

  public PIO getPIO() { return pio; }

  public int getPIOIndex()
  {
    return pio.getIndex();
  }

  public int getAddress(final PIOEmuRegisters.Regs register)
  {
    return getAddress(getPIOIndex(), register);
  }

  public int getSMAddress(final PIOEmuRegisters.Regs register, final int smNum)
  {
    return getSMAddress(getPIOIndex(), smNum, register);
  }

  public int getFIFOMemAddress(final int smNum, final int address)
  {
    return getFIFOMemAddress(getPIOIndex(), smNum, address);
  }

  public int getMemoryAddress(final int memoryAddress)
  {
    return getMemoryAddress(getPIOIndex(), memoryAddress);
  }

  private void setFIFOMemValue(final int regsOffset, final int value,
                               final int mask, final boolean xor)
  {
    final int smNum = regsOffset / SM_SIZE;
    Constants.checkSmNum(smNum);
    final int address = regsOffset - SM_SIZE * smNum;
    final FIFO fifo = pio.getSM(smNum).getFIFO();
    fifo.setMemValue(address,
                     Constants.hwSetBits(fifo.getMemValue(address),
                                         value, mask, xor));
  }

  @Override
  public void writeRegister(final int regNum, final int value,
                            final int mask, final boolean xor)
  {
    checkRegNum(regNum);
    final Regs register = REGS[regNum];
    switch (register) {
    case SM0_REGX:
    case SM1_REGX:
    case SM2_REGX:
    case SM3_REGX:
      pio.getSM((regNum - Regs.SM0_REGX.ordinal()) / SM_SIZE).
        setX(value, mask, xor);
      break;
    case SM0_REGY:
    case SM1_REGY:
    case SM2_REGY:
    case SM3_REGY:
      pio.getSM((regNum - Regs.SM0_REGY.ordinal()) / SM_SIZE).
        setY(value, mask, xor);
      break;
    case SM0_PC:
    case SM1_PC:
    case SM2_PC:
    case SM3_PC:
      pio.getSM((regNum - Regs.SM0_PC.ordinal()) / SM_SIZE).
        setPC(value, mask, xor);
      break;
    case SM0_ISR:
    case SM1_ISR:
    case SM2_ISR:
    case SM3_ISR:
      pio.getSM((regNum - Regs.SM0_ISR.ordinal()) / SM_SIZE).
        setISRValue(value, mask, xor);
      break;
    case SM0_ISR_SHIFT_COUNT:
    case SM1_ISR_SHIFT_COUNT:
    case SM2_ISR_SHIFT_COUNT:
    case SM3_ISR_SHIFT_COUNT:
      pio.getSM((regNum - Regs.SM0_ISR_SHIFT_COUNT.ordinal()) / SM_SIZE).
        setISRShiftCount(value, mask, xor);
      break;
    case SM0_OSR:
    case SM1_OSR:
    case SM2_OSR:
    case SM3_OSR:
      pio.getSM((regNum - Regs.SM0_OSR.ordinal()) / SM_SIZE).
        setOSRValue(value, mask, xor);
      break;
    case SM0_OSR_SHIFT_COUNT:
    case SM1_OSR_SHIFT_COUNT:
    case SM2_OSR_SHIFT_COUNT:
    case SM3_OSR_SHIFT_COUNT:
      pio.getSM((regNum - Regs.SM0_OSR_SHIFT_COUNT.ordinal()) / SM_SIZE).
        setOSRShiftCount(value, mask, xor);
      break;
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
      setFIFOMemValue(regNum - Regs.SM0_FIFO_MEM0.ordinal(), value, mask, xor);
      break;
    case SM0_CLEAR_FORCED:
    case SM1_CLEAR_FORCED:
    case SM2_CLEAR_FORCED:
    case SM3_CLEAR_FORCED:
      pio.getSM((regNum - Regs.SM0_CLEAR_FORCED.ordinal()) / SM_SIZE).
        clearPendingForcedInstruction();
      break;
    case SM0_CLEAR_EXECD:
    case SM1_CLEAR_EXECD:
    case SM2_CLEAR_EXECD:
    case SM3_CLEAR_EXECD:
      pio.getSM((regNum - Regs.SM0_CLEAR_EXECD.ordinal()) / SM_SIZE).
        clearPendingExecdInstruction();
      break;
    case SM0_INSTR_ORIGIN:
    case SM1_INSTR_ORIGIN:
    case SM2_INSTR_ORIGIN:
    case SM3_INSTR_ORIGIN:
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
    case SM0_FORCED_INSTR:
    case SM1_FORCED_INSTR:
    case SM2_FORCED_INSTR:
    case SM3_FORCED_INSTR:
      break; // (for now) read-only address
    case SM0_EXECD_INSTR:
    case SM1_EXECD_INSTR:
    case SM2_EXECD_INSTR:
    case SM3_EXECD_INSTR:
      pio.getSM((regNum - Regs.SM0_EXECD_INSTR.ordinal()) / SM_SIZE).
        execInstruction(value & mask);
      break;
    case SM0_CLK_ENABLE:
    case SM1_CLK_ENABLE:
    case SM2_CLK_ENABLE:
    case SM3_CLK_ENABLE:
      break; // (for now) read-only address
    case SM0_NEXT_CLK_ENABLE:
    case SM1_NEXT_CLK_ENABLE:
    case SM2_NEXT_CLK_ENABLE:
    case SM3_NEXT_CLK_ENABLE:
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
    case INSTR_MEM0:
    case INSTR_MEM1:
    case INSTR_MEM2:
    case INSTR_MEM3:
    case INSTR_MEM4:
    case INSTR_MEM5:
    case INSTR_MEM6:
    case INSTR_MEM7:
    case INSTR_MEM8:
    case INSTR_MEM9:
    case INSTR_MEM10:
    case INSTR_MEM11:
    case INSTR_MEM12:
    case INSTR_MEM13:
    case INSTR_MEM14:
    case INSTR_MEM15:
    case INSTR_MEM16:
    case INSTR_MEM17:
    case INSTR_MEM18:
    case INSTR_MEM19:
    case INSTR_MEM20:
    case INSTR_MEM21:
    case INSTR_MEM22:
    case INSTR_MEM23:
    case INSTR_MEM24:
    case INSTR_MEM25:
    case INSTR_MEM26:
    case INSTR_MEM27:
    case INSTR_MEM28:
    case INSTR_MEM29:
    case INSTR_MEM30:
    case INSTR_MEM31:
      pio.getMemory().set(regNum - Regs.INSTR_MEM0.ordinal(), value, mask, xor);
      break;
    case RXF0:
    case RXF1:
    case RXF2:
    case RXF3:
      pio.getSM(regNum - Regs.RXF0.ordinal()).putRXF(value & mask);
      break;
    case TXF0:
    case TXF1:
    case TXF2:
    case TXF3:
      break; // read-only address
    case FREAD_PTR:
      break; // read-only address
    case GPIO_PINS:
      pio.getPIOGPIO().setPinsMask(value, mask, xor);
      break;
    case GPIO_PINDIRS:
      pio.getPIOGPIO().setPinDirsMask(value, mask, xor);
      break;
    case IRQ:
      break; // read-only address
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

  private int getNextClockEnable(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final boolean nextClockEnable =
      pio.getSM(smNum).getPLL().getNextClockEnable();
    return nextClockEnable ? 0x1 : 0x0;
  }

  private int getFIFOMemValue(final int regsOffset)
  {
    final int smNum = regsOffset / SM_SIZE;
    final int address = regsOffset - SM_SIZE * smNum;
    Constants.checkSmNum(smNum);
    return pio.getSM(smNum).getFIFO().getMemValue(address);
  }

  private int getFIFOReadPointers()
  {
    int readPointers = 0;
    for (int smNum = SM_COUNT - 1; smNum >= 0; smNum--) {
      final FIFO fifo = pio.getSM(smNum).getFIFO();
      readPointers <<= 8;
      readPointers |= (fifo.getRXReadPointer() & 0x7) << 4;
      readPointers |= fifo.getTXReadPointer() & 0x7;
    }
    return readPointers;
  }

  @Override
  public synchronized int readRegister(final int regNum)
  {
    checkRegNum(regNum);
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
    case SM0_CLEAR_FORCED:
    case SM1_CLEAR_FORCED:
    case SM2_CLEAR_FORCED:
    case SM3_CLEAR_FORCED:
      return 0; // write-only address
    case SM0_CLEAR_EXECD:
    case SM1_CLEAR_EXECD:
    case SM2_CLEAR_EXECD:
    case SM3_CLEAR_EXECD:
      return 0; // write-only address
    case SM0_INSTR_ORIGIN:
    case SM1_INSTR_ORIGIN:
    case SM2_INSTR_ORIGIN:
    case SM3_INSTR_ORIGIN:
      return
        pio.getSM((regNum - Regs.SM0_INSTR_ORIGIN.ordinal()) / SM_SIZE).
        getINSTR_ORIGIN();
    case SM0_DELAY:
    case SM1_DELAY:
    case SM2_DELAY:
    case SM3_DELAY:
      return
        pio.getSM((regNum - Regs.SM0_DELAY.ordinal()) / SM_SIZE).
        getTotalDelay();
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
    case SM0_FORCED_INSTR:
    case SM1_FORCED_INSTR:
    case SM2_FORCED_INSTR:
    case SM3_FORCED_INSTR:
      return
        pio.getSM((regNum - Regs.SM0_FORCED_INSTR.ordinal()) / SM_SIZE).
        getFORCED_INSTR();
    case SM0_EXECD_INSTR:
    case SM1_EXECD_INSTR:
    case SM2_EXECD_INSTR:
    case SM3_EXECD_INSTR:
      return
        pio.getSM((regNum - Regs.SM0_EXECD_INSTR.ordinal()) / SM_SIZE).
        getEXECD_INSTR();
    case SM0_CLK_ENABLE:
    case SM1_CLK_ENABLE:
    case SM2_CLK_ENABLE:
    case SM3_CLK_ENABLE:
      return getClockEnable((regNum - Regs.SM0_CLK_ENABLE.ordinal()) / SM_SIZE);
    case SM0_NEXT_CLK_ENABLE:
    case SM1_NEXT_CLK_ENABLE:
    case SM2_NEXT_CLK_ENABLE:
    case SM3_NEXT_CLK_ENABLE:
      return getNextClockEnable((regNum - Regs.SM0_CLK_ENABLE.ordinal()) / SM_SIZE);
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
    case INSTR_MEM0:
    case INSTR_MEM1:
    case INSTR_MEM2:
    case INSTR_MEM3:
    case INSTR_MEM4:
    case INSTR_MEM5:
    case INSTR_MEM6:
    case INSTR_MEM7:
    case INSTR_MEM8:
    case INSTR_MEM9:
    case INSTR_MEM10:
    case INSTR_MEM11:
    case INSTR_MEM12:
    case INSTR_MEM13:
    case INSTR_MEM14:
    case INSTR_MEM15:
    case INSTR_MEM16:
    case INSTR_MEM17:
    case INSTR_MEM18:
    case INSTR_MEM19:
    case INSTR_MEM20:
    case INSTR_MEM21:
    case INSTR_MEM22:
    case INSTR_MEM23:
    case INSTR_MEM24:
    case INSTR_MEM25:
    case INSTR_MEM26:
    case INSTR_MEM27:
    case INSTR_MEM28:
    case INSTR_MEM29:
    case INSTR_MEM30:
    case INSTR_MEM31:
      return pio.getMemory().get(regNum - Regs.INSTR_MEM0.ordinal()) & 0xffff;
    case TXF0:
    case TXF1:
    case TXF2:
    case TXF3:
      return pio.getSM(regNum - Regs.TXF0.ordinal()).getTXF();
    case RXF0:
    case RXF1:
    case RXF2:
    case RXF3:
      return 0; // write-only address
    case FREAD_PTR:
      return getFIFOReadPointers();
    case GPIO_PINS:
      return pio.getPIOGPIO().getPins(0, GPIO_NUM);
    case GPIO_PINDIRS:
      return pio.getPIOGPIO().getPinDirs(0, GPIO_NUM);
    case IRQ:
      return pio.getIRQ().getIRQ();
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
