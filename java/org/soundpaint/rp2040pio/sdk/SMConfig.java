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
public class SMConfig implements Constants
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
    if (outBase > GPIO_NUM - 1) {
      throw new IllegalArgumentException("outBase > " + (GPIO_NUM - 1) + ": " +
                                         outBase);
    }
    if (outCount < 0) {
      throw new IllegalArgumentException("outCount < 0: " + outCount);
    }
    if (outCount > GPIO_NUM) {
      throw new IllegalArgumentException("outCount > " + GPIO_NUM + ": " +
                                         outCount);
    }
    pinCtrl &= ~(SM0_PINCTRL_OUT_COUNT_BITS | SM0_PINCTRL_OUT_BASE_BITS);
    pinCtrl |= outCount << SM0_PINCTRL_OUT_COUNT_LSB;
    pinCtrl |= outBase << SM0_PINCTRL_OUT_BASE_LSB;
  }

  public void setSetPins(final int setBase, final int setCount)
  {
    if (setBase < 0) {
      throw new IllegalArgumentException("setBase < 0: " + setBase);
    }
    if (setBase > GPIO_NUM - 1) {
      throw new IllegalArgumentException("setBase > " + (GPIO_NUM - 1) + ": " +
                                         setBase);
    }
    if (setCount < 0) {
      throw new IllegalArgumentException("setCount < 0: " + setCount);
    }
    if (setCount > 5) {
      throw new IllegalArgumentException("setCount > 5: " + setCount);
    }
    pinCtrl &= ~(SM0_PINCTRL_SET_COUNT_BITS | SM0_PINCTRL_SET_BASE_BITS);
    pinCtrl |= setCount << SM0_PINCTRL_SET_COUNT_LSB;
    pinCtrl |= setBase << SM0_PINCTRL_SET_BASE_LSB;
  }

  public void setInPins(final int inBase)
  {
    if (inBase < 0) {
      throw new IllegalArgumentException("inBase < 0: " + inBase);
    }
    if (inBase > GPIO_NUM - 1) {
      throw new IllegalArgumentException("inBase > " + (GPIO_NUM - 1) + ": " +
                                         inBase);
    }
    pinCtrl &= ~SM0_PINCTRL_IN_BASE_BITS;
    pinCtrl |= inBase << SM0_PINCTRL_IN_BASE_LSB;
  }

  public void setSideSetPins(final int sideSetBase)
  {
    if (sideSetBase < 0) {
      throw new IllegalArgumentException("sideSetBase < 0: " + sideSetBase);
    }
    if (sideSetBase > GPIO_NUM - 1) {
      throw new IllegalArgumentException("sideSetBase > " +
                                         (GPIO_NUM - 1) + ": " +
                                         sideSetBase);
    }
    pinCtrl &= ~SM0_PINCTRL_SIDESET_BASE_BITS;
    pinCtrl |= sideSetBase << SM0_PINCTRL_SIDESET_BASE_LSB;
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
    pinCtrl &= ~SM0_PINCTRL_SIDESET_COUNT_BITS;
    pinCtrl |= bitCount << SM0_PINCTRL_SIDESET_COUNT_LSB;
    execCtrl &= ~(SM0_EXECCTRL_SIDE_EN_BITS | SM0_EXECCTRL_SIDE_PINDIR_BITS);
    execCtrl |= (optional ? 1 : 0) << SM0_EXECCTRL_SIDE_EN_LSB;
    execCtrl |= (pinDirs ? 1 : 0) << SM0_EXECCTRL_SIDE_PINDIR_LSB;
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
    clkDiv &= ~(SM0_CLKDIV_INT_BITS | SM0_CLKDIV_FRAC_BITS);
    clkDiv |= divInt << SM0_CLKDIV_INT_LSB;
    clkDiv |= divFrac << SM0_CLKDIV_FRAC_LSB;
  }

  public void setWrap(final int wrapTarget, final int wrap)
  {
    if (wrapTarget < 0) {
      throw new IllegalArgumentException("wrap target < 0: " + wrapTarget);
    }
    if (wrapTarget > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("wrap target > " +
                                         (MEMORY_SIZE - 1) + ": " +
                                         wrapTarget);
    }
    if (wrap < 0) {
      throw new IllegalArgumentException("wrap < 0: " + wrap);
    }
    if (wrap > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("wrap > " + (MEMORY_SIZE - 1) + ": " +
                                         wrap);
    }
    execCtrl &= ~(SM0_EXECCTRL_WRAP_TOP_BITS | SM0_EXECCTRL_WRAP_BOTTOM_BITS);
    execCtrl |= wrap << SM0_EXECCTRL_WRAP_TOP_LSB;
    execCtrl |= wrapTarget << SM0_EXECCTRL_WRAP_BOTTOM_LSB;
  }

  public void setJmpPin(final int pin)
  {
    if (pin < 0) {
      throw new IllegalArgumentException("jmp pin < 0: " + pin);
    }
    if (pin > GPIO_NUM - 1) {
      throw new IllegalArgumentException("jmp pin > " + (GPIO_NUM - 1) + ": " +
                                         pin);
    }
    execCtrl &= ~SM0_EXECCTRL_JMP_PIN_BITS;
    execCtrl |= pin << SM0_EXECCTRL_JMP_PIN_LSB;
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
    shiftCtrl &= ~(SM0_SHIFTCTRL_PUSH_THRESH_BITS |
                   SM0_SHIFTCTRL_IN_SHIFTDIR_BITS |
                   SM0_SHIFTCTRL_AUTOPUSH_BITS);
    shiftCtrl |= pushThreshold << SM0_SHIFTCTRL_PUSH_THRESH_LSB;
    shiftCtrl |= (autoPush ? 1 : 0) << SM0_SHIFTCTRL_AUTOPUSH_LSB;
    shiftCtrl |= (shiftRight ? 1 : 0) << SM0_SHIFTCTRL_IN_SHIFTDIR_LSB;
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
    shiftCtrl &= ~(SM0_SHIFTCTRL_PULL_THRESH_BITS |
                   SM0_SHIFTCTRL_OUT_SHIFTDIR_BITS |
                   SM0_SHIFTCTRL_AUTOPULL_BITS);
    shiftCtrl |= pullThreshold << SM0_SHIFTCTRL_PULL_THRESH_LSB;
    shiftCtrl |= (shiftRight ? 1 : 0) << SM0_SHIFTCTRL_OUT_SHIFTDIR_LSB;
    shiftCtrl |= (autoPull ? 1 : 0) << SM0_SHIFTCTRL_AUTOPULL_LSB;
  }

  public void setFIFOJoin(final FIFOJoin join)
  {
    shiftCtrl &= ~(SM0_SHIFTCTRL_FJOIN_RX_BITS | SM0_SHIFTCTRL_FJOIN_TX_BITS);
    shiftCtrl |= join.ordinal() << SM0_SHIFTCTRL_FJOIN_TX_LSB;
  }

  public void setOutSpecial(final boolean sticky, final boolean hasEnablePin,
                            final int enablePinIndex)
  {
    if (enablePinIndex < 0) {
      throw new IllegalArgumentException("enable pin index < 0: " +
                                         enablePinIndex);
    }
    if (enablePinIndex > GPIO_NUM - 1) {
      throw new IllegalArgumentException("enable pin index > " +
                                         (GPIO_NUM - 1) + ": " +
                                         enablePinIndex);
    }
    execCtrl &= ~(SM0_EXECCTRL_OUT_EN_SEL_BITS |
                  SM0_EXECCTRL_INLINE_OUT_EN_BITS |
                  SM0_EXECCTRL_OUT_STICKY_BITS);
    execCtrl |= enablePinIndex << SM0_EXECCTRL_OUT_EN_SEL_LSB;
    execCtrl |= (hasEnablePin ? 1 : 0) << SM0_EXECCTRL_INLINE_OUT_EN_LSB;
    execCtrl |= (sticky ? 1 : 0) << SM0_EXECCTRL_OUT_STICKY_LSB;
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
    execCtrl &= ~(SM0_EXECCTRL_STATUS_SEL_BITS | SM0_EXECCTRL_STATUS_N_BITS);
    execCtrl |= statusSel.ordinal() << SM0_EXECCTRL_STATUS_SEL_LSB;
    execCtrl |= statusN << SM0_EXECCTRL_STATUS_N_LSB;
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
