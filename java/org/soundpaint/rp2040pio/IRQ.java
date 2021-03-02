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
  private int regIRQ; // bits 0..7 of IRQ
  private int regIRQ_FORCE; // bits 0..7 of IRQ_FORCE
  private int regINTR; // bits 0..11 of INTR
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
    regIRQ_FORCE = 0;
    regINTR = 0;
    regIRQ0_INTE = 0;
    regIRQ0_INTF = 0;
    regIRQ1_INTE = 0;
    regIRQ1_INTF = 0;
  }

  public int readRegIRQ()
  {
    return regIRQ | regIRQ_FORCE;
  }

  public void writeRegIRQ(final int value)
  {
    regIRQ &= ~(value & 0xff); // ignore reserved bits 31:8
    regINTR &= ~((value & 0xf) << 8);
  }

  public int readRegIRQ_FORCE()
  {
    return regIRQ_FORCE;
  }

  public void writeRegIRQ_FORCE(final int value)
  {
    regIRQ_FORCE &= ~(value & 0xff); // ignore reserved bits 31:8
  }

  public Bit get(final int index)
  {
    if (index < 0) {
      throw new IllegalArgumentException("IRQ index < 0: " + index);
    }
    if (index > 7) {
      throw new IllegalArgumentException("IRQ index > 7: " + index);
    }
    return ((readRegIRQ() >> index) & 0x1) == 0x0 ? Bit.LOW : Bit.HIGH;
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
    regINTR &= ~(0x1 << (index + 8));
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
    regINTR |= (0x1 << (index + 8));
  }

  public void clearINTR_SMX_TXNFULL(final int smNum)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("SM number < 0: " + smNum);
    }
    if (smNum >= SM_COUNT) {
      final String message =
        String.format("SM number >= %d: %d", SM_COUNT, smNum);
      throw new IllegalArgumentException(message);
    }
    regINTR &= ~(0x1 << (smNum + 4));
  }

  public void setINTR_SMX_TXNFULL(final int smNum)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("SM number < 0: " + smNum);
    }
    if (smNum >= SM_COUNT) {
      final String message =
        String.format("SM number >= %d: %d", SM_COUNT, smNum);
      throw new IllegalArgumentException(message);
    }
    regINTR |= 0x1 << (smNum + 4);
  }

  public void clearINTR_SMX_RXNEMPTY(final int smNum)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("SM number < 0: " + smNum);
    }
    if (smNum >= SM_COUNT) {
      final String message =
        String.format("SM number >= %d: %d", SM_COUNT, smNum);
      throw new IllegalArgumentException(message);
    }
    regINTR &= ~(0x1 << smNum);
  }

  public void setINTR_SMX_RXNEMPTY(final int smNum)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("SM number < 0: " + smNum);
    }
    if (smNum >= SM_COUNT) {
      final String message =
        String.format("SM number >= %d: %d", SM_COUNT, smNum);
      throw new IllegalArgumentException(message);
    }
    regINTR |= 0x1 << smNum;
  }

  public int getIRQ0_INTE()
  {
    return regIRQ0_INTE;
  }

  public void setIRQ0_INTE(final int value)
  {
    regIRQ0_INTE = value & 0xfff; // ignore reserved bits 31:12
  }

  public int getIRQ1_INTE()
  {
    return regIRQ1_INTE;
  }

  public void setIRQ1_INTE(final int value)
  {
    regIRQ1_INTE = value & 0xfff; // ignore reserved bits 31:12
  }

  public int getIRQ0_INTF()
  {
    return regIRQ0_INTF;
  }

  public void setIRQ0_INTF(final int value)
  {
    regIRQ0_INTF = value & 0xfff; // ignore reserved bits 31:12
  }

  public int getIRQ1_INTF()
  {
    return regIRQ1_INTF;
  }

  public void setIRQ1_INTF(final int value)
  {
    regIRQ1_INTF = value & 0xfff; // ignore reserved bits 31:12
  }

  public int readINTR()
  {
    return regINTR;
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
