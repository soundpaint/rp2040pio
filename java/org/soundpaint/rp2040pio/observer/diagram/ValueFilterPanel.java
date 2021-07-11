/*
 * @(#)ValueFilterPanel.java 1.00 21/07/11
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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class ValueFilterPanel extends JPanel
{
  private static final long serialVersionUID = -5391629852386365778L;

  private final Diagram diagram;
  private final SDK sdk;
  private final JCheckBox cbDelayFilter;
  private final JCheckBox cbUseSourcePio;
  private final ButtonGroup pioButtons;
  private final JCheckBox cbUseSourceSm;
  private final ButtonGroup smButtons;
  private int selectedPio;
  private int selectedSm;

  private ValueFilterPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public ValueFilterPanel(final Diagram diagram, final SDK sdk)
  {
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(sdk);
    this.diagram = diagram;
    this.sdk = sdk;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    cbDelayFilter = new JCheckBox("Accept only non-delay cycles of the " +
                                  "following state machine:");
    cbDelayFilter.addChangeListener((event) -> updateEnableForDelayFilter());
    addDelayFilter();
    cbUseSourcePio = new JCheckBox("Use PIO of source value, if available");
    pioButtons = new ButtonGroup();
    addPioSelection();
    cbUseSourceSm = new JCheckBox("Use SM of source value, if available");
    smButtons = new ButtonGroup();
    addSmSelection();
  }

  private void addDelayFilter()
  {
    final JPanel line = new JPanel();
    line.setLayout(new BoxLayout(line, BoxLayout.LINE_AXIS));
    line.add(cbDelayFilter);
    line.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(line);
    add(line);
  }

  private void addPioSelection()
  {
    final JPanel pioLine = new JPanel();
    pioLine.setLayout(new BoxLayout(pioLine, BoxLayout.LINE_AXIS));
    for (int pioNum = 0; pioNum < Constants.PIO_NUM; pioNum++) {
      final JRadioButton rbPio = new JRadioButton("PIO" + pioNum, pioNum == 0);
      pioButtons.add(rbPio);
      final int pio = pioNum;
      rbPio.addActionListener((action) -> selectedPio = pio);
      pioLine.add(Box.createHorizontalStrut(20));
      pioLine.add(rbPio);
    }
    selectedPio = 0;
    SwingUtils.setPreferredHeightAsMaximum(pioLine);
    add(pioLine);
    final JPanel sourcePioLine = new JPanel();
    sourcePioLine.setLayout(new BoxLayout(sourcePioLine, BoxLayout.LINE_AXIS));
    sourcePioLine.add(Box.createHorizontalStrut(20));
    sourcePioLine.add(cbUseSourcePio);
    SwingUtils.setPreferredHeightAsMaximum(sourcePioLine);
    add(sourcePioLine);
  }

  private void addSmSelection()
  {
    final JPanel smLine = new JPanel();
    smLine.setLayout(new BoxLayout(smLine, BoxLayout.LINE_AXIS));
    for (int smNum = 0; smNum < Constants.SM_COUNT; smNum++) {
      final JRadioButton rbSm = new JRadioButton("SM" + smNum, smNum == 0);
      smButtons.add(rbSm);
      final int sm = smNum;
      rbSm.addActionListener((action) -> selectedSm = sm);
      smLine.add(Box.createHorizontalStrut(20));
      smLine.add(rbSm);
    }
    selectedPio = 0;
    SwingUtils.setPreferredHeightAsMaximum(smLine);
    add(smLine);
    final JPanel sourceSmLine = new JPanel();
    sourceSmLine.setLayout(new BoxLayout(sourceSmLine, BoxLayout.LINE_AXIS));
    sourceSmLine.add(Box.createHorizontalStrut(20));
    sourceSmLine.add(cbUseSourceSm);
    SwingUtils.setPreferredHeightAsMaximum(sourceSmLine);
    add(sourceSmLine);
  }

  private void updateEnableForDelayFilter()
  {
    final boolean enabled = isEnabled() && cbDelayFilter.isSelected();
    setButtonsEnabled(pioButtons, enabled);
    cbUseSourcePio.setEnabled(enabled);
    setButtonsEnabled(smButtons, enabled);
    cbUseSourceSm.setEnabled(enabled);
  }

  public Supplier<Boolean> createFilter(final int sourcePioNum,
                                        final int sourceSmNum)
  {
    if (!cbDelayFilter.isSelected()) {
      return null;
    }
    final int pioNum =
      (cbUseSourcePio.isSelected() && sourcePioNum >= 0) ?
      sourcePioNum : selectedPio;
    final int smNum =
      (cbUseSourceSm.isSelected() && sourceSmNum >= 0) ?
      sourceSmNum : selectedSm;
    return createDelayFilter(sdk, pioNum, smNum);
  }

  public static Supplier<Boolean> createDelayFilter(final SDK sdk,
                                                    final int pioNum,
                                                    final int smNum)
  {
    final Supplier<Boolean> displayFilter = () -> {
      final int smDelayCycleAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_DELAY_CYCLE);
      try {
        final boolean isDelayCycle =
          sdk.readAddress(smDelayCycleAddress) != 0x0;
        return !isDelayCycle;
      } catch (final IOException e) {
        // TODO: Maybe log warning that we failed to evaluate delay?
        return false;
      }
    };
    return displayFilter;
  }

  public int getPioNum()
  {
    return selectedPio;
  }

  private void setButtonsEnabled(final ButtonGroup buttons,
                                 final boolean enabled)
  {
    final Enumeration<AbstractButton> elements = buttons.getElements();
    while (elements.hasMoreElements()) {
      elements.nextElement().setEnabled(enabled);
    }
  }

  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    cbDelayFilter.setEnabled(enabled);
    updateEnableForDelayFilter();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
