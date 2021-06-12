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
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.monitor.MonitorUtils;
import org.soundpaint.rp2040pio.sdk.GPIOSDK;
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
  private enum Policy
  {
    PASS("pass"), INVERT("invert"), LOW("low"), HIGH("high");

    private final String displayValue;

    private Policy(final String displayValue)
    {
      this.displayValue = displayValue;
    }

    public String getDisplayValue()
    {
      return displayValue;
    }

    public static Policy fromValue(final int value)
    {
      if ((value < 0) || (value >= POLICIES.length)) {
        throw new IllegalArgumentException("value: " + value);
      }
      return POLICIES[value];
    }
  }

  private static final Policy[] POLICIES = Policy.values();

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
    "If none of options \"-i\", \"-s\", \"-c\", \"-e\", \"-d\", nor any of%n" +
    "the override options is specified, the current status of all GPIO pins%n" +
    "will be displayed, depending on option \"-p\" for either of the PIOs or,%n" +
    "if \"-p\" is not specified, for the GPIO after function selection." +
    "%n" +
    "One of options \"--override-irq\", \"--override-in\", \"--override-oe\", and%n" +
    "\"--override-out\" may be specified together with one of policy options%n" +
    "\"--pass\", \"--invert\", \"--low\", \"--high\" to change override policy%n" +
    "of the specified GPIO pin.  If no policy option is specified, the current%n" +
    "policy is displayed for the specified override target.";

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
                                "set GPIO pin of the specified PIO or input");
  private static final CmdOptions.FlagOptionDeclaration optClear =
    CmdOptions.createFlagOption(false, 'c', "clear", CmdOptions.Flag.OFF,
                                "clear GPIO pin of the specified PIO or input");
  private static final CmdOptions.FlagOptionDeclaration optEnable =
    CmdOptions.createFlagOption(false, 'e', "enable", CmdOptions.Flag.OFF,
                                "enable GPIO output of the specified PIO, " +
                                "setting direction to \"out\"");
  private static final CmdOptions.FlagOptionDeclaration optDisable =
    CmdOptions.createFlagOption(false, 'd', "disable", CmdOptions.Flag.OFF,
                                "disable GPIO output of the specified PIO, " +
                                "setting direction to \"in\"");
  private static final CmdOptions.FlagOptionDeclaration optBefore =
    CmdOptions.createFlagOption(false, null, "before", CmdOptions.Flag.OFF,
                                "when displaying global GPIO status, show " +
                                "status before rather than after override");
  private static final CmdOptions.FlagOptionDeclaration optOverrideIrq =
    CmdOptions.createFlagOption(false, null, "override-irq", CmdOptions.Flag.OFF,
                                "specify override policy for a GPIO pin " +
                                "interrupt input");
  private static final CmdOptions.FlagOptionDeclaration optOverrideIn =
    CmdOptions.createFlagOption(false, null, "override-in", CmdOptions.Flag.OFF,
                                "specify override policy for a GPIO pin " +
                                "peripheral input");
  private static final CmdOptions.FlagOptionDeclaration optOverrideOe =
    CmdOptions.createFlagOption(false, null, "override-oe", CmdOptions.Flag.OFF,
                                "specify override policy for a GPIO pin " +
                                "output enable");
  private static final CmdOptions.FlagOptionDeclaration optOverrideOut =
    CmdOptions.createFlagOption(false, null, "override-out", CmdOptions.Flag.OFF,
                                "specify override policy for a GPIO pin " +
                                "output level");
  private static final CmdOptions.FlagOptionDeclaration optPass =
    CmdOptions.createFlagOption(false, null, "pass", CmdOptions.Flag.OFF,
                                "select 'pass' override policy");
  private static final CmdOptions.FlagOptionDeclaration optInvert =
    CmdOptions.createFlagOption(false, null, "invert", CmdOptions.Flag.OFF,
                                "select 'invert' override policy");
  private static final CmdOptions.FlagOptionDeclaration optLow =
    CmdOptions.createFlagOption(false, null, "low", CmdOptions.Flag.OFF,
                                "select 'low' override policy");
  private static final CmdOptions.FlagOptionDeclaration optHigh =
    CmdOptions.createFlagOption(false, null, "high", CmdOptions.Flag.OFF,
                                "select 'high' override policy");

  private final SDK sdk;

  public Gpio(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optGpio,
              optInit, optSet, optClear, optEnable, optDisable, optBefore,
              optOverrideIrq, optOverrideIn, optOverrideOe, optOverrideOut,
              optPass, optInvert, optLow, optHigh });
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
      int overrideOpCount = 0;
      if (options.getValue(optOverrideIrq) == CmdOptions.Flag.ON) overrideOpCount++;
      if (options.getValue(optOverrideIn) == CmdOptions.Flag.ON) overrideOpCount++;
      if (options.getValue(optOverrideOe) == CmdOptions.Flag.ON) overrideOpCount++;
      if (options.getValue(optOverrideOut) == CmdOptions.Flag.ON) overrideOpCount++;
      int overridePolicyCount = 0;
      if (options.getValue(optPass) == CmdOptions.Flag.ON) overridePolicyCount++;
      if (options.getValue(optInvert) == CmdOptions.Flag.ON) overridePolicyCount++;
      if (options.getValue(optLow) == CmdOptions.Flag.ON) overridePolicyCount++;
      if (options.getValue(optHigh) == CmdOptions.Flag.ON) overridePolicyCount++;

      final int count = editOpCount + manageOpCount + overrideOpCount;
      if (count > 1) {
        throw new CmdOptions.
          ParseException("at most one of options \"-i\", \"-s\", \"-c\", " +
                         "\"-e\", \"-d\" and the override options may be specified " +
                         "at the same time");
      }
      if (count > 0) {
        if (!options.isDefined(optGpio)) {
          throw new CmdOptions.
            ParseException("option not specified: " + optGpio);
        }
        if (options.getValue(optBefore) == CmdOptions.Flag.ON) {
          throw new CmdOptions.
            ParseException("option 'before' only valid when no operation is specified");
        }
      }
      if (count == 0) {
        if (options.isDefined(optGpio)) {
          throw new CmdOptions.
            ParseException("option may be specified only together with an operation: " +
                           optGpio);
        }
      }
      if (overridePolicyCount > 1) {
        throw new CmdOptions.
          ParseException("at most one of options \"--pass\", \"--invert\", " +
                         "\"--low\", and \"--high\" may be specified " +
                         "at the same time");
      }
      if (overrideOpCount < overridePolicyCount) {
        throw new CmdOptions.
          ParseException("override policy may not be specified without override option");
      }
      if (manageOpCount > 0) {
        if (!options.isDefined(optPio)) {
          throw new CmdOptions.
            ParseException("option not specified: " + optPio);
        }
      }
      if (overrideOpCount > 0) {
        if (options.isDefined(optPio)) {
          throw new CmdOptions.
            ParseException("option must not be specified for overrides: " + optPio);
        }
      }
    }
  }

  private void displayGpio(final Integer optPioValue, final boolean before)
    throws IOException
  {
    final GPIOSDK.Override override =
      before ? GPIOSDK.Override.BEFORE : GPIOSDK.Override.AFTER;
    final String gpioDisplay =
      optPioValue != null ?
      MonitorUtils.gpioDisplay(sdk, optPioValue) :
      MonitorUtils.gpioDisplay(sdk, override);
    console.printf(gpioDisplay);
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
      console.printf("(pio%d:sm*) set GPIO output pin %02x of PIO%d to 1%n",
                     pioNum, gpioNum, pioNum);
    } else {
      final int address =
        PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.GPIO_PADIN);
      final int mask = 0x1 << gpioNum;
      sdk.hwSetBits(address, mask);
      console.printf("(pio*:sm*) set GPIO external input %02x to 1%n",
                     gpioNum, pioNum);
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
      console.printf("(pio%d:sm*) set GPIO output pin %02x of PIO%d to 0%n",
                     pioNum, gpioNum, pioNum);
    } else {
      final int address =
        PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.GPIO_PADIN);
      final int mask = 0x1 << gpioNum;
      sdk.hwClearBits(address, mask);
      console.printf("(pio*:sm*) set GPIO external input %02x to 0%n",
                     gpioNum, pioNum);
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

  private void setOverride(final String target, final String policy, final int gpioNum,
                           final int overridePolicy, final int lsb, final int policyBits)
    throws IOException
  {
    final int address =
      GPIOIOBank0Registers.getGPIOAddress(gpioNum,
                                          GPIOIOBank0Registers.Regs.GPIO0_CTRL);
    sdk.hwWriteMasked(address, overridePolicy << lsb, policyBits);
    console.printf("(pio*:sm*) set %s override of GPIO pin %02x to policy '%s'%n",
                   target, gpioNum, policy);
  }

  private void displayOverride(final String target, final int gpioNum,
                               final int lsb, final int policyBits)
    throws IOException
  {
    final int address =
      GPIOIOBank0Registers.getGPIOAddress(gpioNum,
                                          GPIOIOBank0Registers.Regs.GPIO0_CTRL);
    final int ctrl = sdk.readAddress(address);
    final Policy policy = Policy.fromValue((ctrl & policyBits) >>> lsb);
    console.printf("(pio*:sm*) %s override of GPIO pin %02x policy is '%s'%n",
                   target, gpioNum, policy.getDisplayValue());
  }

  private void displayOrSetOverride(final String target, final String policy,
                                    final int gpioNum, final Integer overridePolicy,
                                    final int lsb, final int policyBits)
    throws IOException
  {
    if (policy != null) {
      setOverride(target, policy, gpioNum, overridePolicy, lsb, policyBits);
    } else {
      displayOverride(target, gpioNum, lsb, policyBits);
    }
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
    final boolean overrideIrq = options.getValue(optOverrideIrq) == CmdOptions.Flag.ON;
    final boolean overrideIn = options.getValue(optOverrideIn) == CmdOptions.Flag.ON;
    final boolean overrideOe = options.getValue(optOverrideOe) == CmdOptions.Flag.ON;
    final boolean overrideOut = options.getValue(optOverrideOut) == CmdOptions.Flag.ON;
    final String policy;
    final Integer policyBits;
    if (options.getValue(optHigh) == CmdOptions.Flag.ON) {
      policy = "high";
      policyBits = 0x3;
    } else if (options.getValue(optLow) == CmdOptions.Flag.ON) {
      policy = "low";
      policyBits = 0x2;
    } else if (options.getValue(optInvert) == CmdOptions.Flag.ON) {
      policy = "invert";
      policyBits = 0x1;
    } else if (options.getValue(optPass) == CmdOptions.Flag.ON) {
      policy = "pass";
      policyBits = 0x0;
    } else {
      policy = null;
      policyBits = null;
    }
    if (init || clear || set || enable || disable ||
        overrideIrq || overrideIn || overrideOe || overrideOut) {
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
      } else if (overrideIrq) {
        displayOrSetOverride("IRQ ", policy, gpioNum, policyBits,
                             Constants.IO_BANK0_GPIO0_CTRL_IRQOVER_LSB,
                             Constants.IO_BANK0_GPIO0_CTRL_IRQOVER_BITS);
      } else if (overrideIn) {
        displayOrSetOverride("input", policy, gpioNum, policyBits,
                             Constants.IO_BANK0_GPIO0_CTRL_INOVER_LSB,
                             Constants.IO_BANK0_GPIO0_CTRL_INOVER_BITS);
      } else if (overrideOe) {
        displayOrSetOverride("output enable", policy, gpioNum, policyBits,
                             Constants.IO_BANK0_GPIO0_CTRL_OEOVER_LSB,
                             Constants.IO_BANK0_GPIO0_CTRL_OEOVER_BITS);
      } else if (overrideOut) {
        displayOrSetOverride("output", policy, gpioNum, policyBits,
                             Constants.IO_BANK0_GPIO0_CTRL_OUTOVER_LSB,
                             Constants.IO_BANK0_GPIO0_CTRL_OUTOVER_BITS);
      } else {
        throw new InternalError("unexpected case fall-through");
      }
    } else {
      final boolean before = options.getValue(optBefore) == CmdOptions.Flag.ON;
      displayGpio(options.getValue(optPio), before);
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
