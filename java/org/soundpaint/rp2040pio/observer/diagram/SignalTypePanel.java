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
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
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
  private final ValueRenderingPanel valueRenderingPanel;
  private final ValueFilterPanel valueFilterPanel;
  private final SmSelectionPanel smSelectionPanel;
  private boolean visible;

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
    valueSourcePanel =
      new ValueSourcePanel(diagram, sdk, suggestedLabelSetter,
                           (dummy) -> updateSmSelectionInfo());
    valueRenderingPanel =
      new ValueRenderingPanel(diagram, sdk,
                              (dummy) -> updateSmSelectionEnableStatus());
    valueFilterPanel =
      new ValueFilterPanel(diagram, sdk,
                           (dummy) -> updateSmSelectionEnableStatus());
    createAndAddCycleRulerRadio();
    createAndAddClockRadio();
    createAndAddValuedRadio();
    createAndAddValuePanels();
    smSelectionPanel = new SmSelectionPanel(diagram, sdk);
    createAndAddSmSelectionPanel();
    valueSourcePanel.initRegistersForSelectedRegisterSet();
    selectValued();
    visible = false;
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
    createAndAddValueRenderingPanel(valueTabs);
    createAndAddValueFilterPanel(valueTabs);
    row.add(valueTabs);
    SwingUtils.setPreferredHeightAsMaximum(row);
    add(row);
  }

  private void createAndAddValueSourcePanel(final JTabbedPane valueTabs)
  {
    valueTabs.addTab("Source", null, valueSourcePanel,
                     "specify how to get values of this signal");
    final int tabIndex = valueTabs.indexOfComponent(valueSourcePanel);
    valueTabs.setMnemonicAt(tabIndex, KeyEvent.VK_S);
  }

  private void createAndAddValueRenderingPanel(final JTabbedPane valueTabs)
  {
    valueTabs.addTab("Rendering", null, valueRenderingPanel,
                     "specify how to render values of this signal");
    final int tabIndex = valueTabs.indexOfComponent(valueRenderingPanel);
    valueTabs.setMnemonicAt(tabIndex, KeyEvent.VK_R);
  }

  private void createAndAddValueFilterPanel(final JTabbedPane valueTabs)
  {
    valueTabs.addTab("Filter", null, valueFilterPanel,
                     "specify which filters to apply to decide if signal " +
                     "value is defined");
    final int tabIndex = valueTabs.indexOfComponent(valueFilterPanel);
    valueTabs.setMnemonicAt(tabIndex, KeyEvent.VK_F);
  }

  private void createAndAddSmSelectionPanel()
  {
    final JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
    row.add(Box.createHorizontalStrut(20));
    row.add(smSelectionPanel);
    SwingUtils.setPreferredHeightAsMaximum(row);
    add(row);
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
    valueRenderingPanel.setEnabled(enabled);
    valueFilterPanel.setEnabled(enabled);
    valueTabs.setEnabled(enabled);
    updateSmSelectionEnableStatus();
  }

  private void updateSmSelectionEnableStatus()
  {
    final boolean enabled =
      valueTabs.isEnabled() &&
      (valueRenderingPanel.isSmSelectionRelevant() ||
       valueFilterPanel.isSmSelectionRelevant());
    smSelectionPanel.setEnabled(enabled);
  }

  private void updateSmSelectionInfo()
  {
    final int sourcePioNum = valueSourcePanel.getSelectedRegisterSetPio();
    final int sourceSmNum = valueSourcePanel.getSelectedRegisterSm();
    smSelectionPanel.updateSourceInfo(sourcePioNum, sourceSmNum);
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
      final int sourcePioNum = valueSourcePanel.getSelectedRegisterSetPio();
      final int sourceSmNum = valueSourcePanel.getSelectedRegisterSm();
      final int pioNum = smSelectionPanel.getPioNum(sourcePioNum);
      final int smNum = smSelectionPanel.getSmNum(sourceSmNum);
      final List<SignalFilter> displayFilters =
        valueFilterPanel.createFilters();
      return
        valueRenderingPanel.createSignal(label, pioNum, smNum,
                                         address, msb, lsb, displayFilters,
                                         visible);
    }
    return null;
  }

  public void load(final Signal signal)
  {
    visible = signal != null ? signal.getVisible() : false;
    valueTabs.setSelectedIndex(0);
    final ValuedSignal<?> valuedSignal =
      signal instanceof ValuedSignal ? (ValuedSignal<?>)signal : null;
    valueSourcePanel.load(valuedSignal);
    final int pioNum =
      valuedSignal != null ? valueSourcePanel.getSelectedRegisterSetPio() : -1;
    final int smNum =
      valuedSignal != null ? valueSourcePanel.getSelectedRegisterSm() : -1;
    valueRenderingPanel.load(valuedSignal);
    valueFilterPanel.load(valuedSignal);
    smSelectionPanel.load(valuedSignal, pioNum, smNum);
    if ((signal == null) ||
        (signal instanceof ValuedSignal)) {
      rbValued.setSelected(true);
      selectValued();
    } else if (signal instanceof CycleRuler) {
      rbCycleRuler.setSelected(true);
      selectCycleRuler();
    } else if (signal instanceof ClockSignal) {
      rbClock.setSelected(true);
      selectClock();
    } else {
      final String message =
        String.format("warning: failed loading signal preload values: " +
                      "unknown signal type: %s", signal.getClass());
      diagram.getConsole().println(message);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
