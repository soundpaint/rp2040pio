/*
 * @(#)IRQ.java 1.00 21/02/06
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
 * IRQ Register Set
 */
public class IRQ implements Constants
{
  private int regIRQ; // bits 0…7 of IRQ
  private int regIRQ0_INTE; // bits 0…11 of IRQ0_INTE
  private int regIRQ0_INTF; // bits 0…11 of IRQ0_INTF
  private int regIRQ1_INTE; // bits 0…11 of IRQ1_INTE
  private int regIRQ1_INTF; // bits 0…11 of IRQ1_INTF
  private int fifoStatus;

  public IRQ()
  {
    reset();
  }

  public void reset()
  {
    regIRQ = 0;
    regIRQ0_INTE = 0;
    regIRQ0_INTF = 0;
    regIRQ1_INTE = 0;
    regIRQ1_INTF = 0;
    fifoStatus = 0;
  }

  public void setTxNFull(final int smNum, final boolean nFull)
  {
    Constants.checkSmNum(smNum);
    if (nFull) {
      fifoStatus |= 0x10 << smNum;
    } else {
      fifoStatus &= ~(0x10 << smNum);
    }
  }

  public void setRxNEmpty(final int smNum, final boolean nEmpty)
  {
    Constants.checkSmNum(smNum);
    if (nEmpty) {
      fifoStatus |= 0x1 << smNum;
    } else {
      fifoStatus &= ~(0x1 << smNum);
    }
  }

  public void writeRegIRQ(final int value)
  {
    regIRQ &= (~value) & 0xff; // ignore reserved bits 31:8
  }

  public void writeRegIRQ_FORCE(final int value)
  {
    regIRQ |= value & 0xff; // ignore reserved bits 31:8
  }

  public Bit get(final int index)
  {
    if (index < 0) {
      throw new IllegalArgumentException("IRQ index < 0: " + index);
    }
    if (index > 7) {
      throw new IllegalArgumentException("IRQ index > 7: " + index);
    }
    return ((regIRQ >> index) & 0x1) == 0x0 ? Bit.LOW : Bit.HIGH;
  }

  public void clear(final int index)
  {
    if (index < 0) {
      throw new IllegalArgumentException("IRQ index < 0: " + index);
    }
    if (index > 7) {
      throw new IllegalArgumentException("IRQ index > 7: " + index);
    }
    regIRQ &= ~(0x1 << index);
  }

  public void set(final int index)
  {
    if (index < 0) {
      throw new IllegalArgumentException("IRQ index < 0: " + index);
    }
    if (index > 7) {
      throw new IllegalArgumentException("IRQ index > 7: " + index);
    }
    regIRQ |= 0x1 << index;
  }

  public int getIRQ()
  {
    return regIRQ;
  }

  public int getIRQ0_INTE()
  {
    return regIRQ0_INTE;
  }

  public void setIRQ0_INTE(final int value, final int mask, final boolean xor)
  {
    setIRQ0_INTE(Constants.hwSetBits(regIRQ0_INTE, value, mask, xor));
  }

  private void setIRQ0_INTE(final int value)
  {
    regIRQ0_INTE = value & 0xfff; // ignore reserved bits 31:12
  }

  public int getIRQ1_INTE()
  {
    return regIRQ1_INTE;
  }

  public void setIRQ1_INTE(final int value, final int mask, final boolean xor)
  {
    setIRQ1_INTE(Constants.hwSetBits(regIRQ1_INTE, value, mask, xor));
  }

  private void setIRQ1_INTE(final int value)
  {
    regIRQ1_INTE = value & 0xfff; // ignore reserved bits 31:12
  }

  public int getIRQ0_INTF()
  {
    return regIRQ0_INTF;
  }

  public void setIRQ0_INTF(final int value, final int mask, final boolean xor)
  {
    setIRQ0_INTF(Constants.hwSetBits(regIRQ0_INTF, value, mask, xor));
  }

  private void setIRQ0_INTF(final int value)
  {
    regIRQ0_INTF = value & 0xfff; // ignore reserved bits 31:12
  }

  public int getIRQ1_INTF()
  {
    return regIRQ1_INTF;
  }

  public void setIRQ1_INTF(final int value, final int mask, final boolean xor)
  {
    setIRQ1_INTF(Constants.hwSetBits(regIRQ1_INTF, value, mask, xor));
  }

  private void setIRQ1_INTF(final int value)
  {
    regIRQ1_INTF = value & 0xfff; // ignore reserved bits 31:12
  }

  public int readINTR()
  {
    return ((regIRQ & 0x7) << 8) | fifoStatus;
  }

  public int readIRQ0_INTS()
  {
    return (readINTR() & regIRQ0_INTE) | regIRQ0_INTF;
  }

  public int readIRQ1_INTS()
  {
    return (readINTR() & regIRQ1_INTE) | regIRQ1_INTF;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
