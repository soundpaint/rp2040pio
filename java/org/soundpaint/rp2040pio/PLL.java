/*
 * @(#)PLL.java 1.00 21/02/05
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

/**
 * Phase Locked Loop (PLL)
 */
public class PLL implements Clock.TransitionListener
{
  private final PrintStream console;
  private int regCLKDIV_INT; // bits 16…31 of SMx_CLKDIV
  private int regCLKDIV_FRAC; // bits 8…15 of SMx_CLKDIV
  private int countIntegerBits;
  private int countFractionalBits;
  private boolean clockEnable;
  private boolean nextClockEnable;

  private PLL()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public PLL(final PrintStream console)
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.console = console;
    reset();
  }

  public void reset()
  {
    regCLKDIV_INT = 0x0001;
    regCLKDIV_FRAC = 0x00;
    countIntegerBits = 0x1;
    countFractionalBits = 0x0;
    clockEnable = false;
    nextClockEnable = false;
  }

  public int getDivIntegerBits()
  {
    return regCLKDIV_INT;
  }

  public void setDivIntegerBits(final int divIntegerBits)
  {
    if (divIntegerBits < 0) {
      throw new IllegalArgumentException("div integer bits < 0: " +
                                         divIntegerBits);
    }
    if (divIntegerBits > 0xffff) {
      throw new IllegalArgumentException("div integer bits > 65535: " +
                                         divIntegerBits);
    }
    if (divIntegerBits == 0) {
      if (regCLKDIV_FRAC != 0) {
        // RP2040 datasheet, Table 391: "If INT is 0, FRAC must also
        // be 0."
        final String message =
          String.format("warning: ignoring request for setting CLK int bits " +
                        "to 0, since CLK frac bits are non-zero");
        console.printf("%s%n", message);
        return;
      }
      // TODO: Clarify: Should we ignore the change (as implemented),
      // or should we silently set also FRAC to 0?
    }
    this.regCLKDIV_INT = divIntegerBits;
  }

  public int getDivFractionalBits()
  {
    return regCLKDIV_FRAC;
  }

  public void setDivFractionalBits(final int divFractionalBits)
  {
    if (divFractionalBits < 0) {
      throw new IllegalArgumentException("div fractional bits < 0: " +
                                         divFractionalBits);
    }
    if (divFractionalBits > 0xff) {
      throw new IllegalArgumentException("div fractional bits > 255: " +
                                         divFractionalBits);
    }
    if (regCLKDIV_INT == 0) {
      if (divFractionalBits != 0) {
        // RP2040 datasheet, Table 391: "If INT is 0, FRAC must also
        // be 0."
        final String message =
          String.format("warning: ignoring request for setting CLK frac bits " +
                        "to non-zero value, since CLK int bits are zero");
        console.printf("%s%n", message);
        return;
      }
    }
    this.regCLKDIV_FRAC = divFractionalBits;
  }

  private void setCLKDIV(final int divIntegerBits, final int divFractionalBits)
  {
    if ((divIntegerBits == 0) && (divFractionalBits == 0)) {
      // Special case: RP2040 datasheet, Table 391: "If INT is 0, FRAC
      // must also be 0."
      regCLKDIV_INT = 0;
      regCLKDIV_FRAC = 0;
    } else {
      setDivIntegerBits(divIntegerBits);
      setDivFractionalBits(divFractionalBits);
    }
  }

  public void setCLKDIV(final int clkdiv)
  {
    setCLKDIV(clkdiv >>> 16, (clkdiv >>> 8) & 0xff);
  }

  public int getCLKDIV()
  {
    return
      (getDivIntegerBits() << 16) |
      (getDivFractionalBits() << 8);
  }

  public boolean getClockEnable()
  {
    return clockEnable;
  }

  public boolean getNextClockEnable()
  {
    return nextClockEnable;
  }

  private void prepareClockEnable()
  {
    /*
     * TODO: Clarify: Sect. 3.5.5. "Clock Dividers", Fig. 46: "clock
     * divider … emits an enable pulse when it reaches 1"
     *
     * -- Really "1", not "0"?
     */
    if (countIntegerBits <= 1) {
      countIntegerBits += regCLKDIV_INT;
      countFractionalBits += regCLKDIV_FRAC;
      if (countFractionalBits >= 0x100) {
        countFractionalBits -= 0x100;
        countIntegerBits++;
      }
      nextClockEnable = true;
    } else {
      nextClockEnable = false;
    }
    countIntegerBits--;
  }

  @Override
  public void risingEdge(final long wallClock)
  {
    clockEnable = nextClockEnable;
  }

  @Override
  public void fallingEdge(final long wallClock) {
    prepareClockEnable();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
