/*
 * @(#)CollapsiblePanel.java 1.00 21/06/05
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
package org.soundpaint.rp2040pio;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JToggleButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CollapsiblePanel extends JPanel
{
  private static final long serialVersionUID = -1313958807949980972L;

  private final Component component;
  private final JToggleButton btToggle;

  public CollapsiblePanel(final Component component,
                          final String label,
                          final boolean initiallyOpen)
  {
    Objects.requireNonNull(component);
    Objects.requireNonNull(label);
    this.component = component;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    btToggle = new JToggleButton();
    btToggle.setBorderPainted(false);
    btToggle.setContentAreaFilled(false);
    btToggle.setFocusPainted(false);
    btToggle.setOpaque(false);
    btToggle.setBorder(BorderFactory.createEmptyBorder());
    final ActionListener toggleAction = (action) -> {
      final boolean isSelected = btToggle.isSelected();
      component.setVisible(isSelected);
      btToggle.setText(isSelected ? "⊟" : "⊞");
    };
    btToggle.addActionListener(toggleAction);
    add(createToggleButtonLine(label));
    add(component);
    btToggle.setSelected(initiallyOpen);
    toggleAction.actionPerformed(null);
  }

  private Box createToggleButtonLine(final String label)
  {
    final Box hBox = new Box(BoxLayout.LINE_AXIS);
    hBox.add(btToggle);

    // Workaround: hBox.add(Box.createHorizontalStrut(5)) modifies Y
    // layout behavior. => Add JLabel instead.
    hBox.add(new JLabel(" "));

    hBox.add(new JLabel(label));
    hBox.add(Box.createHorizontalGlue());
    return hBox;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
