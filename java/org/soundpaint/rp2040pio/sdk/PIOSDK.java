/*
 * @(#)PIOSDK.java 1.00 21/02/25
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
package org.soundpaint.rp2040pio.sdk;

import java.io.IOException;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PIO;
import org.soundpaint.rp2040pio.PIORegisters;

/**
 * PIO SDK Interface
 */
public class PIOSDK implements Constants
{
  private final PIORegisters registers;
  private final int pioBaseAddress;
  private final GPIOSDK gpioSdk;

  private PIOSDK()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public PIOSDK(final PIO pio, final int pioBaseAddress)
  {
    this(new PIORegisters(pio, pioBaseAddress), new GPIOSDK(pio.getGPIO()));
  }

  public PIOSDK(final PIORegisters registers,
                final GPIOSDK gpioSdk)
  {
    if (registers == null) {
      throw new NullPointerException("registers");
    }
    if (gpioSdk == null) {
      throw new NullPointerException("gpio sdk");
    }
    this.registers = registers;
    this.pioBaseAddress = registers.getBaseAddress();
    this.gpioSdk = gpioSdk;
  }

  public PIORegisters getRegisters() { return registers; }

  // ---- Functions for compatibility with the Pico SDK, SM Config Group ----

  public static SMConfig getDefaultSmConfig()
  {
    return SMConfig.getDefault();
  }

