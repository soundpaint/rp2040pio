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

import java.io.BufferedReader;
import java.io.IOException;
import org.soundpaint.rp2040pio.AddressSpace;
import org.soundpaint.rp2040pio.Bit;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.Decoder;
import org.soundpaint.rp2040pio.Direction;
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.Instruction;
import org.soundpaint.rp2040pio.PinState;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;

/**
 * PIO SDK Interface
 */
public class PIOSDK implements Constants
{
  private static final Decoder decoder = new Decoder();

  private final int pioNum;
  private final AddressSpace memory;
  private final GPIOSDK gpioSdk;

  private PIOSDK()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public PIOSDK(final int pioNum, final AddressSpace memory,
                final GPIOSDK gpioSdk)
  {
    Constants.checkPioNum(pioNum, "PIO index number");
    if (memory == null) {
      throw new NullPointerException("memory");
    }
    if (gpioSdk == null) {
      throw new NullPointerException("gpio sdk");
    }
    this.pioNum = pioNum;
    this.memory = memory;
    this.gpioSdk = gpioSdk;
  }

  /**
   * Holds a copy of all info of a specific Instruction during a
   * specific cycle that is relevant for the timing diagram.
   */
  public static class InstructionInfo
  {
    private final int origin;
    private final String mnemonic;
    private final String fullStatement;
    private final boolean isDelayCycle;
    private final int delay;

    private InstructionInfo()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    /**
     * @param origin Either memory address (0…31), or
     * INSTR_ORIGIN_FORCED for an enforced instruction, or
     * INSTR_ORIGIN_EXECD for an EXEC'd instruction, or
     * INSTR_ORIGIN_UNKNOWN if the origin is not available.
     */
    public InstructionInfo(final int origin,
                           final String mnemonic, final String fullStatement,
                           final boolean isDelayCycle, final int delay)
    {
      // instruction & state machine will change, hence save snapshot
      // of relevant info
      if (origin < INSTR_ORIGIN_UNKNOWN) {
        final String message =
          String.format("origin < %d: %d", INSTR_ORIGIN_UNKNOWN, origin);
        throw new IllegalArgumentException(message);
      }
      if (origin >= MEMORY_SIZE) {
        final String message =
          String.format("origin >= %d: %d", MEMORY_SIZE, origin);
        throw new IllegalArgumentException(message);
      }
      this.origin = origin;
      this.mnemonic = mnemonic;
      this.fullStatement = fullStatement;
      this.isDelayCycle = isDelayCycle;
      this.delay = delay;
    }

    public InstructionInfo(final Exception e)
    {
      this.origin = INSTR_ORIGIN_UNKNOWN;
      this.mnemonic = "err";
      this.fullStatement = e.getMessage();
      this.isDelayCycle = false;
      this.delay = 0;
    }

    public int getOrigin() { return origin; }

    public String getMnemnonic() { return mnemonic; }

    public String getFullStatement() { return fullStatement; }

    public boolean isDelayCycle() { return isDelayCycle; }

    public int getDelay() { return delay; }

    @Override
    public boolean equals(final Object obj)
    {
      if (!(obj instanceof InstructionInfo)) return false;
      final InstructionInfo other = (InstructionInfo)obj;
      if (isDelayCycle && other.isDelayCycle) return true;
      return this == other;
    }

    @Override
    public int hashCode()
    {
      return isDelayCycle ? 0 : super.hashCode();
    }

    public String getToolTipText()
    {
      return isDelayCycle ? "[delay]" : fullStatement;
    }

    @Override
    public String toString()
    {
      return isDelayCycle ? "[" + delay + "]" : mnemonic;
    }
  }

