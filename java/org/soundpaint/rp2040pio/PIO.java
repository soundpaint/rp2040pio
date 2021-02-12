/*
 * @(#)PIO.java 1.00 21/01/31
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
import java.util.List;

/**
 * Peripheral I/O Unit
 */
public class PIO implements Clock.TransitionListener
{
  private static final int SM_COUNT = 4;

  private final int index;
  private final List<Decoder.DecodeException> caughtExceptions;
  private final GPIO gpio;
  private final Memory memory;
  private final IRQ irq;
  private final SM[] sms;
  private int smEnabled; // bits 0..3 of CTRL_SM_ENABLE

  public enum PinDir {
    GPIO_LEVELS(0, "levels"),
    GPIO_DIRECTIONS(1, "directions");

    private final int value;
    private final String label;

    private PinDir(final int value, final String label)
    {
      this.value = value;
      this.label = label;
    }

    public int getValue() { return value; }

    public static PinDir fromValue(final int value)
    {
      if (value == 0)
        return GPIO_LEVELS;
      if (value == 1)
        return GPIO_DIRECTIONS;
      throw new IllegalArgumentException("value neither 0 nor 1");
    }

    public static PinDir fromValue(final int value, final PinDir defaultValue)
    {
      if (value == 0)
        return GPIO_LEVELS;
      if (value == 1)
        return GPIO_DIRECTIONS;
      return defaultValue;
    }
  };

  public enum ShiftDir {
    SHIFT_LEFT(0, "shift left"),
    SHIFT_RIGHT(1, "shift right");

    private final int value;
    private final String label;

    private ShiftDir(final int value, final String label)
    {
      this.value = value;
      this.label = label;
    }

    public int getValue() { return value; }

    public static ShiftDir fromValue(final int value)
    {
      if (value == 0)
        return SHIFT_LEFT;
      if (value == 1)
        return SHIFT_RIGHT;
      throw new IllegalArgumentException("value neither 0 nor 1");
    }

    public static ShiftDir fromValue(final int value,
                                     final ShiftDir defaultValue)
    {
      if (value == 0)
        return SHIFT_LEFT;
      if (value == 1)
        return SHIFT_RIGHT;
      return defaultValue;
    }
  };

  private PIO()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public PIO(final int index, final Clock clock)
  {
    if (clock == null) {
      throw new NullPointerException("clock");
    }
    if (index < 0) {
      throw new IllegalArgumentException("PIO index < 0: " + index);
    }
    if (index > 1) {
      throw new IllegalArgumentException("PIO index > 1: " + index);
    }
    this.index = index;
    clock.addTransitionListener(this);
    caughtExceptions = new ArrayList<Decoder.DecodeException>();
    gpio = new GPIO();
    memory = new Memory();
    irq = new IRQ();
    sms = new SM[SM_COUNT];
    for (int smNum = 0; smNum < SM_COUNT; smNum++) {
      sms[smNum] = new SM(smNum, gpio, memory, irq);
    }
    smEnabled = 0x0;
  }

  public int getDBG_CFGINFO_IMEM_SIZE()
  {
    return Memory.SIZE;
  }

  public int getDBG_CFGINFO_SM_COUNT()
  {
    return SM_COUNT;
  }

  public int getDBG_CFGINFO_FIFO_DEPTH()
  {
    return FIFO.DEPTH;
  }

  public Memory getMemory()
  {
    return memory;
  }

  public SM getSM(final int index)
  {
    if (index < 0) {
      throw new IllegalArgumentException("state machine index < 0");
    }
    if (index > SM_COUNT) {
      throw new IllegalArgumentException("state machine index > " + SM_COUNT);
    }
    return sms[index];
  }

  public void setSideSetCount(final int count)
  {
    if (count < 0) {
      throw new IllegalArgumentException("side set count < 0");
    }
    if (count > 5) {
      throw new IllegalArgumentException("side set count > 5");
    }
    for (final SM sm : sms) {
      sm.setSideSetCount(count);
    }
  }

  public void setSideSetBase(final int base)
  {
    if (base < 0) {
      throw new IllegalArgumentException("side set base < 0");
    }
    if (base > 31) {
      throw new IllegalArgumentException("side set base > 31");
    }
    for (final SM sm : sms) {
      sm.setSideSetBase(base);
    }
  }

