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

import java.awt.event.KeyEvent;
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
import javax.swing.JTabbedPane;
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
  private final JTabbedPane valueTabs;
  private final ValueSourcePanel valueSourcePanel;
  private final ValueFormatPanel valueFormatPanel;
  private final ValueFilterPanel valueFilterPanel;

  private SignalTypePanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public SignalTypePanel(final Diagram diagram, final SDK sdk,
                         final Consumer<String> suggestedLabelSetter)
  {
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(suggestedLabelSetter);
    this.diagram = diagram;
    this.suggestedLabelSetter = suggestedLabelSetter;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createTitledBorder("Signal Type"));
    signalType = new ButtonGroup();
    rbCycleRuler = new JRadioButton("Cycle Ruler");
    rbClock = new JRadioButton("Clock");
    rbValued = new JRadioButton("Valued Signal", true);
    valueTabs = new JTabbedPane();
    valueSourcePanel = new ValueSourcePanel(diagram, sdk, suggestedLabelSetter);
    valueFormatPanel = new ValueFormatPanel(diagram, sdk);
    valueFilterPanel = new ValueFilterPanel(diagram, sdk);
    createAndAddCycleRulerRadio();
    createAndAddClockRadio();
    createAndAddValuedRadio();
    createAndAddValuePanels();
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

  private void createAndAddValuePanels()
  {
    final JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
    row.add(Box.createHorizontalStrut(20));
    createAndAddValueSourcePanel(valueTabs);
    createAndAddValueFormatPanel(valueTabs);
    createAndAddValueFilterPanel(valueTabs);
    row.add(valueTabs);
    SwingUtils.setPreferredHeightAsMaximum(row);
    add(row);
  }

  private void createAndAddValueSourcePanel(final JTabbedPane valueTabs)
  {
    valueTabs.addTab("Value Source", null, valueSourcePanel,
                     "specify how to get values of this signal");
    final int tabIndex = valueTabs.indexOfComponent(valueSourcePanel);
    valueTabs.setMnemonicAt(tabIndex, KeyEvent.VK_S);
  }

  private void createAndAddValueFormatPanel(final JTabbedPane valueTabs)
  {
    valueTabs.addTab("Value Rendering", null, valueFormatPanel,
                     "specify how to render values of this signal");
    final int tabIndex = valueTabs.indexOfComponent(valueFormatPanel);
    valueTabs.setMnemonicAt(tabIndex, KeyEvent.VK_R);
  }

  private void createAndAddValueFilterPanel(final JTabbedPane valueTabs)
  {
    valueTabs.addTab("Value Filter", null, valueFilterPanel,
                     "specify which filters to apply to decide if signal " +
                     "value is defined");
    final int tabIndex = valueTabs.indexOfComponent(valueFilterPanel);
    valueTabs.setMnemonicAt(tabIndex, KeyEvent.VK_F);
  }

  private void selectCycleRuler()
  {
    setValueEnabled(false);
    suggestedLabelSetter.accept("cycle#");
  }

  private void selectClock()
  {
    setValueEnabled(false);
    suggestedLabelSetter.accept("clock");
  }

  private void selectValued()
  {
    setValueEnabled(true);
    valueSourcePanel.updateSuggestedLabel();
  }

  private void setValueEnabled(final boolean enabled)
  {
    valueSourcePanel.setEnabled(enabled);
    valueFormatPanel.setEnabled(enabled);
    valueFilterPanel.setEnabled(enabled);
    valueTabs.setEnabled(enabled);
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
      final int pioNum = valueSourcePanel.getSelectedRegisterSetPio();
      final int smNum = valueSourcePanel.getSelectedRegisterSm();
      final Supplier<Boolean> displayFilter =
        valueFilterPanel.createFilter(pioNum, smNum);
      return
        valueFormatPanel.createSignal(label, address, msb, lsb, displayFilter);
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
