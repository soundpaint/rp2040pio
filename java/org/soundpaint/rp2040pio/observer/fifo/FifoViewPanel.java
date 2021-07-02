/*
 * @(#)FifoViewPanel.java 1.00 21/04/28
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
package org.soundpaint.rp2040pio.observer.fifo;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class FifoViewPanel extends JPanel
{
  private static final long serialVersionUID = -2782955870967022886L;
  private static final boolean initialAutoScroll = true;
  private static final String autoScrollToolTipText =
    "automatically rotates display of FIFO cyclic buffer such that the " +
    "next entry to be dequeued is displayed leftmost";

  private final PrintStream console;
  private final SDK sdk;
  private final FifoEntriesViewPanel fifoEntriesViewPanel;
  private final JCheckBox cbAutoScroll;
  private int pioNum;
  private int smNum;

  private FifoViewPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public FifoViewPanel(final PrintStream console, final SDK sdk,
                       final String borderTitle)
    throws IOException
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    this.console = console;
    this.sdk = sdk;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createTitledBorder(borderTitle));
    fifoEntriesViewPanel = new FifoEntriesViewPanel(console, sdk,
                                                    initialAutoScroll);

    final Box pioSelection = new Box(BoxLayout.LINE_AXIS);
    add(pioSelection);
    final JLabel lbPio = new JLabel("PIO");
    pioSelection.add(lbPio);
    pioSelection.add(Box.createHorizontalStrut(15));
    addPioButtons(pioSelection);
    pioSelection.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(pioSelection);

    final Box smSelection = new Box(BoxLayout.LINE_AXIS);
    add(smSelection);
    final JLabel lbSm = new JLabel("SM");
    lbSm.setMinimumSize(lbPio.getPreferredSize());
    lbSm.setPreferredSize(lbPio.getPreferredSize());
    smSelection.add(lbSm);
    smSelection.add(Box.createHorizontalStrut(15));
    addSmButtons(smSelection);
    smSelection.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(smSelection);

    final Box scrollSelectionLine = new Box(BoxLayout.LINE_AXIS);
    add(scrollSelectionLine);
    cbAutoScroll = new JCheckBox("Auto-scroll to front entry");
    cbAutoScroll.setMnemonic('a');
    cbAutoScroll.setToolTipText(autoScrollToolTipText);
    cbAutoScroll.setSelected(initialAutoScroll);
    cbAutoScroll.addActionListener((event) ->
                                   fifoEntriesViewPanel.
                                   setAutoScroll(cbAutoScroll.isSelected()));
    scrollSelectionLine.add(cbAutoScroll);
    scrollSelectionLine.add(Box.createHorizontalGlue());

    final Box fifoEntriesViewBox = new Box(BoxLayout.LINE_AXIS);
    add(fifoEntriesViewBox);
    fifoEntriesViewBox.add(fifoEntriesViewPanel);
    fifoEntriesViewBox.add(Box.createHorizontalGlue());
    fifoEntriesViewPanel.smChanged(pioNum, smNum);
  }

  private void addPioButtons(final Box pioSelection)
  {
    final ButtonGroup bgPio = new ButtonGroup();
    for (int pioNum = 0; pioNum < Constants.PIO_NUM; pioNum++) {
      if (pioNum != 0) pioSelection.add(Box.createHorizontalStrut(10));
      final JRadioButton rbPio = new JRadioButton(String.valueOf(pioNum));
      rbPio.setSelected(pioNum == 0);
      final int finalPioNum = pioNum;
      rbPio.addActionListener((event) -> {
          this.pioNum = finalPioNum;
          fifoEntriesViewPanel.smChanged(finalPioNum, smNum);
        });
      bgPio.add(rbPio);
      pioSelection.add(rbPio);
    }
  }

  private void addSmButtons(final Box smSelection)
  {
    final ButtonGroup bgSm = new ButtonGroup();
    for (int smNum = 0; smNum < Constants.SM_COUNT; smNum++) {
      if (smNum != 0) smSelection.add(Box.createHorizontalStrut(10));
      final JRadioButton rbSm = new JRadioButton(String.valueOf(smNum));
      rbSm.setSelected(smNum == 0);
      rbSm.setMnemonic(KeyEvent.VK_0 + smNum);
      final int finalSmNum = smNum;
      rbSm.addActionListener((event) -> {
          this.smNum = finalSmNum;
          fifoEntriesViewPanel.smChanged(pioNum, finalSmNum);
        });
      bgSm.add(rbSm);
      smSelection.add(rbSm);
    }
  }

  public void updateView()
  {
    fifoEntriesViewPanel.smChanged(pioNum, smNum);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
