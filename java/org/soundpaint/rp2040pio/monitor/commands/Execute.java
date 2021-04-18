/*
 * @(#)Execute.java 1.00 21/04/04
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
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "execute" writes an instruction for immediate
 * execution (including jumps) and then resuming execution.
 *
 * Note: The idea of this command is to provide explicit access to the
 * PIO SMx_INSTR registers.  Downside of this approach is, that
 * writing to the registers schedules an instruction for execution
 * during the _next_ clock cycle, while reading from the register will
 * return the instruction that is executed during the _current_ clock
 * cycle, that is, in general, a different instruction than the
 * inserted one.  Also, once inserted, there is no way to revert
 * insertion.  An alternative approach is to separate both functions:
 * Provide a command "insert" for inserting an instruction to be
 * executed in the upcoming clock cycle, for showing the inserted
 * instruction, if any, and for deleting the inserted instruction, if
 * any; and also, have a separate command for showing the currently
 * executed instruction (maybe show it together with the registers via
 * the "registers" command).
 */
public class Execute extends Command
{
  private static final String fullName = "execute";
  private static final String singleLineDescription =
    "write instruction for immediate execution or display " +
    "instruction currently executed";
  private static final String notes =
    "Writes an instruction for immediate execution (including jumps)%n" +
    "and then resuming execution or displays the currently excuted%n" +
    "instruction.  Immediate execution means execution during the next%n" +
    "clock cycle.%n" +
    "%n" +
    "Options -p and -s select the state machine that this command%n" +
    "applies to.  Default is PIO0 and SM0.%n" +
    "%n" +
    "If option -i is not specified, the instruction currently being%n" +
    "executed by the selected state machine will be displayed.%n" +
    "%n" +
    "If option -i is specified, the specified instruction is written%n" +
    "for immediate execution.";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optInstruction =
    CmdOptions.createIntegerOption("CODE", false, 'i', "instruction", null,
                                   "opcode of instruction to be executed");

  private final SDK sdk;

  public Execute(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optInstruction });
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

  private void displayInstruction(final int pioNum, final int smNum,
                                  final PIOSDK pioSdk, final int instr)
    throws IOException
  {
    final PIOSDK.InstructionInfo instructionInfo =
      pioSdk.getInstructionFromOpCode(smNum, "", instr, false, false, 0);
    console.printf("(pio%d:sm%d) %s%n",
                   pioNum, smNum, instructionInfo.getToolTipText());
  }

  private void displayCurrentInstruction(final int pioNum, final int smNum,
                                         final SDK sdk, final PIOSDK pioSdk)
    throws IOException
  {
    final int instrAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_INSTR);
    final int instr = sdk.readAddress(instrAddress);
    console.println("instruction currently being executed:");
    displayInstruction(pioNum, smNum, pioSdk, instr);
  }

  private void writeInstruction(final int pioNum, final int smNum,
                                final SDK sdk, final PIOSDK pioSdk,
                                final int instr)
    throws IOException
  {
    final int instrAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_INSTR);
    sdk.writeAddress(instrAddress, instr);
    console.println("instruction written for insertion:");
    displayInstruction(pioNum, smNum, pioSdk, instr);
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
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    final Integer optInstructionValue = options.getValue(optInstruction);
    if (optInstructionValue == null) {
      displayCurrentInstruction(pioNum, smNum, sdk, pioSdk);
    } else {
      writeInstruction(pioNum, smNum, sdk, pioSdk, optInstructionValue);
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
