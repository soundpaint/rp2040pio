/*
 * @(#)ScriptDialog.java 1.00 21/04/09
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
package org.soundpaint.rp2040pio.diagram;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;

public class ScriptDialog extends JDialog
{
  private static final long serialVersionUID = 5065109349974149543L;

  private class ActionPanel extends Box
  {
    private static final long serialVersionUID = -3909730642902134887L;

    private final JButton btClose;

    public ActionPanel()
    {
      super(BoxLayout.X_AXIS);
      add(Box.createHorizontalGlue());
      btClose = new JButton("Close");
      btClose.setMnemonic(KeyEvent.VK_C);
      btClose.addActionListener((event) -> {
          ScriptDialog.this.setVisible(false);
        });
      add(btClose);
    }
  }

  private ScriptDialog()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public ScriptDialog(final TimingDiagram timingDiagram,
                      final PrintStream console)
  {
    super(timingDiagram, "Load");
    Objects.requireNonNull(console);
    getContentPane().add(new ScriptSelectionPanel(console));
    getContentPane().add(new ActionPanel(), BorderLayout.SOUTH);
    pack();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
