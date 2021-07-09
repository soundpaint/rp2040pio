/*
 * @(#)SignalTypePanel.java 1.00 21/06/30
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

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class SignalTypePanel extends JPanel
{
  private static final long serialVersionUID = -3023263223044270621L;

  private final Diagram diagram;
  private final Consumer<String> suggestedLabelSetter;
  private final ButtonGroup signalType;
  private final JRadioButton rbCycleRuler;
  private final JRadioButton rbClock;
  private final JRadioButton rbValued;
  private final ValueSourcePanel valueSourcePanel;
  private final ValueRepresentationPanel valueRepresentationPanel;

  private SignalTypePanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public SignalTypePanel(final Diagram diagram, final SDK sdk,
                         final Consumer<String> suggestedLabelSetter)
  {
    Objects.requireNonNull(diagram);
    this.diagram = diagram;
    Objects.requireNonNull(suggestedLabelSetter);
    this.suggestedLabelSetter = suggestedLabelSetter;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createTitledBorder("Signal Type"));
    signalType = new ButtonGroup();
    rbCycleRuler = new JRadioButton("Cycle Ruler");
    rbClock = new JRadioButton("Clock");
    rbValued = new JRadioButton("Valued Signal", true);
    valueSourcePanel = new ValueSourcePanel(diagram, sdk, suggestedLabelSetter);
    valueRepresentationPanel = new ValueRepresentationPanel(diagram, sdk);
    createAndAddCycleRulerRadio();
    createAndAddClockRadio();
    createAndAddValuedRadio();
    createAndAddValueSourcePanel();
    createAndAddValueRepresentationPanel();
    selectValued();
  }

  private void createAndAddCycleRulerRadio()
  {
    final JPanel cycleRulerRadio = new JPanel();
    cycleRulerRadio.
      setLayout(new BoxLayout(cycleRulerRadio, BoxLayout.LINE_AXIS));
    rbCycleRuler.addActionListener((action) -> selectCycleRuler());
    signalType.add(rbCycleRuler);
    cycleRulerRadio.add(rbCycleRuler);
    cycleRulerRadio.add(Box.createHorizontalGlue());
    add(cycleRulerRadio);
  }

  private void createAndAddClockRadio()
  {
    final JPanel clockRadio = new JPanel();
    clockRadio.setLayout(new BoxLayout(clockRadio, BoxLayout.LINE_AXIS));
    rbClock.addActionListener((action) -> selectClock());
    signalType.add(rbClock);
    clockRadio.add(rbClock);
    clockRadio.add(Box.createHorizontalGlue());
    add(clockRadio);
  }

  private void createAndAddValuedRadio()
  {
    final JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
    rbValued.addActionListener((action) -> selectValued());
    signalType.add(rbValued);
    row.add(rbValued);
    row.add(Box.createHorizontalGlue());
    add(row);
  }

  private void createAndAddValueSourcePanel()
  {
    final JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
    row.add(Box.createHorizontalStrut(20));
    row.add(valueSourcePanel);
    SwingUtils.setPreferredHeightAsMaximum(row);
    add(row);
  }

  private void createAndAddValueRepresentationPanel()
  {
    final JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
    row.add(Box.createHorizontalStrut(20));
    row.add(valueRepresentationPanel);
    SwingUtils.setPreferredHeightAsMaximum(row);
    add(row);
  }

  private void selectCycleRuler()
  {
    valueSourcePanel.setEnabled(false);
    valueRepresentationPanel.setEnabled(false);
    suggestedLabelSetter.accept("cycle#");
  }

  private void selectClock()
  {
    valueSourcePanel.setEnabled(false);
    valueRepresentationPanel.setEnabled(false);
    suggestedLabelSetter.accept("clock");
  }

  private void selectValued()
  {
    valueSourcePanel.setEnabled(true);
    valueRepresentationPanel.setEnabled(true);
    valueSourcePanel.updateSuggestedLabel();
  }

  public Signal createSignal(final String label)
  {
    final ButtonModel button = signalType.getSelection();
    if (button == rbCycleRuler.getModel()) {
      return SignalFactory.createRuler(label);
    }
    if (button == rbClock.getModel()) {
      return SignalFactory.createClockSignal(label);
    }
    if (button == rbValued.getModel()) {
      final int address = valueSourcePanel.getSelectedRegisterAddress();
      final int msb = valueSourcePanel.getSelectedRegisterMsb();
      final int lsb = valueSourcePanel.getSelectedRegisterLsb();
      final Supplier<Boolean> displayFilter = null; // TODO
      return
        valueRepresentationPanel.createSignal(label, address, msb, lsb,
                                              displayFilter);
    }
    return null;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
