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

/**
 * State Machine
 */
public class SM
{
  private final GPIO gpio;
  private final Memory memory;
  private final Status status;
  private final Decoder decoder;

  public class Status
  {
    public final int smNum;
    public int regX;
    public int regY;
    public int regPC;
    public int osrValue;
    public int osrFillLevel;
    public int isrValue;
    public int isrFillLevel;
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
    public int pullThresh;

    public GPIO.Bit jmpPin()
    {
      return gpio.getBit(jmpPin);
    }

    public boolean osrEmpty()
    {
      return osrFillLevel == 0;
    }

    private Status()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    public Status(final int smNum)
    {
      this.smNum = smNum;
      regX = 0;
      regY = 0;
      regPC = 0;
      osrValue = 0;
      osrFillLevel = 0;
      isrValue = 0;
      isrFillLevel = 0;
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
      pullThresh = 0;
    }
  }

  private SM()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public SM(final int smNum, final GPIO gpio, final Memory memory)
  {
    if (gpio == null) throw new NullPointerException("gpio");
    if (memory == null) throw new NullPointerException("memory");
    this.gpio = gpio;
    this.memory = memory;
    status = new Status(smNum);
    decoder = new Decoder(memory, status);
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

  public void setOutShiftDir(final PIO.ShiftDir shiftDir)
  {
    if (shiftDir == null) { throw new NullPointerException("shiftDir"); }
    status.outShiftDir = shiftDir;
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

  public void execute() throws Decoder.DecodeException
  {
    final short word = fetch();
    final Instruction instruction = decoder.decode(word);
    instruction.execute();
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
