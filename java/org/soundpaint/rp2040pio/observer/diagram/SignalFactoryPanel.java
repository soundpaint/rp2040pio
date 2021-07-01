/*
 * @(#)SignalFactoryPanel.java 1.00 21/06/30
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

import java.util.Objects;
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

public class SignalFactoryPanel extends JPanel
{
  private static final long serialVersionUID = -3023263223044270621L;

  private final Diagram diagram;
  private final ButtonGroup signalType;
  private final JRadioButton rbCycleRuler;
  private final JRadioButton rbClock;
  private final JRadioButton rbValued;
  private final JTextField tfLabel;
  private final ValuedSignalPropertiesPanel valuedProperties;

  private SignalFactoryPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public SignalFactoryPanel(final Diagram diagram, final SDK sdk)
  {
    Objects.requireNonNull(diagram);
    this.diagram = diagram;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    signalType = new ButtonGroup();
    rbCycleRuler = new JRadioButton("Cycle Ruler");
    signalType.add(rbCycleRuler);
    add(rbCycleRuler);
    rbCycleRuler.addActionListener((action) -> selectCycleRuler());
    rbClock = new JRadioButton("Clock");
    signalType.add(rbClock);
    add(rbClock);
    rbClock.addActionListener((action) -> selectClock());
    rbValued = new JRadioButton("Valued Signal");
    signalType.add(rbValued);
    add(rbValued);
    rbValued.addActionListener((action) -> selectValued());
    rbValued.setSelected(true);
    add(valuedProperties = new ValuedSignalPropertiesPanel(diagram, sdk));
    selectValued();
    SwingUtils.setPreferredWidthAsMaximum(tfLabel = new JTextField(20));
    add(Box.createVerticalStrut(5));
    add(createLabelLine());
    add(Box.createVerticalGlue());
  }

  private JPanel createValuedProperties()
  {
    final JPanel valuedProperties = new JPanel();
    return valuedProperties;
  }

  private JPanel createLabelLine()
  {
    final JPanel labelLine = new JPanel();
    labelLine.setLayout(new BoxLayout(labelLine, BoxLayout.X_AXIS));
    labelLine.add(new JLabel("Signal Label"));
    labelLine.add(Box.createHorizontalStrut(5));
    labelLine.add(tfLabel);
    labelLine.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(labelLine);
    return labelLine;
  }

  private void selectCycleRuler()
  {
    valuedProperties.setEnabled(false);
  }

  private void selectClock()
  {
    valuedProperties.setEnabled(false);
  }

  private void selectValued()
  {
    valuedProperties.setEnabled(true);
  }

  public Signal createSignal()
  {
    final String label = tfLabel.getText();
    if (label.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Signal label must not be empty.",
                                    "Invalid Signal Label",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
    final ButtonModel button = signalType.getSelection();
    if (button == rbCycleRuler.getModel()) {
      return SignalFactory.createRuler(label);
    }
    if (button == rbClock.getModel()) {
      return SignalFactory.createClockSignal(label);
    }
    if (button == rbValued.getModel()) {
      return valuedProperties.createSignal(label);
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
