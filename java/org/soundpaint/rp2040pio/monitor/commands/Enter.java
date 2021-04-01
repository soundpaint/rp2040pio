/*
 * @(#)Enter.java 1.00 21/03/31
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "enter" lets the user enter instruction opcodes to
 * be stored in a PIO's program memory.
 */
public class Enter extends Command
{
  private static final String fullName = "enter";
  private static final String singleLineDescription =
    "enter instruction opcodes; exit by entering an empty line";

  private final SDK sdk;
  private final BufferedReader in;

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", null,
                                   "start address");

  public Enter(final PrintStream out, final SDK sdk, final BufferedReader in)
  {
    super(out, fullName, singleLineDescription,
          new CmdOptions.OptionDeclaration<?>[] { optPio, optAddress });
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    if (in == null) {
      throw new NullPointerException("in");
    }
    this.sdk = sdk;
    this.in = in;
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
    final Integer startAddress = options.getValue(optAddress);
    int address =
      startAddress != null ?
      startAddress & Constants.MEMORY_SIZE - 1 :
      0;
    out.println("per input line, enter 16 bit hex word without '0x' prefix");
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    int count = 0;
    while (true) {
      final int currentValue =
        sdk.readAddress(PIOEmuRegisters.getMemoryAddress(pioNum, address));
      out.printf("(pio%d) %02x: (%04x) ", pioNum, address, currentValue);
      final String line = in.readLine().trim();
      if ((line == null) || line.isEmpty()) break;
      try {
        final int value = Integer.parseInt(line, 16);
        sdk.writeAddress(PIORegisters.getMemoryAddress(pioNum, address), value);
        //storeOpCode(address, value, pioNum);
        final PIOSDK.InstructionInfo instructionInfo =
          pioSdk.getMemoryInstruction(0, address, false, true);
        out.println("                  " + instructionInfo.getToolTipText());
      } catch (final NumberFormatException e) {
        out.println("not a valid 16 bit word: " + line);
        continue;
      }
      address = (address + 1) & Constants.MEMORY_SIZE - 1;
      count++;
    }
    out.printf("entered %d words%n", count);
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
