/*
 * @(#)InstructionOptionsPanel.java 1.00 21/07/10
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

import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class InstructionOptionsPanel extends JPanel implements Constants
{
  private static final long serialVersionUID = -4932796542558706478L;

  private final Diagram diagram;
  private final SDK sdk;
  private final JLabel lbPio;
  private final JLabel lbSm;
  private final JRadioButton rbPio0;
  private final JRadioButton rbPio1;
  private final JRadioButton rbSm0;
  private final JRadioButton rbSm1;
  private final JRadioButton rbSm2;
  private final JRadioButton rbSm3;
  private int selectedPioNum;
  private int selectedSmNum;

  private InstructionOptionsPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public InstructionOptionsPanel(final Diagram diagram, final SDK sdk)
  {
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(sdk);
    this.diagram = diagram;
    this.sdk = sdk;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.
              createTitledBorder("Mnemonic as for State Machine"));
    lbPio = new JLabel("PIO");
    lbPio.setPreferredSize(PREFERRED_LABEL_SIZE);
    rbPio0 = new JRadioButton("PIO0", true);
    rbPio1 = new JRadioButton("PIO1", false);
    addPioSelection();
    lbSm = new JLabel("State Machine");
    lbSm.setPreferredSize(PREFERRED_LABEL_SIZE);
    rbSm0 = new JRadioButton("SM0", true);
    rbSm1 = new JRadioButton("SM1", false);
    rbSm2 = new JRadioButton("SM2", false);
    rbSm3 = new JRadioButton("SM3", false);
    addSmSelection();
    SwingUtils.setPreferredHeightAsMaximum(this);
    pioSelected(0);
    smSelected(0);
  }

  private void addPioSelection()
  {
    final ButtonGroup pioButtons = new ButtonGroup();
    pioButtons.add(rbPio0);
    pioButtons.add(rbPio1);
    final JPanel pioSelection = new JPanel();
    pioSelection.setLayout(new BoxLayout(pioSelection, BoxLayout.LINE_AXIS));
    pioSelection.add(lbPio);
    pioSelection.add(Box.createHorizontalStrut(5));
    pioSelection.add(rbPio0);
    pioSelection.add(Box.createHorizontalStrut(5));
    pioSelection.add(rbPio1);
    rbPio0.addActionListener((action) -> pioSelected(0));
    rbPio1.addActionListener((action) -> pioSelected(1));
    pioSelection.add(Box.createHorizontalGlue());
    add(pioSelection);
  }

  private void addSmSelection()
  {
    final ButtonGroup smButtons = new ButtonGroup();
    smButtons.add(rbSm0);
    smButtons.add(rbSm1);
    smButtons.add(rbSm2);
    smButtons.add(rbSm3);
    final JPanel smSelection = new JPanel();
    smSelection.setLayout(new BoxLayout(smSelection, BoxLayout.LINE_AXIS));
    smSelection.add(lbSm);
    smSelection.add(Box.createHorizontalStrut(5));
    smSelection.add(rbSm0);
    smSelection.add(Box.createHorizontalStrut(5));
    smSelection.add(rbSm1);
    smSelection.add(Box.createHorizontalStrut(5));
    smSelection.add(rbSm2);
    smSelection.add(Box.createHorizontalStrut(5));
    smSelection.add(rbSm3);
    rbSm0.addActionListener((action) -> smSelected(0));
    rbSm1.addActionListener((action) -> smSelected(1));
    rbSm2.addActionListener((action) -> smSelected(2));
    rbSm3.addActionListener((action) -> smSelected(3));
    smSelection.add(Box.createHorizontalGlue());
    add(smSelection);
  }

  private void pioSelected(final int pioNum)
  {
    selectedPioNum = pioNum;
    // TODO
  }

  public int getSelectedPio() { return selectedPioNum; }

  private void smSelected(final int smNum)
  {
    selectedSmNum = smNum;
    // TODO
  }

  public int getSelectedSm() { return selectedSmNum; }

  @Override
  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    lbPio.setEnabled(enabled);
    rbPio0.setEnabled(enabled);
    rbPio1.setEnabled(enabled);
    lbSm.setEnabled(enabled);
    rbSm0.setEnabled(enabled);
    rbSm1.setEnabled(enabled);
    rbSm2.setEnabled(enabled);
    rbSm3.setEnabled(enabled);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
