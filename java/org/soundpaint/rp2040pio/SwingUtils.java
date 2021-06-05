/*
 * @(#)SwingUtils.java 1.00 21/04/07
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
import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;

public class SwingUtils
{
  public static ImageIcon createImageIcon(final String iconFileName,
                                          final String label)
    throws IOException
  {
    final String iconPath = "/media/" + iconFileName;
    final URL iconURL = SwingUtils.class.getResource(iconPath);
    if (iconURL != null) {
      return new ImageIcon(iconURL, label);
    }
    throw new IOException("icon resource not found: " + iconPath);
  }

  public static JMenuItem createIconMenuItem(final String iconFileName,
                                             final String label)
  {
    final JMenuItem menuItem = new JMenuItem(label);
    try {
      final ImageIcon icon = createImageIcon(iconFileName, label);
      menuItem.setIcon(icon);
    } catch (final IOException e) {
      System.err.println("failed creating JMenuItem icon: " + e.getMessage());
    }
    return menuItem;
  }

  public static JButton createIconButton(final String iconFileName,
                                         final String label)
  {
    final JButton button = new JButton(label);
    try {
      final ImageIcon icon = createImageIcon(iconFileName, label);
      return new JButton(icon);
    } catch (final IOException e) {
      System.err.println("failed creating JButton icon: " + e.getMessage());
      return null;
    }
  }

  public static void setPreferredWidthAsMaximum(final Component component)
  {
    final Dimension preferredSize = component.getPreferredSize();
    final Dimension maximumSize =
      new Dimension(preferredSize.width, Integer.MAX_VALUE);
    component.setMaximumSize(maximumSize);
  }

  public static void setPreferredHeightAsMaximum(final Component component)
  {
    final Dimension preferredSize = component.getPreferredSize();
    final Dimension maximumSize =
      new Dimension(Integer.MAX_VALUE, preferredSize.height);
    component.setMaximumSize(maximumSize);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
