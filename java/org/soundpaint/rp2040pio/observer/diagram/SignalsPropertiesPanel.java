/*
 * @(#)SignalsPropertiesPanel.java 1.00 21/04/07
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class SignalsPropertiesPanel extends Box
{
  private static final long serialVersionUID = -8921092912489516701L;

  private final Diagram diagram;
  private final List<Signal> signals;
  private final List<JTextField> signalIndices;
  private final List<JTextField> signalLabels;
  private final List<JCheckBox> signalVisibilities;
  private final AddSignalDialog addSignalDialog;

  public SignalsPropertiesPanel(final Diagram diagram, final SDK sdk)
  {
    super(BoxLayout.Y_AXIS);
    Objects.requireNonNull(diagram);
    this.diagram = diagram;
    signals = new ArrayList<Signal>();
    signalIndices = new ArrayList<JTextField>();
    signalLabels = new ArrayList<JTextField>();
    signalVisibilities = new ArrayList<JCheckBox>();
    addSignalDialog =
      new AddSignalDialog(diagram, sdk,
                          (addIndex, signal) -> addSignal(addIndex, signal));
  }

  public void applyChanges()
  {
    int index = 0;
    for (final Signal signal : signals) {
      signal.setVisible(signalVisibilities.get(index).isSelected());
      index++;
    }
    diagram.pushSignals(signals);
  }

  private void swapSignals(final int index)
  {
    Collections.swap(signals, index, index + 1);
    final Signal signal1 = signals.get(index);
    final Signal signal2 = signals.get(index + 1);
    final JTextField tfLabel1 = signalLabels.get(index);
    tfLabel1.setText(signal1.getLabel());
    final JTextField tfLabel2 = signalLabels.get(index + 1);
    tfLabel2.setText(signal2.getLabel());
    final JCheckBox cbVisible1 = signalVisibilities.get(index);
    final JCheckBox cbVisible2 = signalVisibilities.get(index + 1);
    final boolean selected = cbVisible1.isSelected();
    cbVisible1.setSelected(cbVisible2.isSelected());
    cbVisible2.setSelected(selected);
    revalidate();
  }

  private void addSignal(final int addIndex, final Signal signal)
  {
    signals.add(addIndex, signal);
    rebuildSignals();
    rebuildGUI();
  }

  private void deleteSignal(final int delIndex)
  {
    signals.remove(delIndex);
    rebuildSignals();
    rebuildGUI();
  }

  private void rebuildGUI()
  {
    removeAll();
    boolean firstLine = true;
    final Box headerLine = new Box(BoxLayout.X_AXIS);
    add(headerLine);
    headerLine.add(Box.createHorizontalStrut(5));
    headerLine.add(new JLabel("# Label"));
    headerLine.add(Box.createHorizontalGlue());
    headerLine.add(new JLabel("Swap"));
    headerLine.add(Box.createHorizontalStrut(15));
    headerLine.add(new JLabel("Add"));
    headerLine.add(Box.createHorizontalGlue());
    headerLine.add(new JLabel("Show"));
    headerLine.add(Box.createHorizontalStrut(5));
    headerLine.add(new JLabel("Delete"));
    headerLine.add(Box.createHorizontalStrut(5));
    SwingUtils.setPreferredHeightAsMaximum(headerLine);
    add(Box.createVerticalStrut(15));
    int index = 0;
    for (final Signal signal : signals) {
      if (index > 0) {
        final Box infixLine = new Box(BoxLayout.X_AXIS);
        add(infixLine);
        infixLine.add(Box.createHorizontalGlue());

        final JButton btSwap =
          SwingUtils.createIconButton("swapv12x12.png", "⬍");
        final int swapIndex = index - 1;
        btSwap.addActionListener((event) -> swapSignals(swapIndex));
        btSwap.setBorderPainted(false);
        btSwap.setContentAreaFilled(false);
        infixLine.add(btSwap);

        final JButton btAdd =
          SwingUtils.createIconButton("add12x12.png", "+");
        final int addIndex = index;
        btAdd.addActionListener((event) -> addSignalDialog.open(addIndex));
        btAdd.setBorderPainted(false);
        btAdd.setContentAreaFilled(false);
        infixLine.add(btAdd);

        infixLine.add(Box.createHorizontalGlue());
        SwingUtils.setPreferredHeightAsMaximum(infixLine);
      }
      final Box signalLine = new Box(BoxLayout.X_AXIS);
      add(signalLine);
      signalLine.add(signalIndices.get(index));
      signalLine.add(Box.createHorizontalStrut(5));
      signalLine.add(signalLabels.get(index));
      signalLine.add(Box.createHorizontalGlue());
      signalLine.add(signalVisibilities.get(index));
      signalLine.add(Box.createHorizontalStrut(12));
      final JButton btDel =
        SwingUtils.createIconButton("del12x12.png", "+");
      final int delIndex = index;
      btDel.addActionListener((event) -> deleteSignal(delIndex));
      btDel.setBorderPainted(false);
      btDel.setContentAreaFilled(false);
      signalLine.add(btDel);
      SwingUtils.setPreferredHeightAsMaximum(signalLine);
      index++;
    }
    add(Box.createVerticalGlue());
    revalidate();
  }

  private void rebuildSignals()
  {
    signalIndices.clear();
    signalLabels.clear();
    signalVisibilities.clear();
    int index = 0;
    for (final Signal signal : signals) {
      final JTextField tfIndex = new JTextField() {
          @Override
          public void setBorder(final Border border) {}
        };
      tfIndex.setText(String.format("#%d", index));
      tfIndex.setEditable(false);
      SwingUtils.setPreferredWidthAsMaximum(tfIndex);
      signalIndices.add(tfIndex);

      final JTextField tfLabel = new JTextField() {
          @Override
          public void setBorder(final Border border) {}
        };
      tfLabel.setText(signal.getLabel());
      tfLabel.setEditable(false);
      SwingUtils.setPreferredWidthAsMaximum(tfLabel);
      signalLabels.add(tfLabel);

      final JCheckBox cbVisible = new JCheckBox();
      cbVisible.setSelected(signal.getVisible());
      signalVisibilities.add(cbVisible);

      index++;
    }
  }

  public void rebuild()
  {
    diagram.pullSignals(signals);
    rebuildSignals();
    rebuildGUI();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
