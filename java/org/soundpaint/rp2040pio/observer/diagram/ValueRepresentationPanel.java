/*
 * @(#)ValueRepresentationPanel.java 1.00 21/07/08
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

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.soundpaint.rp2040pio.sdk.SDK;

public class ValueRepresentationPanel extends JPanel
{
  private static final long serialVersionUID = -7271655535997736886L;
  private static final Dimension PREFERRED_LABEL_SIZE = new Dimension(120, 32);

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
             SignalFactory.createInstructionSignal(signalParams.sdk,
                                                   signalParams.pioNum,
                                                   signalParams.smNum,
                                                   signalParams.label,
                                                   signalParams.address,
                                                   signalParams.showAddress,
                                                   signalParams.displayFilter));
    /* TODO:
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
    */

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
      return null; // TODO
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
  private final JLabel lbFormat;
  private final JComboBox<Representation> cbFormat;

  private ValueRepresentationPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public ValueRepresentationPanel(final Diagram diagram, final SDK sdk)
  {
    Objects.requireNonNull(diagram);
    this.diagram = diagram;
    Objects.requireNonNull(sdk);
    this.sdk = sdk;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createTitledBorder("Value Representation"));
    lbFormat = new JLabel("Format");
    lbFormat.setPreferredSize(PREFERRED_LABEL_SIZE);
    cbFormat = createFormatSelection();
    addFormat();
  }

  private JComboBox<Representation> createFormatSelection()
  {
    final JComboBox<Representation> cbFormat =
      new JComboBox<Representation>(Representation.values());
    cbFormat.addItemListener((event) -> {
        if (event.getStateChange() == ItemEvent.SELECTED) {
          formatSelected((Representation)event.getItem());
        }
      });
    cbFormat.setMaximumSize(cbFormat.getPreferredSize());
    cbFormat.setSelectedItem(Representation.Signed);
    return cbFormat;
  }

  private void addFormat()
  {
    final JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
    row.add(lbFormat);
    row.add(Box.createHorizontalStrut(5));
    row.add(cbFormat);
    row.add(Box.createHorizontalGlue());
    add(row);
  }

  private void formatSelected(final Representation representation)
  {
    // TODO
  }

  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    lbFormat.setEnabled(enabled);
    cbFormat.setEnabled(enabled);
  }

  public Signal createSignal(final String label,
                             final int address,
                             final int msb,
                             final int lsb,
                             final Supplier<Boolean> displayFilter)
  {
    final int pioNum = 0; // TODO
    final int smNum = 0; // TODO
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
      final Representation representation =
        cbFormat.getItemAt(cbFormat.getSelectedIndex());
      return
        representation.signalCreator.apply(signalParams, msb - lsb + 1);
    } catch (final IOException e) {
      JOptionPane.showMessageDialog(this, e.getMessage(),
                                    "I/O Exception",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
