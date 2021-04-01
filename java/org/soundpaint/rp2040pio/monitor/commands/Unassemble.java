/*
 * @(#)Unassemble.java 1.00 21/03/28
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
 * Monitor command "unassemble" displays instructions of a PIO's
 * instruction memory in a human-readable form.
 */
public class Unassemble extends Command
{
  private static final String fullName = "unassemble";
  private static final String singleLineDescription =
    "unassemble program memory";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optStart =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", 0,
                                   "start address");
  private static final CmdOptions.IntegerOptionDeclaration optCount =
    CmdOptions.createIntegerOption("COUNT", false, 'c', "count",
                                   Constants.MEMORY_SIZE,
                                   "number of instructions to unassemble");

  /*
   * TODO: While a PIO's instruction memory is shared among all of the
   * 4 state machines of that PIO, the unassembled form of the same
   * instruction still may vary among different state machines, since
   * the number of side-set / delay bits may vary according to the
   * PINCTRL_SIDESET_COUNT and EXECCTR_SIDE_EN configuration of each
   * state machine.
   *
   * Consequently, for unassembling program memory with correctly
   * shown side-set and delay values, one needs to know for which
   * state machine it is unassembled.  The selected state machine's
   * side-set / delay configuration should then be read and used to
   * correctly unassemble instructions.
   *
   * Therefore, we need another option argument for selecting the
   * state machine (0..3), maybe with state machine 0 as default.
   */

  private final SDK sdk;

  public Unassemble(final PrintStream out, final SDK sdk)
  {
    super(out, fullName, singleLineDescription,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optStart, optCount });
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
      if ((pioNum < 0) || (pioNum > 1)) {
        throw new CmdOptions.
          ParseException("PIO number must be either 0 or 1");
      }
      final int smNum = options.getValue(optSm);
      if ((smNum < 0) || (smNum > 1)) {
        throw new CmdOptions.
          ParseException("SM number must be one of 0, 1, 2 or 3");
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
    final int pioNum = options.getValue(optPio);
    final int smNum = options.getValue(optSm);
    final int count = options.getValue(optCount);
    if (count == 0) return true;
    final int startAddress = options.getValue(optStart);
    final int stopAddress =
      (startAddress + count) & (Constants.MEMORY_SIZE - 1);
    int address = startAddress;
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    do {
      final PIOSDK.InstructionInfo instructionInfo =
        pioSdk.getMemoryInstruction(smNum, address, true, true);
      out.println("(pio" + pioNum + ") " + instructionInfo.getToolTipText());
      address = (address + 1) & (Constants.MEMORY_SIZE - 1);
    } while (address != stopAddress);
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
