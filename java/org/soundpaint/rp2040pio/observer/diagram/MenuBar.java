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
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class MenuBar
  extends org.soundpaint.rp2040pio.observer.MenuBar<Diagram>
  implements Constants
{
  private static final long serialVersionUID = -5984414867480448181L;

  private final Diagram diagram;
  private final SignalsDialog signalsDialog;

  public MenuBar(final Diagram diagram, final SDK sdk)
  {
    super(diagram);
    if (diagram == null) {
      throw new NullPointerException("diagram");
    }
    this.diagram = diagram;
    signalsDialog = new SignalsDialog(diagram, sdk);
  }

  @Override
  protected void addAdditionalFileMenuItems(final JMenu fileMenu,
                                            final Diagram diagram)
  {
    final JMenuItem script =
      SwingUtils.createIconMenuItem("floppy-blue16x16.png", "Open…");
    script.setMnemonic(KeyEvent.VK_O);
    script.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                                                 ActionEvent.ALT_MASK));
    script.getAccessibleContext().
      setAccessibleDescription("Open Script for Execution");
    script.addActionListener((event) -> diagram.showScriptDialog());
    fileMenu.add(script);
  }

  @Override
  protected void addAdditionalMenus(final Diagram diagram)
  {
    add(createEditMenu());
    add(createEmulationMenu());
  }

  private JMenu createEditMenu()
  {
    final JMenu edit = new JMenu("Edit");
    edit.setMnemonic(KeyEvent.VK_E);

    final JMenuItem properties = new JMenuItem("Signals…");
    properties.setMnemonic(KeyEvent.VK_P);
    properties.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                                                     ActionEvent.ALT_MASK));
    properties.getAccessibleContext().
      setAccessibleDescription("Open Properties Dialog");
    properties.addActionListener((event) -> signalsDialog.open());
    edit.add(properties);

    return edit;
  }

  private JMenu createEmulationMenu()
  {
    final JMenu emulation = new JMenu("Emulation");
    emulation.setMnemonic(KeyEvent.VK_M);

    final JMenuItem emulate =
      SwingUtils.createIconMenuItem("cycle16x16.png", "Emulate Cycles");
    emulate.setMnemonic(KeyEvent.VK_E);
    emulate.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,
                                                  ActionEvent.ALT_MASK));
    emulate.getAccessibleContext().
      setAccessibleDescription(TOOLTIP_TEXT_EMULATE);
    emulate.addActionListener((event) -> diagram.applyCycles());
    emulation.add(emulate);

    final JMenuItem clear =
      SwingUtils.createIconMenuItem("trash16x16.png", "Clear Recorded Cycles");
    clear.setMnemonic(KeyEvent.VK_C);
    clear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                                                ActionEvent.ALT_MASK));
    clear.getAccessibleContext().
      setAccessibleDescription(TOOLTIP_TEXT_CLEAR);
    clear.addActionListener((event) -> diagram.clear());
    emulation.add(clear);

    return emulation;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
