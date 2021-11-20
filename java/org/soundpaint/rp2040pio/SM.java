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
        (sm) -> sm.status.regPINCTRL_OUT_COUNT);

    private final Function<SM, Integer> baseGetter;
    private final Function<SM, Integer> countGetter;

    private IOMapping(final Function<SM, Integer> baseGetter,
                      final Function<SM, Integer> countGetter)
    {
      this.baseGetter = baseGetter;
      this.countGetter = countGetter;
    }

    public void collatePins(final SM sm, final int data)
    {
      sm.status.
        collatePins(data, baseGetter.apply(sm), countGetter.apply(sm), false);
    }

    public void collatePinDirs(final SM sm, final int data)
    {
      sm.status.
        collatePinDirs(data, baseGetter.apply(sm), countGetter.apply(sm));
    }
  };

  public class Status
  {
    public Instruction instruction;
    public int origin;
    public Instruction.ResultState resultState;
    public boolean processing;
    public boolean smEnabled;
    public boolean clockEnabled;
    public boolean isDelayCycle;
    public int collateSideSetPins;
    public int collateSideSetBase;
    public int collateSideSetCount;
    public int outStickyPins;
    public int outStickyBase;
    public int outStickyCount;
    public boolean havePendingOutOrSetPins;
    public int regX;
    public int regY;
    public int isrValue;
    public int isrShiftCount;
    public int osrValue;
    public int osrShiftCount;
    public int totalDelay;
    public int pendingDelay;
    public int pendingForcedInstruction;
    public boolean isForcedInstruction;
    public int pendingExecdInstruction;
    public int regADDR; // bits 0…4 of SMx_ADDR
    public boolean regEXECCTRL_SIDE_EN; // bit 30 of SMx_EXECCTRL
    public PIO.PinDir regEXECCTRL_SIDE_PINDIR; // bit 29 of SMx_EXECCTRL
    public int regEXECCTRL_JMP_PIN; // bits 24…28 of SMx_EXECCTRL
    public int regEXECCTRL_OUT_EN_SEL; // bits 19…23 of SMx_EXECCTRL
    public boolean regEXECCTRL_INLINE_OUT_EN; // bit 18 of SMx_EXECCTRL
    public boolean regEXECCTRL_OUT_STICKY; // bit 17 of SMx_EXECCTRL
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
      origin = INSTR_ORIGIN_UNKNOWN;
      resultState = null;
      processing = false;
      smEnabled = false;
      clockEnabled = false;
      isDelayCycle = false;
      collateSideSetPins = 0;
      collateSideSetBase = 0;
      collateSideSetCount = 0;
      outStickyPins = 0;
      outStickyBase = 0;
      outStickyCount = 0;
      havePendingOutOrSetPins = false;
      regX = 0;
      regY = 0;
      isrValue = 0;
      isrShiftCount = 0;
      osrValue = 0;
      osrShiftCount = 32;
      totalDelay = 0;
      pendingDelay = 0;
      pendingForcedInstruction = -1;
      isForcedInstruction = false;
      pendingExecdInstruction = -1;
      regADDR = 0;
      regEXECCTRL_STATUS_SEL = false;
      regEXECCTRL_STATUS_N = 0;
      regEXECCTRL_SIDE_EN = false;
      regEXECCTRL_SIDE_PINDIR = PIO.PinDir.GPIO_LEVELS;
      regEXECCTRL_JMP_PIN = 0;
      regEXECCTRL_OUT_EN_SEL = 0;
      regEXECCTRL_INLINE_OUT_EN = false;
      regEXECCTRL_OUT_STICKY = false;
      regEXECCTRL_WRAP_TOP = MEMORY_SIZE - 1;
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

    public void restart()
    {
      /*
       * See RP2040 datasheet, Table 378: CTRL Register, SM_RESTART:
       *
       * Specifically, the following are cleared: input and output
       * shift counters; the contents of the input shift register; the
       * delay counter; the waiting-on-IRQ state; any stalled
       * instruction written to SMx_INSTR or run by OUT/MOV EXEC; any
       * pin write left asserted due to OUT_STICKY.
       */
      isrShiftCount = 0;
      osrShiftCount = 32;
      isrValue = 0;
      isDelayCycle = false;
      outStickyPins = 0;
      outStickyBase = 0;
      outStickyCount = 0;
      havePendingOutOrSetPins = false;
      totalDelay = 0;
      pendingDelay = 0;
      pendingForcedInstruction = -1;
      isForcedInstruction = false;
      pendingExecdInstruction = -1;
      regEXECCTRL_OUT_STICKY = false;
    }

    public Bit jmpPin()
    {
      /*
       * RP2040 datasheet 3.4.2. "JMP": "JMP PIN branches on the GPIO
       * … independently of the state machine's other input mapping."
       * => Return global GPIO's input to peripherals rather than the
       * local GPIO pin state of this state machine's PIO.
       */
      return pioGpio.getGPIO().getInToPeri(regEXECCTRL_JMP_PIN);
    }

    public void collatePins(final int pins, final int base, final int count,
                            final boolean isSideSetOperation)
    {
      if (isSideSetOperation) {
        collateSideSetPins = pins;
        collateSideSetBase = base;
        collateSideSetCount = count;
      } else {
        outStickyPins = pins;
        outStickyBase = base;
        outStickyCount = count;
        havePendingOutOrSetPins = true;
      }
    }

    private void flushCollatePins()
    {
      if (havePendingOutOrSetPins || regEXECCTRL_OUT_STICKY) {
        final boolean outEn =
          !regEXECCTRL_INLINE_OUT_EN ||
          (((outStickyPins >>> regEXECCTRL_OUT_EN_SEL) & 0x1) == 0x1);
        if (outEn) {
          pioGpio.collatePins(outStickyPins, outStickyBase, outStickyCount);
        }
        havePendingOutOrSetPins = false;
      }
      /*
       * RP2040 datasheet, Sect. 3.5.6. "GPIO Mapping": If side-set
       * overlaps with OUT/SET, side-set takes precedence.  => Perform
       * collatePins() for side-set as last step.
       */
      if (collateSideSetCount > 0) {
        pioGpio.collatePins(collateSideSetPins, collateSideSetBase,
                            collateSideSetCount);
        collateSideSetCount = 0;
      }
    }

    public void collatePinDirs(final int pins, final int base, final int count)
    {
      pioGpio.collatePinDirs(pins, base, count);
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

    public boolean isIsrCountBeyondThreshold()
    {
      return isrShiftCount >=
        (regSHIFTCTRL_PUSH_THRESH != 0 ? regSHIFTCTRL_PUSH_THRESH : 32);
    }

    public boolean isOsrCountBeyondThreshold()
    {
      return osrShiftCount >=
        (regSHIFTCTRL_PULL_THRESH != 0 ? regSHIFTCTRL_PULL_THRESH : 32);
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
      this.totalDelay = delay;
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
    fifo = new FIFO(num, irq);
    pll = new PLL(console);
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

  public int outEnSel()
  {
    return status.regEXECCTRL_OUT_EN_SEL;
  }

  public boolean inlineOutEn()
  {
    return status.regEXECCTRL_INLINE_OUT_EN;
  }

  public boolean outSticky()
  {
    return status.regEXECCTRL_OUT_STICKY;
  }

  public void setEXECCTRL(final int execctrl, final int mask, final boolean xor)
  {
    setEXECCTRL(Constants.hwSetBits(getEXECCTRL(), execctrl, mask, xor));
  }

  private void setEXECCTRL(final int execctrl)
  {
    status.regEXECCTRL_SIDE_EN =
      ((execctrl & SM0_EXECCTRL_SIDE_EN_BITS) >>>
       SM0_EXECCTRL_SIDE_EN_LSB) != 0x0;
    status.regEXECCTRL_SIDE_PINDIR =
      PIO.PinDir.fromValue((execctrl & SM0_EXECCTRL_SIDE_PINDIR_BITS) >>>
                           SM0_EXECCTRL_SIDE_PINDIR_LSB);
    status.regEXECCTRL_JMP_PIN =
      (execctrl & SM0_EXECCTRL_JMP_PIN_BITS) >>> SM0_EXECCTRL_JMP_PIN_LSB;
    status.regEXECCTRL_OUT_EN_SEL =
      (execctrl & SM0_EXECCTRL_OUT_EN_SEL_BITS) >>> SM0_EXECCTRL_OUT_EN_SEL_LSB;
    status.regEXECCTRL_INLINE_OUT_EN =
      ((execctrl & SM0_EXECCTRL_INLINE_OUT_EN_BITS) >>>
       SM0_EXECCTRL_INLINE_OUT_EN_LSB) != 0x0;
    status.regEXECCTRL_OUT_STICKY =
      ((execctrl & SM0_EXECCTRL_OUT_STICKY_BITS) >>>
       SM0_EXECCTRL_OUT_STICKY_LSB) != 0x0;
    status.regEXECCTRL_WRAP_TOP =
      (execctrl & SM0_EXECCTRL_WRAP_TOP_BITS) >>> SM0_EXECCTRL_WRAP_TOP_LSB;
    status.regEXECCTRL_WRAP_BOTTOM =
      (execctrl & SM0_EXECCTRL_WRAP_BOTTOM_BITS) >>>
      SM0_EXECCTRL_WRAP_BOTTOM_LSB;
    status.regEXECCTRL_STATUS_SEL =
      ((execctrl & SM0_EXECCTRL_STATUS_SEL_BITS) >>>
       SM0_EXECCTRL_STATUS_SEL_LSB) != 0x0;
    status.regEXECCTRL_STATUS_N =
      (execctrl & SM0_EXECCTRL_STATUS_N_BITS) >>> SM0_EXECCTRL_STATUS_N_LSB;
  }

  public int getEXECCTRL()
  {
    return
      (isExecStalled() ? 1 : 0) << SM0_EXECCTRL_EXEC_STALLED_LSB |
      (status.regEXECCTRL_SIDE_EN ? 1 : 0) << SM0_EXECCTRL_SIDE_EN_LSB |
      status.regEXECCTRL_SIDE_PINDIR.getValue() <<
      SM0_EXECCTRL_SIDE_PINDIR_LSB |
      status.regEXECCTRL_JMP_PIN << SM0_EXECCTRL_JMP_PIN_LSB |
      status.regEXECCTRL_OUT_EN_SEL << SM0_EXECCTRL_OUT_EN_SEL_LSB |
      (status.regEXECCTRL_INLINE_OUT_EN ? 1 : 0) <<
      SM0_EXECCTRL_INLINE_OUT_EN_LSB |
      (status.regEXECCTRL_OUT_STICKY ? 1 : 0) << SM0_EXECCTRL_OUT_STICKY_LSB |
      status.regEXECCTRL_WRAP_TOP << SM0_EXECCTRL_WRAP_TOP_LSB |
      status.regEXECCTRL_WRAP_BOTTOM << SM0_EXECCTRL_WRAP_BOTTOM_LSB |
      (status.regEXECCTRL_STATUS_SEL ? 1 : 0) << SM0_EXECCTRL_STATUS_SEL_LSB |
      status.regEXECCTRL_STATUS_N << SM0_EXECCTRL_STATUS_N_LSB;
  }

  public void setSHIFTCTRL(final int shiftctrl, final int mask,
                           final boolean xor)
  {
    setSHIFTCTRL(Constants.hwSetBits(getSHIFTCTRL(), shiftctrl, mask, xor));
  }

  private void setSHIFTCTRL(final int shiftctrl)
  {
    fifo.setJoinRX(((shiftctrl & SM0_SHIFTCTRL_FJOIN_RX_BITS) >>>
                    SM0_SHIFTCTRL_FJOIN_RX_LSB) != 0x0);
    fifo.setJoinTX(((shiftctrl & SM0_SHIFTCTRL_FJOIN_TX_BITS) >>>
                    SM0_SHIFTCTRL_FJOIN_TX_LSB) != 0x0);
    status.regSHIFTCTRL_PULL_THRESH =
      (shiftctrl & SM0_SHIFTCTRL_PULL_THRESH_BITS) >>>
      SM0_SHIFTCTRL_PULL_THRESH_LSB;
    status.regSHIFTCTRL_PUSH_THRESH =
      (shiftctrl & SM0_SHIFTCTRL_PUSH_THRESH_BITS) >>>
      SM0_SHIFTCTRL_PUSH_THRESH_LSB;
    status.regSHIFTCTRL_OUT_SHIFTDIR =
      PIO.ShiftDir.fromValue((shiftctrl & SM0_SHIFTCTRL_OUT_SHIFTDIR_BITS) >>>
                             SM0_SHIFTCTRL_OUT_SHIFTDIR_LSB);
    status.regSHIFTCTRL_IN_SHIFTDIR =
      PIO.ShiftDir.fromValue((shiftctrl & SM0_SHIFTCTRL_IN_SHIFTDIR_BITS) >>>
                             SM0_SHIFTCTRL_IN_SHIFTDIR_LSB);
    status.regSHIFTCTRL_AUTOPULL =
      ((shiftctrl & SM0_SHIFTCTRL_AUTOPULL_BITS) >>>
       SM0_SHIFTCTRL_AUTOPULL_LSB) != 0x0;
    status.regSHIFTCTRL_AUTOPUSH =
      ((shiftctrl & SM0_SHIFTCTRL_AUTOPUSH_BITS) >>>
       SM0_SHIFTCTRL_AUTOPUSH_LSB) != 0x0;
  }

  public int getSHIFTCTRL()
  {
    return
      (fifo.getJoinRX() ? 1 : 0) << SM0_SHIFTCTRL_FJOIN_RX_LSB |
      (fifo.getJoinTX() ? 1 : 0) << SM0_SHIFTCTRL_FJOIN_TX_LSB |
      status.regSHIFTCTRL_PULL_THRESH << SM0_SHIFTCTRL_PULL_THRESH_LSB |
      status.regSHIFTCTRL_PUSH_THRESH << SM0_SHIFTCTRL_PUSH_THRESH_LSB |
      status.regSHIFTCTRL_OUT_SHIFTDIR.getValue() <<
      SM0_SHIFTCTRL_OUT_SHIFTDIR_LSB |
      status.regSHIFTCTRL_IN_SHIFTDIR.getValue() <<
      SM0_SHIFTCTRL_IN_SHIFTDIR_LSB |
      (status.regSHIFTCTRL_AUTOPULL ? 1 : 0) << SM0_SHIFTCTRL_AUTOPULL_LSB |
      (status.regSHIFTCTRL_AUTOPUSH ? 1 : 0) << SM0_SHIFTCTRL_AUTOPUSH_LSB;
  }

  public void setPINCTRL(final int pinctrl, final int mask, final boolean xor)
  {
    setPINCTRL(Constants.hwSetBits(getPINCTRL(), pinctrl, mask, xor));
  }

  private void setPINCTRL(final int pinctrl)
  {
    status.regPINCTRL_SIDESET_COUNT =
      (pinctrl & SM0_PINCTRL_SIDESET_COUNT_BITS) >>>
      SM0_PINCTRL_SIDESET_COUNT_LSB;
    status.regPINCTRL_SET_COUNT =
      (pinctrl & SM0_PINCTRL_SET_COUNT_BITS) >>> SM0_PINCTRL_SET_COUNT_LSB;
    status.regPINCTRL_OUT_COUNT =
      (pinctrl & SM0_PINCTRL_OUT_COUNT_BITS) >>> SM0_PINCTRL_OUT_COUNT_LSB;
    status.regPINCTRL_IN_BASE =
      (pinctrl & SM0_PINCTRL_IN_BASE_BITS) >>> SM0_PINCTRL_IN_BASE_LSB;
    status.regPINCTRL_SIDESET_BASE =
      (pinctrl & SM0_PINCTRL_SIDESET_BASE_BITS) >>>
      SM0_PINCTRL_SIDESET_BASE_LSB;
    status.regPINCTRL_SET_BASE =
      (pinctrl & SM0_PINCTRL_SET_BASE_BITS) >>> SM0_PINCTRL_SET_BASE_LSB;
    status.regPINCTRL_OUT_BASE =
      (pinctrl & SM0_PINCTRL_OUT_BASE_BITS) >>> SM0_PINCTRL_OUT_BASE_LSB;
  }

  public int getPINCTRL()
  {
    return
      status.regPINCTRL_SIDESET_COUNT << SM0_PINCTRL_SIDESET_COUNT_LSB |
      status.regPINCTRL_SET_COUNT << SM0_PINCTRL_SET_COUNT_LSB |
      status.regPINCTRL_OUT_COUNT << SM0_PINCTRL_OUT_COUNT_LSB |
      status.regPINCTRL_IN_BASE << SM0_PINCTRL_IN_BASE_LSB |
      status.regPINCTRL_SIDESET_BASE << SM0_PINCTRL_SIDESET_BASE_LSB |
      status.regPINCTRL_SET_BASE << SM0_PINCTRL_SET_BASE_LSB |
      status.regPINCTRL_OUT_BASE << SM0_PINCTRL_OUT_BASE_LSB;
  }

  public void clockRisingEdge(final boolean smEnabled, final long wallClock)
  {
    status.smEnabled = smEnabled;
    if (smEnabled) {
      pll.risingEdge(wallClock);
      status.clockEnabled = pll.getClockEnable();
    } else {
      status.clockEnabled = false;
    }
    /*
     * Sect. 3.5.7.: ... instructions written to the INSTR register
     * ... execute immediately, ignoring the state machine clock
     * divider.
     */
    status.processing =
      status.clockEnabled || (status.pendingForcedInstruction >= 0);
    if (status.processing) {
      try {
        if ((status.pendingForcedInstruction >= 0) ||
            (status.pendingExecdInstruction >= 0) ||
            !status.consumePendingDelay()) {
          status.isDelayCycle = false;
          fetchAndDecode();
        } else {
          status.isDelayCycle = true;
        }
      } catch (final Decoder.DecodeException e) {
        console.println(e.getMessage());
      }
    } else {
      status.origin = INSTR_ORIGIN_UNKNOWN;
    }
  }

  public void clockFallingEdge(final long wallClock)
  {
    if (status.smEnabled) {
      pll.fallingEdge(wallClock);
    }
    if (status.processing) {
      try {
        execute();
      } catch (final RuntimeException e) {
        e.printStackTrace(console);
        console.printf("internal error: %s%n", e.getMessage());
      }
    }
  }

  public void restart()
  {
    status.restart();
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
    final boolean isrCountBeyondThreshold = status.isIsrCountBeyondThreshold();
    if (!ifFull || isrCountBeyondThreshold) {
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
    final boolean osrCountBeyondThreshold = status.isOsrCountBeyondThreshold();
    if (!ifEmpty || osrCountBeyondThreshold) {
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

  public static int saturate(final int base, final int increment,
                             final int limit)
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

  public int getISRShiftCount() { return status.isrShiftCount; }

  public void setISRShiftCount(final int value)
  {
    status.isrShiftCount = value;
  }

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

  public int getOSRShiftCount() { return status.osrShiftCount; }


  public void setOSRShiftCount(final int value)
  {
    status.osrShiftCount = value;
  }

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
    }
  }

  public void putRXF(final int data)
  {
    synchronized(fifo) {
      fifo.rxPush(data, false);
    }
  }

  public int get()
  {
    synchronized(fifo) {
      final int value = fifo.rxDMARead();
      return value;
    }
  }

  public int getTXF()
  {
    synchronized(fifo) {
      final int value = fifo.txPull(false);
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
    // sync: don't change regADDR while PC is updated
    synchronized(memory.FETCH_LOCK) {
      status.regADDR = value;
    }
  }

  public void setPC(final int value, final int mask, final boolean xor)
  {
    final int pc = Constants.hwSetBits(status.regADDR, value, mask, xor);
    setPC(pc & (MEMORY_SIZE - 1));
  }

  private void updatePC()
  {
    synchronized(memory.FETCH_LOCK) {
      if (status.regADDR == status.regEXECCTRL_WRAP_TOP) {
        status.regADDR = status.regEXECCTRL_WRAP_BOTTOM;
      } else {
        status.regADDR = (status.regADDR + 1) & (MEMORY_SIZE - 1);
      }
      if (((status.regBREAKPOINTS >>> status.regADDR) & 0x1) != 0x0) {
        masterClock.setMode(MasterClock.Mode.SINGLE_STEP);
      }
    }
  }

  private short fetch()
  {
    final int pendingForcedInstruction = status.pendingForcedInstruction;
    if (pendingForcedInstruction >= 0) {
      status.pendingForcedInstruction = -1;
      status.isForcedInstruction = true;
      status.origin = INSTR_ORIGIN_FORCED;
      return (short)pendingForcedInstruction;
    }
    final int pendingExecdInstruction = status.pendingExecdInstruction;
    if (pendingExecdInstruction >= 0) {
      status.pendingExecdInstruction = -1;
      status.origin = INSTR_ORIGIN_EXECD;
      return (short)pendingExecdInstruction;
    }
    // notify blocking methods that condition may have changed
    memory.FETCH_LOCK.notifyAll();
    status.origin = status.regADDR & (MEMORY_SIZE - 1);
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

  public int getPendingForcedInstruction()
  {
    return status.pendingForcedInstruction;
  }

  public int getFORCED_INSTR()
  {
    if (status.pendingForcedInstruction >= 0) {
      return 0x00010000 | (status.pendingForcedInstruction & 0x0000ffff);
    }
    return 0x0;
  }

  public void clearPendingForcedInstruction()
  {
    status.pendingForcedInstruction = -1;
  }

  public int getPendingExecdInstruction()
  {
    return status.pendingExecdInstruction;
  }

  public int getEXECD_INSTR()
  {
    if (status.pendingExecdInstruction >= 0) {
      return 0x00010000 | (status.pendingExecdInstruction & 0x0000ffff);
    }
    return 0x0;
  }

  public void clearPendingExecdInstruction()
  {
    status.pendingExecdInstruction = -1;
  }

  public void forceInstruction(final int instruction)
  {
    if (instruction < 0) {
      throw new IllegalArgumentException("instruction < 0: " + instruction);
    }
    if (instruction > 65535) {
      throw new IllegalArgumentException("instruction > 65535: " +
                                         instruction);
    }
    final boolean discarded;
    synchronized(memory.FETCH_LOCK) {
      discarded = status.pendingForcedInstruction >= 0;
      status.pendingForcedInstruction = instruction;
    }
    if (discarded) {
      console.println("WARNING: " +
                      "discarding already pending forced instruction");
    }
  }

  public void execInstruction(final int instruction)
  {
    synchronized(memory.FETCH_LOCK) {
      if (status.pendingExecdInstruction >= 0) {
        throw new InternalError("already have pending EXEC instruction");
      }
      if (instruction < 0) {
        throw new IllegalArgumentException("instruction < 0: " + instruction);
      }
      if (instruction > 65535) {
        throw new IllegalArgumentException("instruction > 65535: " +
                                           instruction);
      }
      status.pendingExecdInstruction = instruction;
    }
  }

  public boolean isExecStalled()
  {
    synchronized(memory.FETCH_LOCK) {
      return (status.pendingForcedInstruction >= 0) && isStalled();
    }
  }

  public int getINSTR_ORIGIN()
  {
    final int origin = status.origin;
    final int mode = origin < 0 ? origin & 0x3 : INSTR_ORIGIN_MEMORY;
    final int address = origin < 0 ? 0 : origin & (MEMORY_SIZE - 1);
    return (mode << 5) | address;
  }

  /**
   * Tracking the total delay of the latest instruction with delay in
   * effect is, strictly speaking, not necessary for the emulation,
   * but highly useful as additional information for client
   * applications, e.g. when displaying the percentage of already
   * passed delay.
   */
  public int getTotalDelay()
  {
    return status.totalDelay;
  }

  public int getPendingDelay()
  {
    return status.pendingDelay;
  }

  private void fetchAndDecode() throws Decoder.DecodeException
  {
    synchronized(memory.FETCH_LOCK) {
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

  private void executeInstruction()
  {
    if (status.isDelayCycle && !status.isForcedInstruction) {
      return;
    }
    final Instruction instruction = status.instruction;
    if (instruction == null) {
      throw new InternalError("seems emulator started with falling " +
                              "clock edge:  can not execute instruction " +
                              "before decode");
    }
    status.resultState = instruction.execute(this);
    if (status.resultState == Instruction.ResultState.COMPLETE) {
      /*
       * Sect. 3.4.2.2.: "Delay cycles … take place after … the program
       * counter is updated" (though this specifically refers to JMP
       * instruction). => Update PC immediately, before executing
       * delay.
       */
      updatePC();
    }
    /*
     * Sect. 3.5.7.: "Delay cycles are ignored on instructions written
     * to the INSTR register."
     */
    if (!status.isForcedInstruction) {
      if (status.resultState != Instruction.ResultState.STALL) {
        status.setPendingDelay(instruction.getDelay());
      }
    } else {
      status.isForcedInstruction = false;
    }
  }

  private void executeAsyncAutoPull()
  {
    /*
     * Cp. pseudocode sequence for non-"OUT" cycles in RP2040
     * datasheet, Sect. 3.5.4.2. "Autopull Details".
     */
    final boolean osrCountBeyondThreshold = status.isOsrCountBeyondThreshold();
    if (osrCountBeyondThreshold) {
      final boolean txFifoEmpty = fifo.fstatTxEmpty();
      /*
       * TODO: Check: Possible race condition between above
       * fifo.fstatTxEmpty() and below fifo.txPull()?
       */
      if (!txFifoEmpty) {
        status.osrValue = fifo.txPull(false);
        status.osrShiftCount = 0;
      }
    }
  }

  private void execute()
  {
    executeInstruction();
    /*
     * TODO: Clarify when the asynchronous fill mechanism is enabled.
     * On each master clock cycle?  Or only when the state machine is
     * enabled?  Or even only if clock enabled is true
     * (i.e. considering clock divider)?  Currently, we check for
     * clock enable.
     */
    if (status.clockEnabled && status.regSHIFTCTRL_AUTOPULL) {
      if (!(status.instruction instanceof Instruction.Out)) {
        executeAsyncAutoPull();
      }
    }
    status.flushCollatePins();
  }

  public boolean isStalled()
  {
    return status.resultState == Instruction.ResultState.STALL;
  }

  public boolean isDelayCycle()
  {
    return !status.isForcedInstruction && status.isDelayCycle;
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
