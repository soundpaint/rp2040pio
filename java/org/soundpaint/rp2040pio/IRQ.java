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
public class IRQ
{
  private int regIRQ; // bits 0..7 of IRQ
  private int regIRQ0_INTE; // bits 0..11 of IRQ0_INTE
  private int regIRQ0_INTF; // bits 0..11 of IRQ0_INTF
  private int regIRQ1_INTE; // bits 0..11 of IRQ1_INTE
  private int regIRQ1_INTF; // bits 0..11 of IRQ1_INTF

  public IRQ()
  {
    reset();
  }

  private void reset()
  {
    regIRQ = 0;
    regIRQ0_INTE = 0;
    regIRQ0_INTF = 0;
    regIRQ1_INTE = 0;
    regIRQ1_INTF = 0;
  }

  public int getIRQ0_INTE()
  {
    return regIRQ0_INTE;
  }

  public void setIRQ0_INTE(final int value)
  {
    regIRQ0_INTE = value & 0xfff;
  }

  public Bit getIRQ0_INTE_SM0_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x1, Bit.HIGH);
  }

  public Bit getIRQ0_INTE_SM1_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x2, Bit.HIGH);
  }

  public Bit getIRQ0_INTE_SM2_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x4, Bit.HIGH);
  }

  public Bit getIRQ0_INTE_SM3_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x8, Bit.HIGH);
  }

  public Bit getIRQ0_INTE_SM0_TXNFULL()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x10, Bit.HIGH);
  }

  public Bit getIRQ0_INTE_SM1_TXNFULL()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x20, Bit.HIGH);
  }

  public Bit getIRQ0_INTE_SM2_TXNFULL()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x40, Bit.HIGH);
  }

  public Bit getIRQ0_INTE_SM3_TXNFULL()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x80, Bit.HIGH);
  }

  public Bit getIRQ0_INTE_SM0()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x100, Bit.HIGH);
  }

  public Bit getIRQ0_INTE_SM1()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x200, Bit.HIGH);
  }

  public Bit getIRQ0_INTE_SM2()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x400, Bit.HIGH);
  }

  public Bit getIRQ0_INTE_SM3()
  {
    return Bit.fromValue(regIRQ0_INTE & 0x800, Bit.HIGH);
  }

  public int getIRQ1_INTE()
  {
    return regIRQ1_INTE;
  }

  public void setIRQ1_INTE(final int value)
  {
    regIRQ1_INTE = value & 0xfff;
  }

  public Bit getIRQ1_INTE_SM0_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x1, Bit.HIGH);
  }

  public Bit getIRQ1_INTE_SM1_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x2, Bit.HIGH);
  }

  public Bit getIRQ1_INTE_SM2_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x4, Bit.HIGH);
  }

  public Bit getIRQ1_INTE_SM3_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x8, Bit.HIGH);
  }

  public Bit getIRQ1_INTE_SM0_TXNFULL()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x10, Bit.HIGH);
  }

  public Bit getIRQ1_INTE_SM1_TXNFULL()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x20, Bit.HIGH);
  }

  public Bit getIRQ1_INTE_SM2_TXNFULL()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x40, Bit.HIGH);
  }

  public Bit getIRQ1_INTE_SM3_TXNFULL()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x80, Bit.HIGH);
  }

  public Bit getIRQ1_INTE_SM0()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x100, Bit.HIGH);
  }

  public Bit getIRQ1_INTE_SM1()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x200, Bit.HIGH);
  }

  public Bit getIRQ1_INTE_SM2()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x400, Bit.HIGH);
  }

  public Bit getIRQ1_INTE_SM3()
  {
    return Bit.fromValue(regIRQ1_INTE & 0x800, Bit.HIGH);
  }

  public int getIRQ0_INTF()
  {
    return regIRQ0_INTF;
  }

  public void setIRQ0_INTF(final int value)
  {
    regIRQ0_INTF = value & 0xfff;
  }

  public Bit getIRQ0_INTF_SM0_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x1, Bit.HIGH);
  }

  public Bit getIRQ0_INTF_SM1_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x2, Bit.HIGH);
  }

  public Bit getIRQ0_INTF_SM2_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x4, Bit.HIGH);
  }

  public Bit getIRQ0_INTF_SM3_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x8, Bit.HIGH);
  }

  public Bit getIRQ0_INTF_SM0_TXNFULL()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x10, Bit.HIGH);
  }

  public Bit getIRQ0_INTF_SM1_TXNFULL()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x20, Bit.HIGH);
  }

  public Bit getIRQ0_INTF_SM2_TXNFULL()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x40, Bit.HIGH);
  }

  public Bit getIRQ0_INTF_SM3_TXNFULL()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x80, Bit.HIGH);
  }

  public Bit getIRQ0_INTF_SM0()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x100, Bit.HIGH);
  }

  public Bit getIRQ0_INTF_SM1()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x200, Bit.HIGH);
  }

  public Bit getIRQ0_INTF_SM2()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x400, Bit.HIGH);
  }

  public Bit getIRQ0_INTF_SM3()
  {
    return Bit.fromValue(regIRQ0_INTF & 0x800, Bit.HIGH);
  }

  public int getIRQ1_INTF()
  {
    return regIRQ1_INTF;
  }

  public void setIRQ1_INTF(final int value)
  {
    regIRQ1_INTF = value & 0xfff;
  }

  public Bit getIRQ1_INTF_SM0_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x1, Bit.HIGH);
  }

  public Bit getIRQ1_INTF_SM1_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x2, Bit.HIGH);
  }

  public Bit getIRQ1_INTF_SM2_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x4, Bit.HIGH);
  }

  public Bit getIRQ1_INTF_SM3_RXNEMPTY()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x8, Bit.HIGH);
  }

  public Bit getIRQ1_INTF_SM0_TXNFULL()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x10, Bit.HIGH);
  }

  public Bit getIRQ1_INTF_SM1_TXNFULL()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x20, Bit.HIGH);
  }

  public Bit getIRQ1_INTF_SM2_TXNFULL()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x40, Bit.HIGH);
  }

  public Bit getIRQ1_INTF_SM3_TXNFULL()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x80, Bit.HIGH);
  }

  public Bit getIRQ1_INTF_SM0()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x100, Bit.HIGH);
  }

  public Bit getIRQ1_INTF_SM1()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x200, Bit.HIGH);
  }

  public Bit getIRQ1_INTF_SM2()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x400, Bit.HIGH);
  }

  public Bit getIRQ1_INTF_SM3()
  {
    return Bit.fromValue(regIRQ1_INTF & 0x800, Bit.HIGH);
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
    regIRQ |= (0x1 << index);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
