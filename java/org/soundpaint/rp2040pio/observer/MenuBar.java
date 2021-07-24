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

public class MenuBar<T extends GUIObserver> extends JMenuBar
{
  private static final long serialVersionUID = -5235397033202919401L;

  private final T observer;
  private final JDialog aboutDialog;
  private final JDialog licenseDialog;

  private MenuBar()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public MenuBar(final T observer)
  {
    Objects.requireNonNull(observer);
    this.observer = observer;
    aboutDialog = createAboutDialog();
    final LicenseView licenseView = new LicenseView(observer.getConsole());
    licenseDialog = licenseView.createDialog(this, licenseView.getTitle());
    licenseDialog.setModal(false);
    add(createFileMenu());
    addAdditionalMenus(observer);
    add(createHelpMenu());
  }

  private JDialog createAboutDialog()
  {
    final JOptionPane aboutPane = new JOptionPane();
    final String message =
      String.format("%s%n%s for%n%s%n%s",
                    observer.getAppTitle(), observer.getAppFullName(),
                    Constants.getEmulatorIdAndVersionWithOs(),
                    Constants.getGuiCopyrightNotice());
    aboutPane.setMessage(message);
    aboutPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
    final JDialog aboutDialog = aboutPane.createDialog(this, "About");
    aboutDialog.setModal(false);
    return aboutDialog;
  }

  /**
   * Override this method to add additional menus to appear between
   * the file and the help menus that this class already provides.
   * The default implementation of this method is empty.
   */
  protected void addAdditionalMenus(final T observer)
  {
  }

  private JMenu createFileMenu()
  {
    final JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);

    addAdditionalFileMenuItems(fileMenu, observer);

    final JMenuItem connect = new JMenuItem("Connect…");
    connect.setMnemonic(KeyEvent.VK_N);
    connect.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                                                  ActionEvent.ALT_MASK));
    connect.getAccessibleContext().
      setAccessibleDescription("Connect to Emulation Server");
    connect.addActionListener((event) -> observer.openConnectDialog());
    fileMenu.add(connect);

    final JMenuItem quit =
      SwingUtils.createIconMenuItem("quit16x16.png", "Quit");
    quit.setMnemonic(KeyEvent.VK_Q);
    quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                                               ActionEvent.ALT_MASK));
    quit.getAccessibleContext().setAccessibleDescription("Exit Application");
    quit.addActionListener((event) -> observer.close());
    fileMenu.add(quit);
    return fileMenu;
  }

  /**
   * Override this method to add additional file menu items to appear
   * before the "Quit" item in the file menu that this class provides.
   * The default implementation of this method is empty.
   */
  protected void addAdditionalFileMenuItems(final JMenu fileMenue,
                                            final T observer)
  {
  }

  private JMenu createHelpMenu()
  {
    final JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic(KeyEvent.VK_H);

    final String appTitle = observer.getAppTitle();
    final JMenuItem about = new JMenuItem(String.format("About %s…", appTitle));
    about.setMnemonic(KeyEvent.VK_A);
    about.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                                                ActionEvent.ALT_MASK));
    about.getAccessibleContext().setAccessibleDescription("About this app");
    about.addActionListener((event) -> aboutDialog.setVisible(true));
    helpMenu.add(about);

    final JMenuItem license = new JMenuItem("License…");
    license.setMnemonic(KeyEvent.VK_L);
    license.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                                                  ActionEvent.ALT_MASK));
    license.getAccessibleContext().
      setAccessibleDescription("Show Copying License");
    license.addActionListener((event) -> licenseDialog.setVisible(true));
    helpMenu.add(license);

    return helpMenu;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
