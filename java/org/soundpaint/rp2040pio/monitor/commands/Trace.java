/*
 * @(#)Trace.java 1.00 21/03/28
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
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "trace" lets the emulator execute clock cycles step
 * by step.
 */
public class Trace extends Command
{
  private static final String fullName = "trace";
  private static final String singleLineDescription =
    "trace program by performing a number of clock cycles";

  private static final CmdOptions.IntegerOptionDeclaration optCycles =
    CmdOptions.createIntegerOption("COUNT", false, 'c', "cycles", 1,
                                   "number of cycles to apply");
  private static final int[][] addressPioSmPc = {{
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_PC),
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM1_PC),
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM2_PC),
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM3_PC)
    }, {
      PIOEmuRegisters.getAddress(1, PIOEmuRegisters.Regs.SM0_PC),
      PIOEmuRegisters.getAddress(1, PIOEmuRegisters.Regs.SM1_PC),
      PIOEmuRegisters.getAddress(1, PIOEmuRegisters.Regs.SM2_PC),
      PIOEmuRegisters.getAddress(1, PIOEmuRegisters.Regs.SM3_PC)
    }};

  private final SDK sdk;

  public Trace(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription,
          new CmdOptions.OptionDeclaration<?>[] { optCycles });
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    this.sdk = sdk;
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final int cycles = options.getValue(optCycles);
    for (int i = 0; i < cycles; i++) {
      sdk.triggerCyclePhase0(true);
      sdk.triggerCyclePhase1(true);
      for (int pioNum = 0; pioNum < Constants.PIO_NUM; pioNum++) {
        for (int smNum = 0; smNum < Constants.SM_COUNT; smNum++) {
          console.printf("(pio%d:sm%d) PC=%02x%n", pioNum, smNum,
                         sdk.readAddress(addressPioSmPc[pioNum][smNum]));
        }
      }
      // TODO: Print gpioSdk.asBitArrayDisplay()) ?
    }
    console.println(cycles + " clock cycle" + (cycles != 1 ? "s" : "") +
                    " executed.");
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
