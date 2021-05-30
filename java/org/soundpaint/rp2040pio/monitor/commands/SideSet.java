/*
 * @(#)SideSet.java 1.00 21/04/03
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
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "side-set" provides the same functionality like the
 * PIOASM directive ".side_set".
 */
public class SideSet extends Command
{
  private static final String fullName = "side-set";
  private static final String singleLineDescription =
    "display or control a state machine's side-set configuration";
  private static final String notes =
    "Options -p and -s select the state machine that this command%n" +
    "applies to.  Default is PIO0 and SM0.%n" +
    "%n" +
    "If none of the options -c, -b, ±o, ±d is specified, the currently%n" +
    "configured side-set of the selected state machine will be%n" +
    "displayed.  If at least one of the options -c, -b, ±o, ±d is%n" +
    "specified, the corresponding settings will be adjusted, while for%n" +
    "those not specified the corresponding settings will keep unmodified.";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", 0,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optCount =
    CmdOptions.createIntegerOption("COUNT", false, 'c', "count", null,
                                   "number of side-set bits to be " +
                                   "used (0…5)");
  private static final CmdOptions.IntegerOptionDeclaration optBase =
    CmdOptions.createIntegerOption("NUMBER", false, 'b', "base", null,
                                   "base GPIO pin (0…31) number "+
                                   "of side-set");
  protected static final CmdOptions.BooleanOptionDeclaration optOpt =
    CmdOptions.createBooleanOption(false, 'o', "opt", null,
                                   "make side-set values optional for " +
                                   "instructions");
  protected static final CmdOptions.BooleanOptionDeclaration optPinDirs =
    CmdOptions.createBooleanOption(false, 'd', "pindirs", null,
                                   "apply side-set values to the PINDIRs and " +
                                   "not the PINs");

  private final SDK sdk;

