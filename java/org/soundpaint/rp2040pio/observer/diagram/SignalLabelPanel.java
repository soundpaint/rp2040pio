/*
 * @(#)SignalLabelPanel.java 1.00 21/07/03
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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.soundpaint.rp2040pio.SwingUtils;

public class SignalLabelPanel extends JPanel
{
  private static final long serialVersionUID = 5522534111472664050L;

  private final Diagram diagram;
  private final JTextField tfLabel;
  private final JTextField tfSuggestedLabel;

  private SignalLabelPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public SignalLabelPanel(final Diagram diagram)
  {
    Objects.requireNonNull(diagram);
    this.diagram = diagram;
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    setBorder(BorderFactory.createTitledBorder("Signal Label"));
    tfLabel = new JTextField(20);
    add(tfLabel);
    add(Box.createHorizontalStrut(20));
    tfSuggestedLabel = new JTextField(20);
    tfSuggestedLabel.setEditable(false);
    final JButton btApply = new JButton("←");
    btApply.addActionListener((action) ->
                              tfLabel.setText(tfSuggestedLabel.getText()));
    add(btApply);
    add(Box.createHorizontalStrut(20));
    add(new JLabel("Suggested Label"));
    add(Box.createHorizontalStrut(5));
    add(tfSuggestedLabel);
    add(Box.createHorizontalGlue());
    SwingUtils.setPreferredWidthAsMaximum(tfLabel);
    SwingUtils.setPreferredHeightAsMaximum(this);
  }

  public String getText()
  {
    return tfLabel.getText();
  }

  public void setSuggestedText(final String label)
  {
    if (label == null) {
      throw new NullPointerException("label");
    }
    tfSuggestedLabel.setText(label);
  }

  public void load(final Signal signal)
  {
    final String label = signal != null ? signal.getLabel() : null;
    tfLabel.setText(label);
    if (label != null) tfSuggestedLabel.setText(label);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
