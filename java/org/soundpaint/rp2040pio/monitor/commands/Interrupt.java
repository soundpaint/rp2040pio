/*
 * @(#)Interrupt.java 1.00 21/06/08
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
 * Monitor command "interrupt" displays or modifies a PIO's interrupt
 * flags configuration and status.
 */
public class Interrupt extends Command
{
  private static final String fullName = "interrupt";
  private static final String singleLineDescription =
    "display or change a PIO's IRQs configuration and status";
  private static final String notes =
    "Use option \"-p\"to select a PIO.%n" +
    "If none of the IRQ modification options is specified, the status%n"+
    "of the IRQ bits of the selected state machine is displayed.%n" +
    "For modification operation, additionally specify either option%n" +
    "\"-s\" for selecting a state machine within the selected PIO when%n" +
    "going to modify an SM specific IRQ flag.  Or specify option \"-i\"%n" +
    "for selecting one of those IRQ flags that are visible to all SMs.%n" +
    "Use option \"-t\" to apply modification on the TXNFULL group of%n" +
    "IRQ flags, or option \"-r\" to apply on the RXNEMPTY group of%n" +
    "IRQ flags; if none of those two options is specified, modification%n" +
    "will affect the SM group of IRQ flags.%n" +
    "For disabling the selected IRQ flag, use option \"-d\".%n" +
    "For enabling the selected IRQ flag, use option \"-e\".%n" +
    "For enforcing the selected IRQ flag to be set, use option \"-f\".%n" +
    "For undoing enforcement, use option \"-u\".%n" +
    "For selecting to which IRQ (IRQ0, IRQ1) option \"-e\", \"-d\",%n" +
    "\"-f\" or \"-u\" will apply, use option \"-0\" or \"-1\".%n" +
    "For setting or clearing one of the IRQs visible to all SMs, use%n" +
    "option \"-v\".";

  private static final CmdOptions.IntegerOptionDeclaration optPio =
    CmdOptions.createIntegerOption("NUMBER", false, 'p', "pio", 0,
                                   "PIO number, either 0 or 1");
  private static final CmdOptions.IntegerOptionDeclaration optSm =
    CmdOptions.createIntegerOption("NUMBER", false, 's', "sm", null,
                                   "SM number, one of 0, 1, 2 or 3");
  private static final CmdOptions.IntegerOptionDeclaration optIrq =
    CmdOptions.createIntegerOption("NUMBER", false, 'i', "irq", null,
                                   "PIO IRQ number (0…7)");
  private static final CmdOptions.FlagOptionDeclaration optTxNFull =
    CmdOptions.createFlagOption(false, 't', "txnfull", CmdOptions.Flag.OFF,
                                "select TXNFULL interrupt flag of the " +
                                "selected state machine for modification");
  private static final CmdOptions.FlagOptionDeclaration optRxNEmpty =
    CmdOptions.createFlagOption(false, 'r', "rxnempty", CmdOptions.Flag.OFF,
                                "select RXNEMPTY interrupt flag of the " +
                                "selected state machine for modification");
  private static final CmdOptions.FlagOptionDeclaration optDisable =
    CmdOptions.createFlagOption(false, 'd', "disable", CmdOptions.Flag.OFF,
                                "override selected interrupt flag to " +
                                "be always cleared");
  private static final CmdOptions.FlagOptionDeclaration optEnable =
    CmdOptions.createFlagOption(false, 'e', "enable", CmdOptions.Flag.OFF,
                                "revert \"-d\" option for the specified" +
                                "interrupt flag");
  private static final CmdOptions.FlagOptionDeclaration optForce =
    CmdOptions.createFlagOption(false, 'f', "force", CmdOptions.Flag.OFF,
                                "override selected interrupt flag to " +
                                "be always set");
  private static final CmdOptions.FlagOptionDeclaration optUnforce =
    CmdOptions.createFlagOption(false, 'u', "unforce", CmdOptions.Flag.OFF,
                                "revert \"-f\" option for the specified" +
                                "interrupt flag");
  private static final CmdOptions.FlagOptionDeclaration optZero =
    CmdOptions.createFlagOption(false, '0', "irq0", CmdOptions.Flag.OFF,
                                "select PIO IRQ0 as override target");
  private static final CmdOptions.FlagOptionDeclaration optOne =
    CmdOptions.createFlagOption(false, '1', "irq1", CmdOptions.Flag.OFF,
                                "select PIO IRQ1 as override target");
  private static final CmdOptions.BooleanOptionDeclaration optValue =
    CmdOptions.createBooleanOption(false, 'v', "value", null,
                                   "set value for the selected IRQ flag " +
                                   "of those visible to all SMs");