  public void setSideSetEnable(final boolean enable)
  {
    for (final SM sm : sms) {
      sm.setSideSetEnable(enable);
    }
  }

  public void setSideSetPinDir(final PinDir pinDir)
  {
    if (pinDir == null) { throw new NullPointerException("pinDir"); }
    for (final SM sm : sms) {
      sm.setSideSetPinDir(pinDir);
    }
  }

  public void setJmpPin(final int pin)
  {
    if (pin < 0) {
      throw new IllegalArgumentException("exec ctrl jmp pin < 0");
    }
    if (pin > 31) {
      throw new IllegalArgumentException("exec ctrl jmp pin > 31");
    }
    for (final SM sm : sms) {
      sm.setJmpPin(pin);
    }
  }

  public void setInShiftDir(final ShiftDir shiftDir)
  {
    if (shiftDir == null) { throw new NullPointerException("shiftDir"); }
    for (final SM sm : sms) {
      sm.setInShiftDir(shiftDir);
    }
  }

  public void setOutShiftDir(final ShiftDir shiftDir)
  {
    if (shiftDir == null) { throw new NullPointerException("shiftDir"); }
    for (final SM sm : sms) {
      sm.setOutShiftDir(shiftDir);
    }
  }

  public void setPushThresh(final int thresh)
  {
    if (thresh < 0) {
      throw new IllegalArgumentException("shift ctrl push threshold < 0");
    }
    if (thresh > 31) {
      throw new IllegalArgumentException("shift ctrl push threshold > 31");
    }
    for (final SM sm : sms) {
      sm.setPushThresh(thresh);
    }
  }

  public void setPullThresh(final int thresh)
  {
    if (thresh < 0) {
      throw new IllegalArgumentException("shift ctrl pull threshold < 0");
    }
    if (thresh > 31) {
      throw new IllegalArgumentException("shift ctrl pull threshold > 31");
    }
    for (final SM sm : sms) {
      sm.setPullThresh(thresh);
    }
  }

  /**
   * Returns a copy of the list of all exceptions that have been
   * collected during the most recent clock cycle.
   */
  public List<Decoder.DecodeException> getExceptions()
  {
    return List.copyOf(caughtExceptions);
  }

  @Override
  public void raisingEdge(final long wallClock)
  {
    caughtExceptions.clear();
    synchronized(sms) {
      for (int smNum = 0; smNum < SM_COUNT; smNum++) {
        if (smIsEnabled(smNum)) {
          try {
            final SM sm = getSM(smNum);
            sm.clockRaisingEdge(wallClock);
          } catch (final Decoder.DecodeException e) {
            caughtExceptions.add(e);
          }
        }
      }
    }
  }

  @Override
  public void fallingEdge(final long wallClock) {
    synchronized(sms) {
      for (int smNum = 0; smNum < SM_COUNT; smNum++) {
        if (smIsEnabled(smNum)) {
          try {
            final SM sm = getSM(smNum);
            sm.clockFallingEdge(wallClock);
          } catch (final Decoder.DecodeException e) {
            caughtExceptions.add(e);
          }
        }
      }
    }
  }

  // -------- Functions for compatibility with the Pico SDK --------

  public static final MasterClock MASTER_CLOCK =
    MasterClock.getDefaultInstance();
  public static final PIO PIO0 = new PIO(0, MASTER_CLOCK);
  public static final PIO PIO1 = new PIO(1, MASTER_CLOCK);

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

  /**
   * Used for resetting the clocks of multiple state machines as an
   * atomic operation.
   */
  private Object CLKDIV_LOCK = new Object();

  public void smSetConfig(final int smNum, final SMConfig smConfig)
  {
    final SM sm = getSM(smNum);
    sm.setCLKDIV(smConfig.getClkDiv());
    sm.setEXECCTRL(smConfig.getExecCtrl());
    sm.setSHIFTCTRL(smConfig.getShiftCtrl());
    sm.setPINCTRL(smConfig.getPinCtrl());
  }

  public void smSetOutPins(final int smNum,
                           final int outBase, final int outCount)
  {
    final SM sm = getSM(smNum);
    sm.setOutBase(outBase);
    sm.setOutCount(outCount);
  }

  public void smSetSetPins(final int smNum,
                           final int setBase, final int setCount)
  {
    final SM sm = getSM(smNum);
    sm.setSetBase(setBase);
    sm.setSetCount(setCount);
  }