  public void smSetOutPins(final int smNum,
                           final int outBase, final int outCount)
  {
    Constants.checkSmNum(smNum);
    if (outBase < 0) {
      throw new IllegalArgumentException("outBase < 0: " + outBase);
    }
    if (outBase > GPIO_NUM - 1) {
      throw new IllegalArgumentException("outBase > " + (GPIO_NUM - 1) + ": " +
                                         outBase);
    }
    if (outCount < 0) {
      throw new IllegalArgumentException("outCount < 0: " + outCount);
    }
    if (outCount > GPIO_NUM) {
      throw new IllegalArgumentException("outCount > " + GPIO_NUM + ": " +
                                         outCount);
    }
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_PINCTRL, smNum);
    synchronized(registers) {
      int pinCtrl = registers.readAddress(address);
      pinCtrl &= ~(SM0_PINCTRL_OUT_COUNT_BITS | SM0_PINCTRL_OUT_BASE_BITS);
      pinCtrl |= outCount << SM0_PINCTRL_OUT_COUNT_LSB;
      pinCtrl |= outBase << SM0_PINCTRL_OUT_BASE_LSB;
      registers.writeAddress(address, pinCtrl);
    }
  }

  public void smSetSetPins(final int smNum,
                           final int setBase, final int setCount)
  {
    Constants.checkSmNum(smNum);
    if (setBase < 0) {
      throw new IllegalArgumentException("setBase < 0: " + setBase);
    }
    if (setBase > GPIO_NUM - 1) {
      throw new IllegalArgumentException("setBase > " + (GPIO_NUM - 1) + ": " +
                                         setBase);
    }
    if (setCount < 0) {
      throw new IllegalArgumentException("setCount < 0: " + setCount);
    }
    if (setCount > 5) {
      throw new IllegalArgumentException("setCount > 5: " + setCount);
    }
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_PINCTRL, smNum);
    synchronized(registers) {
      int pinCtrl = registers.readAddress(address);
      pinCtrl &= ~(SM0_PINCTRL_SET_COUNT_BITS | SM0_PINCTRL_SET_BASE_BITS);
      pinCtrl |= setCount << SM0_PINCTRL_SET_COUNT_LSB;
      pinCtrl |= setBase << SM0_PINCTRL_SET_BASE_LSB;
      registers.writeAddress(address, pinCtrl);
    }
  }

  public void smSetInPins(final int smNum, final int inBase)
  {
    Constants.checkSmNum(smNum);
    if (inBase < 0) {
      throw new IllegalArgumentException("inBase < 0: " + inBase);
    }
    if (inBase > GPIO_NUM - 1) {
      throw new IllegalArgumentException("inBase > " + (GPIO_NUM - 1) + ": " +
                                         inBase);
    }
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_PINCTRL, smNum);
    synchronized(registers) {
      int pinCtrl = registers.readAddress(address);
      pinCtrl &= ~SM0_PINCTRL_IN_BASE_BITS;
      pinCtrl |= inBase << SM0_PINCTRL_IN_BASE_LSB;
      registers.writeAddress(address, pinCtrl);
    }
  }

  public void smSetSideSetPins(final int smNum, final int sideSetBase)
  {
    Constants.checkSmNum(smNum);
    if (sideSetBase < 0) {
      throw new IllegalArgumentException("sideSetBase < 0: " + sideSetBase);
    }
    if (sideSetBase > GPIO_NUM - 1) {
      throw new IllegalArgumentException("sideSetBase > " +
                                         (GPIO_NUM - 1) + ": " +
                                         sideSetBase);
    }
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_PINCTRL, smNum);
    synchronized(registers) {
      int pinCtrl = registers.readAddress(address);
      pinCtrl &= ~SM0_PINCTRL_SIDESET_BASE_BITS;
      pinCtrl |= sideSetBase << SM0_PINCTRL_SIDESET_BASE_LSB;
      registers.writeAddress(address, pinCtrl);
    }
  }

  // ---- Functions for compatibility with the Pico SDK, PIO Group ----

  /**
   * Tracking allocation of instruction memory is not a feature of the
   * RP2040 itself, but a feature of the SDK.  This is, why we do not
   * put this stuff into the memory class.
   */
  private Integer memoryAllocation = 0x0;

  /**
   * Tracking claim of state machines is not a feature of the RP2040
   * itself, but a feature of the SDK.  This is, why we do not put
   * this stuff into the state machine class.
   */
  private Integer stateMachineClaimed = 0x0;

  public void smSetConfig(final int smNum, final SMConfig smConfig)
  {
    Constants.checkSmNum(smNum);
    if (smConfig == null) {
      throw new NullPointerException("smConfig");
    }
    synchronized(registers) {
      final int smClkDivAddr =
        registers.getSMAddress(PIORegisters.Regs.SM0_CLKDIV, smNum);
      registers.writeAddress(smClkDivAddr, smConfig.getClkDiv());
      final int smExecCtrlAddr =
        registers.getSMAddress(PIORegisters.Regs.SM0_EXECCTRL, smNum);
      registers.writeAddress(smExecCtrlAddr, smConfig.getExecCtrl());
      final int smShiftCtrlAddr =
        registers.getSMAddress(PIORegisters.Regs.SM0_SHIFTCTRL, smNum);
      registers.writeAddress(smShiftCtrlAddr, smConfig.getShiftCtrl());
      final int smPinCtrlAddr =
        registers.getSMAddress(PIORegisters.Regs.SM0_PINCTRL, smNum);
      registers.writeAddress(smPinCtrlAddr, smConfig.getPinCtrl());
    }
  }

  public int getIndex()
  {
    return registers.getPIOIndex();
  }

  public void gpioInit(final int pin)
  {
    final GPIO_Function function =
      getIndex() == 1 ? GPIO_Function.PIO1 : GPIO_Function.PIO0;
    gpioSdk.setFunction(pin, function);
  }

  /**
   * Given one of the 8 DMA channels (RX and TX for each state
   * machine) between DMA and this PIO, return the corresponding DREQ
   * number, as specified in Table 120, Sect. 2.5 ("DMA") of the
   * RP2040 data sheet.
   */
  public int getDREQ(final int smNum, final boolean isTX)
  {
    Constants.checkSmNum(smNum);
    return (getIndex() << 3) | (isTX ? 0 : SM_COUNT) | smNum;
  }

  /**
   * Tries to allocate memory for the specified allocation mask and
   * origin.  Returns address (0..31) where the allocation is
   * performed.
   * @param allocationMask Bit mask of instruction addresses (0..31)
   * to allocate.
   * @param origin Address where to allocate, of -1, if any address is
   * acceptable.
   * @param checkOnly If true, allocation is only checked for, but not
   * performed.  Also, if allocation is not possible, -1 is returned
   * rather than throwing an exception.
   */
  private int allocateMemory(final int allocationMask, final int origin,
                             final boolean checkOnly)
  {
    synchronized(memoryAllocation) {
      if (origin >= 0) {
        if ((memoryAllocation & ~allocationMask) == 0x0) {
          if (!checkOnly) memoryAllocation |= allocationMask;
          return origin;
        }
        if (checkOnly) return -1;
        final String message =
          String.format("allocation at %02x failed", origin);
        throw new RuntimeException(message);
      }
      for (int offset = 0; offset < MEMORY_SIZE; offset++) {
        final int allocationMaskForOffset =
          (allocationMask << offset) |
          (allocationMask << (offset - MEMORY_SIZE));
        if ((memoryAllocation & ~allocationMaskForOffset) == 0x0) {
          if (!checkOnly) memoryAllocation |= allocationMask;
          return offset;
        }
      }
    }
    if (checkOnly) return -1;
    final String message =
      String.format("allocation at %02x failed", origin);
    throw new RuntimeException(message);
  }

  public boolean canAddProgram(final Program program)
  {
    if (program == null) {
      throw new NullPointerException("program");
    }
    final int allocationMask = program.getAllocationMask();
    final int origin = program.getOrigin();
    return allocateMemory(allocationMask, origin, true) >= 0;
  }

  public boolean canAddProgramAtOffset(final Program program, final int offset)
  {
    if (program == null) {
      throw new NullPointerException("program");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset < 0: " + offset);
    }
    if (offset > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("offset > " +
                                         (MEMORY_SIZE - 1) + ": " +
                                         offset);
    }
    final int origin = program.getOrigin();
    if (origin >= 0) {
      // do not allocate program with fixed origin at different offset
      if (origin != offset) return false;
    }
    final int allocationMask = program.getAllocationMask();
    final int allocationMaskForOffset =
      origin >= 0 ?
      allocationMask :
      (allocationMask << offset) | (allocationMask << (offset - MEMORY_SIZE));
    return allocateMemory(allocationMaskForOffset, offset, true) >= 0;
  }

  private void writeProgram(final Program program, final int addressOffset)
  {
    if (program == null) {
      throw new NullPointerException("program");
    }
    if (addressOffset < 0) {
      throw new IllegalArgumentException("address offset < 0: " +
                                         addressOffset);
    }
    if (addressOffset > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("addressOffset > " +
                                         (MEMORY_SIZE - 1) + ": " +
                                         addressOffset);
    }
    final int length = program.getLength();
    synchronized(registers) {
      for (int index = 0; index < length; index++) {
        final short instruction = program.getInstruction(index);
        final int memoryAddress = (addressOffset + index) & 0x1f;
        registers.writeAddress(registers.getMemoryAddress(memoryAddress),
                               instruction);
      }
    }
  }

  public int addProgram(final String resourcePath) throws IOException
  {
    return addProgram(ProgramParser.parse(resourcePath));
  }

  public int addProgram(final Program program)
  {
    if (program == null) {
      throw new NullPointerException("program");
    }
    final int allocationMask = program.getAllocationMask();
    final int origin = program.getOrigin();
    final int address = allocateMemory(allocationMask, origin, false);
    writeProgram(program, address);
    return address;
  }

  public int addProgramAtOffset(final String resourcePath, final int offset)
    throws IOException
  {
    return addProgramAtOffset(ProgramParser.parse(resourcePath), offset);
  }

  public int addProgramAtOffset(final Program program, final int offset)
  {
    if (program == null) {
      throw new NullPointerException("program");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset < 0: " + offset);
    }
    if (offset > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("offset > " +
                                         (MEMORY_SIZE - 1) + ": " +
                                         offset);
    }
    final int origin = program.getOrigin();
    if (origin >= 0) {
      // do not allocate program with fixed origin at different offset
      if (origin != offset) {
        final String message =
          String.format("allocation at %02x failed for program %s: " +
                        "conflicting origin: %02x",
                        offset, program, origin);
        throw new RuntimeException(message);
      }
    }
    final int allocationMask = program.getAllocationMask();
    final int allocationMaskForOffset =
      origin >= 0 ?
      allocationMask :
      (allocationMask << offset) |
      (allocationMask << (offset - MEMORY_SIZE));
    final int address = allocateMemory(allocationMaskForOffset, offset, false);
    writeProgram(program, address);
    return address;
  }

  public void removeProgram(final Program program, final int loadedOffset)
  {
    if (program == null) {
      throw new NullPointerException("program");
    }
    if (loadedOffset < 0) {
      throw new IllegalArgumentException("loaded offset < 0: " + loadedOffset);
    }
    if (loadedOffset > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("loaded offset > " +
                                         (MEMORY_SIZE - 1) + ": " +
                                         loadedOffset);
    }
    final int origin = program.getOrigin();
    if (origin >= 0) {
      // can not remove program from offset it is not designed for
      if (origin != loadedOffset) {
        final String message =
          String.format("can not remove program %s from offset %02x: " +
                        "program has conflicting origin: %02x",
                        program, loadedOffset, origin);
        throw new RuntimeException(message);
      }
    }
    final int allocationMask = program.getAllocationMask();
    final int allocationMaskForOffset =
      origin >= 0 ?
      allocationMask :
      (allocationMask << loadedOffset) |
      (allocationMask << (loadedOffset - MEMORY_SIZE));
    synchronized(memoryAllocation) {
      if ((memoryAllocation &= ~allocationMaskForOffset) !=
          allocationMaskForOffset) {
        final String message =
          String.format("deallocation at %02x failed for program %s: " +
                        "allocation bits corrupted",
                        loadedOffset, program);
        throw new RuntimeException(message);
      }
      memoryAllocation &= ~allocationMaskForOffset;
      synchronized(registers) {
        for (int index = 0; index < program.getLength(); index++) {
          final int memoryAddress = (loadedOffset + index) & 0x1f;
          registers.writeAddress(registers.getMemoryAddress(memoryAddress), 0);
        }
      }
    }
  }

  public void clearInstructionMemory()
  {
    synchronized(memoryAllocation) {
      memoryAllocation = 0;
      synchronized(registers) {
        for (int memoryAddress = 0; memoryAddress < MEMORY_SIZE;
             memoryAddress++) {
          registers.writeAddress(registers.getMemoryAddress(memoryAddress), 0);
        }
      }
    }
  }

  public void smInit(final int smNum, final int initialPC,
                     final SMConfig config)
  {
    Constants.checkSmNum(smNum);
    smSetEnabled(smNum, false);
    smSetConfig(smNum, config != null ? config : getDefaultSmConfig());
    smClearFIFOs(smNum);
    final int fDebug =
      ((0x1 << FDEBUG_TXSTALL_LSB) |
       (0x1 << FDEBUG_TXOVER_LSB) |
       (0x1 << FDEBUG_RXUNDER_LSB) |
       (0x1 << FDEBUG_RXSTALL_LSB)) << smNum;
    registers.writeAddress(registers.getAddress(PIORegisters.Regs.FDEBUG),
                           fDebug);
    smRestart(smNum);
    smClkDivRestart(smNum);
    final int jmpInstruction =
      initialPC & 0x001f; // no sideset/delay => all other bits are 0
    smExec(smNum, (short)jmpInstruction);
  }

  public void smSetEnabled(final int smNum, final boolean enabled)
  {
    Constants.checkSmNum(smNum);
    setSmMaskEnabled(0x1 << smNum, enabled);
  }

  public void setSmMaskEnabled(final int mask, final boolean enabled)
  {
    final int address = registers.getAddress(PIORegisters.Regs.CTRL);
    synchronized(registers) {
      int ctrl = registers.readAddress(address);
      ctrl = (ctrl & ~mask) | (enabled ? mask : 0x0);
      registers.writeAddress(address, ctrl);
    }
  }

  public void smRestart(final int smNum)
  {
    Constants.checkSmNum(smNum);
    restartSmMask(0x1 << smNum);
  }

  public void restartSmMask(final int mask)
  {
    if (mask < 0) {
      throw new IllegalArgumentException("mask < 0: " + mask);
    }
    if (mask > (0x1 << SM_COUNT) - 1) {
      throw new IllegalArgumentException("mask > " +
                                         ((0x1 << SM_COUNT) - 1) + ": " +
                                         mask);
    }
    final int address = registers.getAddress(PIORegisters.Regs.CTRL);
    synchronized(registers) {
      int ctrl = registers.readAddress(address);
      ctrl |= (mask << CTRL_SM_RESTART_LSB) & CTRL_SM_RESTART_BITS;
      registers.writeAddress(address, ctrl);
    }
  }

  public void smClkDivRestart(final int smNum)
  {
    Constants.checkSmNum(smNum);
    clkDivRestartSmMask(0x1 << smNum);
  }

  public void clkDivRestartSmMask(final int mask)
  {
    if (mask < 0) {
      throw new IllegalArgumentException("mask < 0: " + mask);
    }
    if (mask > (0x1 << SM_COUNT) - 1) {
      throw new IllegalArgumentException("mask > " +
                                         ((0x1 << SM_COUNT) - 1) + ": " +
                                         mask);
    }
    final int address = registers.getAddress(PIORegisters.Regs.CTRL);
    synchronized(registers) {
      int ctrl = registers.readAddress(address);
      ctrl |= (mask << CTRL_CLKDIV_RESTART_LSB) & CTRL_CLKDIV_RESTART_BITS;
      registers.writeAddress(address, ctrl);
    }
  }

  public void enableSmMaskInSync(final int mask)
  {
    if (mask < 0) {
      throw new IllegalArgumentException("mask < 0: " + mask);
    }
    if (mask > (0x1 << SM_COUNT) - 1) {
      throw new IllegalArgumentException("mask > " +
                                         ((0x1 << SM_COUNT) - 1) + ": " +
                                         mask);
    }
    final int address = registers.getAddress(PIORegisters.Regs.CTRL);
    synchronized(registers) {
      int ctrl = registers.readAddress(address);
      ctrl |=
        ((mask << CTRL_CLKDIV_RESTART_LSB) & CTRL_CLKDIV_RESTART_BITS) |
        ((mask << CTRL_SM_ENABLE_LSB) & CTRL_SM_ENABLE_BITS);
      registers.writeAddress(address, ctrl);
    }
  }

  public int smGetPC(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_ADDR, smNum);
    return registers.readAddress(address);
  }

  public void smExec(final int smNum, final short instr)
  {
    Constants.checkSmNum(smNum);
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_ADDR, smNum);
    registers.writeAddress(address, instr & 0xffff);
  }

  public boolean smIsExecStalled(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_EXECCTRL, smNum);
    final int execCtrl = registers.readAddress(address);
    return (execCtrl & SM0_EXECCTRL_EXEC_STALLED_BITS) != 0x0;
  }

  public void smExecWaitBlocking(final int smNum, final short instr)
  {
    Constants.checkSmNum(smNum);
    smExec(smNum, instr);
    while (smIsExecStalled(smNum)) Thread.yield();
  }

  public void smSetWrap(final int smNum, final int wrapTarget,
                        final int wrap)
  {
    Constants.checkSmNum(smNum);
    if (wrapTarget < 0) {
      throw new IllegalArgumentException("wrap target < 0: " + wrapTarget);
    }
    if (wrapTarget > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("wrap target > " +
                                         (MEMORY_SIZE - 1) + ": " +
                                         wrapTarget);
    }
    if (wrap < 0) {
      throw new IllegalArgumentException("wrap < 0: " + wrap);
    }
    if (wrap > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("wrap > " + (MEMORY_SIZE - 1) + ": " +
                                         wrap);
    }
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_EXECCTRL, smNum);
    synchronized(registers) {
      int execCtrl = registers.readAddress(address);
      execCtrl &= ~(SM0_EXECCTRL_WRAP_TOP_BITS | SM0_EXECCTRL_WRAP_BOTTOM_BITS);
      execCtrl |= wrap << SM0_EXECCTRL_WRAP_TOP_LSB;
      execCtrl |= wrapTarget << SM0_EXECCTRL_WRAP_BOTTOM_LSB;
      registers.writeAddress(address, execCtrl);
    }
  }

  public void smPut(final int smNum, final int data)
  {
    Constants.checkSmNum(smNum);
    registers.writeAddress(registers.getTXFAddress(smNum), data);
  }

  public int smGet(final int smNum)
  {
    Constants.checkSmNum(smNum);
    return registers.readAddress(registers.getRXFAddress(smNum));
  }

  public boolean smIsRXFIFOFull(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final int fStat =
      registers.readAddress(registers.getAddress(PIORegisters.Regs.FSTAT));
    return (fStat & (0x1 << (FSTAT_RXFULL_LSB + smNum))) != 0x0;
  }

  public boolean smIsRXFIFOEmpty(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final int fStat =
      registers.readAddress(registers.getAddress(PIORegisters.Regs.FSTAT));
    return (fStat & (0x1 << (FSTAT_RXEMPTY_LSB + smNum))) != 0x0;
  }

  public int smGetRXFIFOLevel(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final int shiftCount =
      FLEVEL_RX0_LSB + smNum * (FLEVEL_RX1_LSB - FLEVEL_RX0_LSB);
    final int mask = FLEVEL_RX0_BITS >> FLEVEL_RX0_LSB;
    return
      (registers.readAddress(registers.getAddress(PIORegisters.Regs.FLEVEL)) >>
       shiftCount) & mask;
  }

  public boolean smIsTXFIFOFull(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final int fStat =
      registers.readAddress(registers.getAddress(PIORegisters.Regs.FSTAT));
    return (fStat & (0x1 << (FSTAT_TXFULL_LSB + smNum))) != 0x0;
  }

  public boolean smIsTXFIFOEmpty(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final int fStat =
      registers.readAddress(registers.getAddress(PIORegisters.Regs.FSTAT));
    return (fStat & (0x1 << (FSTAT_TXEMPTY_LSB + smNum))) != 0x0;
  }

  public int smGetTXFIFOLevel(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final int shiftCount =
      FLEVEL_TX0_LSB + smNum * (FLEVEL_TX1_LSB - FLEVEL_TX0_LSB);
    final int mask = FLEVEL_TX0_BITS >> FLEVEL_TX0_LSB;
    return
      (registers.readAddress(registers.getAddress(PIORegisters.Regs.FLEVEL)) >>
       shiftCount) & mask;
  }

  public void smPutBlocking(final int smNum, final int data)
  {
    Constants.checkSmNum(smNum);
    while (smIsTXFIFOFull(smNum)) {
      Thread.yield();
    }
    smPut(smNum, data);
  }

  public int smGetBlocking(final int smNum)
  {
    Constants.checkSmNum(smNum);
    while (smIsRXFIFOEmpty(smNum)) {
      Thread.yield();
    }
    return smGet(smNum);
  }

  public void smDrainTXFIFO(final int smNum)
  {
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_SHIFTCTRL, smNum);
    final boolean autoPull =
      (registers.readAddress(address) & SM0_SHIFTCTRL_AUTOPULL_BITS) != 0x0;
    final int instruction =
      autoPull ? 0x6060 : 0x8080;
    while (smIsTXFIFOEmpty(smNum)) {
      smExec(smNum, (short)instruction);
      // TODO: Wait for completion of inserted instruction?
    }
  }

  public void smSetClkDiv(final int smNum, final float div)
  {
    if (div < 0.0f) {
      throw new IllegalArgumentException("div < 0: " + div);
    }
    if (div >= 65536.0f) {
      throw new IllegalArgumentException("div >= 65536: " + div);
    }
    final int divInt = (int)div;
    final int divFrac = (int)((div - divInt) * 256.0);
    smSetClkDivIntFrac(smNum, divInt, divFrac);
  }

  public void smSetClkDivIntFrac(final int smNum,
                                 final int divInt, final int divFrac)
  {
    Constants.checkSmNum(smNum);
    if (divInt < 0) {
      throw new IllegalArgumentException("div integer bits < 0: " +
                                         divInt);
    }
    if (divInt > 0xffff) {
      throw new IllegalArgumentException("div integer bits > 65535: " +
                                         divInt);
    }
    if (divFrac < 0) {
      throw new IllegalArgumentException("div fractional bits < 0: " +
                                         divFrac);
    }
    if (divFrac > 0xff) {
      throw new IllegalArgumentException("div fractional bits > 255: " +
                                         divFrac);
    }
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_CLKDIV, smNum);
    final int clkDiv =
      divInt << SM0_CLKDIV_INT_LSB | divFrac << SM0_CLKDIV_FRAC_LSB;
    registers.writeAddress(address, clkDiv);
  }

  public void smClearFIFOs(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_SHIFTCTRL, smNum);
    synchronized(registers) {
      int shiftCtrl = registers.readAddress(address);
      // toggle RX join bit to force clearance of both, RX and TX
      shiftCtrl ^= SM0_SHIFTCTRL_FJOIN_RX_BITS;
      registers.writeAddress(address, shiftCtrl);
      // toggle once again to restore previous value
      shiftCtrl ^= SM0_SHIFTCTRL_FJOIN_RX_BITS;
      registers.writeAddress(address, shiftCtrl);
    }
    /*
     * TODO: Check if FIFO implementation behaves correctly for this
     * code.
     *
     * TODO: What if TX join bit is set upon executing this method?
     * Will this code reconfigure FIFOs to join RX / TX differently?
     * (Cp. RP Pico SDK Issue #201.)
     */
  }

  public void smSetPins(final int smNum, int pins)
  {
    Constants.checkSmNum(smNum);
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_PINCTRL, smNum);
    synchronized(registers) {
      final int pinCtrlSaved = registers.readAddress(address);
      int remaining = 32;
      int base = 0;
      while (remaining > 0) {
        final int decrement = remaining > 5 ? 5 : remaining;
        registers.writeAddress(address,
                               (decrement << SM0_PINCTRL_SET_COUNT_LSB) |
                               (base << SM0_PINCTRL_SET_BASE_LSB));
        final int setInstruction =
          0xe000 | (pins & 0x1f); // no sideset/delay => all other bits are 0
        smExec(smNum, (short)setInstruction);
        remaining -= decrement;
        base += decrement;
        pins >>>= 5;
      }
      registers.writeAddress(address, pinCtrlSaved);
    }
  }

  public void smSetPinsWithMask(final int smNum, final int pinValues,
                                int pinMask)
  {
    Constants.checkSmNum(smNum);
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_PINCTRL, smNum);
    synchronized(registers) {
      final int pinCtrlSaved = registers.readAddress(address);
      while (pinMask > 0) {
        final int base = Constants.ctz(pinMask);
        registers.writeAddress(address,
                               (0x1 << SM0_PINCTRL_SET_COUNT_LSB) |
                               (base << SM0_PINCTRL_SET_BASE_LSB));
        final int setInstruction =
          0xe000 | ((pinValues >> base) & 0x1); // no sideset/delay =>
        // all other bits are 0
        smExec(smNum, (short)setInstruction);
        pinMask &= pinMask - 1;
      }
      registers.writeAddress(address, pinCtrlSaved);
    }
  }

  public void smSetPinDirsWithMask(final int smNum, final int pinDirs,
                                   int pinMask)
  {
    Constants.checkSmNum(smNum);
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_PINCTRL, smNum);
    synchronized(registers) {
      final int pinCtrlSaved = registers.readAddress(address);
      while (pinMask > 0) {
        final int base = Constants.ctz(pinMask);
        registers.writeAddress(address,
                               (0x1 << SM0_PINCTRL_SET_COUNT_LSB) |
                               (base << SM0_PINCTRL_SET_BASE_LSB));
        final int setInstruction =
          0xe080 | ((pinDirs >> base) & 0x1); // no sideset/delay =>
                                              // all other bits are 0
        smExec(smNum, (short)setInstruction);
        pinMask &= pinMask - 1;
      }
      registers.writeAddress(address, pinCtrlSaved);
    }
  }

  public void smSetConsecutivePinDirs(final int smNum,
                                      int pinBase, int pinCount,
                                      final boolean isOut)
  {
    Constants.checkSmNum(smNum);
    if (pinBase < 0) {
      throw new IllegalArgumentException("pin base < 0: " + pinBase);
    }
    if (pinBase > 31) {
      throw new IllegalArgumentException("pin base > 31: " + pinBase);
    }
    if (pinCount < 0) {
      throw new IllegalArgumentException("pin count < 0: " + pinCount);
    }
    if (pinCount > 31) {
      throw new IllegalArgumentException("pin count > 31: " + pinCount);
    }
    final int address =
      registers.getSMAddress(PIORegisters.Regs.SM0_PINCTRL, smNum);
    synchronized(registers) {
      final int pinCtrlSaved = registers.readAddress(address);
      final int pinDirValue = isOut ? 0x1f : 0x0;
      while (pinCount > 5) {
        registers.writeAddress(address,
                               (0x5 << SM0_PINCTRL_SET_COUNT_LSB) |
                               (pinBase << SM0_PINCTRL_SET_BASE_LSB));
        final int setInstruction =
          0xe080 | pinDirValue; // no sideset/delay => all other bits
                                // are 0
        smExec(smNum, (short)setInstruction);
        pinCount -= 5;
        pinBase = (pinBase + 5) & 0x1f;
      }
      registers.writeAddress(address,
                             (pinCount << SM0_PINCTRL_SET_COUNT_LSB) |
                             (pinBase << SM0_PINCTRL_SET_BASE_LSB));
      final int setInstruction =
        0xe080 | pinDirValue; // no sideset/delay => all other bits
                              // are 0
      smExec(smNum, (short)setInstruction);
      registers.writeAddress(address, pinCtrlSaved);
    }
  }

  public void smClaim(final int smNum)
  {
    Constants.checkSmNum(smNum);
    claimSmMask(0x1 << smNum);
  }

  private String listMaskBits(final int mask) {
    final StringBuffer s = new StringBuffer();
    for (int count = 0; count < 32; count++) {
      if ((mask & (0x1 << count)) != 0x0) {
        if (s.length() > 0) s.append(", ");
        s.append(count);
      }
    }
    return s.toString();
  }

  public void claimSmMask(final int mask)
  {
    if (mask < 0) {
      throw new IllegalArgumentException("mask < 0: " + mask);
    }
    if (mask > (0x1 << SM_COUNT) - 1) {
      throw new IllegalArgumentException("mask > " +
                                         ((0x1 << SM_COUNT) - 1) + ": " +
                                         mask);
    }
    synchronized(stateMachineClaimed) {
      if ((stateMachineClaimed & mask) != 0x0) {
        final String message =
          String.format("claim failed: state machine(s) already in use: %s",
                        listMaskBits(mask));
        throw new RuntimeException(message);
      }
      stateMachineClaimed |= mask;
    }
  }

  public void smUnclaim(final int smNum)
  {
    Constants.checkSmNum(smNum);
    final int mask = 0x1 << smNum;
    synchronized(stateMachineClaimed) {
      stateMachineClaimed &= ~mask;
    }
  }

  public int claimUnusedSm(final boolean required)
  {
    synchronized(stateMachineClaimed) {
      final int unclaimed = ~stateMachineClaimed & ((0x1 << SM_COUNT) - 1);
      if (unclaimed == 0x0) {
        if (required) {
          final String message =
            "claim failed: all state machines already in use";
          throw new RuntimeException(message);
        }
        return -1;
      }
      for (int smNum = 0; smNum < SM_COUNT; smNum++) {
        if ((unclaimed & (0x1 << smNum)) != 0x0) {
          return smNum;
        }
      }
      throw new InternalError("unexpected fall-through");
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
