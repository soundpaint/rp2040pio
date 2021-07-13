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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class ValueFilterPanel extends JPanel
{
  private static final long serialVersionUID = -5391629852386365778L;

  private final Diagram diagram;
  private final SDK sdk;
  private final Consumer<Void> filterChangedListener;
  private final JCheckBox cbNoDelayFilter;
  private final JCheckBox cbClkEnabledFilter;

  private ValueFilterPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public ValueFilterPanel(final Diagram diagram, final SDK sdk,
                          final Consumer<Void> filterChangedListener)
  {
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(sdk);
    Objects.requireNonNull(filterChangedListener);
    this.diagram = diagram;
    this.sdk = sdk;
    this.filterChangedListener = filterChangedListener;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    cbNoDelayFilter = new JCheckBox("Accept only non-delay cycles of the " +
                                    "below target state machine.");
    cbNoDelayFilter.
      addChangeListener((event) -> filterChangedListener.accept(null));
    addNoDelayFilter();
    cbClkEnabledFilter = new JCheckBox("Accept only cycles with CLK signal " +
                                       "enabled for the below " +
                                       "target state machine.");
    cbClkEnabledFilter.
      addChangeListener((event) -> filterChangedListener.accept(null));
    addClkEnabledFilter();
  }

  private void addNoDelayFilter()
  {
    final JPanel line = new JPanel();
    line.setLayout(new BoxLayout(line, BoxLayout.LINE_AXIS));
    line.add(cbNoDelayFilter);
    line.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(line);
    add(line);
  }

  private void addClkEnabledFilter()
  {
    final JPanel line = new JPanel();
    line.setLayout(new BoxLayout(line, BoxLayout.LINE_AXIS));
    line.add(cbClkEnabledFilter);
    line.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(line);
    add(line);
  }

  public boolean isSmSelectionRelevant()
  {
    return
      isEnabled() &&
      (cbNoDelayFilter.isSelected() || cbClkEnabledFilter.isSelected());
  }

  public Supplier<Boolean> createFilter(final int pioNum, final int smNum)
  {
    return createFilter(sdk,
                        cbNoDelayFilter.isSelected(),
                        cbClkEnabledFilter.isSelected(),
                        pioNum, smNum);
  }

  private static Supplier<Boolean> createFilter(final SDK sdk,
                                                final boolean createNoDelay,
                                                final boolean createClkEnabled,
                                                final int pioNum,
                                                final int smNum)
  {
    final List<Supplier<Boolean>> suppliers =
      new ArrayList<Supplier<Boolean>>();
    if (createNoDelay) {
      suppliers.add(createNoDelayFilter(sdk, pioNum, smNum));
    }
    if (createClkEnabled) {
      suppliers.add(createClkEnabledFilter(sdk, pioNum, smNum));
    }
    return () -> suppliers.stream().allMatch(supplier -> supplier.get());
  }

  public static Supplier<Boolean> createNoDelayFilter(final SDK sdk,
                                                      final int pioNum,
                                                      final int smNum)
  {
    final Supplier<Boolean> filter = () -> {
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
    return filter;
  }

  public static Supplier<Boolean> createClkEnabledFilter(final SDK sdk,
                                                         final int pioNum,
                                                         final int smNum)
  {
    final Supplier<Boolean> filter = () -> {
      final int clkEnableAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_CLK_ENABLE);
      try {
        final int clkEnable = sdk.readAddress(clkEnableAddress) & 0x1;
        return clkEnable != 0x0;
      } catch (final IOException e) {
        // TODO: Maybe log warning that we failed to evaluate delay?
        return false;
      }
    };
    return filter;
  }

  @Override
  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    cbNoDelayFilter.setEnabled(enabled);
    cbClkEnabledFilter.setEnabled(enabled);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
