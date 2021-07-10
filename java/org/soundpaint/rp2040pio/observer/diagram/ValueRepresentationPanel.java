/*
 * @(#)ValueRepresentationPanel.java 1.00 21/07/08
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

import java.awt.event.ItemEvent;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.soundpaint.rp2040pio.sdk.SDK;

public class ValueRepresentationPanel extends JPanel implements Constants
{
  private static final long serialVersionUID = -7271655535997736886L;

  private final Diagram diagram;
  private final SDK sdk;
  private final JLabel lbFormat;
  private final ValueFormatPanel valueFormatPanel;

  private ValueRepresentationPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public ValueRepresentationPanel(final Diagram diagram, final SDK sdk)
  {
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(sdk);
    this.diagram = diagram;
    this.sdk = sdk;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createTitledBorder("Value Representation"));
    lbFormat = new JLabel("Format");
    lbFormat.setPreferredSize(PREFERRED_LABEL_SIZE);
    valueFormatPanel = new ValueFormatPanel(diagram, sdk);
    add(valueFormatPanel);
  }

  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    lbFormat.setEnabled(enabled);
    valueFormatPanel.setEnabled(enabled);
  }

  public Signal createSignal(final String label,
                             final int address,
                             final int msb,
                             final int lsb,
                             final Supplier<Boolean> displayFilter)
  {
    return
      valueFormatPanel.createSignal(label, address, msb, lsb, displayFilter);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
