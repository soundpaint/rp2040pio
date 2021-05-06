/*
 * @(#)SM.java 1.00 21/01/31
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

import java.io.PrintStream;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * State Machine
 */
public class SM implements Constants
{
  private static final int[] SHIFT_MASK = new int[32];
  static {
    int maskValue = 0;
    for (int i = 0; i < SHIFT_MASK.length; i++) {
      maskValue = (maskValue << 1) | 0x1;
      SHIFT_MASK[i] = maskValue;
    }
  };

  private final int num;
  private final PrintStream console;
  private final MasterClock masterClock;
  private final PIOGPIO pioGpio;
  private final Memory memory;
  private final IRQ irq;
  private final Status status;
  private final Decoder decoder;
  private final FIFO fifo;
  private final PLL pll;

  public enum IOMapping
  {
    SET((sm) -> sm.status.regPINCTRL_SET_BASE,
        (sm) -> sm.status.regPINCTRL_SET_COUNT),
    OUT((sm) -> sm.status.regPINCTRL_OUT_BASE,
        (sm) -> sm.status.regPINCTRL_OUT_COUNT),
    SIDE_SET((sm) -> sm.status.regPINCTRL_SIDESET_BASE,
             (sm) -> sm.status.regPINCTRL_SIDESET_COUNT);

    private final Function<SM, Integer> baseGetter;
    private final Function<SM, Integer> countGetter;

    private IOMapping(final Function<SM, Integer> baseGetter,
                      final Function<SM, Integer> countGetter)
    {
      this.baseGetter = baseGetter;
      this.countGetter = countGetter;
    }

    public void setPins(final SM sm, final int data)
    {
      sm.pioGpio.setPins(data, baseGetter.apply(sm), countGetter.apply(sm));
    }

    public void setPinDirs(final SM sm, final int data)
    {
      sm.pioGpio.setPinDirs(data, baseGetter.apply(sm), countGetter.apply(sm));
    }
  };

  public class Status
  {
    public Instruction instruction;
    public Instruction.ResultState resultState;
    public boolean processing;
    public boolean smEnabled;
    public boolean clockEnabled;
    public boolean isDelayCycle;
    public int regX;
    public int regY;
    public int isrValue;
    public int isrShiftCount;
    public int osrValue;
    public int osrShiftCount;
    public int pendingDelay;
    public int pendingDMAInstruction;
    public int pendingExecInstruction;
    public int regADDR; // bits 0…4 of SMx_ADDR
    public boolean regEXECCTRL_SIDE_EN; // bit 30 of SMx_EXECCTRL
    public PIO.PinDir regEXECCTRL_SIDE_PINDIR; // bit 29 of SMx_EXECCTRL
    public int regEXECCTRL_JMP_PIN; // bits 24…28 of SMx_EXECCTRL
    public int regEXECCTRL_WRAP_TOP; // bits 12…16 of SMx_EXECCTRL
    public int regEXECCTRL_WRAP_BOTTOM; // bits 7…11 of SMx_EXECCTRL
    public boolean regEXECCTRL_STATUS_SEL; // bit 4 of SMx_EXECCTRL
    public int regEXECCTRL_STATUS_N; // bits 0…3 of SMx_EXECCTRL
    public int regSHIFTCTRL_PULL_THRESH; // bits 25…29 of SMx_SHIFTCTRL
    public int regSHIFTCTRL_PUSH_THRESH; // bits 20…24 of SMx_SHIFTCTRL
    public PIO.ShiftDir regSHIFTCTRL_IN_SHIFTDIR; // bit 18 of SMx_SHIFTCTRL
    public boolean regSHIFTCTRL_AUTOPULL; // bit 17 of SMx_SHIFTCTRL
    public PIO.ShiftDir regSHIFTCTRL_OUT_SHIFTDIR; // bit 19 of SMx_SHIFTCTRL
    public boolean regSHIFTCTRL_AUTOPUSH; // bit 16 of SMx_SHIFTCTRL
    public int regPINCTRL_SIDESET_COUNT; // bits 29…31 of SMx_PINCTRL
    public int regPINCTRL_SET_COUNT; // bits 26…28 of SMx_PINCTRL
    public int regPINCTRL_OUT_COUNT; // bits 20…25 of SMx_PINCTRL
    public int regPINCTRL_IN_BASE; // bits 15…19 of SMx_PINCTRL
    public int regPINCTRL_SIDESET_BASE; // bits 10…14 of SMx_PINCTRL
    public int regPINCTRL_SET_BASE; // bits 5…9 of SMx_PINCTRL
    public int regPINCTRL_OUT_BASE; // bits 0…4 of SMx_PINCTRL

