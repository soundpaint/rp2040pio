/*
 * @(#)Sm.java 1.00 21/04/01
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
import org.soundpaint.rp2040pio.Decoder;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "sm" enables or disables a PIO's state machine
 * or shows if it is enabled.
 */
public class Sm extends Command
{
  private static final String fullName = "sm";
  private static final String singleLineDescription =
    "enable or disable or restart state machine(s) or show if enabled";
  private static final String notes =
    "Use options \"-p\" and \"-s\" to select a state machine.%n" +
    "Enable or disable the selected state machine with option%n" +
    "\"+e\" or \"-e\", respectively.  Restart the selected%n" +
    "state machine with option \"+r\".%n" +
    "If none of options \"+e\", \"-e\", \"-r\" is specified,%n" +
    "show if the state machine is currently enabled.";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.BooleanOptionDeclaration optEnable =
    CmdOptions.createBooleanOption(false, 'e', "enable", null,
                                   "enable or disable the selected " +
                                   "state machine");
  private static final CmdOptions.FlagOptionDeclaration optRestart =
    CmdOptions.createFlagOption(false, 'r', "restart", CmdOptions.Flag.OFF,
                                "restart the selected state machine");

  private final SDK sdk;

  public Sm(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optEnable, optRestart });
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
      final int smNum = options.getValue(optSm);
      if ((smNum < 0) || (smNum > Constants.SM_COUNT - 1)) {
        throw new CmdOptions.
          ParseException("SM number must be one of 0, 1, 2 or 3");
      }
    }
  }

  private void displaySmStatus(final int pioNum, final int smNum)
    throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    final boolean enabled = pioSdk.smGetEnabled(smNum);
    console.printf("(pio%d:sm%d) %s%n", pioNum, smNum,
                   enabled ? "enabled" : "disabled");
  }

  private void setEnableStatus(final int pioNum, final int smNum,
                               final boolean enable)
    throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    pioSdk.smSetEnabled(smNum, enable);
    console.printf("(pio%d:sm%d) set %s%n", pioNum, smNum,
                   enable ? "enabled" : "disabled");
  }

  private void restart(final int pioNum, final int smNum) throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    pioSdk.smRestart(smNum);
    console.printf("(pio%d:sm%d) restarted%n", pioNum, smNum);
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final int pioNum = options.getValue(optPio);
    final int smNum = options.getValue(optSm);
    final Boolean optEnableValue = options.getValue(optEnable);
    final boolean optRestartValue = options.getValue(optRestart).isOn();
    final boolean haveModOp = (optEnableValue != null) || optRestartValue;
    if (!haveModOp) {
      displaySmStatus(pioNum, smNum);
    }
    if (optEnableValue != null) {
      setEnableStatus(pioNum, smNum, optEnableValue);
    }
    if (optRestartValue) {
      restart(pioNum, smNum);
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
