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
public class PIO
{
  private static final int SM_COUNT = 4;

  private final int index;
  private final List<Decoder.DecodeException> caughtExceptions;
  private final GPIO gpio;
  private final Memory memory;
  private final IRQ irq;
  private final SM[] sms;

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
    caughtExceptions = new ArrayList<Decoder.DecodeException>();
    gpio = new GPIO();
    memory = new Memory();
    irq = new IRQ();
    sms = new SM[SM_COUNT];
    for (int smNum = 0; smNum < SM_COUNT; smNum++) {
      sms[smNum] = new SM(smNum, clock, gpio, memory, irq);
    }
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

  public void clockRaisingEdge() throws Decoder.MultiDecodeException
  {
    caughtExceptions.clear();
    for (final SM sm : sms) {
      try {
        sm.clockRaisingEdge();
      } catch (final Decoder.DecodeException e) {
        caughtExceptions.add(e);
      }
    }
    if (caughtExceptions.size() > 0) {
      throw new Decoder.MultiDecodeException(List.copyOf(caughtExceptions));
    }
  }

  // -------- Functions for compatibility with the Pico SDK --------

  public static final MasterClock MASTER_CLOCK =
    MasterClock.getDefaultInstance();
  public static final PIO PIO0 = new PIO(0, MASTER_CLOCK);
  public static final PIO PIO1 = new PIO(1, MASTER_CLOCK);

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
   * Add here those SDK functions that are still missing.
   * Next missing function is "pio_get_dreq()".
   * See:
   * https://raspberrypi.github.io/pico-sdk-doxygen/group__hardware__pio.html
   */

  public void removeProgram(final Program program, final int loadedOffset)
  {
    memory.removeProgram(program, loadedOffset);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
