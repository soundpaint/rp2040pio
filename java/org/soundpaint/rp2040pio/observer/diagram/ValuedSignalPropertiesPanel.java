/*
 * @(#)ValuedSignalPropertiesPanel.java 1.00 21/06/30
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
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class ValuedSignalPropertiesPanel extends JPanel
{
  private static final long serialVersionUID = -726756788602613552L;

  private final Diagram diagram;

  private ValuedSignalPropertiesPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public ValuedSignalPropertiesPanel(final Diagram diagram)
  {
    Objects.requireNonNull(diagram);
    this.diagram = diagram;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    // TODO: Add more components.
  }

  @Override
  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    // TODO: Enable / disable child components.
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
