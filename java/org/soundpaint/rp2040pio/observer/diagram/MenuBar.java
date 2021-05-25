/*
 * @(#)MenuBar.java 1.00 21/04/07
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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.PrintStream;
import javax.swing.KeyStroke;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class MenuBar
  extends org.soundpaint.rp2040pio.observer.MenuBar<Diagram>
{
  private static final long serialVersionUID = -5984414867480448181L;

  private final Diagram diagram;
  private final ViewPropertiesDialog viewPropertiesDialog;

  public MenuBar(final Diagram diagram, final PrintStream console)
  {
    super(diagram, console);
    if (diagram == null) {
      throw new NullPointerException("diagram");
    }
    this.diagram = diagram;
    viewPropertiesDialog = new ViewPropertiesDialog(diagram);
  }

  @Override
  protected void addAdditionalFileMenuItems(final JMenu fileMenu,
                                            final Diagram diagram)
  {
    final JMenuItem script = new JMenuItem("Load…");
    script.setMnemonic(KeyEvent.VK_L);
    script.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                                                 ActionEvent.ALT_MASK));
    script.getAccessibleContext().setAccessibleDescription("Run load script");
    script.addActionListener((event) -> diagram.showScriptDialog());
    fileMenu.add(script);
  }

  @Override
  protected void addAdditionalMenus(final Diagram diagram)
  {
    add(createViewMenu());
  }

  private JMenu createViewMenu()
  {
    final JMenu view = new JMenu("View");
    view.setMnemonic(KeyEvent.VK_V);

    final JMenuItem properties = new JMenuItem("Properties…");
    properties.setMnemonic(KeyEvent.VK_P);
    properties.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                                                     ActionEvent.ALT_MASK));
    properties.getAccessibleContext().
      setAccessibleDescription("View properties");
    properties.addActionListener((event) -> viewPropertiesDialog.open());
    view.add(properties);

    final JMenuItem clear = new JMenuItem("Clear View");
    clear.setMnemonic(KeyEvent.VK_C);
    clear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                                                ActionEvent.ALT_MASK));
    clear.getAccessibleContext().setAccessibleDescription("Clear View");
    clear.addActionListener((event) -> diagram.clear());
    view.add(clear);

    return view;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
