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
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PinState;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.monitor.MonitorUtils;
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
    "display or change status of GPIO pins";
  private static final String notes =
    "Each PIO has a set of local GPIO pins that, depending on the GPIO's%n" +
    "function selection settings, are propagated to the RP2040's GPIO%n" +
    "pins or not.  Use this command for displaying the RP2040's GPIO pins%n" +
    "after function selection, or as directly output by a specific PIO's%n" +
    "local GPIO pins.%n" +
    "%n" +
    "Use one of options \"-i\", \"-s\", \"-c\", \"-e\", \"-d\", together%n" +
    "with option \"-g\", for either initializing a GPIO pin for a PIO, or%n" +
    "for clearing or setting its status or for specifying its pin%n" +
    "direction by enabling or disabling its output, respectively.%n" +
    "Use options \"-p\" and \"-g\" option to specify which PIO and GPIO%n" +
    "pin to apply the operation.  Option \"-p\" can be ommitted when%n" +
    "clearing or setting GPIO pin status; in that case, the operation%n" +
    "will apply the new pin status as external input for the specified%n" +
    "pin.%n" +
    "%n" +
    "If none of options \"-i\", \"-s\", \"-c\", \"-e\", \"-d\" is%n" +
    "specified, the current status of all GPIO pins will be displayed,%n" +
    "depending on option \"-p\" for either of the PIOs or, if \"-p\" is%n" +
    "not specified, for the GPIO after function selection.";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", null,
                                   "PIO number, either 0 or 1 or undefined");
  private static final CmdOptions.IntegerOptionDeclaration optGpio =
    CmdOptions.createIntegerOption("NUMBER", false, 'g', "gpio", null,
                                   "number of GPIO pin (0…31)");
  private static final CmdOptions.FlagOptionDeclaration optInit =
    CmdOptions.createFlagOption(false, 'i', "init", CmdOptions.Flag.OFF,
                                "initialize GPIO pin for use with the " +
                                "specified PIO");
  private static final CmdOptions.FlagOptionDeclaration optSet =
    CmdOptions.createFlagOption(false, 's', "set", CmdOptions.Flag.OFF,
                                "set GPIO pin of the specified PIO");
  private static final CmdOptions.FlagOptionDeclaration optClear =
    CmdOptions.createFlagOption(false, 'c', "clear", CmdOptions.Flag.OFF,
                                "clear GPIO pin of the specified PIO");
  private static final CmdOptions.FlagOptionDeclaration optEnable =
    CmdOptions.createFlagOption(false, 'e', "enable", CmdOptions.Flag.OFF,
                                "enable GPIO output of the specified PIO, " +
                                "setting direction to \"out\"");
  private static final CmdOptions.FlagOptionDeclaration optDisable =
    CmdOptions.createFlagOption(false, 'd', "disable", CmdOptions.Flag.OFF,
                                "disable GPIO output of the specified PIO, " +
                                "setting direction to \"in\"");

  private final SDK sdk;

  public Gpio(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optGpio,
              optInit, optSet, optClear, optEnable, optDisable });
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
      if (options.isDefined(optPio)) {
        final int pioNum = options.getValue(optPio);
        if ((pioNum < 0) || (pioNum > Constants.PIO_NUM - 1)) {
          throw new CmdOptions.
            ParseException("PIO number must be either 0 or 1");
        }
      }
      if (options.isDefined(optGpio)) {
        final int gpioNum = options.getValue(optGpio);
        if ((gpioNum < 0) || (gpioNum > Constants.GPIO_NUM - 1)) {
          final String message =
            String.format("GPIO number must be in the range 0x00…0x%02x",
                          Constants.GPIO_NUM - 1);
          throw new CmdOptions.ParseException(message);
        }
      }
      int editOpCount = 0;
      if (options.getValue(optSet) == CmdOptions.Flag.ON) editOpCount++;
      if (options.getValue(optClear) == CmdOptions.Flag.ON) editOpCount++;
      int manageOpCount = 0;
      if (options.getValue(optInit) == CmdOptions.Flag.ON) manageOpCount++;
      if (options.getValue(optEnable) == CmdOptions.Flag.ON) manageOpCount++;
      if (options.getValue(optDisable) == CmdOptions.Flag.ON) manageOpCount++;

      final int count = editOpCount + manageOpCount;
      if (count > 1) {
        throw new CmdOptions.
          ParseException("at most one of options \"-i\", \"-s\", \"-c\", " +
                         "\"e\" and \"-d\" may be specified at the same time");
      }
      if (count > 0) {
        if (!options.isDefined(optGpio)) {
          throw new CmdOptions.
            ParseException("option not specified: " + optGpio);
        }
      }
      if (manageOpCount > 0) {
        if (!options.isDefined(optPio)) {
          throw new CmdOptions.
            ParseException("option not specified: " + optPio);
        }
      }
    }
  }

  private void displayGpio(final Integer optPioValue) throws IOException
  {
    console.printf(MonitorUtils.gpioDisplay(sdk, optPioValue));
  }

  private void initGpio(final int pioNum, final int gpioNum) throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    pioSdk.gpioInit(gpioNum);
    console.printf("(pio%d:sm*) initialized GPIO pin %02x for use with PIO%d%n",
                   pioNum, gpioNum, pioNum);
  }

  private void setGpio(final Integer pioNum, final int gpioNum)
    throws IOException
  {
    if (pioNum != null) {
      final int address =
        PIOEmuRegisters.getAddress(pioNum, PIOEmuRegisters.Regs.GPIO_PINS);
      final int mask = 0x1 << gpioNum;
      sdk.hwSetBits(address, mask);
      console.printf("(pio%d:sm*) set GPIO pin %02x of PIO%d to 1%n",
                     pioNum, gpioNum, pioNum);
    } else {
      final int address =
        PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.GPIO_PINS);
      final int mask = 0x1 << gpioNum;
      sdk.hwSetBits(address, mask);
      console.printf("(pio*:sm*) set GPIO pin %02x to 1%n", gpioNum, pioNum);
    }
  }

  private void clearGpio(final Integer pioNum, final int gpioNum)
    throws IOException
  {
    if (pioNum != null) {
      final int address =
        PIOEmuRegisters.getAddress(pioNum, PIOEmuRegisters.Regs.GPIO_PINS);
      final int mask = 0x1 << gpioNum;
      sdk.hwClearBits(address, mask);
      console.printf("(pio%d:sm*) set GPIO pin %02x of PIO%d to 0%n",
                     pioNum, gpioNum, pioNum);
    } else {
      final int address =
        PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.GPIO_PINS);
      final int mask = 0x1 << gpioNum;
      sdk.hwClearBits(address, mask);
      console.printf("(pio*:sm*) set GPIO pin %02x to 0%n", gpioNum, pioNum);
    }
  }

  private void enableGpio(final int pioNum, final int gpioNum)
    throws IOException
  {
    final int address =
      PIOEmuRegisters.getAddress(pioNum, PIOEmuRegisters.Regs.GPIO_PINDIRS);
    final int mask = 0x1 << gpioNum;
    sdk.hwSetBits(address, mask);
    console.printf("(pio%d:sm*) set direction of GPIO pin %02x of PIO%d to " +
                   "\"out\"%n",
                   pioNum, gpioNum, pioNum);
  }

  private void disableGpio(final int pioNum, final int gpioNum)
    throws IOException
  {
    final int address =
      PIOEmuRegisters.getAddress(pioNum, PIOEmuRegisters.Regs.GPIO_PINDIRS);
    final int mask = 0x1 << gpioNum;
    sdk.hwClearBits(address, mask);
    console.printf("(pio%d:sm*) set direction of GPIO pin %02x of PIO%d to " +
                   "\"in\"%n",
                   pioNum, gpioNum, pioNum);
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final boolean init = options.getValue(optInit) == CmdOptions.Flag.ON;
    final boolean clear = options.getValue(optClear) == CmdOptions.Flag.ON;
    final boolean set = options.getValue(optSet) == CmdOptions.Flag.ON;
    final boolean enable = options.getValue(optEnable) == CmdOptions.Flag.ON;
    final boolean disable = options.getValue(optDisable) == CmdOptions.Flag.ON;
    if (init || clear || set | enable | disable) {
      final Integer pioNum = options.getValue(optPio);
      final Integer gpioNum = options.getValue(optGpio);
      if (init) {
        initGpio(pioNum, gpioNum);
      } else if (set) {
        setGpio(pioNum, gpioNum);
      } else if (clear) {
        clearGpio(pioNum, gpioNum);
      } else if (enable) {
        enableGpio(pioNum, gpioNum);
      } else if (disable) {
        disableGpio(pioNum, gpioNum);
      }
    } else {
      displayGpio(options.getValue(optPio));
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