    // PIOEmuRegisters Status
    public int regBREAKPOINTS; // bits 0…31 of SMx_BREAKPOINTS
    public int regTRACEPOINTS; // bits 0…31 of SMx_TRACEPOINTS

    public Status()
    {
      reset();
    }

    private void reset()
    {
      instruction = null;
      resultState = null;
      processing = false;
      smEnabled = false;
      clockEnabled = false;
      isDelayCycle = false;
      regX = 0;
      regY = 0;
      isrValue = 0;
      isrShiftCount = 0;
      osrValue = 0;
      osrShiftCount = 32;
      pendingDelay = 0;
      pendingDMAInstruction = -1;
      pendingExecInstruction = -1;
      regADDR = 0;
      regEXECCTRL_STATUS_SEL = false;
      regEXECCTRL_STATUS_N = 0;
      regEXECCTRL_SIDE_EN = false;
      regEXECCTRL_SIDE_PINDIR = PIO.PinDir.GPIO_LEVELS;
      regEXECCTRL_JMP_PIN = 0;
      regEXECCTRL_WRAP_TOP = 0x1f;
      regEXECCTRL_WRAP_BOTTOM = 0x00;
      regSHIFTCTRL_PULL_THRESH = 0;
      regSHIFTCTRL_PUSH_THRESH = 0;
      regSHIFTCTRL_IN_SHIFTDIR = PIO.ShiftDir.SHIFT_LEFT;
      regSHIFTCTRL_AUTOPULL = false;
      regSHIFTCTRL_OUT_SHIFTDIR = PIO.ShiftDir.SHIFT_LEFT;
      regSHIFTCTRL_AUTOPUSH = false;
      regPINCTRL_SIDESET_COUNT = 0;
      regPINCTRL_SET_COUNT = 0x5;
      regPINCTRL_OUT_COUNT = 0;
      regPINCTRL_IN_BASE = 0;
      regPINCTRL_SIDESET_BASE = 0;
      regPINCTRL_SET_BASE = 0;
      regPINCTRL_OUT_BASE = 0;

      // PIOEmuRegisters Status
      regBREAKPOINTS = 0;
      regTRACEPOINTS = 0;
    }

    public Bit jmpPin()
    {
      return pioGpio.getLevel(regEXECCTRL_JMP_PIN);
    }

    public void setPins(final int pins, final int base, final int count)
    {
      pioGpio.setPins(pins, base, count);
    }

    public void setPinDirs(final int pins, final int base, final int count)
    {
      pioGpio.setPinDirs(pins, base, count);
    }

    public boolean osrEmpty()
    {
      return osrShiftCount == 0;
    }

    public int getFIFOStatus()
    {
      final boolean fulfilled;
      if (regEXECCTRL_STATUS_SEL) {
        fulfilled = fifo.getRXLevel() < regEXECCTRL_STATUS_N;
      } else {
        fulfilled = fifo.getTXLevel() < regEXECCTRL_STATUS_N;
      }
      return fulfilled ? ~0 : 0;
    }

    private boolean consumePendingDelay()
    {
      if (pendingDelay == 0)
        return false;
      pendingDelay--;
      return true;
    }

    private void setPendingDelay(final int delay)
    {
      if (delay < 0) {
        throw new IllegalArgumentException("delay < 0: " + delay);
      }
      if (delay > 31) {
        throw new IllegalArgumentException("delay > 31: " + delay);
      }
      this.pendingDelay = delay;
    }

    @Override
    public String toString()
    {
      return String.format("Status(SM%d)", SM.this.num);
    }
  }

