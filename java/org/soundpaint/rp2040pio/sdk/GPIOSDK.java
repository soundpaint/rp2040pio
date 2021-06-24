/*
 * @(#)GPIOSDK.java 1.00 21/03/02
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

import java.io.IOException;
import org.soundpaint.rp2040pio.AddressSpace;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.Bit;
import org.soundpaint.rp2040pio.Direction;
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.GPIOPadsBank0Registers;
import org.soundpaint.rp2040pio.PinState;

/**
 * Minimal subset of GPIO SDK Interface, just enough to provide all
 * required GPIO-related functionality for PIO SDK.
 */
public class GPIOSDK implements Constants
{
  public enum Override
  {
    BEFORE, AFTER
  }

  private final AddressSpace memory;

  public GPIOSDK(final AddressSpace memory)
  {
    if (memory == null) {
      throw new NullPointerException("memory");
    }
    this.memory = memory;
  }

  public void setFunction(final int gpioNum, final GPIO_Function fn)
    throws IOException
  {
    Constants.checkGpioPin(gpioNum, "GPIO pin number");

    final int padsGpioAddress =
      GPIOPadsBank0Registers.getGPIOAddress(gpioNum);
    final int padsValues = Bit.LOW.getValue() << PADS_BANK0_GPIO0_IE_LSB;
    final int padsWriteMask = PADS_BANK0_GPIO0_IE_BITS;
    memory.hwWriteMasked(padsGpioAddress, padsValues, padsWriteMask);

    final GPIOIOBank0Registers.Regs ioBank0Reg =
      GPIOIOBank0Registers.Regs.GPIO0_CTRL;
    final int ioGpioAddress =
      GPIOIOBank0Registers.getGPIOAddress(gpioNum, ioBank0Reg);
    final int ioValues = fn.getValue() << IO_BANK0_GPIO0_CTRL_FUNCSEL_LSB;
    final int ioWriteMask = IO_BANK0_GPIO0_CTRL_FUNCSEL_BITS;
    memory.hwWriteMasked(ioGpioAddress, ioValues, ioWriteMask);
  }

  private static Direction getDirectionFromStatus(final int statusValue,
                                                  final Override override)
  {
    if (override == null) {
      throw new NullPointerException("override");
    }
    final int gpioOe = override == Override.BEFORE ?
      (statusValue & Constants.IO_BANK0_GPIO0_STATUS_OEFROMPERI_BITS) >>>
      Constants.IO_BANK0_GPIO0_STATUS_OEFROMPERI_LSB :
      (statusValue & Constants.IO_BANK0_GPIO0_STATUS_OETOPAD_BITS) >>>
      Constants.IO_BANK0_GPIO0_STATUS_OETOPAD_LSB;
    return Direction.fromValue(gpioOe);
  }

  private static Bit getOutputLevelFromStatus(final int statusValue,
                                              final Override override)
  {
    if (override == null) {
      throw new NullPointerException("override");
    }
    final int gpioOut = override == Override.BEFORE ?
      (statusValue & Constants.IO_BANK0_GPIO0_STATUS_OUTFROMPERI_BITS) >>>
      Constants.IO_BANK0_GPIO0_STATUS_OUTFROMPERI_LSB :
      (statusValue & Constants.IO_BANK0_GPIO0_STATUS_OUTTOPAD_BITS) >>>
      Constants.IO_BANK0_GPIO0_STATUS_OUTTOPAD_LSB;
    return Bit.fromValue(gpioOut);
  }

  private static Bit getInputLevelFromStatus(final int statusValue,
                                             final Override override)
  {
    if (override == null) {
      throw new NullPointerException("override");
    }
    final int gpioIn = override == Override.BEFORE ?
      (statusValue & Constants.IO_BANK0_GPIO0_STATUS_INFROMPAD_BITS) >>>
      Constants.IO_BANK0_GPIO0_STATUS_INFROMPAD_LSB :
      (statusValue & Constants.IO_BANK0_GPIO0_STATUS_INTOPERI_BITS) >>>
      Constants.IO_BANK0_GPIO0_STATUS_INTOPERI_LSB;
    return Bit.fromValue(gpioIn);
  }

  public Bit getInputLevel(final int gpioNum, final Override override)
    throws IOException
  {
    Constants.checkGpioPin(gpioNum, "GPIO pin number");
    final int gpioStatusAddress =
      GPIOIOBank0Registers.
      getGPIOAddress(gpioNum, GPIOIOBank0Registers.Regs.GPIO0_STATUS);
    final int gpioStatusValue = memory.readAddress(gpioStatusAddress);
    return getInputLevelFromStatus(gpioStatusValue, override);
  }

  public PinState[] getPinStates(final Override override) throws IOException
  {
    final PinState[] pinStates = new PinState[Constants.GPIO_NUM];
    for (int gpioNum = 0; gpioNum < Constants.GPIO_NUM; gpioNum++) {
      final int gpioStatusAddress =
        GPIOIOBank0Registers.
        getGPIOAddress(gpioNum, GPIOIOBank0Registers.Regs.GPIO0_STATUS);
      final int gpioStatusValue = memory.readAddress(gpioStatusAddress);
      final int gpioOeFromPeri =
        (gpioStatusValue & Constants.IO_BANK0_GPIO0_STATUS_OEFROMPERI_BITS) >>>
        Constants.IO_BANK0_GPIO0_STATUS_OEFROMPERI_LSB;
      final Direction direction = Direction.fromValue(gpioOeFromPeri);
      final Bit level;
      if (direction == Direction.OUT) {
        level = getOutputLevelFromStatus(gpioStatusValue, override);
      } else {
        level = getInputLevelFromStatus(gpioStatusValue, override);
      }
      pinStates[gpioNum] = PinState.fromValues(direction, level);
    }
    return pinStates;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
