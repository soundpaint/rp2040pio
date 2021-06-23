/*
 * @(#)Wait.java 1.00 21/03/30
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
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "wait" observes a register's bits and will not
 * return until the register's value matches an expected bit pattern,
 * or when a timeout has occurred.
 *
 * TODO: There is currently no way to cancel an ongoing wait command
 * other than killing the monitor application.
 */
public class Wait extends Command
{
  private static final String fullName = "wait";
  private static final String singleLineDescription =
    "wait for a register's bits to match an expected value";

  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", null,
                                   "address (0x00000000…0xffffffff) of the " +
                                   "register to observe");
  private static final CmdOptions.IntegerOptionDeclaration optExpectedValue =
    CmdOptions.createIntegerOption("VALUE", false, 'v', "value", null,
                                   "expected value to match");
  private static final CmdOptions.IntegerOptionDeclaration optMask =
    CmdOptions.createIntegerOption("MASK", false, 'm', "mask", 0xffffffff,
                                   "bit mask to select bits to match");
  private static final CmdOptions.IntegerOptionDeclaration optCycles =
    CmdOptions.createIntegerOption("COUNT", false, 'c', "cycles", 0,
                                   "timeout after <COUNT> cycles or no timeout, if 0");
  private static final CmdOptions.IntegerOptionDeclaration optTime =
    CmdOptions.createIntegerOption("COUNT", false, 't', "time", 100000,
                                   "timeout after <COUNT> millis or no timeout, if 0");

  private final SDK sdk;

  public Wait(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription,
          new CmdOptions.OptionDeclaration<?>[] {
            optAddress, optExpectedValue, optMask, optCycles, optTime });
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
      if (!options.isDefined(optAddress)) {
        throw new CmdOptions.
          ParseException("option not specified", optAddress);
      }
      if (!options.isDefined(optExpectedValue)) {
        throw new CmdOptions.
          ParseException("option not specified", optExpectedValue);
      }
    }
    final int cycles = options.getValue(optCycles);
    if (cycles < 0) {
      throw new CmdOptions.
        ParseException("COUNT must be a non-negative value", optCycles);
    }
    final int time = options.getValue(optTime);
    if (time < 0) {
      throw new CmdOptions.
        ParseException("COUNT must be a non-negative value", optTime);
    }
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final int address = options.getValue(optAddress);
    final int expectedValue = options.getValue(optExpectedValue);
    final boolean validAddress = sdk.providesAddress(address);
    if (!validAddress) {
      final String message =
        String.format("wait on unsupported address: 0x%08x", address);
      throw new IOException(message);
    }
    final int mask = options.getValue(optMask);
    final int cycles = options.getValue(optCycles);
    final int time = options.getValue(optTime);
    final int result = sdk.wait(address, expectedValue, mask, cycles, time);
    console.printf("wait on 0x%08x for 0x%08x returned 0x%08x%n",
                   address, expectedValue, result);
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