  private SM()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public SM(final int num, final PrintStream console,
            final MasterClock masterClock, final PIOGPIO pioGpio,
            final Memory memory, final IRQ irq)
  {
    if (num < 0) {
      throw new IllegalArgumentException("SM num < 0: " + num);
    }
    if (num > 3) {
      throw new IllegalArgumentException("SM num > 3: " + num);
    }
    if (console == null) {
      throw new NullPointerException("console");
    }
    if (masterClock == null) {
      throw new NullPointerException("masterClock");
    }
    if (pioGpio == null) {
      throw new NullPointerException("pioGpio");
    }
    if (memory == null) {
      throw new NullPointerException("memory");
    }
    if (irq == null) {
      throw new NullPointerException("irq");
    }
    this.num = num;
    this.console = console;
    this.masterClock = masterClock;
    this.pioGpio = pioGpio;
    this.memory = memory;
    this.irq = irq;
    status = new Status();
    decoder = new Decoder();
    fifo = new FIFO();
    pll = new PLL();
  }

  public int getNum() { return num; }

  public PIOGPIO getPIOGPIO() { return pioGpio; }

  public Memory getMemory() { return memory; }

  public Status getStatus() { return status; }

  public FIFO getFIFO() { return fifo; }

  public PLL getPLL() { return pll; }

  public void reset()
  {
    status.reset();
    decoder.reset();
    fifo.reset();
    pll.reset();
  }

  public void setCLKDIV(final int clkdiv, final int mask, final boolean xor)
  {
    pll.setCLKDIV(Constants.hwSetBits(pll.getCLKDIV(), clkdiv, mask, xor));
  }

  public int getCLKDIV()
  {
    return pll.getCLKDIV();
  }

  public void resetCLKDIV()
  {
    pll.reset();
  }

  /*
   * TODO: In all of the following methods, use constants declared in
   * class Constants for bit shifting & masking.
   */

  public void setEXECCTRL(final int execctrl, final int mask, final boolean xor)
  {
    setEXECCTRL(Constants.hwSetBits(getEXECCTRL(), execctrl, mask, xor));
  }

  private void setEXECCTRL(final int execctrl)
  {
    status.regEXECCTRL_SIDE_EN = ((execctrl >>> 30) & 0x1) != 0x0;
    status.regEXECCTRL_SIDE_PINDIR =
      PIO.PinDir.fromValue((execctrl >>> 29) & 0x1);
    status.regEXECCTRL_JMP_PIN = (execctrl >>> 24) & 0x1f;
    status.regEXECCTRL_WRAP_TOP = (execctrl >>> 12) & 0x1f;
    status.regEXECCTRL_WRAP_BOTTOM = (execctrl >>> 7) & 0x1f;
    status.regEXECCTRL_STATUS_SEL = ((execctrl >>> 4) & 0x1) != 0x0;
    status.regEXECCTRL_STATUS_N = execctrl & 0xf;
  }

  public int getEXECCTRL()
  {
    return
      (isExecStalled() ? 1 : 0) << 31 |
      (status.regEXECCTRL_SIDE_EN ? 1 : 0) << 30 |
      status.regEXECCTRL_SIDE_PINDIR.getValue() << 29 |
      status.regEXECCTRL_JMP_PIN << 24 |
      status.regEXECCTRL_WRAP_TOP << 12 |
      status.regEXECCTRL_WRAP_BOTTOM << 7 |
      (status.regEXECCTRL_STATUS_SEL ? 1 : 0) << 4 |
      status.regEXECCTRL_STATUS_N;
  }

  public void setSHIFTCTRL(final int shiftctrl, final int mask,
                           final boolean xor)
  {
    setSHIFTCTRL(Constants.hwSetBits(getSHIFTCTRL(), shiftctrl, mask, xor));
  }

