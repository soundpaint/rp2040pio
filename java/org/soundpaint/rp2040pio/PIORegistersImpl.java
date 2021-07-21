/*
 * @(#)PIORegistersImpl.java 1.00 21/02/25
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
public class PIORegistersImpl extends PIORegisters
{
  private final PIO pio;

  public PIORegistersImpl(final PIO pio)
  {
    super("PIO" + pio.getIndex(),
          Constants.getPIOBaseAddress(pio.getIndex()));
    this.pio = pio;
  }

  public PIO getPIO() { return pio; }

  public int getPIOIndex()
  {
    return pio.getIndex();
  }

  public int getAddress(final PIORegisters.Regs register)
  {
    return getAddress(getPIOIndex(), register);
  }

  public int getSMAddress(final PIORegisters.Regs register, final int smNum)
  {
    return getSMAddress(getPIOIndex(), smNum, register);
  }

  public int getMemoryAddress(final int memoryAddress)
  {
    return getMemoryAddress(getPIOIndex(), memoryAddress);
  }

  public int getTXFAddress(final int smNum)
  {
    return getTXFAddress(getPIOIndex(), smNum);
  }

  public int getRXFAddress(final int smNum)
  {
    return getRXFAddress(getPIOIndex(), smNum);
  }

  /*
   * TODO: In all of the following methods, use constants declared in
   * class Constants for bit shifting & masking.
   */
  private void writeFDebug(final int value, final int mask)
  {
    for (int smNum = 0; smNum < SM_COUNT; smNum++) {
      final SM sm = pio.getSM(smNum);
      final FIFO fifo = sm.getFIFO();
      if (((value >>> (24 + smNum)) & 0x1) != 0x0 &&
          ((mask >>> (24 + smNum)) & 0x1) != 0x0) {
        fifo.clearTXStall();
      }
      if (((value >>> (16 + smNum)) & 0x1) != 0x0 &&
          ((mask >>> (16 + smNum)) & 0x1) != 0x0) {
        fifo.clearTXOver();
      }
      if (((value >>> (8 + smNum)) & 0x1) != 0x0 &&
          ((mask >>> (8 + smNum)) & 0x1) != 0x0) {
        fifo.clearRXUnder();
      }
      if (((value >>> smNum) & 0x1) != 0x0 &&
          ((mask >>> smNum) & 0x1) != 0x0) {
        fifo.clearRXStall();
      }
    }
  }

  @Override
  public void writeRegister(final int regNum, final int value,
                            final int mask, final boolean xor)
  {
    checkRegNum(regNum);
    final Regs register = REGS[regNum];
    switch (register) {
    case CTRL:
      pio.setCtrl(value, mask);
      break;
    case FSTAT:
      break; // read-only address
    case FDEBUG:
      writeFDebug(value, mask);
      break;
    case FLEVEL:
      break; // read-only address
    case TXF0:
    case TXF1:
    case TXF2:
    case TXF3:
      pio.getSM(regNum - Regs.TXF0.ordinal()).put(value & mask);
      break;
    case RXF0:
    case RXF1:
    case RXF2:
    case RXF3:
      break; // read-only address
    case IRQ:
      pio.getIRQ().writeRegIRQ(value & mask);
      break;
    case IRQ_FORCE:
      pio.getIRQ().writeRegIRQ_FORCE(value & mask);
      break;
    case INPUT_SYNC_BYPASS:
      pio.getPIOGPIO().getGPIO().setInputSyncByPass(value, mask, xor);
      break;
    case DBG_PADOUT:
      break; // read-only address
    case DBG_PADOE:
      break; // read-only address
    case DBG_CFGINFO:
      break; // read-only address
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
    case SM0_CLKDIV:
    case SM1_CLKDIV:
    case SM2_CLKDIV:
    case SM3_CLKDIV:
      pio.getSM((regNum - Regs.SM0_CLKDIV.ordinal()) / SM_SIZE).
        setCLKDIV(value, mask, xor);
      break;
    case SM0_EXECCTRL:
    case SM1_EXECCTRL:
    case SM2_EXECCTRL:
    case SM3_EXECCTRL:
      pio.getSM((regNum - Regs.SM0_EXECCTRL.ordinal()) / SM_SIZE).
        setEXECCTRL(value, mask, xor);
      break;
    case SM0_SHIFTCTRL:
    case SM1_SHIFTCTRL:
    case SM2_SHIFTCTRL:
    case SM3_SHIFTCTRL:
      pio.getSM((regNum - Regs.SM0_SHIFTCTRL.ordinal()) / SM_SIZE).
        setSHIFTCTRL(value, mask, xor);
      break;
    case SM0_ADDR:
    case SM1_ADDR:
    case SM2_ADDR:
    case SM3_ADDR:
      break; // read-only address
    case SM0_INSTR:
    case SM1_INSTR:
    case SM2_INSTR:
    case SM3_INSTR:
      pio.getSM((regNum - Regs.SM0_INSTR.ordinal()) / SM_SIZE).
        forceInstruction(value & mask);
      break;
    case SM0_PINCTRL:
    case SM1_PINCTRL:
    case SM2_PINCTRL:
    case SM3_PINCTRL:
      pio.getSM((regNum - Regs.SM0_PINCTRL.ordinal()) / SM_SIZE).
        setPINCTRL(value, mask, xor);
      break;
    case INTR:
      break; // read-only address
    case IRQ0_INTE:
      pio.getIRQ().setIRQ0_INTE(value, mask, xor);
      break;
    case IRQ1_INTE:
      pio.getIRQ().setIRQ1_INTE(value, mask, xor);
      break;
    case IRQ0_INTF:
      pio.getIRQ().setIRQ0_INTF(value, mask, xor);
      break;
    case IRQ1_INTF:
      pio.getIRQ().setIRQ1_INTF(value, mask, xor);
      break;
    case IRQ0_INTS:
    case IRQ1_INTS:
      break; // read-only address
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }

  private int readFStat()
  {
    return
      ((pio.getSM(3).isTXFIFOEmpty() ? 0x1 : 0x0) << 27) |
      ((pio.getSM(2).isTXFIFOEmpty() ? 0x1 : 0x0) << 26) |
      ((pio.getSM(1).isTXFIFOEmpty() ? 0x1 : 0x0) << 25) |
      ((pio.getSM(0).isTXFIFOEmpty() ? 0x1 : 0x0) << 24) |
      ((pio.getSM(3).isTXFIFOFull() ? 0x1 : 0x0) << 19) |
      ((pio.getSM(2).isTXFIFOFull() ? 0x1 : 0x0) << 18) |
      ((pio.getSM(1).isTXFIFOFull() ? 0x1 : 0x0) << 17) |
      ((pio.getSM(0).isTXFIFOFull() ? 0x1 : 0x0) << 16) |
      ((pio.getSM(3).isRXFIFOEmpty() ? 0x1 : 0x0) << 11) |
      ((pio.getSM(2).isRXFIFOEmpty() ? 0x1 : 0x0) << 10) |
      ((pio.getSM(1).isRXFIFOEmpty() ? 0x1 : 0x0) << 9) |
      ((pio.getSM(0).isRXFIFOEmpty() ? 0x1 : 0x0) << 8) |
      ((pio.getSM(3).isRXFIFOFull() ? 0x1 : 0x0) << 3) |
      ((pio.getSM(2).isRXFIFOFull() ? 0x1 : 0x0) << 2) |
      ((pio.getSM(1).isRXFIFOFull() ? 0x1 : 0x0) << 1) |
      ((pio.getSM(0).isRXFIFOFull() ? 0x1 : 0x0) << 0);
  }

  private int readFDebug()
  {
    int value = 0;
    for (int smNum = 0; smNum < SM_COUNT; smNum++) {
      final SM sm = pio.getSM(smNum);
      final FIFO fifo = sm.getFIFO();
      if (fifo.isTXStall()) {
        value |= 0x1 << (24 + smNum);
      }
      if (fifo.isTXOver()) {
        value |= 0x1 << (16 + smNum);
      }
      if (fifo.isRXUnder()) {
        value |= 0x1 << (8 + smNum);
      }
      if (fifo.isRXStall()) {
        value |= 0x1 << smNum;
      }
    }
    return value;
  }

  private int readFLevel()
  {
    return
      (pio.getSM(3).getRXFIFOLevel() << 28) |
      (pio.getSM(3).getTXFIFOLevel() << 24) |
      (pio.getSM(2).getRXFIFOLevel() << 20) |
      (pio.getSM(2).getTXFIFOLevel() << 16) |
      (pio.getSM(1).getRXFIFOLevel() << 12) |
      (pio.getSM(1).getTXFIFOLevel() << 8) |
      (pio.getSM(0).getRXFIFOLevel() << 4) |
      pio.getSM(0).getTXFIFOLevel();
  }

  private int getCfgInfo()
  {
    return
      MEMORY_SIZE << 16 |
      SM_COUNT << 8 |
      FIFO_DEPTH;
  }

  @Override
  public synchronized int readRegister(final int regNum)
  {
    checkRegNum(regNum);
    final Regs register = REGS[regNum];
    switch (register) {
    case CTRL:
      return pio.getCtrl();
    case FSTAT:
      return readFStat();
    case FDEBUG:
      return readFDebug();
    case FLEVEL:
      return readFLevel();
    case TXF0:
    case TXF1:
    case TXF2:
    case TXF3:
      return 0; // write-only address
    case RXF0:
    case RXF1:
    case RXF2:
    case RXF3:
      return pio.getSM(regNum - Regs.RXF0.ordinal()).get();
    case IRQ:
      return 0; // write-only address
    case IRQ_FORCE:
      return 0; // write-only address
    case INPUT_SYNC_BYPASS:
      return pio.getPIOGPIO().getGPIO().getInputSyncByPass();
    case DBG_PADOUT:
      return pio.getPIOGPIO().getPins(0, 32);
    case DBG_PADOE:
      return pio.getPIOGPIO().getPinDirs(0, 32);
    case DBG_CFGINFO:
      return getCfgInfo();
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
      return 0; // write-only address
    case SM0_CLKDIV:
    case SM1_CLKDIV:
    case SM2_CLKDIV:
    case SM3_CLKDIV:
      return
        pio.getSM((regNum - Regs.SM0_CLKDIV.ordinal()) / SM_SIZE).getCLKDIV();
    case SM0_EXECCTRL:
    case SM1_EXECCTRL:
    case SM2_EXECCTRL:
    case SM3_EXECCTRL:
      return
        pio.getSM((regNum - Regs.SM0_EXECCTRL.ordinal()) / SM_SIZE).
        getEXECCTRL();
    case SM0_SHIFTCTRL:
    case SM1_SHIFTCTRL:
    case SM2_SHIFTCTRL:
    case SM3_SHIFTCTRL:
      return
        pio.getSM((regNum - Regs.SM0_SHIFTCTRL.ordinal()) / SM_SIZE).
        getSHIFTCTRL();
    case SM0_ADDR:
    case SM1_ADDR:
    case SM2_ADDR:
    case SM3_ADDR:
      return pio.getSM((regNum - Regs.SM0_ADDR.ordinal()) / SM_SIZE).getPC();
    case SM0_INSTR:
    case SM1_INSTR:
    case SM2_INSTR:
    case SM3_INSTR:
      return
        pio.getSM((regNum - Regs.SM0_INSTR.ordinal()) / SM_SIZE).
        getOpCode();
    case SM0_PINCTRL:
    case SM1_PINCTRL:
    case SM2_PINCTRL:
    case SM3_PINCTRL:
      return
        pio.getSM((regNum - Regs.SM0_PINCTRL.ordinal()) / SM_SIZE).getPINCTRL();
    case INTR:
      return pio.getIRQ().readINTR();
    case IRQ0_INTE:
      return pio.getIRQ().getIRQ0_INTE();
    case IRQ1_INTE:
      return pio.getIRQ().getIRQ1_INTE();
    case IRQ0_INTF:
      return pio.getIRQ().getIRQ0_INTF();
    case IRQ1_INTF:
      return pio.getIRQ().getIRQ1_INTF();
    case IRQ0_INTS:
      return pio.getIRQ().readIRQ0_INTS();
    case IRQ1_INTS:
      return pio.getIRQ().readIRQ1_INTS();
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
