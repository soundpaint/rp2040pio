/*
 * @(#)Registers.java 1.00 21/02/25
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
public class Registers implements Constants
{
  public static final int CTRL = 0x000;
  public static final int FSTAT = 0x004;
  public static final int FDEBUG = 0x008;
  public static final int FLEVEL = 0x00c;
  public static final int TXF0 = 0x010;
  public static final int TXF1 = 0x014;
  public static final int TXF2 = 0x018;
  public static final int TXF3 = 0x01c;
  public static final int RXF0 = 0x020;
  public static final int RXF1 = 0x024;
  public static final int RXF2 = 0x028;
  public static final int RXF3 = 0x02c;
  public static final int IRQ = 0x030;
  public static final int IRQ_FORCE = 0x034;
  public static final int INPUT_SYNC_BYPASS = 0x038;
  public static final int DBG_PADOUT = 0x03c;
  public static final int DBG_PADOE = 0x040;
  public static final int DBG_CFGINFO = 0x044;
  public static final int INSTR_MEM0 = 0x048;
  public static final int INSTR_MEM1 = 0x04c;
  public static final int INSTR_MEM2 = 0x050;
  public static final int INSTR_MEM3 = 0x054;
  public static final int INSTR_MEM4 = 0x058;
  public static final int INSTR_MEM5 = 0x05c;
  public static final int INSTR_MEM6 = 0x060;
  public static final int INSTR_MEM7 = 0x064;
  public static final int INSTR_MEM8 = 0x068;
  public static final int INSTR_MEM9 = 0x06c;
  public static final int INSTR_MEM10 = 0x070;
  public static final int INSTR_MEM11 = 0x074;
  public static final int INSTR_MEM12 = 0x078;
  public static final int INSTR_MEM13 = 0x07c;
  public static final int INSTR_MEM14 = 0x080;
  public static final int INSTR_MEM15 = 0x084;
  public static final int INSTR_MEM16 = 0x088;
  public static final int INSTR_MEM17 = 0x08c;
  public static final int INSTR_MEM18 = 0x090;
  public static final int INSTR_MEM19 = 0x094;
  public static final int INSTR_MEM20 = 0x098;
  public static final int INSTR_MEM21 = 0x09c;
  public static final int INSTR_MEM22 = 0x0a0;
  public static final int INSTR_MEM23 = 0x0a4;
  public static final int INSTR_MEM24 = 0x0a8;
  public static final int INSTR_MEM25 = 0x0ac;
  public static final int INSTR_MEM26 = 0x0b0;
  public static final int INSTR_MEM27 = 0x0b4;
  public static final int INSTR_MEM28 = 0x0b8;
  public static final int INSTR_MEM29 = 0x0bc;
  public static final int INSTR_MEM30 = 0x0c0;
  public static final int INSTR_MEM31 = 0x0c4;
  public static final int SM0_CLKDIV = 0x0c8;
  public static final int SM0_EXECCTRL = 0x0cc;
  public static final int SM0_SHIFTCTRL = 0x0d0;
  public static final int SM0_ADDR = 0x0d4;
  public static final int SM0_INSTR = 0x0d8;
  public static final int SM0_PINCTRL = 0x0dc;
  public static final int SM1_CLKDIV = 0x0e0;
  public static final int SM1_EXECCTRL = 0x0e4;
  public static final int SM1_SHIFTCTRL = 0x0e8;
  public static final int SM1_ADDR = 0x0ec;
  public static final int SM1_INSTR = 0x0f0;
  public static final int SM1_PINCTRL = 0x0f4;
  public static final int SM2_CLKDIV = 0x0f8;
  public static final int SM2_EXECCTRL = 0x0fc;
  public static final int SM2_SHIFTCTRL = 0x100;
  public static final int SM2_ADDR = 0x104;
  public static final int SM2_INSTR = 0x108;
  public static final int SM2_PINCTRL = 0x10c;
  public static final int SM3_CLKDIV = 0x110;
  public static final int SM3_EXECCTRL = 0x114;
  public static final int SM3_SHIFTCTRL = 0x118;
  public static final int SM3_ADDR = 0x11c;
  public static final int SM3_INSTR = 0x120;
  public static final int SM3_PINCTRL = 0x124;
  public static final int INTR = 0x128;
  public static final int IRQ0_INTE = 0x12c;
  public static final int IRQ0_INTF = 0x130;
  public static final int IRQ0_INTS = 0x134;
  public static final int IRQ1_INTE = 0x138;
  public static final int IRQ1_INTF = 0x13c;
  public static final int IRQ1_INTS = 0x140;

  public static final int SM_SIZE = SM1_CLKDIV - SM0_CLKDIV;

  private final PIO pio;

  private Registers()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public Registers(final PIO pio)
  {
    if (pio == null) {
      throw new NullPointerException("pio");
    }
    this.pio = pio;
  }

  public int getIndex()
  {
    return pio.getIndex();
  }

  public PIO getPIO() { return pio; }

  public void gpioInit(final int pin)
  {
    if (pin < 0) {
      throw new IllegalArgumentException("pin < 0: " + pin);
    }
    if (pin > 31) {
      throw new IllegalArgumentException("pin > 31: " + pin);
    }
    final GPIO_Function function =
      getIndex() == 1 ? GPIO_Function.PIO1 : GPIO_Function.PIO0;
    pio.getGPIO().setFunction(pin, function);
  }

  /*
   * TODO: In all of the following methods, use constants declared in
   * class Constants for bit shifting & masking.
   */

  private void writeFDebug(final int value)
  {
    for (int smNum = 0; smNum < SM_COUNT; smNum++) {
      final SM sm = pio.getSM(smNum);
      final FIFO fifo = sm.getFIFO();
      if (((value >>> (24 + smNum)) & 0x1) != 0x0) {
        fifo.clearTXStall();
      }
      if (((value >>> (16 + smNum)) & 0x1) != 0x0) {
        fifo.clearTXOver();
      }
      if (((value >>> (8 + smNum)) & 0x1) != 0x0) {
        fifo.clearRXUnder();
      }
      if (((value >>> smNum) & 0x1) != 0x0) {
        fifo.clearRXStall();
      }
    }
  }

  public synchronized void write(final int address, final int value)
  {
    if (address < 0) {
      throw new IllegalArgumentException("address < 0: " + address);
    }
    if (address > 0x140) {
      throw new IllegalArgumentException("address > 0x140: " +
                                         String.format("%04x", address));
    }
    if ((address & 0x3) != 0x0) {
      throw new IllegalArgumentException("address not word-aligned: " +
                                         String.format("%04x", address));
    }
    switch (address) {
    case CTRL:
      pio.setCtrl(value);
      break;
    case FSTAT:
      break; // read-only address
    case FDEBUG:
      writeFDebug(value);
      break;
    case FLEVEL:
      break; // read-only address
    case TXF0:
    case TXF1:
    case TXF2:
    case TXF3:
      pio.getSM((address - TXF0) >> 2).put(value);
      break;
    case RXF0:
    case RXF1:
    case RXF2:
    case RXF3:
      break; // read-only address
    case IRQ:
      pio.getIRQ().writeRegIRQ(value);
      break;
    case IRQ_FORCE:
      pio.getIRQ().writeRegIRQ_FORCE(value);
      break;
    case INPUT_SYNC_BYPASS:
      pio.getGPIO().setInputSyncByPass(value);
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
      pio.getMemory().set((address - INSTR_MEM0) >> 2, (short)value);
      break;
    case SM0_CLKDIV:
    case SM1_CLKDIV:
    case SM2_CLKDIV:
    case SM3_CLKDIV:
      pio.getSM((address - SM0_CLKDIV) / SM_SIZE).setCLKDIV(value);
      break;
    case SM0_EXECCTRL:
    case SM1_EXECCTRL:
    case SM2_EXECCTRL:
    case SM3_EXECCTRL:
      pio.getSM((address - SM0_EXECCTRL) / SM_SIZE).setEXECCTRL(value);
      break;
    case SM0_SHIFTCTRL:
    case SM1_SHIFTCTRL:
    case SM2_SHIFTCTRL:
    case SM3_SHIFTCTRL:
      pio.getSM((address - SM0_SHIFTCTRL) / SM_SIZE).setSHIFTCTRL(value);
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
      pio.getSM((address - SM0_INSTR) / SM_SIZE).insertDMAInstruction(value);
      break;
    case SM0_PINCTRL:
    case SM1_PINCTRL:
    case SM2_PINCTRL:
    case SM3_PINCTRL:
      pio.getSM((address - SM0_PINCTRL) / SM_SIZE).setPINCTRL(value);
      break;
    case INTR:
      break; // read-only address
    case IRQ0_INTE:
      pio.getIRQ().setIRQ0_INTE(value);
      break;
    case IRQ1_INTE:
      pio.getIRQ().setIRQ1_INTE(value);
      break;
    case IRQ0_INTF:
      pio.getIRQ().setIRQ0_INTF(value);
      break;
    case IRQ1_INTF:
      pio.getIRQ().setIRQ1_INTF(value);
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

  public synchronized int read(final int address)
  {
    if (address < 0) {
      throw new IllegalArgumentException("address < 0: " + address);
    }
    if (address > 0x140) {
      throw new IllegalArgumentException("address > 0x140: " +
                                         String.format("%04x", address));
    }
    if ((address & 0x3) != 0x0) {
      throw new IllegalArgumentException("address not word-aligned: " +
                                         String.format("%04x", address));
    }
    switch (address) {
    case CTRL:
      return pio.getSM_ENABLED();
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
      return pio.getSM((address - TXF0) >> 2).get();
    case IRQ:
      return pio.getIRQ().readRegIRQ();
    case IRQ_FORCE:
      return pio.getIRQ().readRegIRQ_FORCE();
    case INPUT_SYNC_BYPASS:
      return pio.getGPIO().getInputSyncByPass();
    case DBG_PADOUT:
      return pio.getGPIO().getPins(0, 32);
    case DBG_PADOE:
      return pio.getGPIO().getPinDirs(0, 32);
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
      return pio.getSM((address - SM0_CLKDIV) / SM_SIZE).getCLKDIV();
    case SM0_EXECCTRL:
    case SM1_EXECCTRL:
    case SM2_EXECCTRL:
    case SM3_EXECCTRL:
      return pio.getSM((address - SM0_EXECCTRL) / SM_SIZE).getEXECCTRL();
    case SM0_SHIFTCTRL:
    case SM1_SHIFTCTRL:
    case SM2_SHIFTCTRL:
    case SM3_SHIFTCTRL:
      return pio.getSM((address - SM0_SHIFTCTRL) / SM_SIZE).getSHIFTCTRL();
    case SM0_ADDR:
    case SM1_ADDR:
    case SM2_ADDR:
    case SM3_ADDR:
      return pio.getSM((address - SM0_ADDR) / SM_SIZE).getPC();
    case SM0_INSTR:
    case SM1_INSTR:
    case SM2_INSTR:
    case SM3_INSTR:
      return pio.getSM((address - SM0_INSTR) / SM_SIZE).getInstruction();
    case SM0_PINCTRL:
    case SM1_PINCTRL:
    case SM2_PINCTRL:
    case SM3_PINCTRL:
      return pio.getSM((address - SM0_PINCTRL) / SM_SIZE).getPINCTRL();
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