  private void setSHIFTCTRL(final int shiftctrl)
  {
    fifo.setJoinRX(((shiftctrl >>> 31) & 0x1) != 0x0);
    fifo.setJoinTX(((shiftctrl >>> 30) & 0x1) != 0x0);
    status.regSHIFTCTRL_PULL_THRESH = (shiftctrl >>> 25) & 0x1f;
    status.regSHIFTCTRL_PUSH_THRESH = (shiftctrl >>> 20) & 0x1f;
    status.regSHIFTCTRL_AUTOPULL = ((shiftctrl >>> 17) & 0x1) != 0x0;
    status.regSHIFTCTRL_AUTOPUSH = ((shiftctrl >>> 16) & 0x1) != 0x0;
  }

  public int getSHIFTCTRL()
  {
    return
      (fifo.getJoinRX() ? 1 : 0) << 31 |
      (fifo.getJoinTX() ? 1 : 0) << 30 |
      status.regSHIFTCTRL_PULL_THRESH << 25 |
      status.regSHIFTCTRL_PUSH_THRESH << 20 |
      (status.regSHIFTCTRL_AUTOPULL ? 1 : 0) << 17 |
      (status.regSHIFTCTRL_AUTOPUSH ? 1 : 0) << 16;
  }

  public void setPINCTRL(final int pinctrl, final int mask, final boolean xor)
  {
    setPINCTRL(Constants.hwSetBits(getPINCTRL(), pinctrl, mask, xor));
  }

  private void setPINCTRL(final int pinctrl)
  {
    status.regPINCTRL_SIDESET_COUNT = (pinctrl >>> 29) & 0x7;
    status.regPINCTRL_SET_COUNT = (pinctrl >>> 26) & 0x7;
    status.regPINCTRL_OUT_COUNT = (pinctrl >>> 20) & 0x3f;
    status.regPINCTRL_IN_BASE = (pinctrl >>> 15) & 0x1f;
    status.regPINCTRL_SIDESET_BASE = (pinctrl >>> 10) & 0x1f;
    status.regPINCTRL_SET_BASE = (pinctrl >>> 5) & 0x1f;
    status.regPINCTRL_OUT_BASE = pinctrl & 0x1f;
  }

  public int getPINCTRL()
  {
    return
      status.regPINCTRL_SIDESET_COUNT << 29 |
      status.regPINCTRL_SET_COUNT << 26 |
      status.regPINCTRL_OUT_COUNT << 20 |
      status.regPINCTRL_IN_BASE << 15 |
      status.regPINCTRL_SIDESET_BASE << 10 |
      status.regPINCTRL_SET_BASE << 5 |
      status.regPINCTRL_OUT_BASE;
  }

  public void clockRaisingEdge(final boolean smEnabled, final long wallClock)
  {
    status.smEnabled = smEnabled;
    if (smEnabled) {
      pll.raisingEdge(wallClock);
      status.clockEnabled = pll.getClockEnable();
    } else {
      status.clockEnabled = false;
    }
    status.processing =
      status.clockEnabled || (status.pendingDMAInstruction >= 0);
    if (status.processing) {
      try {
        fetchAndDecode();
      } catch (final Decoder.DecodeException e) {
        console.println(e.getMessage());
      }
    }
  }

  public void clockFallingEdge(final long wallClock)
  {
    if (status.smEnabled) {
      pll.fallingEdge(wallClock);
    }
    if (status.processing) {
      execute();
    }
  }

  public void restart()
  {
    // TODO: What about the program counter?  Always reset to 0x00?
    // Or is the .origin value of a compiled program somewhere stored?
    status.reset();
  }

  public Bit getGPIO(final int index)
  {
    return pioGpio.getLevel(index);
  }

  public Bit getIRQ(final int index)
  {
    return irq.get(index);
  }

  public void clearIRQ(final int index)
  {
    irq.clear(index);
  }

  public void setIRQ(final int index)
  {
    irq.set(index);
  }

  /**
   * @return &lt;code&gt;true&lt;/code&gt; if operation stall due to
   * full FIFO.
   */
  public boolean rxPush(final boolean ifFull, final boolean block)
  {
    final boolean isrFull = status.isrShiftCount >= status.regSHIFTCTRL_PUSH_THRESH;
    if (!ifFull || (isrFull && status.regSHIFTCTRL_AUTOPUSH)) {
      final boolean succeeded = fifo.rxPush(status.isrValue, block);
      if (succeeded) {
        status.isrValue = 0;
        status.isrShiftCount = 0;
        return false;
      } else {
        return block;
      }
    }
    return false;
  }

