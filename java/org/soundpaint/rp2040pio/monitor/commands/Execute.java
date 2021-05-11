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
import org.soundpaint.rp2040pio.PIOEmuRegisters;
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
    "set instruction for immediate execution or display " +
    "instructions currently executed or pending for execution";
  private static final String notes =
    "Writes an instruction for immediate execution (including jumps)%n" +
    "and then resuming execution or displays the currently excuted%n" +
    "instruction.  Immediate execution means execution during the next%n" +
    "clock cycle.%n" +
    "%n" +
    "Options -p and -s select the state machine that this command%n" +
    "applies to.  Default is PIO0 and SM0.%n" +
    "%n" +
    "If neither of options -c, -d, -e, -f is specified, the instruction%n" +
    "currently being executed by the selected state machine and any%n" +
    "pending forced or EXEC'd instruction will be displayed.%n" +
    "%n" +
    "If option -f is specified, the specified instruction is written%n" +
    "for immediate execution (forced instruction).%n" +
    "If option -c is specified, any pending forced instruction%n" +
    "will be cancelled.%n" +
    "If option -e is specified, the specified instruction is written%n" +
    "for execution on the next enabled clock cycle (EXEC'd instruction),%n" +
    "provided that there is no pending forced instruction that would have%n" +
    "higher priority of execution.%n" +
    "If option -d is specified, any pending EXEC'd instruction%n" +
    "will be deleted.";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optForce =
    CmdOptions.createIntegerOption("CODE", false, 'f', "force", null,
                                   "set or overwrite opcode of forced " +
                                   "instruction to be executed");
  private static final CmdOptions.IntegerOptionDeclaration optExec =
    CmdOptions.createIntegerOption("CODE", false, 'e', "exec", null,
                                   "set or overwrite opcode of EXEC'd " +
                                   "instruction to be executed");
  private static final CmdOptions.FlagOptionDeclaration optCancel =
    CmdOptions.createFlagOption(false, 'c', "cancel", CmdOptions.Flag.OFF,
                                "cancel pending forced instruction, if any");
  private static final CmdOptions.FlagOptionDeclaration optDelete =
    CmdOptions.createFlagOption(false, 'd', "delete", CmdOptions.Flag.OFF,
                                "delete pending EXEC'd instruction, if any");

  private final SDK sdk;

  public Execute(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optForce, optExec, optCancel, optDelete });
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
      if (options.isDefined(optForce) && options.getValue(optCancel).isOn()) {
        throw new CmdOptions.
          ParseException("at most one of options \"-c\" and \"-f\" may be " +
                         "specified at the same time");
      }
      if (options.isDefined(optExec) && options.getValue(optDelete).isOn()) {
        throw new CmdOptions.
          ParseException("at most one of options \"-e\" and \"-d\" may be " +
                         "specified at the same time");
      }
    }
  }

  private int getPendingDelay(final int pioNum, final int smNum)
    throws IOException
  {
    final int addressPendingDelay =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_PENDING_DELAY);
    final int pendingDelay = sdk.readAddress(addressPendingDelay);
    return pendingDelay & 0x1f;
  }

  private void displayInstruction(final int pioNum, final int smNum,
                                  final PIOSDK pioSdk,
                                  final int origin, final int opCode)
    throws IOException
  {
    final PIOSDK.InstructionInfo instructionInfo =
      pioSdk.getInstructionFromOpCode(smNum, origin, "", opCode,
                                      false, false, 0);
    console.printf("(pio%d:sm%d) last executed: %s%n",
                   pioNum, smNum, instructionInfo.getToolTipText());
  }

  private void displayForcedInstruction(final int pioNum, final int smNum,
                                        final SDK sdk, final PIOSDK pioSdk)
    throws IOException
  {
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
      console.printf("(pio%d:sm%d) forced instr : %s%n", pioNum, smNum,
                     forcedInstrInfo.getFullStatement());
    }
  }

  private void displayExecdInstruction(final int pioNum, final int smNum,
                                       final SDK sdk, final PIOSDK pioSdk)
    throws IOException
  {
    final int execdInstrAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_EXECD_INSTR);
    final int execdInstr = sdk.readAddress(execdInstrAddress);
    if ((execdInstr & 0x00010000) != 0x0) {
      final PIOSDK.InstructionInfo execdInstrInfo =
        pioSdk.getInstructionFromOpCode(smNum,
                                        Constants.INSTR_ORIGIN_EXECD,
                                        "", execdInstr & 0xffff,
                                        true, false, 0);
      console.printf("(pio%d:sm%d) execd instr  : %s%n", pioNum, smNum,
                     execdInstrInfo.getFullStatement());
    }
  }

  private void displayPendingDelay(final int pioNum, final int smNum,
                                   final PIOSDK.InstructionInfo currentInstrInfo)
    throws IOException
  {
    final int pendingDelay = getPendingDelay(pioNum, smNum);
    final int totalDelay = currentInstrInfo.getDelay();
    if (totalDelay > 0) {
      console.printf("(pio%d:sm%d) pending delay: %d of %d cycles done%n",
                     pioNum, smNum, totalDelay - pendingDelay, totalDelay);
    }
  }

  private void displayInstructions(final int pioNum, final int smNum,
                                   final SDK sdk, final PIOSDK pioSdk)
    throws IOException
  {
    final PIOSDK.InstructionInfo currentInstrInfo =
      pioSdk.getCurrentInstruction(smNum, true, true);
    console.printf("(pio%d:sm%d) last executed: %s%n", pioNum, smNum,
                   currentInstrInfo.getFullStatement());
    displayForcedInstruction(pioNum, smNum, sdk, pioSdk);
    displayExecdInstruction(pioNum, smNum, sdk, pioSdk);
    displayPendingDelay(pioNum, smNum, currentInstrInfo);
  }

  private void deleteExecdInstruction(final int pioNum, final int smNum,
                                      final SDK sdk, final PIOSDK pioSdk)
    throws IOException
  {
    final int clearExecdAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_CLEAR_EXECD);
    sdk.writeAddress(clearExecdAddress, 0);
    console.printf("(pio%d:sm%d) deleted any pending EXEC'd instruction%n",
                   pioNum, smNum);
  }

  private void setExecdInstruction(final int pioNum, final int smNum,
                                   final SDK sdk, final PIOSDK pioSdk,
                                   final int instr)
    throws IOException
  {
    final int execdInstrAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_EXECD_INSTR);
    sdk.writeAddress(execdInstrAddress, instr);
    console.println("EXEC'd instruction written for pending execution:");
    displayInstruction(pioNum, smNum, pioSdk,
                       Constants.INSTR_ORIGIN_EXECD, instr);
  }

  private void cancelForcedInstruction(final int pioNum, final int smNum,
                                       final SDK sdk, final PIOSDK pioSdk)
    throws IOException
  {
    final int clearForcedAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_CLEAR_FORCED);
    sdk.writeAddress(clearForcedAddress, 0);
    console.printf("(pio%d:sm%d) cancelled any pending forced instruction%n",
                   pioNum, smNum);
  }

  private void setForcedInstruction(final int pioNum, final int smNum,
                                    final SDK sdk, final PIOSDK pioSdk,
                                    final int instr)
    throws IOException
  {
    final int instrAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_INSTR);
    sdk.writeAddress(instrAddress, instr);
    console.println("forced instruction written for pending execution:");
    displayInstruction(pioNum, smNum, pioSdk,
                       Constants.INSTR_ORIGIN_FORCED, instr);
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
    final Integer optForceValue = options.getValue(optForce);
    final Integer optExecValue = options.getValue(optExec);
    if (optForceValue != null) {
      setForcedInstruction(pioNum, smNum, sdk, pioSdk, optForceValue);
    }
    if (options.getValue(optCancel).isOn()) {
      cancelForcedInstruction(pioNum, smNum, sdk, pioSdk);
    }
    if (optExecValue != null) {
      setExecdInstruction(pioNum, smNum, sdk, pioSdk, optExecValue);
    }
    if (options.getValue(optDelete).isOn()) {
      deleteExecdInstruction(pioNum, smNum, sdk, pioSdk);
    }
    if ((optForceValue == null) &&
        (optExecValue == null) &&
        !options.getValue(optCancel).isOn() &&
        !options.getValue(optDelete).isOn()) {
      displayInstructions(pioNum, smNum, sdk, pioSdk);
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
