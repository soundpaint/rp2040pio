/*
 * @(#)SMConfig.java 1.00 21/02/06
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
package org.soundpaint.rp2040pio.sdk;

import org.soundpaint.rp2040pio.Constants;

/**
 * This class is for compatibility with the Raspberry Pi Pico SDK.
 * It represents C struct pio_sm_config and all functions that
 * manipulate this struct.
 */
public class SMConfig
{
  public enum FIFOJoin {
    NONE, TX, RX;
  };

  public enum MoveStatusType {
    STATUS_TX_LESSTHAN, STATUS_RX_LESSTHAN;
  };

  private int clkDiv;
  private int execCtrl;
  private int shiftCtrl;
  private int pinCtrl;

  private SMConfig()
  {
    reset();
  }

  private void reset()
  {
    /*
     * See values in "Reset" column of tables 392, 393, 394, 397 of
     * RP2040 datasheet, Sect. 3.7.
     */
    clkDiv = 0x00010000;
    execCtrl = 0x0001f000;
    shiftCtrl = 0x000c0000;
    pinCtrl = 0x14000000;
  }

  public int getClkDiv() { return clkDiv; }
  public int getExecCtrl() { return execCtrl; }
  public int getShiftCtrl() { return shiftCtrl; }
  public int getPinCtrl() { return pinCtrl; }

  // ---- Functions for compatibility with the Pico SDK, SM Config Group ----