  /**
   * @return True if operation stall due to empty FIFO.
   */
  public boolean txPull(final boolean ifEmpty, final boolean block)
  {
    /*
     * TODO: Clarify: Need "status.osrShiftCount = 0" prior to the
     * following code, as 3.5.4.2 ("Autopull Details") of RP2040
     * datasheet suggests?  Also, stall behaviour may be wrong?
     */
    final boolean osrEmpty = status.osrShiftCount >= status.regSHIFTCTRL_PULL_THRESH;
    if (!ifEmpty || (osrEmpty && status.regSHIFTCTRL_AUTOPULL)) {
      synchronized(fifo) {
        final boolean fifoEmpty = fifo.fstatTxEmpty();
        if (fifoEmpty) {
          if (!block) {
            status.osrValue = status.regX;
            status.osrShiftCount = 0;
          }
          return block; // stall on block
        } else {
          status.osrValue = fifo.txPull(block);
          status.osrShiftCount = 0;
          return false;
        }
      }
    } else {
      return false;
    }
  }

  private int saturate(final int base, final int increment, final int limit)
  {
    final int sum = base + increment;
    return sum < limit ? sum : limit;
  }

  public int getISRValue() { return status.isrValue; }

  public void setISRValue(final int value)
  {
    status.isrValue = value;
  }

  public void setISRValue(final int value, final int mask, final boolean xor)
  {
    status.isrValue = Constants.hwSetBits(status.isrValue, value, mask, xor);
  }

  public boolean shiftISRLeft(final int bitCount, final int data)
  {
    // TODO: Clarify: Shift ISR always or only if not (isrFull &&
    // status.regSHIFTCTRL_AUTOPUSH)?
    status.isrValue <<= bitCount;
    status.isrValue |= data & SHIFT_MASK[bitCount];
    status.isrShiftCount = saturate(status.isrShiftCount, bitCount, 32);
    return rxPush(true, true);
  }

  public boolean shiftISRRight(final int bitCount, final int data)
  {
    // TODO: Clarify: Shift ISR always or only if not (isrFull &&
    // status.regSHIFTCTRL_AUTOPUSH)?
    status.isrValue >>>= bitCount;
    status.isrValue |= (data & SHIFT_MASK[bitCount]) << (32 - bitCount);
    status.isrShiftCount = saturate(status.isrShiftCount, bitCount, 32);
    return rxPush(true, true);
  }

  public int getISRShiftCount() { return status.isrShiftCount; }

  public void setISRShiftCount(final int value, final int mask,
                               final boolean xor)
  {
    status.isrShiftCount =
      Constants.hwSetBits(status.isrShiftCount, value, mask, xor);
  }

  public int getOSRValue() { return status.osrValue; }

  public void setOSRValue(final int value)
  {
    status.osrValue = value;
  }

  public void setOSRValue(final int value, final int mask, final boolean xor)
  {
    status.osrValue = Constants.hwSetBits(status.osrValue, value, mask, xor);
  }

  public boolean shiftOSRLeft(final int bitCount,
                              final IntConsumer destination)
  {
    // TODO: Clarify: Shift OSR always or only if not (isrEmpty &&
    // status.regSHIFTCTRL_AUTOPUSH)?
    final int data =
      (status.osrValue & ~SHIFT_MASK[32 - bitCount]) >>> (32 - bitCount);
    status.osrValue <<= bitCount;
    status.osrShiftCount = saturate(status.osrShiftCount, bitCount, 32);
    destination.accept(data);
    return txPull(true, true);
  }

  public boolean shiftOSRRight(final int bitCount,
                               final IntConsumer destination)
  {
    // TODO: Clarify: Shift OSR always or only if not (osrEmpty &&
    // status.regSHIFTCTRL_AUTOPUSH)?
    final int data = status.osrValue & SHIFT_MASK[bitCount];
    status.osrValue >>>= bitCount;
    status.osrShiftCount = saturate(status.osrShiftCount, bitCount, 32);
    destination.accept(data);
    return txPull(true, true);
  }