  private enum FlagsGroup
  {
    IRQ_SM(8, "IRQ / SM"), TXNFULL(4, "TxNFull"), RXNEMPTY(0, "RxNEmpty");

    private final int lsb;
    private final String label;

    private FlagsGroup(final int lsb, final String label)
    {
      this.lsb = lsb;
      this.label = label;
    }

    public int getLSB() { return lsb; }

    public String getLabel() { return label; }

    public static FlagsGroup fromOptions(final boolean txNFull,
                                         final boolean rxNEmpty)
    {
      if (txNFull && rxNEmpty) {
        throw new IllegalArgumentException("txNFull and rxNEmpty both true");
      }
      return txNFull ? TXNFULL : (rxNEmpty ? RXNEMPTY : IRQ_SM);
    }
  }

  private final SDK sdk;

  public Interrupt(final PrintStream console, final SDK sdk)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optPio, optSm, optIrq, optTxNFull, optRxNEmpty,
              optDisable, optEnable, optForce, optUnforce,
              optZero, optOne, optValue });
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
      final boolean txNFull = options.getValue(optTxNFull).isOn();
      final boolean rxNEmpty = options.getValue(optRxNEmpty).isOn();
      final boolean enable = options.getValue(optEnable).isOn();
      final boolean disable = options.getValue(optDisable).isOn();
      final boolean force = options.getValue(optForce).isOn();
      final boolean unforce = options.getValue(optUnforce).isOn();
      final boolean zero = options.getValue(optZero).isOn();
      final boolean one = options.getValue(optOne).isOn();
      final Boolean optValueValue = options.getValue(optValue);
      final boolean haveMaskingOrForcingOp =
        enable || disable || force || unforce;
      final boolean haveModOp =
        haveMaskingOrForcingOp || (optValueValue != null);
      final Integer optSmValue = options.getValue(optSm);
      if (optSmValue != null) {
        final int smNum = optSmValue;
        if ((smNum < 0) || (smNum > Constants.SM_COUNT - 1)) {
          throw new CmdOptions.
            ParseException("SM number must be one of 0, 1, 2 or 3");
        }
        if (!haveModOp) {
          final String message = "missing option: -e, -d, -f, -u, -l or -h";
          throw new CmdOptions.ParseException(message);
        }
      }
      final Integer optIrqValue = options.getValue(optIrq);
      if (optIrqValue != null) {
        final int irqNum = optIrqValue;
        if ((irqNum < 0) || (irqNum > 7)) {
          throw new CmdOptions.
            ParseException("IRQ number must be in the range 0…7");
        }
        if (optValueValue == null) {
          throw new CmdOptions.ParseException("missing option: -v");
        }
        if (optSmValue != null) {
          throw new CmdOptions.ParseException("conflicting options: -s and -i");
        }
        if (txNFull) {
          throw new CmdOptions.ParseException("conflicting options: -i and -t");
        }
        if (rxNEmpty) {
          throw new CmdOptions.ParseException("conflicting options: -i and -t");
        }
      }
      if (enable && disable) {
        throw new CmdOptions.ParseException("conflicting options: -e and -d");
      }
      if (force && unforce) {
        throw new CmdOptions.ParseException("conflicting options: -f and -u");
      }
      if (txNFull | rxNEmpty) {
        if ((optSmValue == null) && (optIrqValue == null)) {
          throw new CmdOptions.ParseException("missing option: -i or -s");
        }
      }
      final boolean haveIrqSelection = zero || one;
      if (haveIrqSelection) {
        if (!haveMaskingOrForcingOp) {
          final String message = "missing option: -e, -d, -f or -u";
          throw new CmdOptions.ParseException(message);
        }
      }
      if (haveMaskingOrForcingOp) {
        if (!haveIrqSelection) {
          throw new CmdOptions.ParseException("missing option: -0 or -1");
        }
      }
      if (haveModOp) {
        if ((optSmValue == null) && (optIrqValue == null)) {
          throw new CmdOptions.ParseException("missing option: -s or -i");
        }
      }
    }
  }

  private static String int2bin(final int d)
  {
    return String.format("%4s", Integer.toBinaryString(d)).replace(' ', '0');
  }

  private void displayInterrupts(final int pioNum) throws IOException
  {
    console.printf("(pio%d:sm*) IRQ / SM  TxNFull  RxNEmpty%n", pioNum);
    console.printf("           76543210     3210      3210%n");
    final int addressIrq =
      PIOEmuRegisters.getAddress(pioNum, PIOEmuRegisters.Regs.IRQ);
    final int irq = sdk.readAddress(addressIrq);
    final int addressIntR =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.INTR);
    final int intR = sdk.readAddress(addressIntR);
    console.printf("           %s%s     %s      %s (INTR)%n",
                   int2bin(irq >> 4),
                   int2bin(irq & 0xf),
                   int2bin((intR >> 4) & 0xf),
                   int2bin(intR & 0xf));
    final int addressIRQ0_IntE =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.IRQ0_INTE);
    final int i0IntE = sdk.readAddress(addressIRQ0_IntE);
    console.printf("               %s     %s      %s (IRQ0_INTE)%n",
                   int2bin((i0IntE >> 8) & 0xf),
                   int2bin((i0IntE >> 4) & 0xf),
                   int2bin(i0IntE & 0xf));
    final int addressIRQ0_IntF =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.IRQ0_INTF);
    final int i0IntF = sdk.readAddress(addressIRQ0_IntF);
    console.printf("               %s     %s      %s (IRQ0_INTF)%n",
                   int2bin((i0IntF >> 8) & 0xf),
                   int2bin((i0IntF >> 4) & 0xf),
                   int2bin(i0IntF & 0xf));
    final int addressIRQ0_IntS =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.IRQ0_INTS);
    final int i0IntS = sdk.readAddress(addressIRQ0_IntS);
    console.printf("               %s     %s      %s (IRQ0_INTS)%n",
                   int2bin((i0IntS >> 8) & 0xf),
                   int2bin((i0IntS >> 4) & 0xf),
                   int2bin(i0IntS & 0xf));
    final int addressIRQ1_IntE =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.IRQ1_INTE);
    final int i1IntE = sdk.readAddress(addressIRQ1_IntE);
    console.printf("               %s     %s      %s (IRQ1_INTE)%n",
                   int2bin((i1IntE >> 8) & 0xf),
                   int2bin((i1IntE >> 4) & 0xf),
                   int2bin(i1IntE & 0xf));
    final int addressIRQ1_IntF =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.IRQ1_INTF);
    final int i1IntF = sdk.readAddress(addressIRQ1_IntF);
    console.printf("               %s     %s      %s (IRQ1_INTF)%n",
                   int2bin((i1IntF >> 8) & 0xf),
                   int2bin((i1IntF >> 4) & 0xf),
                   int2bin(i1IntF & 0xf));
    final int addressIRQ1_IntS =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.IRQ1_INTS);
    final int i1IntS = sdk.readAddress(addressIRQ1_IntS);
    console.printf("               %s     %s      %s (IRQ1_INTS)%n",
                   int2bin((i1IntS >> 8) & 0xf),
                   int2bin((i1IntS >> 4) & 0xf),
                   int2bin(i1IntS & 0xf));
  }

  private void disable(final int pioNum, final int smNum,
                       final FlagsGroup flagsGroup, final int irqNum)
    throws IOException
  {
    final int addressINTE =
      PIORegisters.getAddress(pioNum, irqNum == 0 ?
                              PIORegisters.Regs.IRQ0_INTE :
                              PIORegisters.Regs.IRQ1_INTE);
    final int lsb = flagsGroup.getLSB();
    sdk.hwClearBits(addressINTE, 1 << (lsb + smNum));
    console.printf("(pio%d:sm%d) disabled %s for IRQ%d (IRQ%d_INTE)%n",
                   pioNum, smNum, flagsGroup.getLabel(), irqNum, irqNum);
  }

  private void enable(final int pioNum, final int smNum,
                      final FlagsGroup flagsGroup, final int irqNum)
    throws IOException
  {
    final int addressINTE =
      PIORegisters.getAddress(pioNum, irqNum == 0 ?
                              PIORegisters.Regs.IRQ0_INTE :
                              PIORegisters.Regs.IRQ1_INTE);
    final int lsb = flagsGroup.getLSB();
    sdk.hwSetBits(addressINTE, 1 << (lsb + smNum));
    console.printf("(pio%d:sm%d) enabled %s for IRQ%d (IRQ%d_INTE)%n",
                   pioNum, smNum, flagsGroup.getLabel(), irqNum, irqNum);
  }

  private void force(final int pioNum, final int smNum,
                     final FlagsGroup flagsGroup, final int irqNum)
    throws IOException
  {
    final int addressINTF =
      PIORegisters.getAddress(pioNum, irqNum == 0 ?
                              PIORegisters.Regs.IRQ0_INTF :
                              PIORegisters.Regs.IRQ1_INTF);
    final int lsb = flagsGroup.getLSB();
    sdk.hwSetBits(addressINTF, 1 << (lsb + smNum));
    console.printf("(pio%d:sm%d) set force %s for IRQ%d (IRQ%d_INTF)%n",
                   pioNum, smNum, flagsGroup.getLabel(), irqNum, irqNum);
  }

  private void unforce(final int pioNum, final int smNum,
                       final FlagsGroup flagsGroup, final int irqNum)
    throws IOException
  {
    final int addressINTF =
      PIORegisters.getAddress(pioNum, irqNum == 0 ?
                              PIORegisters.Regs.IRQ0_INTF :
                              PIORegisters.Regs.IRQ1_INTF);
    final int lsb = flagsGroup.getLSB();
    sdk.hwClearBits(addressINTF, 1 << (lsb + smNum));
    console.printf("(pio%d:sm%d) unset force %s for IRQ%d (IRQ%d_INTF)%n",
                   pioNum, smNum, flagsGroup.getLabel(), irqNum, irqNum);
  }

  private void setValue(final int pioNum, final int irqNum, final Boolean value)
    throws IOException
  {
    final int bitValue = value ? 1 : 0;
    final int addressIrq =
      PIORegisters.getAddress(pioNum, value ?
                              PIORegisters.Regs.IRQ_FORCE :
                              PIORegisters.Regs.IRQ);
    sdk.hwWriteMasked(addressIrq, 1 << irqNum, 1 << irqNum);
    console.printf("(pio%d:sm*) set IRQ bit %d to %d%n",
                   pioNum, irqNum, bitValue);
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final int pioNum = options.getValue(optPio);
    final Integer optSmValue = options.getValue(optSm);
    final Integer optIrqValue = options.getValue(optIrq);
    final boolean disable = options.getValue(optDisable).isOn();
    final boolean enable = options.getValue(optEnable).isOn();
    final boolean force = options.getValue(optForce).isOn();
    final boolean unforce = options.getValue(optUnforce).isOn();
    final boolean zero = options.getValue(optZero).isOn();
    final boolean one = options.getValue(optOne).isOn();
    final Boolean optValueValue = options.getValue(optValue);
    final boolean txNFull = options.getValue(optTxNFull).isOn();
    final boolean rxNEmpty = options.getValue(optRxNEmpty).isOn();
    final boolean haveModOp =
      enable || disable || force || unforce || zero || one ||
      (optValueValue != null);
    final FlagsGroup flagsGroup = FlagsGroup.fromOptions(txNFull, rxNEmpty);
    final int irqNum = one ? 1 : (zero ? 0 : -1);

    if (!haveModOp) {
      displayInterrupts(pioNum);
    }
    if (disable) {
      disable(pioNum, optSmValue, flagsGroup, irqNum);
    }
    if (enable) {
      enable(pioNum, optSmValue, flagsGroup, irqNum);
    }
    if (force) {
      force(pioNum, optSmValue, flagsGroup, irqNum);
    }
    if (unforce) {
      unforce(pioNum, optSmValue, flagsGroup, irqNum);
    }
    if (optValueValue != null) {
      setValue(pioNum,
               optSmValue != null ? optSmValue : optIrqValue,
               optValueValue);
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
