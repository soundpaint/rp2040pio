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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Peripheral I/O Unit
 */
public class PIO implements Constants, Clock.TransitionListener
{
  private final int index;
  private final PrintStream console;
  private final MasterClock masterClock;
  private final GPIO gpio;
  private final PIOGPIO pioGpio;
  private final Memory memory;
  private final IRQ irq;
  private final SM[] sms;
  private int smEnabled; // bits 0…3 of CTRL_SM_ENABLE

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
  };

  private PIO()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public PIO(final int index, final PrintStream console,
             final MasterClock masterClock, final GPIO gpio)
  {
    if (index < 0) {
      throw new IllegalArgumentException("PIO index < 0: " + index);
    }
    if (index > 1) {
      throw new IllegalArgumentException("PIO index > 1: " + index);
    }
    Objects.requireNonNull(console);
    Objects.requireNonNull(masterClock);
    Objects.requireNonNull(gpio);
    this.index = index;
    this.console = console;
    this.masterClock = masterClock;
    this.gpio = gpio;
    masterClock.addTransitionListener(this);
    pioGpio = new PIOGPIO(gpio);
    memory = new Memory();
    irq = new IRQ();
    sms = new SM[SM_COUNT];
    for (int smNum = 0; smNum < SM_COUNT; smNum++) {
      sms[smNum] = new SM(smNum, console, masterClock, pioGpio, memory, irq);
    }
    smEnabled = 0x0;
  }

  public void reset()
  {
    pioGpio.reset();
    memory.reset();
    irq.reset();
    for (final SM sm : sms) sm.reset();
    smEnabled = 0x0;
  }

  public int getIndex()
  {
    return index;
  }

  public int getDBG_CFGINFO_IMEM_SIZE()
  {
    return MEMORY_SIZE;
  }

  public int getDBG_CFGINFO_SM_COUNT()
  {
    return SM_COUNT;
  }

  public int getDBG_CFGINFO_FIFO_DEPTH()
  {
    return FIFO_DEPTH;
  }

  public PrintStream getConsole()
  {
    return console;
  }

  public MasterClock getMasterClock()
  {
    return masterClock;
  }

  public GPIO getGPIO()
  {
    return gpio;
  }

  public PIOGPIO getPIOGPIO()
  {
    return pioGpio;
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

  public IRQ getIRQ()
  {
    return irq;
  }

  public int getSM_ENABLED()
  {
    return smEnabled;
  }

  public void setSM_ENABLED(final int smEnabled)
  {
    if (smEnabled < 0) {
      throw new IllegalArgumentException("SM_ENABLED < 0: " + smEnabled);
    }
    if (smEnabled > 15) {
      throw new IllegalArgumentException("SM_ENABLED > 15:" + smEnabled);
    }
    this.smEnabled = smEnabled;
  }

  public int getCtrl()
  {
    return getSM_ENABLED();
  }

  public void setCtrl(final int ctrl, final int mask)
  {
    synchronized(sms) {
      smEnabled = Constants.hwSetBits(smEnabled, ctrl, mask, false) & 0xf;
      for (int smNum = 0; smNum < SM_COUNT; smNum++) {
        final boolean clkDivRestart =
          ((ctrl >> (8 + smNum)) & 0x1) != 0x0 &&
          ((mask >> (8 + smNum)) & 0x1) != 0x0;
        final boolean smRestart =
          ((ctrl >> (4 + smNum)) & 0x1) != 0x0 &&
          ((mask >> (4 + smNum)) & 0x1) != 0x0;
        final SM sm = getSM(smNum);
        if (clkDivRestart) {
          sm.resetCLKDIV();
        }
        if (smRestart) {
          sm.restart();
        }
      }
    }
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

  private boolean smIsEnabled(final int smNum)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("smNum < 0: " + smNum);
    }
    if (smNum > SM_COUNT - 1) {
      throw new IllegalArgumentException("smNum > " +
                                         (SM_COUNT - 1) + ": " +
                                         smNum);
    }
    return (smEnabled & (0x1 << smNum)) != 0x0;
  }

  public Direction getDirection(final int gpio)
  {
    return pioGpio.getDirection(gpio);
  }

  public Bit getLevel(final int gpio)
  {
    return pioGpio.getLevel(gpio);
  }

  @Override
  public void risingEdge(final long wallClock)
  {
    synchronized(sms) {
      for (int smNum = 0; smNum < SM_COUNT; smNum++) {
        final SM sm = getSM(smNum);
        sm.clockRisingEdge(smIsEnabled(smNum), wallClock);
      }
    }
  }

  @Override
  public void fallingEdge(final long wallClock) {
    synchronized(sms) {
      for (int smNum = 0; smNum < SM_COUNT; smNum++) {
        final SM sm = getSM(smNum);
        sm.clockFallingEdge(wallClock);
      }
      pioGpio.applyCollatedWrites();
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