  public int getOSRShiftCount() { return status.osrShiftCount; }

  public void setOSRShiftCount(final int value, final int mask,
                               final boolean xor)
  {
    status.osrShiftCount =
      Constants.hwSetBits(status.osrShiftCount, value, mask, xor);
  }

  public void setSideSetCount(final int count)
  {
    if (count < 0) {
      throw new IllegalArgumentException("side set count < 0: " + count);
    }
    if (count > 5) {
      throw new IllegalArgumentException("side set count > 5: " + count);
    }
    status.regPINCTRL_SIDESET_COUNT = count;
  }

  public int getPins()
  {
    return pioGpio.getPins(status.regPINCTRL_IN_BASE, GPIO_NUM);
  }

  public PIO.ShiftDir getInShiftDir()
  {
    return status.regSHIFTCTRL_IN_SHIFTDIR;
  }

  public PIO.ShiftDir getOutShiftDir()
  {
    return status.regSHIFTCTRL_OUT_SHIFTDIR;
  }

  public int getX() { return status.regX; }

  public void setX(final int value)
  {
    status.regX = value;
  }

  public void setX(final int value, final int mask, final boolean xor)
  {
    status.regX = Constants.hwSetBits(status.regX, value, mask, xor);
  }

  private void decX()
  {
    status.regX--;
  }

  public int getY() { return status.regY; }

  public void setY(final int value)
  {
    status.regY = value;
  }

  public void setY(final int value, final int mask, final boolean xor)
  {
    status.regY = Constants.hwSetBits(status.regY, value, mask, xor);
  }

  private void decY()
  {
    status.regY--;
  }

  public void put(final int data)
  {
    synchronized(fifo) {
      fifo.txDMAWrite(data);
      if (isTXFIFOFull()) {
        irq.setINTR_SMX_TXNFULL(num);
      }
    }
  }

  public void putRXF(final int data)
  {
    synchronized(fifo) {
      fifo.rxPush(data, false);
      if (isRXFIFOFull()) {
        // irq.setINTR_SMX_RXNFULL(num); // TODO: Do we need this?
      }
    }
  }

  public int get()
  {
    synchronized(fifo) {
      final int value = fifo.rxDMARead();
      if (isRXFIFOEmpty()) {
        irq.setINTR_SMX_RXNEMPTY(num);
      }
      return value;
    }
  }

  public int getTXF()
  {
    synchronized(fifo) {
      final int value = fifo.txPull(false);
      if (isTXFIFOEmpty()) {
        // irq.setINTR_SMX_TXNEMPTY(num); // TODO: Do we need this?
      }
      return value;
    }
  }

  public boolean isRXFIFOFull()
  {
    return fifo.fstatRxFull();
  }

  public boolean isRXFIFOEmpty()
  {
    return fifo.fstatRxEmpty();
  }

  public int getRXFIFOLevel()
  {
    return fifo.getRXLevel();
  }

  public boolean isTXFIFOFull()
  {
    return fifo.fstatTxFull();
  }

  public boolean isTXFIFOEmpty()
  {
    return fifo.fstatTxEmpty();
  }

  public int getTXFIFOLevel()
  {
    return fifo.getTXLevel();
  }

  public void setBreakPoints(final int breakPoints,
                             final int mask, final boolean xor)
  {
    status.regBREAKPOINTS =
      Constants.hwSetBits(status.regBREAKPOINTS, breakPoints, mask, xor);
  }

  public int getBreakPoints()
  {
    return status.regBREAKPOINTS;
  }

  public void setTracePoints(final int tracePoints,
                             final int mask, final boolean xor)
  {
    status.regTRACEPOINTS =
      Constants.hwSetBits(status.regTRACEPOINTS, tracePoints, mask, xor);
  }

  public int getTracePoints()
  {
    return status.regTRACEPOINTS;
  }

