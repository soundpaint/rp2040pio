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

  public class Status
  {
    public int regX;
    public int regY;
    public int regPC;
    public int isrValue;
    public int isrShiftCount;
    public int osrValue;
    public int osrShiftCount;
    public int clockDivIntegral;
    public int clockDivFraction;
    public int sideSetCount;
    public int sideSetBase;
    public boolean sideSetEnable;
    public PIO.PinDir sideSetPinDir;
    public int jmpPin;
    public PIO.ShiftDir inShiftDir;
    public PIO.ShiftDir outShiftDir;
    public int pushThresh;
    public boolean autoPush;
    public int pullThresh;
    public boolean autoPull;

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
      clockDivIntegral = 1;
      clockDivFraction = 0;
      sideSetCount = 0;
      sideSetBase = 0;
      sideSetEnable = false;
      sideSetPinDir = PIO.PinDir.GPIO_LEVELS;
      jmpPin = 0;
      inShiftDir = PIO.ShiftDir.SHIFT_LEFT;
      outShiftDir = PIO.ShiftDir.SHIFT_LEFT;
      pushThresh = 0;
      autoPush = false;
      pullThresh = 0;
      autoPull = false;
    }

    public int asWord()
    {
      // TODO
      throw new InternalError("not yet implemented");
    }

    private void restart()
    {
      isrShiftCount = 0;
      osrShiftCount = 32;
    }
  }

  private SM()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public SM(final int num, final GPIO gpio, final Memory memory)
  {
    if (gpio == null) throw new NullPointerException("gpio");
    if (memory == null) throw new NullPointerException("memory");
    this.num = num;
    this.gpio = gpio;
    this.memory = memory;
    status = new Status();
    decoder = new Decoder(this);
    fifo = new FIFO();
  }

  public void restart()
  {
    status.restart();
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

  public GPIO.Bit getPin(final int index)
  {
    return gpio.getBit(mapPin(index));
  }

  public int getAllPins()
  {
    // TODO
    throw new InternalError("not yet implemented");
  }

  public void setAllPins(final int value)
  {
    // TODO
    throw new InternalError("not yet implemented");
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

  public void setNextInstruction(final int code)
  {
    // TODO
    throw new InternalError("not yet implemented");
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

  public void setSideSetCount(final int count)
  {
    if (count < 0) {
      throw new IllegalArgumentException("side set count < 0");
    }
    if (count > 5) {
      throw new IllegalArgumentException("side set count > 5");
    }
    status.sideSetCount = count;
  }

  public void setSideSetBase(final int base)
  {
    if (base < 0) {
      throw new IllegalArgumentException("side set base < 0");
    }
    if (base > 31) {
      throw new IllegalArgumentException("side set base > 31");
    }
    status.sideSetBase = base;
  }

  public void setSideSetEnable(final boolean enable)
  {
    status.sideSetEnable = enable;
  }

  public void setSideSetPinDir(final PIO.PinDir pinDir)
  {
    if (pinDir == null) { throw new NullPointerException("pinDir"); }
    status.sideSetPinDir = pinDir;
  }

  public void setPinDirs(final int pinDirs)
  {
    // TODO
    throw new InternalError("not yet implemented");
  }

  public void setJmpPin(final int pin)
  {
    if (pin < 0) {
      throw new IllegalArgumentException("exec ctrl jmp pin < 0");
    }
    if (pin > 31) {
      throw new IllegalArgumentException("exec ctrl jmp pin > 31");
    }
    status.jmpPin = pin;
  }

  public void setInShiftDir(final PIO.ShiftDir shiftDir)
  {
    if (shiftDir == null) { throw new NullPointerException("shiftDir"); }
    status.inShiftDir = shiftDir;
  }

  public PIO.ShiftDir getInShiftDir()
  {
    return status.inShiftDir;
  }

  public void setOutShiftDir(final PIO.ShiftDir shiftDir)
  {
    if (shiftDir == null) { throw new NullPointerException("shiftDir"); }
    status.outShiftDir = shiftDir;
  }

  public PIO.ShiftDir getOutShiftDir()
  {
    return status.outShiftDir;
  }

  public void setPushThresh(final int thresh)
  {
    if (thresh < 0) {
      throw new IllegalArgumentException("shift ctrl push threshold < 0");
    }
    if (thresh > 31) {
      throw new IllegalArgumentException("shift ctrl push threshold > 31");
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
      throw new IllegalArgumentException("shift ctrl pull threshold < 0");
    }
    if (thresh > 31) {
      throw new IllegalArgumentException("shift ctrl pull threshold > 31");
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

  public void setPC(final int value)
  {
    if (value < 0) {
      throw new IllegalArgumentException("pc value < 0");
    }
    if (value > 31) {
      throw new IllegalArgumentException("pc value > 31");
    }
    status.regPC = value;
  }

  private void incPC()
  {
    status.regPC = (status.regPC + 1) & 0x1f;
  }

  private short fetch()
  {
    final short word = memory.get(status.regPC);
    incPC();
    return word;
  }

  public void pushInstruction(final int word)
  {
    // TODO
    throw new InternalError("not yet implemented");
  }

  public void execute() throws Decoder.DecodeException
  {
    final short word = fetch();
    final Instruction instruction = decoder.decode(word);
    if (!instruction.execute()) incPC();
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
