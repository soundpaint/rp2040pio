/*
 * @(#)SignalRendering.java 1.00 21/07/25
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
package org.soundpaint.rp2040pio.observer.diagram;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

public enum SignalRendering implements Constants
{
  Bit("bit signal shape",
      "single bit value visualized by upper or lower signal pulse",
      (signalParams, bitSize) ->
      SignalFactory.createFromRegister(signalParams.sdk,
                                       signalParams.label,
                                       signalParams.address,
                                       signalParams.msb,
                                       signalParams.displayFilters,
                                       signalParams.pioNum,
                                       signalParams.smNum)),
  Binary("binary digits", "binary rendering of unsigned integer value",
         (signalParams, bitSize) ->
         SignalFactory.createFromRegister(signalParams.sdk,
                                          signalParams.label,
                                          signalParams.address,
                                          signalParams.msb,
                                          signalParams.lsb,
                                          (cycle, value) ->
                                          formatBinary(value, bitSize),
                                          (cycle, value) ->
                                          "0b" + formatBinary(value, bitSize),
                                          signalParams.displayFilters,
                                          signalParams.pioNum,
                                          signalParams.smNum)),
  Unsigned("unsigned decimal",
           "decimal rendering of unsigned integer value",
           (signalParams, bitSize) ->
           SignalFactory.createFromRegister(signalParams.sdk,
                                            signalParams.label,
                                            signalParams.address,
                                            signalParams.msb,
                                            signalParams.lsb,
                                            (cycle, value) ->
                                            formatUnsigned(value),
                                            (cycle, value) ->
                                            formatUnsigned(value),
                                            signalParams.displayFilters,
                                            signalParams.pioNum,
                                            signalParams.smNum)),
  Signed("signed decimal",
         "decimal rendering of signed integer value",
         (signalParams, bitSize) ->
         SignalFactory.createFromRegister(signalParams.sdk,
                                          signalParams.label,
                                          signalParams.address,
                                          signalParams.msb,
                                          signalParams.lsb,
                                          (cycle, value) ->
                                          formatSigned(value),
                                          (cycle, value) ->
                                          formatSigned(value),
                                          signalParams.displayFilters,
                                          signalParams.pioNum,
                                          signalParams.smNum)),
  Hex("hexadecimal",
      "hexadecimal rendering of unsigned integer value",
      (signalParams, bitSize) ->
      SignalFactory.createFromRegister(signalParams.sdk,
                                       signalParams.label,
                                       signalParams.address,
                                       signalParams.msb,
                                       signalParams.lsb,
                                       (cycle, value) ->
                                       formatHex(value, bitSize),
                                       (cycle, value) ->
                                       "0x" + formatHex(value, bitSize),
                                       signalParams.displayFilters,
                                       signalParams.pioNum,
                                       signalParams.smNum)),
  Octal("octal",
        "octal rendering of unsigned integer value",
        (signalParams, bitSize) ->
        SignalFactory.createFromRegister(signalParams.sdk,
                                         signalParams.label,
                                         signalParams.address,
                                         signalParams.msb,
                                         signalParams.lsb,
                                         (cycle, value) ->
                                         formatOctal(value, bitSize),
                                         (cycle, value) ->
                                         "0o" + formatOctal(value, bitSize),
                                         signalParams.displayFilters,
                                         signalParams.pioNum,
                                         signalParams.smNum)),
  Mnemonic("PIO instruction with side-set for below target state machine",
           "mnemonic of PIO instruction with op-code that equals the value",
           (signalParams, bitSize) ->
           SignalFactory.createFromRegister(signalParams.sdk,
                                            signalParams.label,
                                            signalParams.address,
                                            signalParams.msb,
                                            signalParams.lsb,
                                            (cycle, value) ->
                                            formatShortMnemonic(cycle, value,
                                                                signalParams),
                                            (cycle, value) ->
                                            formatFullMnemonic(cycle, value,
                                                               signalParams),
                                            signalParams.displayFilters,
                                            signalParams.pioNum,
                                            signalParams.smNum));

  @FunctionalInterface
  private static interface SignalCreator
  {
    Signal apply(final SignalParams signalParams, final int bitSize)
      throws IOException;
  }

  // TODO: Make private again when removing demo signals from Diagram class
  public static class SignalParams
  {
    private final Diagram diagram;
    private final SDK sdk;
    private final String label;
    private final int address;
    private final int msb;
    private final int lsb;
    // TODO: Make private again when removing demo signals from Diagram class
    public final List<SignalFilter> displayFilters;
    private final int pioNum;
    private final int smNum;
    private final boolean isSmInstr;

    private SignalParams()
    {
      throw new UnsupportedOperationException("unsupported default constructor");
    }

    // TODO: Make private again when removing demo signals from Diagram class
    public SignalParams(final Diagram diagram,
                        final SDK sdk,
                        final String label,
                        final int address,
                        final int msb,
                        final int lsb,
                        final List<SignalFilter> displayFilters,
                        final int pioNum,
                        final int smNum)
      throws IOException
    {
      Objects.requireNonNull(diagram);
      Objects.requireNonNull(sdk);
      Objects.requireNonNull(label);
      this.diagram = diagram;
      this.sdk = sdk;
      this.label = label;
      this.address = address;
      this.msb = msb;
      this.lsb = lsb;
      this.displayFilters = displayFilters;
      this.pioNum = pioNum;
      this.smNum = smNum;
      isSmInstr =
        sdk.getFullLabelForAddress(address).matches("PIO\\d_SM\\d_INSTR");
    }

    /*
     * TODO: Performance: This method is typically called twice with
     * identical arguments (once for value rendering, once for tooltip
     * rendering).  To avoid creating two equal instances of
     * InstructionInfo, maybe cache the instance of the most recent
     * call and return it again, if arguments cycle and value match
     * those of the previous call?
     */
    private PIOSDK.InstructionInfo getInstructionFromOpCode(final int cycle,
                                                            final int value)
    {
      final int pinCtrlSidesetCount;
      final boolean execCtrlSideEn;
      final boolean isDelayCycle;
      final int delay;
      final int origin;
      final String addressLabel;
      final int signalSize = diagram.getModel().getSignalSize();
      if ((pioNum >= 0) && (smNum >= 0) && (cycle >= 0)) {
        final int smPinCtrlSidesetCountAddress =
          PIORegisters.getSMAddress(pioNum, smNum,
                                    PIORegisters.Regs.SM0_PINCTRL);
        pinCtrlSidesetCount =
          (diagram.getInternalSignalByAddress(smPinCtrlSidesetCountAddress).
           getValue(cycle) &
           SM0_PINCTRL_SIDESET_COUNT_BITS) >>> SM0_PINCTRL_SIDESET_COUNT_LSB;
        final int smExecCtrlSideEnAddress =
          PIORegisters.getSMAddress(pioNum, smNum,
                                    PIORegisters.Regs.SM0_EXECCTRL);
        execCtrlSideEn =
          (diagram.getInternalSignalByAddress(smExecCtrlSideEnAddress).
           getValue(cycle) &
           SM0_EXECCTRL_SIDE_EN_BITS) != 0x0;
        if (isSmInstr) {
          final int smDelayCycleAddress =
            PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                         PIOEmuRegisters.Regs.SM0_DELAY_CYCLE);
          isDelayCycle =
            diagram.getInternalSignalByAddress(smDelayCycleAddress).
            getValue(cycle) == 0x1;

          final int smDelayAddress =
            PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                         PIOEmuRegisters.Regs.SM0_DELAY);
          delay =
            diagram.getInternalSignalByAddress(smDelayAddress).
            getValue(cycle);
          final int instrOriginAddress =
            PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                         PIOEmuRegisters.Regs.SM0_INSTR_ORIGIN);
          final int instrOrigin =
            diagram.getInternalSignalByAddress(instrOriginAddress).
            getValue(cycle);
          origin = PIOSDK.decodeInstrOrigin(instrOrigin);
          addressLabel = PIOSDK.renderOrigin(origin) + ": ";
        } else {
          isDelayCycle = false;
          delay = 0;
          origin = INSTR_ORIGIN_UNKNOWN;
          addressLabel = "";
        }
      } else {
        pinCtrlSidesetCount = 0;
        execCtrlSideEn = false;
        isDelayCycle = false;
        delay = 0;
        origin = INSTR_ORIGIN_UNKNOWN;
        addressLabel = "";
      }
      final boolean format = false;
      return
        PIOSDK.getInstructionFromOpCode(pinCtrlSidesetCount, execCtrlSideEn,
                                        origin, addressLabel, value, format,
                                        isDelayCycle, delay);
    }
  }

  private static String formatBinary(final int value, final int bitSize)
  {
    final String digits = Integer.toBinaryString(value);
    return String.format("%" + bitSize + "s", digits).replace(' ', '0');
  }

  private static String formatUnsigned(final int value)
  {
    return Long.toString(Integer.toUnsignedLong(value));
  }

  private static String formatSigned(final int value)
  {
    return Integer.toString(value);
  }

  private static String formatHex(final int value, final int bitSize)
  {
    final String digits = Integer.toHexString(value);
    return
      String.format("%" + (bitSize / 4 + 1) + "s", digits).replace(' ', '0');
  }

  private static String formatOctal(final int value, final int bitSize)
  {
    final String digits = Integer.toOctalString(value);
    return
      String.format("%" + (bitSize / 3 + 1) + "s", digits).replace(' ', '0');
  }

  // TODO: Make private again when removing demo signals from Diagram class
  public static String formatShortMnemonic(final int cycle,
                                           final int value,
                                           final SignalParams signalParams)
  {
    return signalParams.getInstructionFromOpCode(cycle, value).toString();
  }

  // TODO: Make private again when removing demo signals from Diagram class
  public static String formatFullMnemonic(final int cycle,
                                          final int value,
                                          final SignalParams signalParams)
  {
    return
      signalParams.getInstructionFromOpCode(cycle, value).getToolTipText();
  }

  private final String label;
  private final String description;
  private final SignalCreator signalCreator;

  private SignalRendering(final String label, final String description,
                          final SignalCreator signalCreator)
  {
    Objects.requireNonNull(label);
    Objects.requireNonNull(description);
    Objects.requireNonNull(signalCreator);
    this.label = label;
    this.description = description;
    this.signalCreator = signalCreator;
  }

  public Signal createSignal(final Diagram diagram,
                             final SDK sdk,
                             final String label,
                             final int address,
                             final int msb,
                             final int lsb,
                             final List<SignalFilter> displayFilters,
                             final int pioNum,
                             final int smNum)
    throws IOException
  {
    final SignalParams params =
      new SignalParams(diagram, sdk, label, address,
                       msb, lsb, displayFilters, pioNum, smNum);
    return signalCreator.apply(params, params.msb - params.lsb + 1);
  }

  @Override
  public String toString() { return label; }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
