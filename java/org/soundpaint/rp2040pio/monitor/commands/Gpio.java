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
import org.soundpaint.rp2040pio.sdk.PIOSDK;
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

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optInit =
    CmdOptions.createIntegerOption("NUMBER", false, 'i', "init", null,
                                   "GPIO pin number (0..31)");

  private final SDK sdk;

  public Gpio(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[] { optPio, optInit });
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    this.sdk = sdk;
  }

  @Override
  protected void checkValidity(final CmdOptions options)
    throws CmdOptions.ParseException
  {
    if (options.getValue(optHelp) != CmdOptions.Flag.ON) {
      final int pioNum = options.getValue(optPio);
      if ((pioNum < 0) || (pioNum > Constants.PIO_NUM - 1)) {
        throw new CmdOptions.
          ParseException("PIO number must be either 0 or 1");
      }
      final Integer optInitValue = options.getValue(optInit);
      if (optInitValue != null) {
        final int gpioNum = optInitValue;
        if ((gpioNum < 0) || (gpioNum > Constants.GPIO_NUM - 1)) {
          final String message =
            String.format("GPIO number must be in the range 0x00..0x%02x",
                          Constants.GPIO_NUM - 1);
          throw new CmdOptions.ParseException(message);
        }
      }
    }
  }

  private void displayGpio() throws IOException
  {
    final String gpioPinBits = sdk.getGPIOSDK().asBitArrayDisplay();
    console.printf("(pio*:sm*) %s%n", gpioPinBits);
  }

  private void initGpio(final int pioNum, final int gpioNum) throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    pioSdk.gpioInit(gpioNum);
    console.printf("(pio%d:sm*) initialized GPIO pin %02x%n", pioNum, gpioNum);
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final int pioNum = options.getValue(optPio);
    final Integer optInitValue = options.getValue(optInit);
    if (optInitValue == null) {
      displayGpio();
    } else {
      initGpio(pioNum, optInitValue);
    }
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
