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
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.border.Border;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class SignalsPropertiesPanel extends Box
{
  private static final long serialVersionUID = -8921092912489516701L;

  private static class IndexedButton extends JButton
  {
    private static final long serialVersionUID = -7131638926376704356L;

    private final int index;

    public IndexedButton(final String text, final int index)
    {
      super(text);
      this.index = index;
    }

    public int getIndex() { return index; }
  }

  private final Diagram diagram;
  private final List<Signal> signals;
  private final List<JTextField> signalIndices;
  private final List<JTextField> signalLabels;
  private final List<JCheckBox> signalVisibilities;
  private final List<IndexedButton> signalActions;
  private final SignalDialog signalDialog;
  private final JPopupMenu pmActions;

  public SignalsPropertiesPanel(final Diagram diagram, final SDK sdk)
  {
    super(BoxLayout.PAGE_AXIS);
    Objects.requireNonNull(diagram);
    this.diagram = diagram;
    signals = new ArrayList<Signal>();
    signalIndices = new ArrayList<JTextField>();
    signalLabels = new ArrayList<JTextField>();
    signalVisibilities = new ArrayList<JCheckBox>();
    signalActions = new ArrayList<IndexedButton>();
    signalDialog =
      new SignalDialog(diagram, sdk,
                       (index, signal, add) ->
                       addOrSetSignal(index, signal, add),
                       (label, signal) -> checkLabel(label, signal));
    pmActions = createActions();
  }

  private JPopupMenu createActions()
  {
    final JPopupMenu pmActions = new JPopupMenu("Actions");
    pmActions.add("Add Signal…").addActionListener((action) -> addSignal());
    pmActions.add("Edit…").addActionListener((action) -> editSignal());
    pmActions.add("Delete").addActionListener((action) -> deleteSignal());
    return pmActions;
  }

  private void addSignal()
  {
    final IndexedButton btActions = (IndexedButton)pmActions.getInvoker();
    final int index = btActions.getIndex();
    signalDialog.open(index + 1, null);
  }

  private void editSignal()
  {
    final IndexedButton btActions = (IndexedButton)pmActions.getInvoker();
    final int index = btActions.getIndex();
    signalDialog.open(index, signals.get(index));
  }

  private void deleteSignal()
  {
    final IndexedButton btActions = (IndexedButton)pmActions.getInvoker();
    final int index = btActions.getIndex();
    signals.remove(index);
    rebuildSignals();
    rebuildGUI();
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

  private void addOrSetSignal(final int index, final Signal signal,
                              final boolean add)
  {
    if (add) {
      signals.add(index, signal);
    } else {
      signals.set(index, signal);
    }
    rebuildSignals();
    rebuildGUI();
  }

  private String checkLabel(final String label, final Signal ignoreSignal)
  {
    if (label == null) {
      return "Signal label must not be null.";
    }
    if (label.isEmpty()) {
      return "Signal label must not be empty.";
    }
    for (final Signal signal : signals) {
      if (signal != ignoreSignal) {
        if (label.equals(signal.getLabel())) {
          return "Signal label is already in use.";
        }
      }
    }
    return null;
  }

  private void rebuildGUI()
  {
    removeAll();
    boolean firstLine = true;
    final Box headerLine = new Box(BoxLayout.LINE_AXIS);
    add(headerLine);
    headerLine.add(Box.createHorizontalStrut(5));
    headerLine.add(new JLabel("# Label"));
    headerLine.add(Box.createHorizontalGlue());
    headerLine.add(new JLabel("Show"));
    headerLine.add(Box.createHorizontalStrut(5));
    headerLine.add(new JLabel("Actions"));
    headerLine.add(Box.createHorizontalStrut(5));
    SwingUtils.setPreferredHeightAsMaximum(headerLine);
    add(Box.createVerticalStrut(15));
    int index = 0;
    for (final Signal signal : signals) {
      if (index > 0) {
        final Box infixLine = new Box(BoxLayout.LINE_AXIS);
        add(infixLine);

        final JButton btSwap =
          SwingUtils.createIconButton("swapv12x12.png", "⬍");
        final int swapIndex = index - 1;
        btSwap.addActionListener((event) -> swapSignals(swapIndex));
        btSwap.setBorderPainted(false);
        btSwap.setContentAreaFilled(false);
        infixLine.add(btSwap);

        infixLine.add(Box.createHorizontalGlue());
        SwingUtils.setPreferredHeightAsMaximum(infixLine);
      }
      final Box signalLine = new Box(BoxLayout.LINE_AXIS);
      add(signalLine);
      signalLine.add(signalIndices.get(index));
      signalLine.add(Box.createHorizontalStrut(5));
      signalLine.add(signalLabels.get(index));
      signalLine.add(Box.createHorizontalGlue());
      signalLine.add(signalVisibilities.get(index));
      signalLine.add(Box.createHorizontalStrut(12));
      signalLine.add(signalActions.get(index));
      signalLine.add(Box.createHorizontalStrut(5));
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
    signalActions.clear();
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

      final int actionsIndex = index;
      final IndexedButton btActions = new IndexedButton("…", actionsIndex);
      btActions.setBorderPainted(false);
      btActions.setContentAreaFilled(false);
      btActions.addActionListener((action) -> popupActions(btActions));
      signalActions.add(btActions);

      index++;
    }
  }

  private void popupActions(final JButton btActions)
  {
    pmActions.show(btActions, 10, 10);
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
