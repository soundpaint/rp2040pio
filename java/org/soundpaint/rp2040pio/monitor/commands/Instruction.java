/*
 * @(#)Instruction.java 1.00 21/05/07
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
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "instruction" shows last executed and pending
 * instructions or adds an instruction to be executed immediately,
 * i.e. during the next clock cycle.
 */
public class Instruction extends Command
{
  private static final String fullName = "instruction";
  private static final String singleLineDescription =
    "show last executed and pending instructions or force instruction to " +
    "be executed";
  private static final String notes =
    "Options -p and -s select the state machine that this command%n" +
    "applies to.  Default is PIO0 and SM0.%n" +
    "%n" +
    "Use option -f to specify the op-code to store for forced execution%n" +
    "during the next clock cycle.%n" +
    "If options -f is not specified, the last executed instructio,%n" +
    "and, if any, the pending forced instruction are displayed.";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optForce =
    CmdOptions.createIntegerOption("NUMBER", false, 'f', "force", null,
                                   "force instruction with the specified " +
                                   "op-code to be executed");

  private final SDK sdk;

  public Instruction(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription,
          new CmdOptions.OptionDeclaration<?>[] { optPio, optSm, optForce });
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    this.sdk = sdk;
  }

  @Override
  protected void checkValidity(final CmdOptions options)
    throws CmdOptions.ParseException
  {
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

  private void displayInstructions(final int pioNum, final int smNum)
    throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    final PIOSDK.InstructionInfo currentInstrInfo =
      pioSdk.getCurrentInstruction(smNum, true, true);
    console.printf("(pio%d:sm%d) last executed: %s%n", pioNum, smNum,
                   currentInstrInfo.getFullStatement());
    final int forcedInstrAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_FORCED_INSTR);
    final int forcedInstr = sdk.readAddress(forcedInstrAddress);
    if ((forcedInstr & 0x00010000) != 0x0) {
      final PIOSDK.InstructionInfo forcedInstrInfo =
        pioSdk.getInstructionFromOpCode(smNum,
                                        Constants.INSTR_ORIGIN_FORCED,
                                        "", forcedInstr & 0xffff,
                                        true, false, 0);
      console.printf("(pio%d:sm%d) force instruction: %s%n", pioNum, smNum,
                     forcedInstrInfo.getFullStatement());
    }
  }

  private void forceInstruction(final int pioNum, final int smNum,
                                final int opCode)
    throws IOException
  {
    final int instrAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_INSTR);
    sdk.writeAddress(instrAddress, opCode);
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    final PIOSDK.InstructionInfo forcedInstrInfo =
      pioSdk.getInstructionFromOpCode(smNum, Constants.INSTR_ORIGIN_FORCED,
                                      "", opCode, true, false, 0);
    console.printf("(pio%d:sm%d) set force instruction: %s%n", pioNum, smNum,
                   forcedInstrInfo.getFullStatement());
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
    final Integer optForceValue = options.getValue(optForce);
    if (optForceValue == null) {
      displayInstructions(pioNum, smNum);
    } else {
      forceInstruction(pioNum, smNum, optForceValue);
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