  private int encodeJmp(final Instruction.Jmp.Condition condition,
                        final int address)
  {
    if (condition == null) {
      throw new NullPointerException("condition");
    }
    if (address < 0) {
      throw new IllegalArgumentException("address < 0: " + address);
    }
    if (address > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("address > " +
                                         (MEMORY_SIZE - 1) + ": " +
                                         address);
    }
    final Instruction.Jmp instruction = new Instruction.Jmp();
    instruction.setCondition(condition);
    instruction.setAddress(address);
    return instruction.encode(status.regPINCTRL_SIDESET_COUNT,
                              status.regEXECCTRL_SIDE_EN);
  }

  private int encodeOut(final Instruction.Out.Destination dst,
                        final int bitCount)
  {
    if (dst == null) {
      throw new NullPointerException("dst");
    }
    if (bitCount < 0) {
      throw new IllegalArgumentException("bitCount < 0: " + bitCount);
    }
    if (bitCount > 32) {
      throw new IllegalArgumentException("bitCount > 32: " + bitCount);
    }
    final Instruction.Out instruction = new Instruction.Out();
    instruction.setDestination(dst);
    instruction.setBitCount(bitCount);
    return instruction.encode(status.regPINCTRL_SIDESET_COUNT,
                              status.regEXECCTRL_SIDE_EN);
  }

  private int encodePull(final boolean ifEmpty, final boolean block)
  {
    final Instruction.Pull instruction = new Instruction.Pull();
    instruction.setIfEmpty(ifEmpty);
    instruction.setBlock(block);
    return instruction.encode(status.regPINCTRL_SIDESET_COUNT,
                              status.regEXECCTRL_SIDE_EN);
  }

  private int encodeSet(final Instruction.Set.Destination dst, final int data)
  {
    if (dst == null) {
      throw new NullPointerException("dst");
    }
    if (data < 0) {
      throw new IllegalArgumentException("data < 0: " + data);
    }
    if (data > 31) {
      throw new IllegalArgumentException("data > 31: " + data);
    }
    final Instruction.Set instruction = new Instruction.Set();
    instruction.setDestination(dst);
    instruction.setData(data);
    return instruction.encode(status.regPINCTRL_SIDESET_COUNT,
                              status.regEXECCTRL_SIDE_EN);
  }

  public int getPC()
  {
    return status.regADDR;
  }

  public void setPC(final int value)
  {
    if (value < 0) {
      throw new IllegalArgumentException("pc value < 0: " + value);
    }
    if (value > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("pc value > " +
                                         (MEMORY_SIZE - 1) + ": " +
                                         value);
    }
    status.regADDR = value;
  }

  private void updatePC()
  {
    if (status.regADDR == status.regEXECCTRL_WRAP_TOP) {
      status.regADDR = status.regEXECCTRL_WRAP_BOTTOM;
    } else {
      status.regADDR = (status.regADDR + 1) & 0x1f;
    }
    if (((status.regBREAKPOINTS >>> status.regADDR) & 0x1) != 0x0) {
      masterClock.setMode(MasterClock.Mode.SINGLE_STEP);
    }
  }

  private short fetch()
  {
    final int pendingDMAInstruction = status.pendingDMAInstruction;
    if (pendingDMAInstruction >= 0) {
      status.pendingDMAInstruction = -1;
      return (short)pendingDMAInstruction;
    }
    final int pendingExecInstruction = status.pendingExecInstruction;
    if (pendingExecInstruction >= 0) {
      status.pendingExecInstruction = -1;
      return (short)pendingExecInstruction;
    }
    // notify blocking methods that condition may have changed
    memory.FETCH_LOCK.notifyAll();
    return memory.get(status.regADDR);
  }

