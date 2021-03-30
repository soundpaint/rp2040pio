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

/**
 * Monitor "unassemble" command.
 */
public class Unassemble extends Command
{
  private static final String fullName = "unassemble";
  private static final String singleLineDescription =
    "unassemble program memory";

  private static final CmdOptions.IntegerOptionDeclaration optStart =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", 0,
                                   "start address");
  private static final CmdOptions.IntegerOptionDeclaration optCount =
    CmdOptions.createIntegerOption("COUNT", false, 'c', "count",
                                   Constants.MEMORY_SIZE,
                                   "number of instructions to unassemble");

  private final PIOSDK pioSdk;

  public Unassemble(final PrintStream out, final PIOSDK pioSdk)
  {
    super(out, fullName, singleLineDescription,
          new CmdOptions.OptionDeclaration<?>[] { optStart, optCount });
    if (pioSdk == null) {
      throw new NullPointerException("pioSdk");
    }
    this.pioSdk = pioSdk;
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final int count = options.getValue(optCount);
    if (count == 0) return true;
    final int startAddress = options.getValue(optStart);
    final int stopAddress =
      (startAddress + count) & (Constants.MEMORY_SIZE - 1);
    int address = startAddress;
    do {
      final PIOSDK.InstructionInfo instructionInfo =
        pioSdk.getMemoryInstruction(0, address, true);
      out.println(instructionInfo.getToolTipText());
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
