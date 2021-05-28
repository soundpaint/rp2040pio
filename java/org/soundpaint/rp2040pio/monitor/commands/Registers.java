/*
 * @(#)Registers.java 1.00 21/04/05
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
 * Monitor command "registers" displays or modifies a state machine's
 * internal registers.
 */
public class Registers extends Command
{
  private static final String fullName = "registers";
  private static final String singleLineDescription =
    "display or change internal registers of a state machine";
  private static final String notes =
    "If none of the register options is specified, the status of%n"+
    "all those registers is displayed.%n" +
    "Otherwise, for all specified register options, the corresponding%n" +
    "register is set to the specified value.";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optRegX =
    CmdOptions.createIntegerOption("VALUE", false, 'x', "x", null,
                                   "set value of register X");
  private static final CmdOptions.IntegerOptionDeclaration optRegY =
    CmdOptions.createIntegerOption("VALUE", false, 'y', "y", null,
                                   "set value of register Y");
  private static final CmdOptions.IntegerOptionDeclaration optAddress =
    CmdOptions.createIntegerOption("VALUE", false, 'a', "address", null,
                                   "set value of PC to specified address");
  private static final CmdOptions.IntegerOptionDeclaration optIsr =
    CmdOptions.createIntegerOption("VALUE", false, 'i', "isr", null,
                                   "set value of ISR register");
  private static final CmdOptions.IntegerOptionDeclaration optIsrShiftCount =
    CmdOptions.createIntegerOption("VALUE", false, 'k', "isrshiftcount", null,
                                   "set value of ISR shift count register");
  private static final CmdOptions.IntegerOptionDeclaration optOsr =
    CmdOptions.createIntegerOption("VALUE", false, 'o', "osr", null,
                                   "set value of OSR register");
  private static final CmdOptions.IntegerOptionDeclaration optOsrShiftCount =
    CmdOptions.createIntegerOption("VALUE", false, 'q', "osrshiftcount", null,
                                   "set value of OSR shift count register");

  private final SDK sdk;

  public Registers(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optRegX, optRegY, optAddress,
              optIsr, optIsrShiftCount, optOsr, optOsrShiftCount });
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

  private void displayRegisters(final int pioNum, final int smNum)
    throws IOException
  {
    final int addressRegX =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_REGX);
    final int regXValue = sdk.readAddress(addressRegX);
    final int addressRegY =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_REGY);
    final int regYValue = sdk.readAddress(addressRegY);
    final int addressPC =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_PC);
    final int pcValue = sdk.readAddress(addressPC);
    final int addressISR =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_ISR);
    final int isrValue = sdk.readAddress(addressISR);
    final int addressISRShiftCount =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_ISR_SHIFT_COUNT);
    final int isrShiftCountValue = sdk.readAddress(addressISRShiftCount);
    final int addressOSR =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_OSR);
    final int osrValue = sdk.readAddress(addressOSR);
    final int addressOSRShiftCount =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_OSR_SHIFT_COUNT);
    final int osrShiftCountValue = sdk.readAddress(addressOSRShiftCount);
    console.printf("(pio%d:sm%d) X=%08x, Y=%08x, PC=%02x%n",
                   pioNum, smNum, regXValue, regYValue, pcValue);
    console.printf("           ISR=%08x, ISR_SHIFT_COUNT=%08x%n",
                   isrValue, isrShiftCountValue);
    console.printf("           OSR=%08x, OSR_SHIFT_COUNT=%08x%n",
                   osrValue, osrShiftCountValue);
  }

  private void setEmuRegister(final int pioNum, final int smNum,
                              final int value, final int digits,
                              final PIOEmuRegisters.Regs register)
    throws IOException
  {
    if (digits <= 0) {
      throw new IllegalArgumentException("digits <= 0");
    }
    final int address = PIOEmuRegisters.getSMAddress(pioNum, smNum, register);
    sdk.writeAddress(address, value);
    console.printf("(pio%d:sm%d) set %s to value 0x%0" + digits + "x%n",
                   pioNum, smNum, register, value);
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
    final Integer optRegXValue = options.getValue(optRegX);
    final Integer optRegYValue = options.getValue(optRegY);
    final Integer optAddressValue = options.getValue(optAddress);
    final Integer optIsrValue = options.getValue(optIsr);
    final Integer optIsrShiftCountValue = options.getValue(optIsrShiftCount);
    final Integer optOsrValue = options.getValue(optOsr);
    final Integer optOsrShiftCountValue = options.getValue(optOsrShiftCount);
    if ((optRegXValue == null) && (optRegYValue == null) &&
        (optAddressValue == null) &&
        (optIsrValue == null) && (optIsrShiftCountValue == null) &&
        (optOsrValue == null) && (optOsrShiftCountValue == null)) {
      displayRegisters(pioNum, smNum);
    }
    if (optRegXValue != null) {
      setEmuRegister(pioNum, smNum, optRegXValue, 8,
                     PIOEmuRegisters.Regs.SM0_REGX);
    }
    if (optRegYValue != null) {
      setEmuRegister(pioNum, smNum, optRegYValue, 8,
                     PIOEmuRegisters.Regs.SM0_REGY);
    }
    if (optAddressValue != null) {
      setEmuRegister(pioNum, smNum,
                     optAddressValue & (Constants.MEMORY_SIZE - 1), 2,
                     PIOEmuRegisters.Regs.SM0_PC);
    }
    if (optIsrValue != null) {
      setEmuRegister(pioNum, smNum, optIsrValue, 8,
                     PIOEmuRegisters.Regs.SM0_ISR);
    }
    if (optIsrShiftCountValue != null) {
      setEmuRegister(pioNum, smNum, optIsrShiftCountValue, 8,
                     PIOEmuRegisters.Regs.SM0_ISR_SHIFT_COUNT);
    }
    if (optOsrValue != null) {
      setEmuRegister(pioNum, smNum, optOsrValue, 8,
                     PIOEmuRegisters.Regs.SM0_OSR);
    }
    if (optOsrShiftCountValue != null) {
      setEmuRegister(pioNum, smNum, optOsrShiftCountValue, 8,
                     PIOEmuRegisters.Regs.SM0_OSR_SHIFT_COUNT);
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
