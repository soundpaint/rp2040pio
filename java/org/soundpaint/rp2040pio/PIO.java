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

  private final List<Decoder.DecodeException> caughtExceptions;
  private final GPIO gpio;
  private final Memory memory;
  private final IRQ irq;
  private final SM[] sms;

  public enum PinDir {
    GPIO_LEVELS,
    GPIO_DIRECTIONS
  };

  public enum ShiftDir {
    SHIFT_LEFT,
    SHIFT_RIGHT
  };

  private PIO()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public PIO(final Clock clock)
  {
    caughtExceptions = new ArrayList<Decoder.DecodeException>();
    gpio = new GPIO();
    memory = new Memory();
    irq = new IRQ();
    sms = new SM[SM_COUNT];
    for (int smNum = 0; smNum < SM_COUNT; smNum++) {
      sms[smNum] = new SM(smNum, clock, gpio, memory, irq);
    }
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
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
