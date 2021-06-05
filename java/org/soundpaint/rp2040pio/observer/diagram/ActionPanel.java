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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import org.soundpaint.rp2040pio.SwingUtils;

public class ActionPanel
  extends org.soundpaint.rp2040pio.observer.ActionPanel<Diagram>
{
  private static final long serialVersionUID = -4136799373128393432L;
  private static final int defaultCycles = 1;
  private static final ImageIcon iconEmulate;
  private static final ImageIcon iconClear;
  private static final ImageIcon iconScript;

  static {
    try {
      iconEmulate = SwingUtils.createImageIcon("cycle16x16.png", "Emulate");
      iconClear = SwingUtils.createImageIcon("trash16x16.png", "Clear View");
      iconScript = SwingUtils.createImageIcon("floppy-blue16x16.png", "Load…");
    } catch (final IOException e) {
      final String message =
        String.format("failed loading icon: %s", e.getMessage());
      System.out.println(message);
      throw new InternalError(message, e);
    }
  }

  public ActionPanel(final Diagram diagram)
  {
    super(diagram);
  }

  @Override
  protected void addAdditionalButtons(final Diagram diagram)
  {
    final JButton btScript = new JButton(iconScript);
    btScript.setToolTipText("Load…");
    btScript.addActionListener((event) -> { diagram.showScriptDialog(); });
    add(btScript);
    add(Box.createHorizontalStrut(15));
    add(Box.createHorizontalGlue());

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

    final JButton btEmulate = new JButton(iconEmulate);
    btEmulate.setToolTipText("Emulate");
    btEmulate.addActionListener((event) -> {
        final int cycles = (Integer)spCycles.getValue();
        try {
          diagram.applyCycles(cycles);
        } catch (final IOException e) {
          final String title = "Emulation Failed";
          final String message = "I/O error: " + e.getMessage();
          JOptionPane.showMessageDialog(this, message, title,
                                        JOptionPane.WARNING_MESSAGE);
          diagram.clear();
        }
      });
    add(btEmulate);
    add(Box.createHorizontalStrut(15));
    add(Box.createHorizontalGlue());

    final JButton btClear = new JButton(iconClear);
    btClear.setToolTipText("Clear View");
    btClear.addActionListener((event) -> diagram.clear());
    add(btClear);
    add(Box.createHorizontalStrut(15));
    add(Box.createHorizontalGlue());

    final JLabel lbZoom = new JLabel("Zoom");
    lbZoom.setDisplayedMnemonic(KeyEvent.VK_Z);
    add(lbZoom);
    add(Box.createHorizontalStrut(5));
    final JSlider slZoom =
      new JSlider(DiagramView.ZOOM_MIN, DiagramView.ZOOM_MAX,
                  DiagramView.ZOOM_DEFAULT);
    lbZoom.setLabelFor(slZoom);
    slZoom.setMajorTickSpacing(DiagramView.ZOOM_MIN);
    slZoom.setPaintTicks(true);
    slZoom.setLabelTable(slZoom.createStandardLabels(DiagramView.ZOOM_MIN));
    slZoom.setPaintLabels(true);
    slZoom.addChangeListener((event) -> diagram.setZoom(slZoom.getValue()));
    add(slZoom);

    add(Box.createHorizontalStrut(15));
    add(Box.createHorizontalGlue());
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