  public InstructionInfo
    getInstructionFromOpCode(final int smNum, final int origin,
                             final String addressLabel, final int opCode,
                             final boolean format,
                             final boolean isDelayCycle, final int delay)
      throws IOException
  {
    Constants.checkSmNum(smNum);
    final int smPinCtrlSidesetCountAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    final int pinCtrlSidesetCount =
      (memory.readAddress(smPinCtrlSidesetCountAddress) &
       SM0_PINCTRL_SIDESET_COUNT_BITS) >>> SM0_PINCTRL_SIDESET_COUNT_LSB;

    final int smExecCtrlSideEnAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final boolean execCtrlSideEn =
      (memory.readAddress(smExecCtrlSideEnAddress) &
       SM0_EXECCTRL_SIDE_EN_BITS) != 0x0;
    return getInstructionFromOpCode(pinCtrlSidesetCount, execCtrlSideEn,
                                    origin, addressLabel, opCode,
                                    format, isDelayCycle, delay);
  }

  /**
   * Note: This method is synchronized since we have only a single
   * instance of a decoder, with a single instance of each
   * instruction.  Therefore, use of this decoder and access to
   * instances of Instruction objects must be serialized.
   */
  public static synchronized InstructionInfo
    getInstructionFromOpCode(final int pinCtrlSidesetCount,
                             final boolean execCtrlSideEn,
                             final int origin,
                             final String addressLabel, final int opCode,
                             final boolean format,
                             final boolean isDelayCycle, final int delay)
  {
    /*final*/ Instruction instruction;
    try {
      instruction =
        decoder.decode((short)opCode, pinCtrlSidesetCount, execCtrlSideEn);
    } catch (final Decoder.DecodeException e) {
      instruction = null;
    }
    final String mnemonic;
    final String fullStatement;
    if (instruction != null) {
      mnemonic = instruction.getMnemonic();
      fullStatement = addressLabel + instruction.toString();
    } else {
      mnemonic = "???";
      fullStatement = addressLabel + "???";
    }
    final String formattedFullStatement =
      format ? fullStatement : fullStatement.replaceAll("\\s{2,}", " ");

    return new InstructionInfo(origin, mnemonic, formattedFullStatement,
                               isDelayCycle, delay);
  }

  public static int decodeInstrOrigin(final int encoded)
  {
    final int originMode = (encoded >>> 5) & 0x3;
    return
      originMode == INSTR_ORIGIN_MEMORY ?
      encoded & (MEMORY_SIZE - 1) :
      ~((~originMode) & 0x3);
  }

