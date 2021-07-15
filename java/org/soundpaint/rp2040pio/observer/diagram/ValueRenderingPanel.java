/*
 * @(#)ValueRenderingPanel.java 1.00 21/07/10
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
import java.util.Enumeration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

public class ValueRenderingPanel extends JPanel implements Constants
{
  private static final long serialVersionUID = -5251622302191656176L;

  @FunctionalInterface
  private static interface SignalCreator
  {
    Signal apply(final SignalParams signalParams, final int bitSize)
      throws IOException;
  }

  private static class SignalParams
  {
    private final Diagram diagram;
    private final SDK sdk;
    private final int pioNum;
    private final int smNum;
    private final String label;
    private final int address;
    private final int msb;
    private final int lsb;
    private final Supplier<Boolean> displayFilter;
    private final boolean isSmInstr;

    private SignalParams()
    {
      throw new UnsupportedOperationException("unsupported default constructor");
    }

    private SignalParams(final Diagram diagram,
                         final SDK sdk,
                         final int pioNum,
                         final int smNum,
                         final String label,
                         final int address,
                         final int msb,
                         final int lsb,
                         final Supplier<Boolean> displayFilter)
      throws IOException
    {
      Objects.requireNonNull(diagram);
      Objects.requireNonNull(sdk);
      Objects.requireNonNull(label);
      this.diagram = diagram;
      this.sdk = sdk;
      this.pioNum = pioNum;
      this.label = label;
      this.address = address;
      this.smNum = smNum;
      this.msb = msb;
      this.lsb = lsb;
      this.displayFilter = displayFilter;
      /*
       * TODO: For safety against clashing register names in different
       * register sets, we should have version of getLabelForAddress()
       * including the register set's name as prefix, e.g. "PIOx_".
       */
      isSmInstr = sdk.getLabelForAddress(address).matches("SM\\d_INSTR");
    }

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
      if ((pioNum >= 0) && (smNum >= 0) && (cycle > 0)) {
        final int smPinCtrlSidesetCountAddress =
          PIORegisters.getSMAddress(pioNum, smNum,
                                    PIORegisters.Regs.SM0_PINCTRL);
        pinCtrlSidesetCount =
          (diagram.getInternalSignalByAddress(smPinCtrlSidesetCountAddress).
           getValue() &
           SM0_PINCTRL_SIDESET_COUNT_BITS) >>> SM0_PINCTRL_SIDESET_COUNT_LSB;
        final int smExecCtrlSideEnAddress =
          PIORegisters.getSMAddress(pioNum, smNum,
                                    PIORegisters.Regs.SM0_EXECCTRL);
        execCtrlSideEn =
          (diagram.getInternalSignalByAddress(smExecCtrlSideEnAddress).
           getValue() &
           SM0_EXECCTRL_SIDE_EN_BITS) != 0x0;
        if (isSmInstr) {
          final int smDelayCycleAddress =
            PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                         PIOEmuRegisters.Regs.SM0_DELAY_CYCLE);
          /*
            FIXME: For some reason, the following code does not work correctly:

          isDelayCycle =
            diagram.getInternalSignalByAddress(smDelayCycleAddress).
            getValue(cycle) == 0x1;
          */
          isDelayCycle = false; // TODO: Eliminate this workaround.

          final int smDelayAddress =
            PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                         PIOEmuRegisters.Regs.SM0_DELAY);
          delay =
            diagram.getInternalSignalByAddress(smDelayAddress).
            getValue();
          final int instrOriginAddress =
            PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                         PIOEmuRegisters.Regs.SM0_INSTR_ORIGIN);
          final int instrOrigin =
            diagram.getInternalSignalByAddress(instrOriginAddress).
            getValue(cycle - 1);
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

  private enum Representation
  {
    Bit("bit signal shape",
        "single bit value visualized by upper or lower signal pulse",
        (signalParams, bitSize) ->
        SignalFactory.createFromRegister(signalParams.sdk,
                                         signalParams.label,
                                         signalParams.address,
                                         signalParams.msb)),
    Binary("binary digits", "binary representation of unsigned integer value",
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
                                            signalParams.displayFilter)),
    Unsigned("unsigned decimal",
             "decimal representation of unsigned integer value",
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
                                              signalParams.displayFilter)),
    Signed("signed decimal",
           "decimal representation of signed integer value",
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
                                            signalParams.displayFilter)),
    Hex("hexadecimal",
        "hexadecimal representation of unsigned integer value",
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
                                         signalParams.displayFilter)),
    Octal("octal",
          "octal representation of unsigned integer value",
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
                                           signalParams.displayFilter)),
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
                                              signalParams.displayFilter));

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

    private static String formatShortMnemonic(final int cycle,
                                              final int value,
                                              final SignalParams signalParams)
    {
      return signalParams.getInstructionFromOpCode(cycle, value).toString();
    }

    private static String formatFullMnemonic(final int cycle,
                                             final int value,
                                             final SignalParams signalParams)
    {
      /*
       * TODO: Performance: Avoid re-creating the same InstructionInfo
       * again that has already been constructed in the course of
       * executing method formatShortMnemonic().
       */
      return
        signalParams.getInstructionFromOpCode(cycle, value).getToolTipText();
    }

    private final String label;
    private final String description;
    private final SignalCreator signalCreator;

    private Representation(final String label, final String description,
                           final SignalCreator signalCreator)
    {
      this.label = label;
      this.description = description;
      this.signalCreator = signalCreator;
    }

    @Override
    public String toString() { return label; }
  }

  private final Diagram diagram;
  private final SDK sdk;
  private final Consumer<Void> renderingChangedListener;
  private final ButtonGroup buttonGroup;
  private Representation selectedRendering;

  private ValueRenderingPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public ValueRenderingPanel(final Diagram diagram, final SDK sdk,
                             final Consumer<Void> renderingChangedListener)
  {
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(sdk);
    Objects.requireNonNull(renderingChangedListener);
    this.diagram = diagram;
    this.sdk = sdk;
    this.renderingChangedListener = renderingChangedListener;
    selectedRendering = null;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createTitledBorder("Render Value As"));
    buttonGroup = new ButtonGroup();
    for (final Representation representation : Representation.values()) {
      createAndAddRepresentation(representation);
    }
  }

  private void createAndAddRepresentation(final Representation representation)
  {
    final JPanel line = new JPanel();
    line.setLayout(new BoxLayout(line, BoxLayout.LINE_AXIS));

    final boolean selected = representation == Representation.Signed;
    final JRadioButton rbRepresentation =
      new JRadioButton(representation.toString(), selected);
    if (selected) {
      selectedRendering = representation;
    }
    rbRepresentation.addActionListener((action) ->
                                       renderingSelected(representation));
    buttonGroup.add(rbRepresentation);
    line.add(rbRepresentation);
    line.add(Box.createHorizontalGlue());
    add(line);
  }

  private void renderingSelected(final Representation representation)
  {
    selectedRendering = representation;
    renderingChangedListener.accept(null);
  }

  public boolean isSmSelectionRelevant()
  {
    return selectedRendering == Representation.Mnemonic;
  }

  public Signal createSignal(final String label,
                             final int pioNum,
                             final int smNum,
                             final int address,
                             final int msb,
                             final int lsb,
                             final Supplier<Boolean> displayFilter)
  {
    if ((msb < 0) || (lsb < 0)) {
      JOptionPane.showMessageDialog(this,
                                    "Please select a contiguous range of bits.",
                                    "No Bits Range Selected",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
    try {
      final SignalParams signalParams =
        new SignalParams(diagram, sdk, pioNum, smNum, label, address,
                         msb, lsb, displayFilter);
      return
        selectedRendering.signalCreator.apply(signalParams, msb - lsb + 1);
    } catch (final IOException e) {
      JOptionPane.showMessageDialog(this, e.getMessage(),
                                    "I/O Exception",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
  }

  @Override
  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    final Enumeration<AbstractButton> buttons = buttonGroup.getElements();
    while (buttons.hasMoreElements()) {
      buttons.nextElement().setEnabled(enabled);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
