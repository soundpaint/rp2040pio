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
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.soundpaint.rp2040pio.sdk.SDK;

public class ValueRenderingPanel extends JPanel
{
  private static final long serialVersionUID = -5251622302191656176L;

  private final Diagram diagram;
  private final SDK sdk;
  private final Consumer<Void> renderingChangedListener;
  private final ButtonGroup buttonGroup;
  private SignalRendering selectedRendering;

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
    for (final SignalRendering rendering : SignalRendering.values()) {
      createAndAddRendering(rendering);
    }
  }

  private void createAndAddRendering(final SignalRendering rendering)
  {
    final JPanel line = new JPanel();
    line.setLayout(new BoxLayout(line, BoxLayout.LINE_AXIS));

    final boolean selected = rendering == SignalRendering.Signed;
    final JRadioButton rbRendering =
      new JRadioButton(rendering.toString(), selected);
    if (selected) {
      selectedRendering = rendering;
    }
    rbRendering.addActionListener((action) -> renderingSelected(rendering));
    buttonGroup.add(rbRendering);
    line.add(rbRendering);
    line.add(Box.createHorizontalGlue());
    add(line);
  }

  private void renderingSelected(final SignalRendering rendering)
  {
    selectedRendering = rendering;
    renderingChangedListener.accept(null);
  }

  public boolean isSmSelectionRelevant()
  {
    return selectedRendering == SignalRendering.Mnemonic;
  }

  public Signal createSignal(final String label,
                             final int pioNum,
                             final int smNum,
                             final int address,
                             final int msb,
                             final int lsb,
                             final List<SignalFilter> displayFilters,
                             final boolean visible)
  {
    if ((msb < 0) || (lsb < 0)) {
      JOptionPane.showMessageDialog(this,
                                    "Please select a contiguous range of bits.",
                                    "No Bits Range Selected",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
    try {
      final Signal signal =
        selectedRendering.createSignal(diagram, sdk, label, address,
                                       msb, lsb, displayFilters, pioNum, smNum);
      signal.setVisible(visible);
      return signal;
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

  private void selectRendering(final SignalRendering signalRendering)
  {
    selectedRendering =
      signalRendering != null ? signalRendering : SignalRendering.Signed;
    final Enumeration<AbstractButton> buttons = buttonGroup.getElements();
    while (buttons.hasMoreElements()) {
      final JRadioButton button = (JRadioButton)buttons.nextElement();
      final boolean isSelected =
        /*
         * TODO: Better use a hash map rather than relying on button
         * label.
         */
        button.getText() == selectedRendering.toString();
      button.setSelected(isSelected);
    }
  }

  public void load(final ValuedSignal<?> signal)
  {
    selectRendering(signal != null ? signal.getValueRendering() : null);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
