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

import java.util.Enumeration;
import java.util.Objects;
import javax.swing.AbstractButton;
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

public class InstructionOptionsPanel extends JPanel
  implements org.soundpaint.rp2040pio.observer.diagram.Constants
{
  private static final long serialVersionUID = -4932796542558706478L;

  private final Diagram diagram;
  private final SDK sdk;
  private final JLabel lbPio;
  private final ButtonGroup pioButtons;
  private final JCheckBox cbUseSourcePio;
  private final JLabel lbSm;
  private final ButtonGroup smButtons;
  private final JCheckBox cbUseSourceSm;
  private int selectedPio;
  private int selectedSm;

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
              createTitledBorder("Select Target State Machine"));
    lbPio = new JLabel("PIO");
    cbUseSourcePio = new JCheckBox("Prefer PIO of source value, if available");
    pioButtons = new ButtonGroup();
    addPioSelection();
    lbSm = new JLabel("SM");
    cbUseSourceSm = new JCheckBox("Prefer SM of source value, if available");
    smButtons = new ButtonGroup();
    addSmSelection();
    SwingUtils.setPreferredHeightAsMaximum(this);
  }

  private void addPioSelection()
  {
    final JPanel pioLine = new JPanel();
    pioLine.setLayout(new BoxLayout(pioLine, BoxLayout.LINE_AXIS));
    lbPio.setPreferredSize(PREFERRED_LABEL_SIZE);
    pioLine.add(lbPio);
    for (int pioNum = 0; pioNum < Constants.PIO_NUM; pioNum++) {
      final JRadioButton rbPio = new JRadioButton("PIO" + pioNum, pioNum == 0);
      pioButtons.add(rbPio);
      final int pio = pioNum;
      rbPio.addActionListener((action) -> selectedPio = pio);
      pioLine.add(Box.createHorizontalStrut(20));
      pioLine.add(rbPio);
    }
    pioLine.add(Box.createHorizontalGlue());
    selectedPio = 0;
    SwingUtils.setPreferredHeightAsMaximum(pioLine);
    add(pioLine);
    final JPanel sourcePioLine = new JPanel();
    sourcePioLine.setLayout(new BoxLayout(sourcePioLine, BoxLayout.LINE_AXIS));
    sourcePioLine.add(Box.createHorizontalStrut(PREFERRED_LABEL_SIZE.width));
    sourcePioLine.add(Box.createHorizontalStrut(20));
    sourcePioLine.add(cbUseSourcePio);
    sourcePioLine.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(sourcePioLine);
    add(sourcePioLine);
  }

  private void addSmSelection()
  {
    final JPanel smLine = new JPanel();
    smLine.setLayout(new BoxLayout(smLine, BoxLayout.LINE_AXIS));
    lbSm.setPreferredSize(PREFERRED_LABEL_SIZE);
    smLine.add(lbSm);
    for (int smNum = 0; smNum < Constants.SM_COUNT; smNum++) {
      final JRadioButton rbSm = new JRadioButton("SM" + smNum, smNum == 0);
      smButtons.add(rbSm);
      final int sm = smNum;
      rbSm.addActionListener((action) -> selectedSm = sm);
      smLine.add(Box.createHorizontalStrut(20));
      smLine.add(rbSm);
    }
    smLine.add(Box.createHorizontalGlue());
    selectedPio = 0;
    SwingUtils.setPreferredHeightAsMaximum(smLine);
    add(smLine);
    final JPanel sourceSmLine = new JPanel();
    sourceSmLine.setLayout(new BoxLayout(sourceSmLine, BoxLayout.LINE_AXIS));
    sourceSmLine.add(Box.createHorizontalStrut(PREFERRED_LABEL_SIZE.width));
    sourceSmLine.add(Box.createHorizontalStrut(20));
    sourceSmLine.add(cbUseSourceSm);
    sourceSmLine.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(sourceSmLine);
    add(sourceSmLine);
  }

  public int getPioNum(final int sourcePioNum)
  {
    final boolean useSourcePio = cbUseSourcePio.isSelected();
    return useSourcePio && sourcePioNum >= 0 ? sourcePioNum : selectedPio;
  }

  public int getSmNum(final int sourceSmNum)
  {
    final boolean useSourceSm = cbUseSourceSm.isSelected();
    return useSourceSm && sourceSmNum >= 0 ? sourceSmNum : selectedSm;
  }

  private void setButtonsEnabled(final ButtonGroup buttons,
                                 final boolean enabled)
  {
    final Enumeration<AbstractButton> elements = buttons.getElements();
    while (elements.hasMoreElements()) {
      elements.nextElement().setEnabled(enabled);
    }
  }

  @Override
  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    lbPio.setEnabled(enabled);
    setButtonsEnabled(pioButtons, enabled);
    cbUseSourcePio.setEnabled(enabled);
    lbSm.setEnabled(enabled);
    setButtonsEnabled(smButtons, enabled);
    cbUseSourceSm.setEnabled(enabled);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