  public SideSet(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optCount, optBase, optOpt, optPinDirs });
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
      final Integer optCountValue = options.getValue(optCount);
      if (optCountValue != null) {
        final int count = optCountValue;
        if ((count < 0) || (count > 5)) {
          throw new CmdOptions.
            ParseException("count must be in the range 0…5");
        }
      }
      final Integer optBaseValue = options.getValue(optBase);
      if (optBaseValue != null) {
        final int base = optBaseValue;
        if ((base < 0) || (base > 31)) {
          throw new CmdOptions.
            ParseException("base must be in the range 0…31");
        }
      }
    }
  }

  private void displaySideSet(final int pioNum, final int smNum,
                              final SDK sdk)
    throws IOException
  {
    final int pinCtrlAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    final int pinCtrl = sdk.readAddress(pinCtrlAddress);
    final int count =
      (pinCtrl & Constants.SM0_PINCTRL_SIDESET_COUNT_BITS) >>>
      Constants.SM0_PINCTRL_SIDESET_COUNT_LSB;
    final int base =
      (pinCtrl & Constants.SM0_PINCTRL_SIDESET_BASE_BITS) >>>
      Constants.SM0_PINCTRL_SIDESET_BASE_LSB;
    final int execCtrlAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final int execCtrl = sdk.readAddress(execCtrlAddress);
    final boolean opt =
      ((execCtrl & Constants.SM0_EXECCTRL_SIDE_EN_BITS) >>>
       Constants.SM0_EXECCTRL_SIDE_EN_LSB) != 0x0;
    final boolean pinDirs =
      ((execCtrl & Constants.SM0_EXECCTRL_SIDE_PINDIR_BITS) >>>
       Constants.SM0_EXECCTRL_SIDE_PINDIR_LSB) != 0x0;
    final int nettoCount = opt ? count - 1 : count;
    console.printf("(pio%d:sm%d) count=%d, base=%d, opt=%s, pindirs=%s%n",
                   pioNum, smNum, nettoCount, base, opt, pinDirs);
  }

  private void setSideSetCount(final int pioNum, final int smNum,
                               final SDK sdk, final int nettoCount)
    throws IOException
  {
    final int execCtrlAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final int execCtrl = sdk.readAddress(execCtrlAddress);
    final boolean opt =
      ((execCtrl & Constants.SM0_EXECCTRL_SIDE_EN_BITS) >>>
       Constants.SM0_EXECCTRL_SIDE_EN_LSB) != 0x0;
    if (opt && (nettoCount == 5)) {
      console.printf("(pio%d:sm%d) ERROR: can not set count to 5, " +
                     "since set side-set opt is set%n",
                     pioNum, smNum);
    } else {
      final int count = nettoCount + (opt ? 1 : 0);
      final int pinCtrlAddress =
        PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
      final int writeMask = Constants.SM0_PINCTRL_SIDESET_COUNT_BITS;
      final int values = count << Constants.SM0_PINCTRL_SIDESET_COUNT_LSB;
      sdk.hwWriteMasked(pinCtrlAddress, values, writeMask);
      console.printf("(pio%d:sm%d) set side-set count to %d%n",
                     pioNum, smNum, nettoCount);
    }
  }

  private void setSideSetBase(final int pioNum, final int smNum,
                              final SDK sdk, final int base)
    throws IOException
  {
    final int pinCtrlAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
    final int writeMask = Constants.SM0_PINCTRL_SIDESET_BASE_BITS;
    final int values =
      base << Constants.SM0_PINCTRL_SIDESET_BASE_LSB;
    sdk.hwWriteMasked(pinCtrlAddress, values, writeMask);
    console.printf("(pio%d:sm%d) set side-set base GPIO pin to %d%n",
                   pioNum, smNum, base);
  }

  private void setSideSetOpt(final int pioNum, final int smNum,
                             final SDK sdk, final boolean opt)
    throws IOException
  {
    final int execCtrlAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final int execCtrl = sdk.readAddress(execCtrlAddress);
    if (opt !=
        (((execCtrl & Constants.SM0_EXECCTRL_SIDE_EN_BITS) >>>
          Constants.SM0_EXECCTRL_SIDE_EN_LSB) != 0x0)) {
      /*
       * Note: From this command's perspective, it is a RP2040's
       * design flaw that the SMx_PINCTRL_SIDESET_COUNT is inclusive
       * of the enable bit.  When the user changes side-set opt only,
       * they do not expect to change the available pin bits.  For
       * example, pioasm will allocate *three* side-set bits when
       * declaring ".side_set 2 opt", but only two, when the "opt" is
       * dropped.  Consequently, when changing side-set opt *only*, we
       * have also have to update the number of side-set bits.
       *
       * TODO: Need to lock this block (and any other writer to
       * side-set configuration) as critical section to avoid
       * corruption, if there are concurrent writers, such as another
       * Monitor instance.
       */
      final int pinCtrlAddress =
        PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_PINCTRL);
      final int pinCtrl = sdk.readAddress(pinCtrlAddress);
      final int count =
        (pinCtrl & Constants.SM0_PINCTRL_SIDESET_COUNT_BITS) >>>
        Constants.SM0_PINCTRL_SIDESET_COUNT_LSB;
      if (opt && (count == 5)) {
        console.printf("(pio%d:sm%d) ERROR: can not set opt, since side-set " +
                       "count is set to 5.%n",
                       pioNum, smNum);
      } else {
        final int optBits =
          opt ? 0x1 << Constants.SM0_EXECCTRL_SIDE_EN_LSB : 0x0;
        sdk.hwWriteMasked(execCtrlAddress, optBits,
                          Constants.SM0_EXECCTRL_SIDE_EN_BITS);
        final int newCount = count + (opt ? 1 : (count > 0 ? - 1 : 0));
        final int countBits =
          newCount << Constants.SM0_PINCTRL_SIDESET_COUNT_LSB;
        sdk.hwWriteMasked(pinCtrlAddress, countBits,
                          Constants.SM0_PINCTRL_SIDESET_COUNT_BITS);
        console.printf("(pio%d:sm%d) set side-set opt=%s%n",
                       pioNum, smNum, opt);
      }
    } else {
      console.printf("(pio%d:sm%d) set side-set opt=%s%n",
                     pioNum, smNum, opt);
    }
  }

  private void setSideSetPinDirs(final int pioNum, final int smNum,
                                 final SDK sdk, final boolean pinDirs)
    throws IOException
  {
    final int execCtrlAddress =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final int writeMask = Constants.SM0_EXECCTRL_SIDE_PINDIR_BITS;
    final int values =
      pinDirs ? 0x1 << Constants.SM0_EXECCTRL_SIDE_PINDIR_LSB : 0x0;
    sdk.hwWriteMasked(execCtrlAddress, values, writeMask);
    console.printf("(pio%d:sm%d) set side-set pindirs=%s%n",
                   pioNum, smNum, pinDirs);
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
    final Integer optCountValue = options.getValue(optCount);
    final Integer optBaseValue = options.getValue(optBase);
    final Boolean optOptValue = options.getValue(optOpt);
    final Boolean optPinDirsValue = options.getValue(optPinDirs);
    if ((optCountValue == null) && (optBaseValue == null) &&
        (optOptValue == null) && (optPinDirsValue == null)) {
      displaySideSet(pioNum, smNum, sdk);
    } else {
      if (optCountValue != null) {
        setSideSetCount(pioNum, smNum, sdk, optCountValue);
      }
      if (optBaseValue != null) {
        setSideSetBase(pioNum, smNum, sdk, optBaseValue);
      }
      if (optOptValue != null) {
        setSideSetOpt(pioNum, smNum, sdk, optOptValue);
      }
      if (optPinDirsValue != null) {
        setSideSetPinDirs(pioNum, smNum, sdk, optPinDirsValue);
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
