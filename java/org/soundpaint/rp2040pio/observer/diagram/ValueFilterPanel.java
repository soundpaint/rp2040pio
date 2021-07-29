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
import javax.swing.BorderFactory;
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
    setBorder(BorderFactory.
              createTitledBorder("Gray Out Cycle As Undefined When"));
    cbNoDelayFilter = new JCheckBox("Cycle is a delay cycle on " +
                                    "below target state machine.");
    cbNoDelayFilter.
      addChangeListener((event) -> filterChangedListener.accept(null));
    addNoDelayFilter();
    cbClkEnabledFilter = new JCheckBox("CLK enable signal is false for " +
                                       "below target state machine.");
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

  /**
   * Filter returns true, if current signal value passes the filter's
   * condition(s) for value display, and false otherwise.
   */
  public List<SignalFilter> createFilters()
  {
    return createFilters(cbNoDelayFilter.isSelected(),
                         cbClkEnabledFilter.isSelected());
  }

  // TODO: Make private again when removing demo signals from Diagram class
  public static List<SignalFilter> createFilters(final boolean createNoDelay,
                                                 final boolean createClkEnabled)
  {
    final List<SignalFilter> filters = new ArrayList<SignalFilter>();
    if (createNoDelay) {
      filters.add(SignalFilter.NO_DELAY);
    }
    if (createClkEnabled) {
      filters.add(SignalFilter.CLK_ENABLED);
    }
    return filters;
  }

  @Override
  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    cbNoDelayFilter.setEnabled(enabled);
    cbClkEnabledFilter.setEnabled(enabled);
  }

  public void load(final ValuedSignal<?> signal)
  {
    final List<SignalFilter> displayFilters;
    if (signal != null) {
      final SignalRendering.SignalParams signalParams =
        signal.getSignalParams();
      displayFilters = signalParams.getDisplayFilters();
    } else {
      displayFilters = null;
    }
    if (displayFilters != null) {
      boolean selectNoDelay = false;
      boolean selectClkenabled = false;
      for (final SignalFilter filter : displayFilters) {
        selectNoDelay |= filter == SignalFilter.NO_DELAY;
        selectClkenabled |= filter == SignalFilter.CLK_ENABLED;
      }
      cbNoDelayFilter.setSelected(selectNoDelay);
      cbClkEnabledFilter.setSelected(selectClkenabled);
    } else {
      cbNoDelayFilter.setSelected(false);
      cbClkEnabledFilter.setSelected(false);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