  public void setOutPins(final int outBase, final int outCount)
  {
    if (outBase < 0) {
      throw new IllegalArgumentException("outBase < 0: " + outBase);
    }
    if (outBase > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("outBase > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         outBase);
    }
    if (outCount < 0) {
      throw new IllegalArgumentException("outCount < 0: " + outCount);
    }
    if (outCount > Constants.GPIO_NUM) {
      throw new IllegalArgumentException("outCount > " +
                                         Constants.GPIO_NUM + ": " + outCount);
    }
    pinCtrl &= ~(Constants.SM0_PINCTRL_OUT_COUNT_BITS |
                 Constants.SM0_PINCTRL_OUT_BASE_BITS);
    pinCtrl |= outCount << Constants.SM0_PINCTRL_OUT_COUNT_LSB;
    pinCtrl |= outBase << Constants.SM0_PINCTRL_OUT_BASE_LSB;
  }

  public void setSetPins(final int setBase, final int setCount)
  {
    if (setBase < 0) {
      throw new IllegalArgumentException("setBase < 0: " + setBase);
    }
    if (setBase > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("setBase > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         setBase);
    }
    if (setCount < 0) {
      throw new IllegalArgumentException("setCount < 0: " + setCount);
    }
    if (setCount > 5) {
      throw new IllegalArgumentException("setCount > 5: " + setCount);
    }
    pinCtrl &= ~(Constants.SM0_PINCTRL_SET_COUNT_BITS |
                 Constants.SM0_PINCTRL_SET_BASE_BITS);
    pinCtrl |= setCount << Constants.SM0_PINCTRL_SET_COUNT_LSB;
    pinCtrl |= setBase << Constants.SM0_PINCTRL_SET_BASE_LSB;
  }

  public void setInPins(final int inBase)
  {
    if (inBase < 0) {
      throw new IllegalArgumentException("inBase < 0: " + inBase);
    }
    if (inBase > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("inBase > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         inBase);
    }
    pinCtrl &= ~Constants.SM0_PINCTRL_IN_BASE_BITS;
    pinCtrl |= inBase << Constants.SM0_PINCTRL_IN_BASE_LSB;
  }

  public void setSideSetPins(final int sideSetBase)
  {
    if (sideSetBase < 0) {
      throw new IllegalArgumentException("sideSetBase < 0: " + sideSetBase);
    }
    if (sideSetBase > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("sideSetBase > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         sideSetBase);
    }
    pinCtrl &= ~Constants.SM0_PINCTRL_SIDESET_BASE_BITS;
    pinCtrl |= sideSetBase << Constants.SM0_PINCTRL_SIDESET_BASE_LSB;
  }

  public void setSideSet(final int bitCount,
                         final boolean optional, final boolean pinDirs)
  {
    if (bitCount < 0) {
      throw new IllegalArgumentException("bitCount < 0: " + bitCount);
    }
    if (bitCount > 7) {
      throw new IllegalArgumentException("bitCount > 7: " + bitCount);
    }
    pinCtrl &= 0x1fffffff;
    pinCtrl |= bitCount << 29; // PINCTRL_SIDESET_COUNT
    execCtrl &= 0x9fffffff;
    execCtrl |= (optional ? 1 : 0) << 30; // EXECCTRL_SIDE_EN
    execCtrl |= (pinDirs ? 1 : 0) << 29; // EXECCTRL_SIDE_PINDIR
  }

  public void setClkDiv(final float div)
  {
    if (div < 0.0f) {
      throw new IllegalArgumentException("div < 0: " + div);
    }
    if (div >= 65536.0f) {
      throw new IllegalArgumentException("div >= 65536: " + div);
    }
    final int divInt = (int)div;
    final int divFrac = (int)((div - divInt) * 256.0);
    setClkDivIntFrac(divInt, divFrac);
  }

  public void setClkDivIntFrac(final int divInt, final int divFrac)
  {
    if (divInt < 0) {
      throw new IllegalArgumentException("div int < 0: " + divInt);
    }
    if (divInt > 65535) {
      throw new IllegalArgumentException("div int > 65535: " + divInt);
    }
    if (divFrac < 0) {
      throw new IllegalArgumentException("div int < 0: " + divFrac);
    }
    if (divFrac > 255) {
      throw new IllegalArgumentException("div int > 255: " + divFrac);
    }
    clkDiv &= 0x000000ff;
    clkDiv |= divInt << 16; // CLKDIV_INT
    clkDiv |= divFrac << 8; // CLKDIV_FRAC
  }

  public void setWrap(final int wrapTarget, final int wrap)
  {
    if (wrapTarget < 0) {
      throw new IllegalArgumentException("wrap target < 0: " + wrapTarget);
    }
    if (wrapTarget > Constants.MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("wrap target > " +
                                         (Constants.MEMORY_SIZE - 1) + ": " +
                                         wrapTarget);
    }
    if (wrap < 0) {
      throw new IllegalArgumentException("wrap < 0: " + wrap);
    }
    if (wrap > Constants.MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("wrap > " +
                                         (Constants.MEMORY_SIZE - 1) + ": " +
                                         wrap);
    }
    execCtrl &= ~(Constants.SM0_EXECCTRL_WRAP_TOP_BITS |
                  Constants.SM0_EXECCTRL_WRAP_BOTTOM_BITS);
    execCtrl |= wrap << Constants.SM0_EXECCTRL_WRAP_TOP_LSB;
    execCtrl |= wrapTarget << Constants.SM0_EXECCTRL_WRAP_BOTTOM_LSB;
  }

  public void setJmpPin(final int pin)
  {
    if (pin < 0) {
      throw new IllegalArgumentException("jmp pin < 0: " + pin);
    }
    if (pin > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("jmp pin > " +
                                         (Constants.GPIO_NUM - 1) + ": " + pin);
    }
    execCtrl &= 0xe0ffffff;
    execCtrl |= pin << 24; // EXECCTRL_JMP_PIN
  }

  public void setInShift(final boolean shiftRight,
                         final boolean autoPush,
                         final int pushThreshold)
  {
    if (pushThreshold < 0) {
      throw new IllegalArgumentException("push threshold < 0: " +
                                         pushThreshold);
    }
    if (pushThreshold > 31) {
      throw new IllegalArgumentException("push threshold > 31: " +
                                         pushThreshold);
    }
    shiftCtrl &= 0xfe0affff;
    shiftCtrl |= (shiftRight ? 1 : 0) << 18; // SHIFTCTRL_IN_SHIFTDIR
    shiftCtrl |= (autoPush ? 1 : 0) << 16; // SHIFTCTRL_AUTOPUSH
    shiftCtrl |= pushThreshold << 20; // SHIFTCTRL_PUSH_THRESH
  }

  public void setOutShift(final boolean shiftRight,
                          final boolean autoPull,
                          final int pullThreshold)
  {
    if (pullThreshold < 0) {
      throw new IllegalArgumentException("pull threshold < 0: " +
                                         pullThreshold);
    }
    if (pullThreshold > 31) {
      throw new IllegalArgumentException("pull threshold > 31: " +
                                         pullThreshold);
    }
    shiftCtrl &= 0xc1f5ffff;
    shiftCtrl |= (shiftRight ? 1 : 0) << 19; // SHIFTCTRL_OUT_SHIFTDIR
    shiftCtrl |= (autoPull ? 1 : 0) << 17; // SHIFTCTRL_AUTOPULL
    shiftCtrl |= pullThreshold << 25; // SHIFTCTRL_PULL_THRESH
  }

  public void setFIFOJoin(final FIFOJoin join)
  {
    shiftCtrl &= 0x3fffffff;
    shiftCtrl |= join.ordinal() << 30; // SHIFTCTRL_FJOIN_RX/TX
  }

  public void setOutSpecial(final boolean sticky, final boolean hasEnablePin,
                            final int enablePinIndex)
  {
    if (enablePinIndex < 0) {
      throw new IllegalArgumentException("enable pin index < 0: " +
                                         enablePinIndex);
    }
    if (enablePinIndex > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("enable pin index > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         enablePinIndex);
    }
    execCtrl &= 0xff01ffff;
    execCtrl |= (sticky ? 1 : 0) << 17; // EXECCTRL_OUT_STICKY
    execCtrl |= (hasEnablePin ? 1 : 0) << 18; // EXECCTRL_INLINE_OUT_EN
    execCtrl |= enablePinIndex << 19; // EXECCTRL_OUT_EN_SEL
  }

  public void setMoveStatus(final MoveStatusType statusSel,
                            final int statusN)
  {
    if (statusN < 0) {
      throw new IllegalArgumentException("status n < 0: " +
                                         statusN);
    }
    if (statusN > 15) {
      throw new IllegalArgumentException("status n > 15: " +
                                         statusN);
    }
    execCtrl &= 0xffffffe0;
    execCtrl |= statusSel.ordinal() << 4; // EXECCTRL_STATUS_SEL
    execCtrl |= statusN; // EXECCTRL_STATUS_N
  }

  public static SMConfig getDefault()
  {
    return new SMConfig();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