  public int getOpCode()
  {
    /*
     * TODO / FIXME: getOpCode() works only for instructions created
     * from a call to decode(), but will return 0 for synthesized
     * ones.
     */
    final Instruction instruction = status.instruction;
    if (instruction == null) {
      /*
       * Trying to access instruction before any decode has been
       * executed.
       *
       * TODO: The RP2040 datasheet is unclear for this situation:
       * Table 378 "CTRL Register" states for CTRL_SM_ENABLE:
       *
       * "When disabled, a state machine will cease executing
       * instructions, except those written directly to SMx_INSTR by
       * the system"
       *
       * And Table 395 "SMx_INSTR Registers" does *not* provide a
       * reset value for SMx_INSTR.
       *
       * This means, at startup, as long as a state machine has not
       * yet been enabled and therefore no instruction has been
       * fetched and decoded so far, the value of register SMx_INSTR
       * upon read access is undefined.
       *
       * For this specific case, we assume that SMx_INSTR will return
       * a value of 0.
       */
      return 0;
    }
    return instruction.getOpCode();
  }

  public void insertDMAInstruction(final int instruction)
  {
    synchronized(memory.FETCH_LOCK) {
      if (status.pendingDMAInstruction >= 0) {
        console.println("WARNING: " +
                        "discarding already pending DMA instruction");
      }
      if (instruction < 0) {
        throw new IllegalArgumentException("instruction < 0: " + instruction);
      }
      if (instruction > 65535) {
        throw new IllegalArgumentException("instruction > 65535: " +
                                           instruction);
      }
      status.pendingDMAInstruction = instruction;
    }
  }

  public void insertExecInstruction(final int instruction)
  {
    synchronized(memory.FETCH_LOCK) {
      if (status.pendingExecInstruction >= 0) {
        throw new InternalError("already have pending EXEC instruction");
      }
      if (instruction < 0) {
        throw new IllegalArgumentException("instruction < 0: " + instruction);
      }
      if (instruction > 65535) {
        throw new IllegalArgumentException("instruction > 65535: " +
                                           instruction);
      }
      status.pendingExecInstruction = instruction;
    }
  }

  public boolean isExecStalled()
  {
    synchronized(memory.FETCH_LOCK) {
      return (status.pendingDMAInstruction >= 0) && isStalled();
    }
  }

  public int getPendingDelay()
  {
    return status.pendingDelay;
  }

  private void fetchAndDecode() throws Decoder.DecodeException
  {
    synchronized(memory.FETCH_LOCK) {
      if ((status.pendingDMAInstruction < 0) && status.consumePendingDelay()) {
        status.isDelayCycle = true;
        return;
      }
      status.isDelayCycle = false;
      final short word = fetch();
      final Instruction instruction =
        decoder.decode(word,
                       status.regPINCTRL_SIDESET_COUNT,
                       status.regEXECCTRL_SIDE_EN);
      if (((status.regTRACEPOINTS >>> status.regADDR) & 0x1) != 0x0) {
        console.println("SM" + num + ": " + instruction);
      }
      status.instruction = instruction;
    }
  }

  private void execute()
  {
    if (status.isDelayCycle)
      return;
    final Instruction instruction = status.instruction;
    if (instruction == null) {
      throw new InternalError("seems emulator started with falling " +
                              "clock edge:  can not execute instruction " +
                              "before decode");
    }
    status.resultState = instruction.execute(this);
    if (status.resultState == Instruction.ResultState.COMPLETE) {
      // Sect. 3.4.2.2: "Delay cycles … take place after … the program
      // counter is updated" (though this specifically refers to JMP
      // instruction). => Update PC immediately, before executing
      // delay.
      updatePC();
    }
    if (status.resultState != Instruction.ResultState.STALL) {
      status.setPendingDelay(instruction.getDelay());
    }
  }

  public boolean isStalled()
  {
    return status.resultState == Instruction.ResultState.STALL;
  }

  public int getDelay()
  {
    final Instruction instruction = status.instruction;
    if (instruction == null) {
      /*
       * Trying to access instruction before any decode has been
       * executed.
       *
       * TODO: The RP2040 datasheet is unclear for this situation; for
       * details, see method #getOpCode().
       *
       * For this specific case, we assume a delay value of 0.
       */
      return 0;
    }
    return instruction.getDelay();
  }

  public boolean isDelayCycle()
  {
    return status.isDelayCycle;
  }

  @Override
  public String toString()
  {
    return "SM" + num + "{PC=" + getPC() + "}";
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
