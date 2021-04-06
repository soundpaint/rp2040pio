/*
 * @(#)Gpio.java 1.00 21/04/06
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
package org.soundpaint.rp2040pio.monitor.commands;

import java.io.IOException;
import java.io.PrintStream;
import org.soundpaint.rp2040pio.Bit;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.Direction;
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.PinState;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "gpio" the status of the RP2040's GPIO pins.
 * Support for modifying GPIO status is not yet implemented.
 *
 * TODO: Also show GPIO status individual for each PIO / SM, as well
 * as if PIO GPIO pins are routed to RP2040's GPIO pins or not.  Also
 * consider adding support for changing status of GPIO pins.
 */
public class Gpio extends Command
{
  private static final String fullName = "gpio";
  private static final String singleLineDescription =
    "display status of GPIO pins";
  private static final String notes =
    "Each PIO has a set of GPIO pins that, depending on further%n" +
    "settings of the GPIO block itself, are propagated to the%n"+
    "RP2040's GPIO pins or not.  This command currently only displays%n" +
    "the RP2040's GPIO pins, but (as of now) neither a specific PIO's%n" +
    "GPIO status, nor the GPIO status individual for each SM.%n";

  private final SDK sdk;

  public Gpio(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes);
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    this.sdk = sdk;
  }

  private void displayGpio()
    throws IOException
  {
    final String gpioPinBits = sdk.getGPIOSDK().asBitArrayDisplay();
    console.printf("           %s%n", gpioPinBits);
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    displayGpio();
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
