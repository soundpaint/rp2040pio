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

import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * State Machine
 */
public class SM
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
  private final GPIO gpio;
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
      sm.gpio.setPins(data, baseGetter.apply(sm), countGetter.apply(sm));
    }

    public void setPinDirs(final SM sm, final int data)
    {
      sm.gpio.setPinDirs(data, baseGetter.apply(sm), countGetter.apply(sm));
    }
  };

  public class Status
  {
    public Instruction instruction;
    public Instruction.ResultState resultState;
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
    public int regADDR; // bits 0..4 of SMx_ADDR
    public boolean regEXECCTRL_SIDE_EN; // bit 30 of SMx_EXECCTRL
    public PIO.PinDir regEXECCTRL_SIDE_PINDIR; // bit 29 of SMx_EXECCTRL
    public int regEXECCTRL_JMP_PIN; // bits 24..28 of SMx_EXECCTRL
    public int regEXECCTRL_WRAP_TOP; // bits 12..16 of SMx_EXECCTRL
    public int regEXECCTRL_WRAP_BOTTOM; // bits 7..11 of SMx_EXECCTRL
    public boolean regEXECCTRL_STATUS_SEL; // bit 4 of SMx_EXECCTRL
    public int regEXECCTRL_STATUS_N; // bits 0..3 of SMx_EXECCTRL
    public int regSHIFTCTRL_PULL_THRESH; // bits 25..29 of SMx_SHIFTCTRL
    public int regSHIFTCTRL_PUSH_THRESH; // bits 20..24 of SMx_SHIFTCTRL
    public PIO.ShiftDir regSHIFTCTRL_IN_SHIFTDIR; // bit 18 of SMx_SHIFTCTRL
    public boolean regSHIFTCTRL_AUTOPULL; // bit 17 of SMx_SHIFTCTRL
    public PIO.ShiftDir regSHIFTCTRL_OUT_SHIFTDIR; // bit 19 of SMx_SHIFTCTRL
    public boolean regSHIFTCTRL_AUTOPUSH; // bit 16 of SMx_SHIFTCTRL
    public int regPINCTRL_SIDESET_COUNT; // bits 29..31 of SMx_PINCTRL
    public int regPINCTRL_SET_COUNT; // bits 26..28 of SMx_PINCTRL
    public int regPINCTRL_OUT_COUNT; // bits 20..25 of SMx_PINCTRL
    public int regPINCTRL_IN_BASE; // bits 15..19 of SMx_PINCTRL
    public int regPINCTRL_SIDESET_BASE; // bits 10..14 of SMx_PINCTRL
    public int regPINCTRL_SET_BASE; // bits 5..9 of SMx_PINCTRL
    public int regPINCTRL_OUT_BASE; // bits 0..4 of SMx_PINCTRL

    public Status()
    {
      reset();
    }

    private void reset()
    {
      instruction = null;
      resultState = null;
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
    }

    public Bit jmpPin()
    {
      return gpio.getBit(regEXECCTRL_JMP_PIN);
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
  }

  private SM()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public SM(final int num, final GPIO gpio, final Memory memory, final IRQ irq)
  {
    if (num < 0) {
      throw new IllegalArgumentException("SM num < 0: " + num);
    }
    if (num > 3) {
      throw new IllegalArgumentException("SM num > 3: " + num);
    }
    if (gpio == null) {
      throw new NullPointerException("gpio");
    }
    if (memory == null) {
      throw new NullPointerException("memory");
    }
    if (irq == null) {
      throw new NullPointerException("irq");
    }
    this.num = num;
    this.gpio = gpio;
    this.memory = memory;
    this.irq = irq;
    status = new Status();
    decoder = new Decoder(this);
    fifo = new FIFO();
    pll = new PLL();
  }

  public FIFO getFIFO() { return fifo; }

  public PLL getPLL() { return pll; }

  public void setCLKDIV(final int clkdiv)
  {
    pll.setCLKDIV(clkdiv);
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

  public void setEXECCTRL(final int execctrl)
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

  public void setSHIFTCTRL(final int shiftctrl)
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

  public void setPINCTRL(final int pinctrl)
  {
    status.regPINCTRL_SIDESET_COUNT = (pinctrl >>> 29) & 0x7;
    status.regPINCTRL_SET_COUNT = (pinctrl >>> 26) & 0x7;
    status.regPINCTRL_OUT_COUNT = (pinctrl >>> 20) & 0x3f;
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

  public void clockRaisingEdge(final long wallClock)
    throws Decoder.DecodeException
  {
    pll.raisingEdge(wallClock);
    if (pll.getClockEnable()) {
      fetchAndDecode();
    }
  }

  public void clockFallingEdge(final long wallClock)
    throws Decoder.DecodeException
  {
    pll.fallingEdge(wallClock);
    if (pll.getClockEnable()) {
      execute();
    }
  }

  public void restart()
  {
    // TODO: What about the program counter?  Always reset to 0x00?
    // Or is the .origin value of a compiled program somewhere stored?
    status.reset();
  }

  public Memory getMemory()
  {
    return memory;
  }

  public Status getStatus()
  {
    return status;
  }

  public GPIO getGPIO()
  {
    return gpio;
  }

  public Bit getGPIO(final int index)
  {
    return gpio.getBit(index);
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

  public void clearFIFOs()
  {
    fifo.clear();
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

  public int getOSRValue() { return status.osrValue; }

  public void setOSRValue(final int value)
  {
    status.osrValue = value;
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

  public int getNum()
  {
    return num;
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

  public void setSideSetBase(final int base)
  {
    if (base < 0) {
      throw new IllegalArgumentException("side set base < 0: " + base);
    }
    if (base > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("side set base > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         base);
    }
    status.regPINCTRL_SIDESET_BASE = base;
  }

  public void setInBase(final int base)
  {
    if (base < 0) {
      throw new IllegalArgumentException("in base < 0: " + base);
    }
    if (base > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("in base > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         base);
    }
    status.regPINCTRL_IN_BASE = base;
  }

  public int getPins()
  {
    return gpio.getPins(status.regPINCTRL_IN_BASE, Constants.GPIO_NUM);
  }

  public void setSideSetEnable(final boolean enable)
  {
    status.regEXECCTRL_SIDE_EN = enable;
  }

  public void setSideSetPinDir(final PIO.PinDir pinDir)
  {
    if (pinDir == null) {
      throw new NullPointerException("pinDir");
    }
    status.regEXECCTRL_SIDE_PINDIR = pinDir;
  }

  public void setJmpPin(final int pin)
  {
    if (pin < 0) {
      throw new IllegalArgumentException("exec ctrl jmp pin < 0: " + pin);
    }
    if (pin > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("exec ctrl jmp pin > " +
                                         (Constants.GPIO_NUM - 1) + ": " + pin);
    }
    status.regEXECCTRL_JMP_PIN = pin;
  }

  public void setInShiftDir(final PIO.ShiftDir shiftDir)
  {
    if (shiftDir == null) {
      throw new NullPointerException("shiftDir");
    }
    status.regSHIFTCTRL_IN_SHIFTDIR = shiftDir;
  }

  public PIO.ShiftDir getInShiftDir()
  {
    return status.regSHIFTCTRL_IN_SHIFTDIR;
  }

  public void setOutShiftDir(final PIO.ShiftDir shiftDir)
  {
    if (shiftDir == null) {
      throw new NullPointerException("shiftDir");
    }
    status.regSHIFTCTRL_OUT_SHIFTDIR = shiftDir;
  }

  public PIO.ShiftDir getOutShiftDir()
  {
    return status.regSHIFTCTRL_OUT_SHIFTDIR;
  }

  public void setPushThresh(final int thresh)
  {
    if (thresh < 0) {
      throw new IllegalArgumentException("shift ctrl push threshold < 0: " +
                                         thresh);
    }
    if (thresh > 31) {
      throw new IllegalArgumentException("shift ctrl push threshold > 31: " +
                                         thresh);
    }
    status.regSHIFTCTRL_PUSH_THRESH = thresh;
  }

  public void setAutoPush(final boolean auto)
  {
    status.regSHIFTCTRL_AUTOPUSH = auto;
  }

  public void setPullThresh(final int thresh)
  {
    if (thresh < 0) {
      throw new IllegalArgumentException("shift ctrl pull threshold < 0: " +
                                         thresh);
    }
    if (thresh > 31) {
      throw new IllegalArgumentException("shift ctrl pull threshold > 31: " +
                                         thresh);
    }
    status.regSHIFTCTRL_PULL_THRESH = thresh;
  }

  public void setAutoPull(final boolean auto)
  {
    status.regSHIFTCTRL_AUTOPULL = auto;
  }

  public int getX() { return status.regX; }

  public void setX(final int value)
  {
    status.regX = value;
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

  private void decY()
  {
    status.regY--;
  }

  public void setWrapTop(final int value)
  {
    if (value < 0) {
      throw new IllegalArgumentException("wrap top value < 0: " + value);
    }
    if (value > Constants.MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("wrap top value > " +
                                         (Constants.MEMORY_SIZE - 1) + ": " +
                                         value);
    }
    status.regEXECCTRL_WRAP_TOP = value;
  }

  public void setWrapBottom(final int value)
  {
    if (value < 0) {
      throw new IllegalArgumentException("wrap bottom value < 0: " + value);
    }
    if (value > Constants.MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("wrap bottom value > " +
                                         (Constants.MEMORY_SIZE - 1) + ": " +
                                         value);
    }
    status.regEXECCTRL_WRAP_BOTTOM = value;
  }

  public void deactivateWrap()
  {
    status.regEXECCTRL_WRAP_TOP = 0x1f;
    status.regEXECCTRL_WRAP_BOTTOM = 0x00;
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

  public void putBlocking(final int data)
  {
    synchronized(fifo) {
      while (isTXFIFOFull()) {
        try {
          fifo.wait();
        } catch (final InterruptedException e) {
          // running check isTXFIFOFull() anyway => ignore
        }
      }
      put(data);
    }
  }

  public int getBlocking()
  {
    synchronized(fifo) {
      while (isRXFIFOEmpty()) {
        try {
          fifo.wait();
        } catch (final InterruptedException e) {
          // running check isRXFIFOEmpty() anyway => ignore
        }
      }
      return get();
    }
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
    if (address > Constants.MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("address > " +
                                         (Constants.MEMORY_SIZE - 1) + ": " +
                                         address);
    }
    final Instruction.Jmp instruction = new Instruction.Jmp(this);
    instruction.setCondition(condition);
    instruction.setAddress(address);
    return instruction.encode();
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
    final Instruction.Out instruction = new Instruction.Out(this);
    instruction.setDestination(dst);
    instruction.setBitCount(bitCount);
    return instruction.encode();
  }

  private int encodePull(final boolean ifEmpty, final boolean block)
  {
    final Instruction.Pull instruction = new Instruction.Pull(this);
    instruction.setIfEmpty(ifEmpty);
    instruction.setBlock(block);
    return instruction.encode();
  }

  public void drainTXFIFO()
  {
    final int instruction =
      status.regSHIFTCTRL_AUTOPULL ?
      encodeOut(Instruction.Out.Destination.NULL, 32) :
      encodePull(false, false);
    while (!isTXFIFOEmpty()) {
      insertDMAInstruction(instruction);
      // TODO: Wait for completion of inserted instruction?
    }
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
    final Instruction.Set instruction = new Instruction.Set(this);
    instruction.setDestination(dst);
    instruction.setData(data);
    return instruction.encode();
  }

  public void setPinsWithMask(final int pinValues, int pinMask)
  {
    final int pinCtrlSaved = getPINCTRL();
    while (pinMask != 0x0) {
      final int base = Constants.ctz(pinMask);
      setPINCTRL((1 << Constants.SM0_PINCTRL_SET_COUNT_LSB) |
                 (base << Constants.SM0_PINCTRL_SET_BASE_LSB));
      final int instruction = encodeSet(Instruction.Set.Destination.PINS,
                                        (pinValues >>> base) & 0x1);
      insertDMAInstruction(instruction);
      // TODO: Wait for completion of inserted instruction?
      pinMask &= pinMask - 1;
    }
    setPINCTRL(pinCtrlSaved);
  }

  public void setPinDirsWithMask(final int pinDirs, int pinMask)
  {
    final int pinCtrlSaved = getPINCTRL();
    while (pinMask != 0x0) {
      final int base = Constants.ctz(pinMask);
      setPINCTRL((1 << Constants.SM0_PINCTRL_SET_COUNT_LSB) |
                 (base << Constants.SM0_PINCTRL_SET_BASE_LSB));
      final int instruction = encodeSet(Instruction.Set.Destination.PINDIRS,
                                        (pinDirs >>> base) & 0x1);
      insertDMAInstruction(instruction);
      // TODO: Wait for completion of inserted instruction?
      pinMask &= pinMask - 1;
    }
    setPINCTRL(pinCtrlSaved);
  }

  public void setConsecutivePinDirs(final int pinBase, final int pinCount,
                                    final boolean isOut)
  {
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
    final int pinCtrlSaved = getPINCTRL();
    final int pinDirVal = isOut ? 0x1f : 0x0;
    int pin = pinBase;
    int count = pinCount;
    while (count > 5) {
      setPINCTRL((5 << Constants.SM0_PINCTRL_SET_COUNT_LSB) |
                 (pin << Constants.SM0_PINCTRL_SET_BASE_LSB));
      final int instruction = encodeSet(Instruction.Set.Destination.PINDIRS,
                                        pinDirVal);
      insertDMAInstruction(instruction);
      // TODO: Wait for completion of inserted instruction?
      count -= 5;
      pin = (pin + 5) & 0x1f;
    }
    setPINCTRL((count << Constants.SM0_PINCTRL_SET_COUNT_LSB) |
               (pin << Constants.SM0_PINCTRL_SET_BASE_LSB));
    final int instruction = encodeSet(Instruction.Set.Destination.PINDIRS,
                                      pinDirVal);
    insertDMAInstruction(instruction);
    // TODO: Wait for completion of inserted instruction?
    setPINCTRL(pinCtrlSaved);
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
    if (value > Constants.MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("pc value > " +
                                         (Constants.MEMORY_SIZE - 1) + ": " +
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

  public int getInstruction()
  {
    /*
     * TODO / FIXME: getOpCode() works only for instructions created
     * from a call to decode(), but will return 0 for synthesized
     * ones.
     */
    return status.instruction.getOpCode();
  }

  public void insertDMAInstruction(final int instruction)
  {
    synchronized(memory.FETCH_LOCK) {
      if (status.pendingDMAInstruction >= 0) {
        System.out.println("WARNING: " +
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

  public void smExecWaitBlocking(final int instruction)
  {
    synchronized(memory.FETCH_LOCK) {
      insertDMAInstruction(instruction);
      while (isExecStalled()) {
        try {
          memory.FETCH_LOCK.wait();
        } catch (final InterruptedException e) {
          // running check isExecStalled() anyway => ignore
        }
      }
    }
  }

  public void fetchAndDecode() throws Decoder.DecodeException
  {
    synchronized(memory.FETCH_LOCK) {
      if ((status.pendingDMAInstruction < 0) && status.consumePendingDelay()) {
        status.isDelayCycle = true;
        return;
      }
      status.isDelayCycle = false;
      final short word = fetch();
      status.instruction = decoder.decode(word);
    }
  }

  public void execute()
  {
    if (status.isDelayCycle)
      return;
    status.resultState = status.instruction.execute();
    if (status.resultState == Instruction.ResultState.COMPLETE) {
      // Sect. 3.4.2.2: "Delay cycles ... take place after ... the
      // program counter is updated" (though this specifically refers
      // to JMP instruction). => Update PC immediately, before
      // executing delay.
      updatePC();
    }
    if (status.resultState != Instruction.ResultState.STALL) {
      status.setPendingDelay(status.instruction.getDelay());
    }
  }

  public boolean isStalled()
  {
    return status.resultState == Instruction.ResultState.STALL;
  }

  public boolean isDelayCycle()
  {
    return status.isDelayCycle;
  }

  public Instruction getCurrentInstruction()
  {
    return status.instruction;
  }

  public int getClockDivIntegerBits()
  {
    return pll.getDivIntegerBits();
  }

  public void setClockDivIntegerBits(final int divIntegerBits)
  {
    pll.setDivIntegerBits(divIntegerBits);
  }

  public int getClockDivFractionalBits()
  {
    return pll.getDivFractionalBits();
  }

  public void setClockDivFractionalBits(final int divFractionalBits)
  {
    pll.setDivFractionalBits(divFractionalBits);
  }

  public void dumpMemory()
  {
    for (int address = 0; address < Constants.MEMORY_SIZE; address++) {
      final short word = memory.get(address);
      String opCode;
      try {
        final Instruction instruction = decoder.decode(word);
        opCode = instruction.toString();
      } catch (final Decoder.DecodeException e) {
        opCode = "???";
      }
      System.out.printf("%02x: %04x %s%n", address, word, opCode);
    }
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
