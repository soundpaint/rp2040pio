/*
 * @(#)ActionPanel.java 1.00 21/04/06
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

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;

public class ActionPanel
  extends org.soundpaint.rp2040pio.observer.ActionPanel<Diagram>
{
  private static final long serialVersionUID = -4136799373128393432L;
  private static final int defaultCycles = 20;

  public ActionPanel(final Diagram diagram)
  {
    super(diagram);
  }

  @Override
  protected void addAdditionalButtons(final Diagram diagram)
  {
    final JLabel lbCycles = new JLabel("Cycles");
    lbCycles.setDisplayedMnemonic(KeyEvent.VK_Y);
    add(lbCycles);
    add(Box.createHorizontalStrut(5));
    final SpinnerModel cyclesModel =
      new SpinnerNumberModel(defaultCycles, 1, 1000, 1);
    final JSpinner spCycles = new JSpinner(cyclesModel);
    final int spCyclesHeight = spCycles.getPreferredSize().height;
    spCycles.setMaximumSize(new Dimension(100, spCyclesHeight));
    lbCycles.setLabelFor(spCycles);
    add(spCycles);
    add(Box.createHorizontalStrut(5));

    final JButton btEmulate = new JButton("Emulate");
    btEmulate.setMnemonic(KeyEvent.VK_E);
    btEmulate.addActionListener((event) -> {
        final int cycles = (Integer)spCycles.getValue();
        try {
          diagram.createSnapShot(cycles);
        } catch (final IOException e) {
          final String title = "Failed Creating Snapshot";
          final String message = "I/O Error: " + e.getMessage();
          JOptionPane.showMessageDialog(this, message, title,
                                        JOptionPane.WARNING_MESSAGE);
          diagram.clear();
        }
      });
    add(btEmulate);
    add(Box.createHorizontalStrut(5));

    final JButton btScript = new JButton("Load…");
    btScript.setMnemonic(KeyEvent.VK_L);
    btScript.addActionListener((event) -> { diagram.showScriptDialog(); });
    add(btScript);
    add(Box.createHorizontalGlue());
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
