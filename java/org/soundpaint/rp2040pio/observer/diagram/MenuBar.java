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
import java.util.Objects;
import javax.swing.KeyStroke;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.soundpaint.rp2040pio.SwingUtils;

public class MenuBar extends JMenuBar
{
  private static final long serialVersionUID = -5984414867480448181L;

  private final TimingDiagram timingDiagram;
  private final ViewPropertiesDialog viewPropertiesDialog;

  private MenuBar()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public MenuBar(final TimingDiagram timingDiagram,
                 final ScriptDialog scriptDialog)
  {
    Objects.requireNonNull(timingDiagram);
    this.timingDiagram = timingDiagram;
    add(createFileMenu(scriptDialog));
    add(createViewMenu());
    add(createHelpMenu());
    viewPropertiesDialog = new ViewPropertiesDialog(timingDiagram);
  }

  private JMenu createFileMenu(final ScriptDialog scriptDialog)
  {
    final JMenu file = new JMenu("File");
    file.setMnemonic(KeyEvent.VK_F);

    final JMenuItem script = new JMenuItem("Load…");
    script.setMnemonic(KeyEvent.VK_L);
    script.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                                                 ActionEvent.ALT_MASK));
    script.getAccessibleContext().setAccessibleDescription("Run load script");
    script.addActionListener((event) -> { scriptDialog.setVisible(true); });
    file.add(script);

    final JMenuItem close =
      SwingUtils.createIconMenuItem("quit16x16.png", "Quit");
    close.setMnemonic(KeyEvent.VK_C);
    close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                                                ActionEvent.ALT_MASK));
    close.getAccessibleContext().setAccessibleDescription("Quit");
    close.addActionListener((event) -> { timingDiagram.close(); });
    file.add(close);
    return file;
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
    properties.addActionListener((event) -> { viewPropertiesDialog.open(); });
    view.add(properties);
    return view;
  }

  private JMenu createHelpMenu()
  {
    final JMenu help = new JMenu("Help");
    help.setMnemonic(KeyEvent.VK_H);

    final JMenuItem about = new JMenuItem("About…");
    about.setMnemonic(KeyEvent.VK_A);
    about.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                                                ActionEvent.ALT_MASK));
    about.getAccessibleContext().setAccessibleDescription("About this app");
    about.addActionListener((event) -> {
        JOptionPane.showMessageDialog(this,
                                      TimingDiagram.getAboutText(),
                                      "About",
                                      JOptionPane.INFORMATION_MESSAGE);
      });
    help.add(about);
    return help;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