  public void smSetInPins(final int smNum, final int inBase)
  {
    final SM sm = getSM(smNum);
    sm.setInBase(inBase);
  }

  public void smSetSideSetPins(final int smNum, final int sideSetBase)
  {
    final SM sm = getSM(smNum);
    sm.setSideSetBase(sideSetBase);
  }

  public int getIndex()
  {
    return index;
  }

  public void gpioInit(final int pin)
  {
    gpio.init(pin);
  }

  /*
   * TODO:
   *
   * Add below those SDK functions that are still missing.
   * Next missing function is "pio_get_dreq()".
   * See:
   * https://raspberrypi.github.io/pico-sdk-doxygen/group__hardware__pio.html
   */

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
      for (int offset = 0; offset < 32; offset++) {
        final int allocationMaskForOffset =
          (allocationMask << offset) | (allocationMask << (offset - 32));
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
    if (offset > 31) {
      throw new IllegalArgumentException("offset > 31: " + offset);
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
      (allocationMask << offset) | (allocationMask << (offset - 32));
    return allocateMemory(allocationMaskForOffset, offset, true) >= 0;
  }

  private void writeProgram(final Program program, final int address)
  {
    if (program == null) {
      throw new NullPointerException("program");
    }
    if (address < 0) {
      throw new IllegalArgumentException("address < 0: " + address);
    }
    if (address > 31) {
      throw new IllegalArgumentException("address > 31: " + address);
    }
    final int length = program.getLength();
    for (int index = 0; index < length; index++) {
      final short instruction = program.getInstruction(index);
      final int memoryAddress = (address + index) & 0x1f;
      memory.set(memoryAddress, instruction);
    }
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

  public int addProgramAtOffset(final Program program, final int offset)
  {
    if (program == null) {
      throw new NullPointerException("program");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset < 0: " + offset);
    }
    if (offset > 31) {
      throw new IllegalArgumentException("offset > 31: " + offset);
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
      (allocationMask << offset) | (allocationMask << (offset - 32));
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
    if (loadedOffset > 31) {
      throw new IllegalArgumentException("loaded offset > 31: " + loadedOffset);
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
      (allocationMask << (loadedOffset - 32));
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
      for (int index = 0; index < program.getLength(); index++) {
        final int address = (loadedOffset + index) & 0x1f;
        memory.set(address, (short)0);
      }
    }
  }

  public void clearInstructionMemory()
  {
    synchronized(memoryAllocation) {
      memoryAllocation = 0;
      for (int index = 0; index < Memory.SIZE; index++) {
        memory.set(index, (short)0);
      }
    }
  }

  public void smInit(final int smNum, final int initialPC,
                     final SMConfig config)
  {
    smSetConfig(smNum, config);
    final SM sm = getSM(smNum);
    sm.setPC(initialPC);
  }

  private boolean smIsEnabled(final int smNum)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("smNum < 0: " + smNum);
    }
    if (smNum > SM_COUNT - 1) {
      throw new IllegalArgumentException("smNum > " + (SM_COUNT - 1) +
                                         ": " + smNum);
    }
    return (smEnabled & (0x1 << smNum)) != 0x0;
  }

  public void smSetEnabled(final int smNum, final boolean enabled)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("smNum < 0: " + smNum);
    }
    if (smNum > SM_COUNT - 1) {
      throw new IllegalArgumentException("smNum > " + (SM_COUNT - 1) +
                                         ": " + smNum);
    }
    smSetEnabledMask(0x1 << smNum, enabled);
  }

  public void smSetEnabledMask(final int mask, final boolean enabled)
  {
    if (mask < 0) {
      throw new IllegalArgumentException("mask < 0: " + mask);
    }
    if (mask > (0x1 << SM_COUNT) - 1) {
      throw new IllegalArgumentException("mask > " + ((0x1 << SM_COUNT) - 1) +
                                         ": " + mask);
    }
    synchronized(sms) {
      if (enabled) {
        final int maskAlreadyEnabled = smEnabled & ~mask;
        if (maskAlreadyEnabled == 0x0) {
          smEnabled |= mask;
        } else {
          final String message =
            String.format("state machine(s) already enabled: %s",
                          listMaskBits(maskAlreadyEnabled));
          throw new RuntimeException(message);
        }
      } else {
        final int maskReadyToDisable = smEnabled & mask;
        if (maskReadyToDisable == mask) {
          smEnabled &= ~maskReadyToDisable;
        } else {
          final String message =
            String.format("state machine(s) already disabled: %s",
                          listMaskBits(~maskReadyToDisable & mask));
          throw new RuntimeException(message);
        }
      }
    }
  }

  public void smClkDivRestart(final int smNum)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("smNum < 0: " + smNum);
    }
    if (smNum > SM_COUNT - 1) {
      throw new IllegalArgumentException("smNum > " + (SM_COUNT - 1) +
                                         ": " + smNum);
    }
    smClkDivRestartMask(0x1 << smNum);
  }

  public void smClkDivRestartMask(final int mask)
  {
    if (mask < 0) {
      throw new IllegalArgumentException("mask < 0: " + mask);
    }
    if (mask > (0x1 << SM_COUNT) - 1) {
      throw new IllegalArgumentException("mask > " + ((0x1 << SM_COUNT) - 1) +
                                         ": " + mask);
    }
    synchronized(CLKDIV_LOCK) {
      for (int smNum = 0; smNum < SM_COUNT; smNum++) {
        if (mask >>> smNum != 0x0) {
          final SM sm = getSM(smNum);
          sm.resetCLKDIV();
        }
      }
    }
  }

  public int smGetPC(final int smNum)
  {
    final SM sm = getSM(smNum);
    return sm.getPC();
  }

  public void smSetWrap(final int smNum, final int wrapTarget,
                        final int wrap)
  {
    final SM sm = getSM(smNum);
    sm.setWrapTop(wrap);
    sm.setWrapBottom(wrapTarget);
  }

  public void smPut(final int smNum, final int data)
  {
    final SM sm = getSM(smNum);
    sm.put(data);
  }

  public int smGet(final int smNum)
  {
    final SM sm = getSM(smNum);
    return sm.get();
  }

  public boolean smIsRXFIFOFull(final int smNum)
  {
    final SM sm = getSM(smNum);
    return sm.isRXFIFOFull();
  }

  public boolean smIsRXFIFOEmpty(final int smNum)
  {
    final SM sm = getSM(smNum);
    return sm.isRXFIFOEmpty();
  }

  public int smGetRXFIFOLevel(final int smNum)
  {
    final SM sm = getSM(smNum);
    return sm.getRXFIFOLevel();
  }

  public boolean smIsTXFIFOFull(final int smNum)
  {
    final SM sm = getSM(smNum);
    return sm.isTXFIFOFull();
  }

  public boolean smIsTXFIFOEmpty(final int smNum)
  {
    final SM sm = getSM(smNum);
    return sm.isTXFIFOEmpty();
  }

  public int smGetTXFIFOLevel(final int smNum)
  {
    final SM sm = getSM(smNum);
    return sm.getTXFIFOLevel();
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
    final SM sm = getSM(smNum);
    sm.setClockDivIntegerBits(divInt);
    sm.setClockDivFractionalBits(divFrac);
  }

  public void smClearFIFOs(final int smNum)
  {
    final SM sm = getSM(smNum);
    sm.clearFIFOs();
  }

  public void smSetPins(final int smNum, final int values)
  {
    final SM sm = getSM(smNum);
    SM.IOMapping.SET.setPins(sm, values);
  }

  public void smClaim(final int smNum)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("smNum < 0: " + smNum);
    }
    if (smNum > SM_COUNT - 1) {
      throw new IllegalArgumentException("smNum > " + (SM_COUNT - 1) +
                                         ": " + smNum);
    }
    smClaimMask(0x1 << smNum);
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

  public void smClaimMask(final int mask)
  {
    if (mask < 0) {
      throw new IllegalArgumentException("mask < 0: " + mask);
    }
    if (mask > (0x1 << SM_COUNT) - 1) {
      throw new IllegalArgumentException("mask > " + ((0x1 << SM_COUNT) - 1) +
                                         ": " + mask);
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
    if (smNum < 0) {
      throw new IllegalArgumentException("smNum < 0: " + smNum);
    }
    if (smNum > SM_COUNT - 1) {
      throw new IllegalArgumentException("smNum > " + (SM_COUNT - 1) +
                                         ": " + smNum);
    }
    final int mask = 0x1 << smNum;
    synchronized(stateMachineClaimed) {
      stateMachineClaimed &= ~mask;
    }
  }

  public int smClaimUnused(final boolean required)
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
