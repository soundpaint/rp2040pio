/*
 * @(#)Enable.java 1.00 21/04/01
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
 * Monitor command "enable" enables or disables a PIO's state machine
 * or shows if it is enabled.
 */
public class Enable extends Command
{
  private static final String fullName = "enable";
  private static final String singleLineDescription =
    "enabled or disable state machine(s) or show if enabled";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", null,
                                   "PIO number, either 0 or 1 or both, " +
                                   "if undefined");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", null,
                                   "SM number, one of 0, 1, 2 or 3, or all, " +
                                   "if undefined");
  private static final CmdOptions.BooleanOptionDeclaration optEnable =
    CmdOptions.createBooleanOption(false, 'e', "enable", null,
                                   "enable or disable or show, if undefined");

  /*
   * TODO: With the given set of options, one can enable / disable
   * either a single or all state machines at once, but not two or
   * three out them.  Still, this is not really a restriction, since
   * in single step mode, when this command is called multiple times,
   * it behaves as being executed simultaneously.
   */

  private final SDK sdk;

  public Enable(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optEnable });
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
      final Integer optPioValue = options.getValue(optPio);
      if (optPioValue != null) {
        final int pioNum = optPioValue;
        if ((pioNum < 0) || (pioNum > Constants.PIO_NUM - 1)) {
          throw new CmdOptions.
            ParseException("PIO number must be either 0 or 1, if defined");
        }
      }
      final Integer optSmValue = options.getValue(optSm);
      if (optSmValue != null) {
        final int smNum = optSmValue;
        if ((smNum < 0) || (smNum > Constants.SM_COUNT - 1)) {
          throw new CmdOptions.
            ParseException("SM number must be one of 0, 1, 2 or 3, if defined");
        }
      }
    }
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final Integer optPioValue = options.getValue(optPio);
    final Integer optSmValue = options.getValue(optSm);
    final Boolean optEnableValue = options.getValue(optEnable);
    final int pioNumFirst = optPioValue != null ? optPioValue : 0;
    final int pioNumLast =
      optPioValue != null ? optPioValue : Constants.PIO_NUM - 1;
    final int smNumFirst = optSmValue != null ? optSmValue : 0;
    final int smNumLast =
      optSmValue != null ? optSmValue : Constants.SM_COUNT - 1;
    if (optEnableValue != null) {
      final boolean enable = optEnableValue;
      for (int pioNum = pioNumFirst; pioNum <= pioNumLast; pioNum++) {
        final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
        for (int smNum = smNumFirst; smNum <= smNumLast; smNum++) {
          pioSdk.smSetEnabled(smNum, enable);
        }
      }
    } else {
      for (int pioNum = pioNumFirst; pioNum <= pioNumLast; pioNum++) {
        final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
        for (int smNum = smNumFirst; smNum <= smNumLast; smNum++) {
          final boolean enabled = pioSdk.smGetEnabled(smNum);
          console.printf("(pio%d:sm%d) %s%n", pioNum, smNum,
                         enabled ? "enabled" : "disabled");
        }
      }
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