  private int getInstrOrigin(final int smNum) throws IOException
  {
    final int instrOriginAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_INSTR_ORIGIN);
    final int instrOrigin = memory.readAddress(instrOriginAddress);
    return decodeInstrOrigin(instrOrigin);
  }

  public static String renderOrigin(final int origin)
  {
    if (origin >= 0) {
      return String.format("%02x", origin);
    }
    switch (origin) {
    case INSTR_ORIGIN_UNKNOWN:
      return "??";
    case INSTR_ORIGIN_FORCED:
      return "[forced]";
    case INSTR_ORIGIN_EXECD:
      return "[EXEC'd]";
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }

  public InstructionInfo getCurrentInstruction(final int smNum,
                                               final boolean showOrigin,
                                               final boolean format)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    final int smInstrAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_INSTR);
    final int opCode = memory.readAddress(smInstrAddress) & 0xffff;
    final int origin = getInstrOrigin(smNum);
    final String addressLabel =
      showOrigin ? renderOrigin(origin) + ": " : "";

    final int smDelayCycleAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_DELAY_CYCLE);
    final boolean isDelayCycle = memory.readAddress(smDelayCycleAddress) != 0x0;

    final int smDelayAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_DELAY);
    final int delay = memory.readAddress(smDelayAddress);

    return getInstructionFromOpCode(smNum, origin, addressLabel, opCode, format,
                                    isDelayCycle, delay);
  }

  /**
   * @param smNum Index of state machine.  For decoding an
   * instruction, it is essential to know for which state machine the
   * instruction applies, since the state machine's configuration of
   * the side set / delay settings has an impact on interpretation of
   * the instruction even for disassembling.
   */
  public InstructionInfo getMemoryInstruction(final int smNum,
                                              final int address,
                                              final boolean showAddress,
                                              final boolean format)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    Constants.checkSmMemAddr(address, "memory address");
    final int instrAddress = PIOEmuRegisters.getMemoryAddress(pioNum, address);
    final int opCode = memory.readAddress(instrAddress) & 0xffff;
    final String formattedOpCode = String.format("%04x ", opCode);
    final String addressLabel =
      (showAddress ? String.format("%02x: ", address) : "") + formattedOpCode;
    final boolean isDelayCycle = false;
    final int delay = 0;
    return
      getInstructionFromOpCode(smNum, address, addressLabel, opCode, format,
                               isDelayCycle, delay);
  }

  // ---- Functions for compatibility with the Pico SDK, SM Config Group ----

  public static SMConfig getDefaultSmConfig()
  {
    return SMConfig.getDefault();
  }

  public void smSetOutPins(final int smNum,
                           final int outBase, final int outCount)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    Constants.checkGpioPin(outBase, "GPIO out base");
    Constants.checkGpioPinsCount(outCount, "GPIO out count");
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    memory.hwWriteMasked(address,
                         (outCount << SM0_PINCTRL_OUT_COUNT_LSB) |
                         (outBase << SM0_PINCTRL_OUT_BASE_LSB),
                         SM0_PINCTRL_OUT_COUNT_BITS &
                         SM0_PINCTRL_OUT_BASE_BITS);
  }

  public void smSetSetPins(final int smNum,
                           final int setBase, final int setCount)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    Constants.checkGpioPin(setBase, "GPIO set base");
    if (setCount < 0) {
      throw new IllegalArgumentException("setCount < 0: " + setCount);
    }
    if (setCount > 5) {
      throw new IllegalArgumentException("setCount > 5: " + setCount);
    }
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    memory.hwWriteMasked(address,
                         (setCount << SM0_PINCTRL_SET_COUNT_LSB) |
                         (setBase << SM0_PINCTRL_SET_BASE_LSB),
                         SM0_PINCTRL_SET_COUNT_BITS &
                         SM0_PINCTRL_SET_BASE_BITS);
  }

  public void smSetInPins(final int smNum, final int inBase) throws IOException
  {
    Constants.checkSmNum(smNum);
    Constants.checkGpioPin(inBase, "GPIO in base");
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    memory.hwWriteMasked(address, inBase << SM0_PINCTRL_IN_BASE_LSB,
                         SM0_PINCTRL_IN_BASE_BITS);
  }

  public void smSetSideSetPins(final int smNum, final int sideSetBase)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    Constants.checkGpioPin(sideSetBase, "GPIO side set base");
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    memory.hwWriteMasked(address, sideSetBase << SM0_PINCTRL_SIDESET_BASE_LSB,
                         SM0_PINCTRL_SIDESET_BASE_BITS);
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

  public void reset()
  {
    memoryAllocation = 0;
    stateMachineClaimed = 0;
  }

  public void smSetConfig(final int smNum, final SMConfig smConfig)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    if (smConfig == null) {
      throw new NullPointerException("smConfig");
    }
    synchronized(memory) {
      final int smClkDivAddr =
        PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_CLKDIV);
      memory.writeAddress(smClkDivAddr, smConfig.getClkDiv());
      final int smExecCtrlAddr =
        PIORegisters.getSMAddress(pioNum, smNum,
                                  PIORegisters.Regs.SM0_EXECCTRL);
      memory.writeAddress(smExecCtrlAddr, smConfig.getExecCtrl());
      final int smShiftCtrlAddr =
        PIORegisters.getSMAddress(pioNum, smNum,
                                  PIORegisters.Regs.SM0_SHIFTCTRL);
      memory.writeAddress(smShiftCtrlAddr, smConfig.getShiftCtrl());
      final int smPinCtrlAddr =
        PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
      memory.writeAddress(smPinCtrlAddr, smConfig.getPinCtrl());
    }
  }

  public int getIndex()
  {
    return pioNum;
  }

  public void gpioInit(final int pin) throws IOException
  {
    Constants.checkGpioPin(pin, "GPIO pin number");
    final GPIO_Function function =
      getIndex() == 0 ? GPIO_Function.PIO0 : GPIO_Function.PIO1;
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
   * origin.  Returns address (0…31) where the allocation is
   * performed.
   * @param allocationMask Bit mask of instruction addresses (0…31) to
   * allocate.
   * @param origin Address where to allocate, or -1, if any address is
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
        if ((memoryAllocation & allocationMask) == 0x0) {
          if (!checkOnly) memoryAllocation |= allocationMask;
          return origin;
        }
        if (checkOnly) return -1;
        final String message =
          String.format("allocation at %02x failed", origin);
        throw new Panic(message);
      }
      for (int offset = 0; offset < MEMORY_SIZE; offset++) {
        final int allocationMaskForOffset =
          (allocationMask << offset) |
          (allocationMask << (offset - MEMORY_SIZE));
        if ((memoryAllocation & allocationMaskForOffset) == 0x0) {
          if (!checkOnly) memoryAllocation |= allocationMaskForOffset;
          return offset;
        }
      }
    }
    if (checkOnly) return -1;
    final String message =
      String.format("allocation at %02x failed", origin);
    throw new Panic(message);
  }

  public int getMemoryAllocation() { return memoryAllocation; }

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
    Constants.checkSmMemAddr(offset, "offset");
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
    throws IOException
  {
    if (program == null) {
      throw new NullPointerException("program");
    }
    Constants.checkSmMemAddr(addressOffset, "address offset");
    final int length = program.getLength();
    synchronized(memory) {
      for (int index = 0; index < length; index++) {
        final short instruction = program.getInstruction(index);
        final int memoryAddress = (addressOffset + index) & 0x1f;
        // TODO: FIXME: Code relocation: When (addressOffset != 0),
        // JMP commands need their absolute target address to be
        // adjusted according to the offset.
        memory.writeAddress(PIORegisters.
                            getMemoryAddress(pioNum, memoryAddress),
                            instruction);
      }
    }
  }

  public int addProgram(final String resourceId, final BufferedReader reader)
    throws IOException
  {
    return addProgram(ProgramParser.parse(resourceId, reader));
  }

  public int addProgram(final Program program) throws IOException
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

  public int addProgramAtOffset(final String resourceId,
                                final BufferedReader reader, final int offset)
    throws IOException
  {
    return addProgramAtOffset(ProgramParser.parse(resourceId, reader), offset);
  }

  public int addProgramAtOffset(final Program program, final int offset)
    throws IOException
  {
    if (program == null) {
      throw new NullPointerException("program");
    }
    Constants.checkSmMemAddr(offset, "offset");
    final int origin = program.getOrigin();
    if (origin >= 0) {
      // do not allocate program with fixed origin at different offset
      if (origin != offset) {
        final String message =
          String.format("allocation at %02x failed for program %s: " +
                        "conflicting origin: %02x",
                        offset, program, origin);
        throw new Panic(message);
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

  public void removeProgram(final String resourceId,
                            final BufferedReader reader, final int loadedOffset)
    throws IOException
  {
    removeProgram(ProgramParser.parse(resourceId, reader), loadedOffset);
  }

  public void removeProgram(final Program program, final int loadedOffset)
    throws IOException
  {
    if (program == null) {
      throw new NullPointerException("program");
    }
    Constants.checkSmMemAddr(loadedOffset, "loaded offset");
    final int origin = program.getOrigin();
    if (origin >= 0) {
      // can not remove program from offset it is not designed for
      if (origin != loadedOffset) {
        final String message =
          String.format("can not remove program %s from offset %02x: " +
                        "program has conflicting origin: %02x",
                        program, loadedOffset, origin);
        throw new Panic(message);
      }
    }
    final int allocationMask = program.getAllocationMask();
    final int allocationMaskForOffset =
      origin >= 0 ?
      allocationMask :
      (allocationMask << loadedOffset) |
      (allocationMask << (loadedOffset - MEMORY_SIZE));
    synchronized(memoryAllocation) {
      if ((memoryAllocation & allocationMaskForOffset) !=
          allocationMaskForOffset) {
        final String message =
          String.format("deallocation at %02x failed for program %s: " +
                        "allocation bits corrupted",
                        loadedOffset, program);
        throw new Panic(message);
      }
      memoryAllocation &= ~allocationMaskForOffset;
      synchronized(memory) {
        for (int index = 0; index < program.getLength(); index++) {
          final int memoryAddress = (loadedOffset + index) & 0x1f;
          memory.writeAddress(PIORegisters.getMemoryAddress(pioNum,
                                                            memoryAddress),
                              0);
        }
      }
    }
  }

  public void clearInstructionMemory() throws IOException
  {
    synchronized(memoryAllocation) {
      memoryAllocation = 0;
      synchronized(memory) {
        for (int memoryAddress = 0; memoryAddress < MEMORY_SIZE;
             memoryAddress++) {
          memory.writeAddress(PIORegisters.getMemoryAddress(pioNum,
                                                            memoryAddress),
                              0);
        }
      }
    }
  }

  public void smInit(final int smNum, final int initialPC,
                     final SMConfig config)
    throws IOException
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
    memory.writeAddress(PIORegisters.getAddress(pioNum,
                                                PIORegisters.Regs.FDEBUG),
                        fDebug);
    smRestart(smNum);
    smClkDivRestart(smNum);
    final int jmpInstruction =
      initialPC & 0x001f; // no sideset/delay => all other bits are 0
    smExec(smNum, (short)jmpInstruction);
  }

  public boolean smGetEnabled(final int smNum)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    final int address = PIORegisters.getAddress(pioNum, PIORegisters.Regs.CTRL);
    final int ctrl = memory.readAddress(address);
    return (ctrl & (0x1 << smNum)) != 0x0;
  }

  public void smSetEnabled(final int smNum, final boolean enabled)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    setSmMaskEnabled(0x1 << smNum, enabled);
  }

  public void setSmMaskEnabled(final int mask, final boolean enabled)
    throws IOException
  {
    final int address = PIORegisters.getAddress(pioNum, PIORegisters.Regs.CTRL);
    memory.hwWriteMasked(address, enabled ? mask : 0, mask);
  }

  public void smRestart(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    restartSmMask(0x1 << smNum);
  }

  public void restartSmMask(final int mask) throws IOException
  {
    if (mask < 0) {
      throw new IllegalArgumentException("mask < 0: " + mask);
    }
    if (mask > (0x1 << SM_COUNT) - 1) {
      throw new IllegalArgumentException("mask > " +
                                         ((0x1 << SM_COUNT) - 1) + ": " +
                                         mask);
    }
    final int address = PIORegisters.getAddress(pioNum, PIORegisters.Regs.CTRL);
    memory.hwSetBits(address,
                     (mask << CTRL_SM_RESTART_LSB) & CTRL_SM_RESTART_BITS);
  }

  public void smClkDivRestart(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    clkDivRestartSmMask(0x1 << smNum);
  }

  public void clkDivRestartSmMask(final int mask) throws IOException
  {
    if (mask < 0) {
      throw new IllegalArgumentException("mask < 0: " + mask);
    }
    if (mask > (0x1 << SM_COUNT) - 1) {
      throw new IllegalArgumentException("mask > " +
                                         ((0x1 << SM_COUNT) - 1) + ": " +
                                         mask);
    }
    final int address = PIORegisters.getAddress(pioNum, PIORegisters.Regs.CTRL);
    memory.hwWriteMasked(address, mask << CTRL_CLKDIV_RESTART_LSB,
                         CTRL_CLKDIV_RESTART_BITS);
  }

  public void enableSmMaskInSync(final int mask) throws IOException
  {
    if (mask < 0) {
      throw new IllegalArgumentException("mask < 0: " + mask);
    }
    if (mask > (0x1 << SM_COUNT) - 1) {
      throw new IllegalArgumentException("mask > " +
                                         ((0x1 << SM_COUNT) - 1) + ": " +
                                         mask);
    }
    final int address = PIORegisters.getAddress(pioNum, PIORegisters.Regs.CTRL);
    memory.hwWriteMasked(address,
                         (mask << CTRL_CLKDIV_RESTART_LSB) |
                         (mask << CTRL_SM_ENABLE_LSB),
                         CTRL_CLKDIV_RESTART_BITS | CTRL_SM_ENABLE_BITS);
  }

  public int smGetPC(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_ADDR);
    return memory.readAddress(address);
  }

  public void smExec(final int smNum, final short instr) throws IOException
  {
    Constants.checkSmNum(smNum);
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_ADDR);
    memory.writeAddress(address, instr & 0xffff);
  }

  public boolean smIsExecStalled(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final int execCtrl = memory.readAddress(address);
    return (execCtrl & SM0_EXECCTRL_EXEC_STALLED_BITS) != 0x0;
  }

  public void smExecWaitBlocking(final int smNum, final short instr)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    smExec(smNum, instr);
    while (smIsExecStalled(smNum)) Thread.yield();
  }

  public void smSetWrap(final int smNum, final int wrapTarget,
                        final int wrap)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    Constants.checkSmMemAddr(wrapTarget, "wrap target");
    Constants.checkSmMemAddr(wrap, "wrap");
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    memory.hwWriteMasked(address,
                         (wrap << SM0_EXECCTRL_WRAP_TOP_LSB) |
                         (wrapTarget << SM0_EXECCTRL_WRAP_BOTTOM_LSB),
                         SM0_EXECCTRL_WRAP_TOP_BITS |
                         SM0_EXECCTRL_WRAP_BOTTOM_BITS);
  }

  public void smPut(final int smNum, final int data) throws IOException
  {
    Constants.checkSmNum(smNum);
    memory.writeAddress(PIORegisters.getTXFAddress(pioNum, smNum), data);
  }

  public int smGet(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    return memory.readAddress(PIORegisters.getRXFAddress(pioNum, smNum));
  }

  public boolean smIsRXFIFOFull(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    final int fStat =
      memory.readAddress(PIORegisters.getAddress(pioNum,
                                                 PIORegisters.Regs.FSTAT));
    return (fStat & (0x1 << (FSTAT_RXFULL_LSB + smNum))) != 0x0;
  }

  public boolean smIsRXFIFOEmpty(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    final int fStat =
      memory.readAddress(PIORegisters.getAddress(pioNum,
                                                 PIORegisters.Regs.FSTAT));
    return (fStat & (0x1 << (FSTAT_RXEMPTY_LSB + smNum))) != 0x0;
  }

  public int smGetRXFIFOLevel(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    final int shiftCount =
      FLEVEL_RX0_LSB + smNum * (FLEVEL_RX1_LSB - FLEVEL_RX0_LSB);
    final int mask = FLEVEL_RX0_BITS >> FLEVEL_RX0_LSB;
    return
      (memory.readAddress(PIORegisters.getAddress(pioNum,
                                                  PIORegisters.Regs.FLEVEL)) >>
       shiftCount) & mask;
  }

  public boolean smIsTXFIFOFull(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    final int fStat =
      memory.readAddress(PIORegisters.getAddress(pioNum,
                                                 PIORegisters.Regs.FSTAT));
    return (fStat & (0x1 << (FSTAT_TXFULL_LSB + smNum))) != 0x0;
  }

  public boolean smIsTXFIFOEmpty(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    final int fStat =
      memory.readAddress(PIORegisters.getAddress(pioNum,
                                                 PIORegisters.Regs.FSTAT));
    return (fStat & (0x1 << (FSTAT_TXEMPTY_LSB + smNum))) != 0x0;
  }

  public int smGetTXFIFOLevel(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    final int shiftCount =
      FLEVEL_TX0_LSB + smNum * (FLEVEL_TX1_LSB - FLEVEL_TX0_LSB);
    final int mask = FLEVEL_TX0_BITS >> FLEVEL_TX0_LSB;
    return
      (memory.readAddress(PIORegisters.getAddress(pioNum,
                                                  PIORegisters.Regs.FLEVEL)) >>
       shiftCount) & mask;
  }

  public void smPutBlocking(final int smNum, final int data) throws IOException
  {
    Constants.checkSmNum(smNum);
    while (smIsTXFIFOFull(smNum)) {
      Thread.yield();
    }
    smPut(smNum, data);
  }

  public int smGetBlocking(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    while (smIsRXFIFOEmpty(smNum)) {
      Thread.yield();
    }
    return smGet(smNum);
  }

  public void smDrainTXFIFO(final int smNum) throws IOException
  {
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_SHIFTCTRL);
    final boolean autoPull =
      (memory.readAddress(address) & SM0_SHIFTCTRL_AUTOPULL_BITS) != 0x0;
    final int instruction =
      autoPull ? 0x6060 : 0x8080;
    while (smIsTXFIFOEmpty(smNum)) {
      smExec(smNum, (short)instruction);
      // TODO: Wait for completion of inserted instruction?
    }
  }

  public void smSetClkDiv(final int smNum, final float div) throws IOException
  {
    if (div < 1.0f) {
      throw new IllegalArgumentException("div < 1: " + div);
    }
    if (div > 65536.0f) {
      throw new IllegalArgumentException("div > 65536: " + div);
    }
    final int divInt = (int)div;
    final int divFrac;
    if (divInt == 0) {
      divFrac = 0;
    } else {
      divFrac = (int)((div - divInt) * 256.0);
    }
    smSetClkDivIntFrac(smNum, divInt & 0xffff, divFrac);
  }

  public void smSetClkDivIntFrac(final int smNum,
                                 final int divInt, final int divFrac)
    throws IOException
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
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_CLKDIV);
    final int clkDiv =
      divInt << SM0_CLKDIV_INT_LSB | divFrac << SM0_CLKDIV_FRAC_LSB;
    memory.writeAddress(address, clkDiv);
  }

  public void smClearFIFOs(final int smNum) throws IOException
  {
    Constants.checkSmNum(smNum);
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_SHIFTCTRL);
    synchronized(memory) {
      // toggle RX join bit to force clearance of both, RX and TX
      memory.hwXorBits(address, SM0_SHIFTCTRL_FJOIN_RX_BITS);
      // toggle once again to restore previous value
      memory.hwXorBits(address, SM0_SHIFTCTRL_FJOIN_RX_BITS);
    }
  }

  public void smSetPins(final int smNum, int pins) throws IOException
  {
    Constants.checkSmNum(smNum);
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    synchronized(memory) {
      final int pinCtrlSaved = memory.readAddress(address);
      int remaining = 32;
      int base = 0;
      while (remaining > 0) {
        final int decrement = remaining > 5 ? 5 : remaining;
        memory.writeAddress(address,
                            (decrement << SM0_PINCTRL_SET_COUNT_LSB) |
                            (base << SM0_PINCTRL_SET_BASE_LSB));
        final int setInstruction =
          0xe000 | (pins & 0x1f); // no sideset/delay => all other bits are 0
        smExec(smNum, (short)setInstruction);
        remaining -= decrement;
        base += decrement;
        pins >>>= 5;
      }
      memory.writeAddress(address, pinCtrlSaved);
    }
  }

  public void smSetPinsWithMask(final int smNum, final int pinValues,
                                int pinMask)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    synchronized(memory) {
      final int pinCtrlSaved = memory.readAddress(address);
      while (pinMask > 0) {
        final int base = Constants.ctz(pinMask);
        memory.writeAddress(address,
                            (0x1 << SM0_PINCTRL_SET_COUNT_LSB) |
                            (base << SM0_PINCTRL_SET_BASE_LSB));
        final int setInstruction =
          0xe000 | ((pinValues >> base) & 0x1); // no sideset/delay =>
        // all other bits are 0
        smExec(smNum, (short)setInstruction);
        pinMask &= pinMask - 1;
      }
      memory.writeAddress(address, pinCtrlSaved);
    }
  }

  public void smSetPinDirsWithMask(final int smNum, final int pinDirs,
                                   int pinMask)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    synchronized(memory) {
      final int pinCtrlSaved = memory.readAddress(address);
      while (pinMask > 0) {
        final int base = Constants.ctz(pinMask);
        memory.writeAddress(address,
                            (0x1 << SM0_PINCTRL_SET_COUNT_LSB) |
                            (base << SM0_PINCTRL_SET_BASE_LSB));
        final int setInstruction =
          0xe080 | ((pinDirs >> base) & 0x1); // no sideset/delay =>
                                              // all other bits are 0
        smExec(smNum, (short)setInstruction);
        pinMask &= pinMask - 1;
      }
      memory.writeAddress(address, pinCtrlSaved);
    }
  }

  public void smSetConsecutivePinDirs(final int smNum,
                                      int pinBase, int pinCount,
                                      final boolean isOut)
    throws IOException
  {
    Constants.checkSmNum(smNum);
    Constants.checkGpioPin(pinBase, "GPIO pin base");
    Constants.checkGpioPin(pinCount, "GPIO pin count");
    final int address =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    synchronized(memory) {
      final int pinCtrlSaved = memory.readAddress(address);
      final int pinDirValue = isOut ? 0x1f : 0x0;
      while (pinCount > 5) {
        memory.writeAddress(address,
                            (0x5 << SM0_PINCTRL_SET_COUNT_LSB) |
                            (pinBase << SM0_PINCTRL_SET_BASE_LSB));
        final int setInstruction =
          0xe080 | pinDirValue; // no sideset/delay => all other bits
                                // are 0
        smExec(smNum, (short)setInstruction);
        pinCount -= 5;
        pinBase = (pinBase + 5) & 0x1f;
      }
      memory.writeAddress(address,
                          (pinCount << SM0_PINCTRL_SET_COUNT_LSB) |
                             (pinBase << SM0_PINCTRL_SET_BASE_LSB));
      final int setInstruction =
        0xe080 | pinDirValue; // no sideset/delay => all other bits
                              // are 0
      smExec(smNum, (short)setInstruction);
      memory.writeAddress(address, pinCtrlSaved);
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
        throw new Panic(message);
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
          throw new Panic(message);
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

  public PinState[] getPinStates() throws IOException
  {
    final PinState[] pinStates = new PinState[Constants.GPIO_NUM];
    final int pinsAddress =
      PIOEmuRegisters.getAddress(pioNum, PIOEmuRegisters.Regs.GPIO_PINS);
    final int pinDirsAddress =
      PIOEmuRegisters.getAddress(pioNum, PIOEmuRegisters.Regs.GPIO_PINDIRS);
    final int pins = memory.readAddress(pinsAddress);
    final int pinDirs = memory.readAddress(pinDirsAddress);
    for (int gpioNum = 0; gpioNum < Constants.GPIO_NUM; gpioNum++) {
      final Direction direction =
        Direction.fromValue((pinDirs >>> gpioNum) & 0x1);
      final Bit level;
      if (direction == Direction.OUT) {
        level = Bit.fromValue((pins >>> gpioNum) & 0x1);
      } else {
        level = gpioSdk.getInputLevel(gpioNum, GPIOSDK.Override.AFTER);
      }
      pinStates[gpioNum] = PinState.fromValues(direction, level);
    }
    return pinStates;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
