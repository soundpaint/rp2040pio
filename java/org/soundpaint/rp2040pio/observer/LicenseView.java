/*
 * @(#)LicenseView.java 1.00 21/05/22
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

import java.awt.Dimension;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

public class LicenseView extends JOptionPane
{
  private static final long serialVersionUID = 6794879115024130934L;
  private static final String TITLE = "License";

  public LicenseView(final PrintStream console)
  {
    setMessageType(INFORMATION_MESSAGE);
    final JEditorPane htmlPane = new JEditorPane();
    htmlPane.setEditable(false);

    final String resourcePath = "/media/gpl-2.0-standalone.html";
    final URL resourceURL = LicenseView.class.getResource(resourcePath);
    if (resourceURL == null) {
      console.println("failed opening license file: " + resourcePath);
    }
    try {
      if (resourceURL != null) {
        htmlPane.setPage(resourceURL);
      } else {
        htmlPane.setText("license not found");
      }
    } catch (final IOException e) {
      console.println("attempted to read a bad URL: " + resourceURL);
    }
    final JScrollPane htmlView = new JScrollPane(htmlPane);
    final Dimension preferredSize = new Dimension(600, 480);
    htmlView.setPreferredSize(preferredSize);
    setMessage(htmlView);
  }

  public String getTitle()
  {
    return TITLE;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
