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

public class SignalsPropertiesPanel extends Box
{
  private static final long serialVersionUID = -8921092912489516701L;

  private final Diagram diagram;
  private final List<Signal> signals;
  private final List<JTextField> signalLabels;
  private final List<JCheckBox> signalVisibilities;

  public SignalsPropertiesPanel(final Diagram diagram)
  {
    super(BoxLayout.Y_AXIS);
    Objects.requireNonNull(diagram);
    this.diagram = diagram;
    signals = new ArrayList<Signal>();
    signalLabels = new ArrayList<JTextField>();
    signalVisibilities = new ArrayList<JCheckBox>();
  }

  public void apply()
  {
    int index = 0;
    for (final Signal signal : signals) {
      signal.setVisible(signalVisibilities.get(index).isSelected());
      index++;
    }
    diagram.updateListOfSignals(signals);
  }

  private void swapSignals(final int index)
  {
    Collections.swap(signals, index, index + 1);
    final Signal signal1 = signals.get(index);
    final Signal signal2 = signals.get(index + 1);
    final JTextField tfSignal1 = signalLabels.get(index);
    tfSignal1.setText(signal1.getLabel());
    final JTextField tfSignal2 = signalLabels.get(index + 1);
    tfSignal2.setText(signal2.getLabel());
    final JCheckBox cbVisible1 = signalVisibilities.get(index);
    final JCheckBox cbVisible2 = signalVisibilities.get(index + 1);
    final boolean selected = cbVisible1.isSelected();
    cbVisible1.setSelected(cbVisible2.isSelected());
    cbVisible2.setSelected(selected);
    revalidate();
  }

  private void rebuildGUI()
  {
    removeAll();
    boolean firstLine = true;
    final Box headerLine = new Box(BoxLayout.X_AXIS);
    add(headerLine);
    headerLine.add(Box.createHorizontalStrut(5));
    headerLine.add(new JLabel("Signal"));
    headerLine.add(Box.createHorizontalGlue());
    headerLine.add(new JLabel("Change Order"));
    headerLine.add(Box.createHorizontalGlue());
    headerLine.add(new JLabel("Show"));
    headerLine.add(Box.createHorizontalStrut(5));
    SwingUtils.setPreferredHeightAsMaximum(headerLine);
    add(Box.createVerticalStrut(15));
    int index = 0;
    for (final Signal signal : signals) {
      if (index > 0) {
        final Box swapLine = new Box(BoxLayout.X_AXIS);
        add(swapLine);
        swapLine.add(Box.createHorizontalGlue());
        final JButton btSwap =
          SwingUtils.createIconButton("swapv12x12.png", "⬍");
        final int swapIndex = index - 1;
        btSwap.addActionListener((event) -> swapSignals(swapIndex));
        btSwap.setBorderPainted(false);
        btSwap.setContentAreaFilled(false);
        swapLine.add(btSwap);
        swapLine.add(Box.createHorizontalGlue());
        SwingUtils.setPreferredHeightAsMaximum(swapLine);
      }
      final Box signalLine = new Box(BoxLayout.X_AXIS);
      add(signalLine);
      signalLine.add(Box.createHorizontalStrut(5));
      signalLine.add(signalLabels.get(index));
      signalLine.add(Box.createHorizontalGlue());
      signalLine.add(signalVisibilities.get(index));
      signalLine.add(Box.createHorizontalStrut(12));
      SwingUtils.setPreferredHeightAsMaximum(signalLine);
      index++;
    }
    add(Box.createVerticalGlue());
    revalidate();
  }

  private void rebuildSignals()
  {
    diagram.fillInCurrentSignals(signals);
    signalLabels.clear();
    signalVisibilities.clear();
    int index = 0;
    for (final Signal signal : signals) {
      final JTextField tfSignal = new JTextField() {
          @Override
          public void setBorder(final Border border) {}
        };
      tfSignal.setText(signal.getLabel());
      tfSignal.setEditable(false);
      SwingUtils.setPreferredWidthAsMaximum(tfSignal);
      signalLabels.add(tfSignal);

      final JCheckBox cbVisible = new JCheckBox();
      cbVisible.setSelected(signal.getVisible());
      signalVisibilities.add(cbVisible);

      index++;
    }
  }

  public void rebuild()
  {
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
