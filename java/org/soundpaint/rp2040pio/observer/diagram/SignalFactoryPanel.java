/*
 * @(#)SignalFactoryPanel.java 1.00 21/07/03
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
import java.util.function.BiFunction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.soundpaint.rp2040pio.sdk.SDK;

public class SignalFactoryPanel extends JPanel
{
  private static final long serialVersionUID = 4492836175968992560L;

  private final Diagram diagram;
  private final BiFunction<String, Signal, String> labelChecker;
  private final SignalLabelPanel signalLabelPanel;
  private final SignalTypePanel signalTypePanel;

  private SignalFactoryPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public SignalFactoryPanel(final Diagram diagram, final SDK sdk,
                            final BiFunction<String, Signal, String> labelChecker)
  {
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(labelChecker);
    this.diagram = diagram;
    this.labelChecker = labelChecker;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    signalLabelPanel = new SignalLabelPanel(diagram);
    add(signalLabelPanel);
    signalTypePanel =
      new SignalTypePanel(diagram, sdk,
                          (label) -> signalLabelPanel.setSuggestedText(label));
    add(signalTypePanel);
    add(Box.createVerticalGlue());
  }

  public Signal createSignal(final Signal ignoreSignal)
  {
    final String label = signalLabelPanel.getText();
    final String message = labelChecker.apply(label, ignoreSignal);
    if (message != null) {
      JOptionPane.showMessageDialog(this, message, "Invalid Signal Label",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
    return signalTypePanel.createSignal(label);
  }

  public void load(final Signal signal)
  {
    signalLabelPanel.load(signal);
    signalTypePanel.load(signal);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
