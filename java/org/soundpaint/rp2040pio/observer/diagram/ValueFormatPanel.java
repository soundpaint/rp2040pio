/*
 * @(#)ValueFormatPanel.java 1.00 21/07/10
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
import java.util.function.Supplier;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

public class ValueFormatPanel extends JPanel
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
    private final SDK sdk;
    private final int pioNum;
    private final int smNum;
    private final String label;
    private final int address;
    private final int msb;
    private final int lsb;
    private final boolean showAddress;
    private final Supplier<Boolean> displayFilter;

    private SignalParams()
    {
      throw new UnsupportedOperationException("unsupported default constructor");
    }

    private SignalParams(final SDK sdk,
                         final int pioNum,
                         final int smNum,
                         final String label,
                         final int address,
                         final int msb,
                         final int lsb,
                         final boolean showAddress,
                         final Supplier<Boolean> displayFilter)
    {
      Objects.requireNonNull(sdk);
      Objects.requireNonNull(label);
      this.sdk = sdk;
      this.pioNum = pioNum;
      this.label = label;
      this.address = address;
      this.smNum = smNum;
      this.msb = msb;
      this.lsb = lsb;
      this.showAddress = showAddress;
      this.displayFilter = displayFilter;
    }
  }

  private enum Representation
  {
    Bit("bit", "single bit value visualized by upper or lower signal pulse",
        (signalParams, bitSize) ->
        SignalFactory.createFromRegister(signalParams.sdk,
                                         signalParams.label,
                                         signalParams.address,
                                         signalParams.msb)),
    Binary("binary", "binary representation of unsigned integer value",
           (signalParams, bitSize) ->
           SignalFactory.createFromRegister(signalParams.sdk,
                                            signalParams.label,
                                            signalParams.address,
                                            signalParams.msb,
                                            signalParams.lsb,
                                            (value) ->
                                            formatBinary(value, bitSize),
                                            signalParams.displayFilter)),
    Unsigned("unsigned decimal",
             "decimal representation of unsigned integer value",
             (signalParams, bitSize) ->
             SignalFactory.createFromRegister(signalParams.sdk,
                                              signalParams.label,
                                              signalParams.address,
                                              signalParams.msb,
                                              signalParams.lsb,
                                              (value) ->
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
                                            (value) ->
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
                                         (value) ->
                                         formatHex(value, bitSize),
                                         signalParams.displayFilter)),
    Octal("octal",
          "octal representation of unsigned integer value",
          (signalParams, bitSize) ->
          SignalFactory.createFromRegister(signalParams.sdk,
                                           signalParams.label,
                                           signalParams.address,
                                           signalParams.msb,
                                           signalParams.lsb,
                                           (value) ->
                                           formatOctal(value, bitSize),
                                           signalParams.displayFilter)),
    Mnemonic("PIO instruction",
             "mnemonic of PIO instruction with op-code that equals the value",
             (signalParams, bitSize) ->
             SignalFactory.createFromRegister(signalParams.sdk,
                                              signalParams.label,
                                              signalParams.address,
                                              signalParams.msb,
                                              signalParams.lsb,
                                              (value) ->
                                              formatMnemonic(value,
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

    private static String formatMnemonic(final int value,
                                         final SignalParams signalParams)
    {
      // TODO: determine side-set cfg by signalParams pioNum + smNum
      final int pinCtrlSidesetCount = 0; // TODO
      final boolean execCtrlSideEn = false; // TODO
      final int origin = Constants.INSTR_ORIGIN_UNKNOWN; // TODO
      final String addressLabel = ""; // TODO
      final boolean format = false; // TODO
      final boolean isDelayCycle = false; // TODO
      final int delay = 0; // TODO
      return
        PIOSDK.getInstructionFromOpCode(pinCtrlSidesetCount, execCtrlSideEn,
                                        origin, addressLabel, value, format,
                                        isDelayCycle, delay).toString();
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
  private final ButtonGroup buttonGroup;
  private final InstructionOptionsPanel instructionOptionsPanel;
  private Representation selectedFormat;

  private ValueFormatPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public ValueFormatPanel(final Diagram diagram, final SDK sdk)
  {
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(sdk);
    this.diagram = diagram;
    this.sdk = sdk;
    selectedFormat = null;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    buttonGroup = new ButtonGroup();
    instructionOptionsPanel = new InstructionOptionsPanel(diagram, sdk);
    for (final Representation representation : Representation.values()) {
      createAndAddRepresentation(representation);
    }
    createAndAddInstructionOptionsPanel();
  }

  private void createAndAddRepresentation(final Representation representation)
  {
    final JPanel line = new JPanel();
    line.setLayout(new BoxLayout(line, BoxLayout.LINE_AXIS));

    final boolean selected = representation == Representation.Signed;
    final JRadioButton rbRepresentation =
      new JRadioButton(representation.toString(), selected);
    rbRepresentation.addActionListener((action) ->
                                       formatSelected(representation));
    if (selected) {
      formatSelected(representation);
    }
    buttonGroup.add(rbRepresentation);
    line.add(rbRepresentation);
    line.add(Box.createHorizontalGlue());
    add(line);
  }

  private void createAndAddInstructionOptionsPanel()
  {
    final JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
    row.add(Box.createHorizontalStrut(20));
    row.add(instructionOptionsPanel);
    SwingUtils.setPreferredHeightAsMaximum(row);
    add(row);
  }

  private void formatSelected(final Representation representation)
  {
    selectedFormat = representation;
    instructionOptionsPanel.setEnabled(selectedFormat ==
                                       Representation.Mnemonic);
  }

  public Signal createSignal(final String label,
                             final int address,
                             final int msb,
                             final int lsb,
                             final Supplier<Boolean> displayFilter)
  {
    final int pioNum = instructionOptionsPanel.getSelectedPio();
    final int smNum = instructionOptionsPanel.getSelectedSm();
    final boolean showAddress = false; // TODO
    if ((msb < 0) || (lsb < 0)) {
      JOptionPane.showMessageDialog(this,
                                    "Please select a contiguous range of bits.",
                                    "No Bits Range Selected",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
    try {
      final SignalParams signalParams =
        new SignalParams(sdk, pioNum, smNum, label, address, msb, lsb,
                         showAddress, displayFilter);
      return
        selectedFormat.signalCreator.apply(signalParams, msb - lsb + 1);
    } catch (final IOException e) {
      JOptionPane.showMessageDialog(this, e.getMessage(),
                                    "I/O Exception",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
  }

  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    final Enumeration<AbstractButton> buttons = buttonGroup.getElements();
    while (buttons.hasMoreElements()) {
      buttons.nextElement().setEnabled(enabled);
    }
    final boolean instructionOptionsEnabled =
      enabled && (selectedFormat == Representation.Mnemonic);
    instructionOptionsPanel.setEnabled(instructionOptionsEnabled);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
