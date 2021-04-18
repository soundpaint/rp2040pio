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
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
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
  private static final String notes =
    "Memory locations marked as allocated are prefixed with leading 'X'.%n" +
    "%n" +
    "Note that tracking memory allocation is not a feature of the%n" +
    "RP2040, but local to this monitor instance, just to avoid%n" +
    "accidentally overwriting your own PIO programs.  Other applications%n" +
    "that concurrently access the RP2040 will therefore ignore%n" +
    "this instance's allocation tracking and may arbitrarily%n" +
    "overwrite allocated PIO memory, using their own allocation scheme.%n" +
    "%n" +
    "Note that the same PIO program may unassemble to differently%n" +
    "displayed instructions for different state machines, since%n" +
    "some settings specific to a particular state machine, such as%n" +
    "side-set count, will affect interpretation of op-codes.%n" +
    "Therefore, the unassemble command supports the \"sm\" argument%n" +
    "for displaying the instructions as interpreted by the selected%n" +
    "state machine, according to its current settings.";
  private static final String lockedSymbol = "🔒";
  private static final String unlockedSymbol = "  ";
  private static final String wrapSymbol = "← ";
  private static final String wrapTargetSymbol = "→ ";
  private static final String selfWrapSymbol = "↔ ";
  private static final String noWrapSymbol = "  ";
  private static final String breakPointSymbol = "🛑";
  private static final String noBreakPointSymbol = "  ";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optStart =
    CmdOptions.createIntegerOption("ADDRESS", false, 'a', "address", 0,
                                   "start address (0x00…0x1f)");
  private static final CmdOptions.IntegerOptionDeclaration optCount =
    CmdOptions.createIntegerOption("COUNT", false, 'c', "count",
                                   Constants.MEMORY_SIZE,
                                   "number of instructions to unassemble");

  private final SDK sdk;

  public Unassemble(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
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
    final int startAddress =
      options.getValue(optStart) & (Constants.MEMORY_SIZE - 1);
    final int stopAddress =
      (startAddress + count) & (Constants.MEMORY_SIZE - 1);
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    final int addressAddr =
      PIORegisters.getSMAddress(pioNum, smNum,
                                PIORegisters.Regs.SM0_ADDR);
    final int addrValue =
      sdk.readAddress(addressAddr) & (Constants.MEMORY_SIZE - 1);
    final int memoryAllocation = pioSdk.getMemoryAllocation();
    final int addressExecCtrl =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final int execCtrl = sdk.readAddress(addressExecCtrl);
    final int wrap =
      (execCtrl & Constants.SM0_EXECCTRL_WRAP_TOP_BITS) >>>
      Constants.SM0_EXECCTRL_WRAP_TOP_LSB;
    final int wrapTarget =
      (execCtrl & Constants.SM0_EXECCTRL_WRAP_BOTTOM_BITS) >>>
      Constants.SM0_EXECCTRL_WRAP_BOTTOM_LSB;
    final int addressBreakPoints =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_BREAKPOINTS);
    final int breakPoints = sdk.readAddress(addressBreakPoints);
    int address = startAddress;
    do {
      final boolean isCurrentAddr = address == addrValue;
      final PIOSDK.InstructionInfo instructionInfo =
        pioSdk.getMemoryInstruction(smNum, address, true, true);
      final boolean isAllocated = ((memoryAllocation >>> address) & 0x1) != 0x0;
      final boolean isWrap = address == wrap;
      final boolean isWrapTarget = address == wrapTarget;
      final String displayWrap =
        isWrap ?
        (isWrapTarget ? selfWrapSymbol : wrapSymbol) :
        (isWrapTarget ? wrapTargetSymbol : noWrapSymbol);
      final boolean isBreakPoint = ((breakPoints >>> address) & 0x1) != 0x0;
      if (isCurrentAddr) {
        console.printf("\u001b[38;5;196m");
      }
      console.printf("(pio%d:sm%d) %s %s  %s%s%n",
                     pioNum, smNum,
                     (isBreakPoint ? breakPointSymbol : noBreakPointSymbol),
                     (isAllocated ? lockedSymbol : unlockedSymbol),
                     displayWrap,
                     instructionInfo.getToolTipText());
      if (isCurrentAddr) {
        console.printf("\u001b[0m");
      }
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
