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
  private final Status status;
  private final Decoder decoder;
  private final FIFO fifo;
  private final PLL pll;

  public enum IOMapping
  {
    SET((sm) -> sm.status.setBase, (sm) -> sm.status.setCount),
    OUT((sm) -> sm.status.outBase, (sm) -> sm.status.outCount),
    SIDE_SET((sm) -> sm.status.sideSetBase, (sm) -> sm.status.sideSetCount);

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
    public boolean enabled;
    public boolean clockEnabled;
    public int regX;
    public int regY;
    public int regPC;
    public int isrValue;
    public int isrShiftCount;
    public int osrValue;
    public int osrShiftCount;
    public int setCount;
    public int setBase;
    public int outCount;
    public int outBase;
    public int sideSetCount;
    public int sideSetBase;
    public int inBase;
    public boolean sideSetEnable;
    public PIO.PinDir sideSetPinDir;
    public int jmpPin;
    public PIO.ShiftDir inShiftDir;
    public PIO.ShiftDir outShiftDir;
    public int pushThresh;
    public boolean autoPush;
    public int pullThresh;
    public boolean autoPull;
    public int pendingDelay;
    public int pendingInstruction;
    public int wrapTop;
    public int wrapBottom;

    public GPIO.Bit jmpPin()
    {
      return gpio.getBit(jmpPin);
    }

    public boolean osrEmpty()
    {
      return osrShiftCount == 0;
    }

    public Status()
    {
      regX = 0;
      regY = 0;
      regPC = 0;
      isrValue = 0;
      osrValue = 0;
      setCount = 0;
      setBase = 0;
      outCount = 0;
      outBase = 0;
      sideSetCount = 0;
      sideSetBase = 0;
      inBase = 0;
      sideSetEnable = false;
      sideSetPinDir = PIO.PinDir.GPIO_LEVELS;
      jmpPin = 0;
      inShiftDir = PIO.ShiftDir.SHIFT_LEFT;
      outShiftDir = PIO.ShiftDir.SHIFT_LEFT;
      pushThresh = 0;
      autoPush = false;
      pullThresh = 0;
      autoPull = false;
      wrapTop = -1;
      wrapBottom = -1;
      reset();
    }

    public boolean getStatusSel()
    {
      // TODO
      throw new InternalError("not yet implemented");
      // as defined by EXECCTRL_STATUS_SEL
    }

    private void reset()
    {
      isrShiftCount = 0;
      osrShiftCount = 32;
      pendingDelay = 0;
      pendingInstruction = -1;
      enabled = false;
      clockEnabled = false;
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

  public SM(final int num, final Clock clock,
            final GPIO gpio, final Memory memory)
  {
    if (num < 0) {
      throw new IllegalArgumentException("SM num < 0: " + num);
    }
    if (num > 3) {
      throw new IllegalArgumentException("SM num > 3: " + num);
    }
    if (clock == null) {
      throw new NullPointerException("clock");
    }
    if (gpio == null) {
      throw new NullPointerException("gpio");
    }
    if (memory == null) {
      throw new NullPointerException("memory");
    }
    this.num = num;
    this.gpio = gpio;
    this.memory = memory;
    status = new Status();
    decoder = new Decoder(this);
    fifo = new FIFO();
    pll = new PLL(clock);
    pll.addTransitionListener(new Clock.TransitionListener()
      {
        @Override
        public void raisingEdge(final long wallClock)
        {
          status.clockEnabled = true;
        }
        @Override
        public void fallingEdge(final long wallClock)
        {
          status.clockEnabled = false;
        }
      });
  }

  public void clockRaisingEdge() throws Decoder.DecodeException
  {
    if (status.enabled && status.clockEnabled) {
      execute();
    }
  }

  public void restart()
  {
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

  public GPIO.Bit getGPIO(final int index)
  {
    return gpio.getBit(index);
  }

  public GPIO.Bit getIRQ(final int index)
  {
    // TODO
    throw new InternalError("not yet implemented");
  }

  public GPIO.Bit clearIRQ(final int index)
  {
    // TODO
    throw new InternalError("not yet implemented");
  }

  public GPIO.Bit setIRQ(final int index)
  {
    // TODO
    throw new InternalError("not yet implemented");
  }

  /**
   * @return True if operation stall due to full FIFO.
   */
  public boolean rxPush(final boolean ifFull, final boolean block)
  {
    final boolean isrFull = status.isrShiftCount >= status.pushThresh;
    if (!ifFull || (isrFull && status.autoPush)) {
      final boolean fifoFull = fifo.fstatRxFull();
      if (fifoFull) {
        return block; // stall on block
      } else {
        fifo.rxPush(status.isrValue);
        status.isrValue = 0;
        status.isrShiftCount = 0;
        return false;
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
    final boolean osrEmpty = status.osrShiftCount >= status.pullThresh;
    if (!ifEmpty || (osrEmpty && status.autoPull)) {
      final boolean fifoEmpty = fifo.fstatTxEmpty();
      if (fifoEmpty) {
        if (!block) {
          status.osrValue = status.regX;
          status.osrShiftCount = 0;
        }
        return block; // stall on block
      } else {
        status.osrValue = fifo.txPull();
        status.osrShiftCount = 0;
        return false;
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
    // status.autoPush)?
    status.isrValue <<= bitCount;
    status.isrValue |= data & SHIFT_MASK[bitCount];
    status.isrShiftCount = saturate(status.isrShiftCount, bitCount, 32);
    return rxPush(true, true);
  }

  public boolean shiftISRRight(final int bitCount, final int data)
  {
    // TODO: Clarify: Shift ISR always or only if not (isrFull &&
    // status.autoPush)?
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
    // status.autoPush)?
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
    // status.autoPush)?
    final int data = status.osrValue & SHIFT_MASK[bitCount];
    status.osrValue >>>= bitCount;
    status.osrShiftCount = saturate(status.osrShiftCount, bitCount, 32);
    destination.accept(data);
    return txPull(true, true);
  }

  private int mapPin(final int index)
  {
    // TODO
    throw new InternalError("not yet implemented");
  }

  public int getNum()
  {
    return num;
  }

  public void setSetCount(final int count)
  {
    if (count < 0) {
      throw new IllegalArgumentException("set count < 0: " + count);
    }
    if (count > 5) {
      throw new IllegalArgumentException("set count > 5: " + count);
    }
    status.setCount = count;
  }

  public void setSetBase(final int base)
  {
    if (base < 0) {
      throw new IllegalArgumentException("set base < 0: " + base);
    }
    if (base > 31) {
      throw new IllegalArgumentException("set base > 31: " + base);
    }
    status.setBase = base;
  }

  public void setOutCount(final int count)
  {
    if (count < 0) {
      throw new IllegalArgumentException("out count < 0: " + count);
    }
    if (count > 5) {
      throw new IllegalArgumentException("out count > 5: " + count);
    }
    status.outCount = count;
  }

  public void setOutBase(final int base)
  {
    if (base < 0) {
      throw new IllegalArgumentException("out base < 0: " + base);
    }
    if (base > 31) {
      throw new IllegalArgumentException("out base > 31: " + base);
    }
    status.outBase = base;
  }

  public void setSideSetCount(final int count)
  {
    if (count < 0) {
      throw new IllegalArgumentException("side set count < 0: " + count);
    }
    if (count > 5) {
      throw new IllegalArgumentException("side set count > 5: " + count);
    }
    status.sideSetCount = count;
  }

  public void setSideSetBase(final int base)
  {
    if (base < 0) {
      throw new IllegalArgumentException("side set base < 0: " + base);
    }
    if (base > 31) {
      throw new IllegalArgumentException("side set base > 31: " + base);
    }
    status.sideSetBase = base;
  }

  public void setInBase(final int base)
  {
    if (base < 0) {
      throw new IllegalArgumentException("in base < 0: " + base);
    }
    if (base > 31) {
      throw new IllegalArgumentException("in base > 31: " + base);
    }
    status.inBase = base;
  }

  public int getPins()
  {
    return gpio.getPins(status.inBase, 32);
  }

  public void setSideSetEnable(final boolean enable)
  {
    status.sideSetEnable = enable;
  }

  public void setSideSetPinDir(final PIO.PinDir pinDir)
  {
    if (pinDir == null) {
      throw new NullPointerException("pinDir");
    }
    status.sideSetPinDir = pinDir;
  }

  public void setJmpPin(final int pin)
  {
    if (pin < 0) {
      throw new IllegalArgumentException("exec ctrl jmp pin < 0: " + pin);
    }
    if (pin > 31) {
      throw new IllegalArgumentException("exec ctrl jmp pin > 31: " + pin);
    }
    status.jmpPin = pin;
  }

  public void setInShiftDir(final PIO.ShiftDir shiftDir)
  {
    if (shiftDir == null) {
      throw new NullPointerException("shiftDir");
    }
    status.inShiftDir = shiftDir;
  }

  public PIO.ShiftDir getInShiftDir()
  {
    return status.inShiftDir;
  }

  public void setOutShiftDir(final PIO.ShiftDir shiftDir)
  {
    if (shiftDir == null) {
      throw new NullPointerException("shiftDir");
    }
    status.outShiftDir = shiftDir;
  }

  public PIO.ShiftDir getOutShiftDir()
  {
    return status.outShiftDir;
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
    status.pushThresh = thresh;
  }

  public void setAutoPush(final boolean auto)
  {
    status.autoPush = auto;
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
    status.pullThresh = thresh;
  }

  public void setAutoPull(final boolean auto)
  {
    status.autoPull = auto;
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
    if (value > 31) {
      throw new IllegalArgumentException("wrap top value > 31: " + value);
    }
    status.wrapTop = value;
  }

  public void setWrapBottom(final int value)
  {
    if (value < 0) {
      throw new IllegalArgumentException("wrap bottom value < 0: " + value);
    }
    if (value > 31) {
      throw new IllegalArgumentException("wrap bottom value > 31: " + value);
    }
    status.wrapBottom = value;
  }

  public void deactivateWrap()
  {
    status.wrapTop = -1;
    status.wrapBottom = -1;
  }

  public void setPC(final int value)
  {
    if (value < 0) {
      throw new IllegalArgumentException("pc value < 0: " + value);
    }
    if (value > 31) {
      throw new IllegalArgumentException("pc value > 31: " + value);
    }
    status.regPC = value;
  }

  private void updatePC()
  {
    if (status.regPC == status.wrapTop) {
      if (status.wrapBottom < 0) {
        throw new InternalError("inconsistent wrap configuration");
      }
      status.regPC = status.wrapBottom;
    } else {
      status.regPC = (status.regPC + 1) & 0x1f;
    }
  }

  private short fetch()
  {
    final int pendingInstruction = status.pendingInstruction;
    if (pendingInstruction >= 0) {
      status.pendingInstruction = -1;
      return (short)pendingInstruction;
    }
    return memory.get(status.regPC);
  }

  public void insertInstruction(final int instruction)
  {
    if (status.pendingInstruction >= 0) {
      throw new InternalError("already have pending instruction");
    }
    if (instruction < 0) {
      throw new IllegalArgumentException("instruction < 0: " + instruction);
    }
    if (instruction > 65535) {
      throw new IllegalArgumentException("instruction > 65535: " + instruction);
    }
    status.pendingInstruction = instruction;
  }

  public void execute() throws Decoder.DecodeException
  {
    if (status.consumePendingDelay())
      return;
    final short word = fetch();
    final Instruction instruction = decoder.decode(word);
    final Instruction.ResultState resultState = instruction.execute();
    if (resultState == Instruction.ResultState.COMPLETE) {
      updatePC();
    }
    if (resultState != Instruction.ResultState.STALL) {
      status.setPendingDelay(instruction.getDelay());
    }
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

  public void enable()
  {
    status.enabled = true;
  }

  public void disable()
  {
    status.enabled = false;
  }

  public void dumpMemory()
  {
    for (int address = 0; address < Memory.SIZE; address++) {
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
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
