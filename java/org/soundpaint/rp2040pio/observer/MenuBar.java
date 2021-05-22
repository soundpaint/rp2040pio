/*
 * @(#)MenuBar.java 1.00 21/05/22
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
package org.soundpaint.rp2040pio.observer;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.KeyStroke;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.SwingUtils;

public class MenuBar extends JMenuBar
{
  private static final long serialVersionUID = -5235397033202919401L;

  private final GUIObserver observer;
  private final JDialog licenseDialog;

  private MenuBar()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public MenuBar(final GUIObserver observer, final PrintStream console)
  {
    Objects.requireNonNull(observer);
    this.observer = observer;
    final LicenseView licenseView = new LicenseView(console);
    licenseDialog = licenseView.createDialog(this, licenseView.getTitle());
    add(createFileMenu());
    add(createHelpMenu());
  }

  private JMenu createFileMenu()
  {
    final JMenu file = new JMenu("File");
    file.setMnemonic(KeyEvent.VK_F);

    final JMenuItem close =
      SwingUtils.createIconMenuItem("quit16x16.png", "Quit");
    close.setMnemonic(KeyEvent.VK_C);
    close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                                                ActionEvent.ALT_MASK));
    close.getAccessibleContext().setAccessibleDescription("Quit");
    close.addActionListener((event) -> observer.close());
    file.add(close);
    return file;
  }

  private JMenu createHelpMenu()
  {
    final JMenu help = new JMenu("Help");
    help.setMnemonic(KeyEvent.VK_H);

    final String appTitle = observer.getAppTitle();
    final JMenuItem about = new JMenuItem(String.format("About %s…", appTitle));
    about.setMnemonic(KeyEvent.VK_A);
    about.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                                                ActionEvent.ALT_MASK));
    about.getAccessibleContext().setAccessibleDescription("About this app");
    about.addActionListener((event) -> {
        final String message =
          String.format("%s%n%s for%n%s",
                        observer.getAppTitle(), observer.getAppFullName(),
                        Constants.getEmulatorAbout());
        JOptionPane.showMessageDialog(this, message, "About",
                                      JOptionPane.INFORMATION_MESSAGE);
      });
    help.add(about);

    final JMenuItem license = new JMenuItem("License…");
    license.setMnemonic(KeyEvent.VK_L);
    license.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                                                  ActionEvent.ALT_MASK));
    license.getAccessibleContext().setAccessibleDescription("Copying License");
    license.addActionListener((event) -> licenseDialog.setVisible(true));
    help.add(license);

    return help;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
