/*
 * @(#)AbstractSignal.java 1.00 21/02/12
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractSignal<T> implements Signal
{
  private static class SignalRecord<T>
  {
    private T value;
    private int notChangedSince;

    private SignalRecord()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private SignalRecord(final T value, final int notChangedSince)
    {
      this.value = value;
      this.notChangedSince = notChangedSince;
    }

    @Override
    public String toString()
    {
      return String.format("SignalRecord(value=%s,notChangedSince=%d",
                           value, notChangedSince);
    }
  }

  private final List<SignalRecord<T>> signalRecords;
  private final SignalRendering.SignalParams signalParams;
  private boolean visible;

  private AbstractSignal()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public AbstractSignal(final SignalRendering.SignalParams signalParams)
  {
    Objects.requireNonNull(signalParams);
    this.signalRecords = new ArrayList<SignalRecord<T>>();
    this.signalParams = signalParams;
    visible = false;
  }

  public SignalRendering.SignalParams getSignalParams()
  {
    return signalParams;
  }

  @Override
  public void reset() {
    signalRecords.clear();
    // keep visibility unmodified
  }

  @Override
  public String getLabel() { return signalParams.getLabel(); }

  protected void record(final T value, final boolean enforceChanged)
  {
    final int size = signalRecords.size();
    final T previousValue =
      size > 0 ? signalRecords.get(size - 1).value : null;
    final int previousNotChangedSince =
      size > 0 ? signalRecords.get(size - 1).notChangedSince : 0;
    final int notChangedSince =
      enforceChanged ||
      ((value == null) && (previousValue != null)) ||
      ((value != null) && !value.equals(previousValue)) ?
      0 : previousNotChangedSince + 1;
    final SignalRecord<T> signalRecord =
      new SignalRecord<T>(value, notChangedSince);
    signalRecords.add(signalRecord);
  }

  @Override
  public int size()
  {
    return signalRecords.size();
  }

  public boolean next(final int cycle)
  {
    return cycle < signalRecords.size() - 1;
  }

  public T getValue(final int index)
  {
    return
      index < 0 ?
      null :
      (index >= size() ? null : signalRecords.get(index).value);
  }

  @Override
  public int getNotChangedSince(final int cycle)
  {
    return
      cycle >= 0 ? signalRecords.get(cycle).notChangedSince : 0;
  }

  public boolean changed(final int cycle)
  {
    return getNotChangedSince(cycle) == 0;
  }

  @Override
  public String getToolTipText(final int cycle)
  {
    return null;
  }

  abstract protected double getSignalHeight();

  @Override
  public void createToolTip(final List<ToolTip> toolTips,
                            final int cycle,
                            final boolean isFirstCycle,
                            final boolean isLastCycle,
                            final double zoom,
                            final double xStart,
                            final double yBottom)
  {
    final String previousToolTipText = getToolTipText(cycle - 1);
    if (previousToolTipText != null) {
      final int previousCycles = getNotChangedSince(cycle - 1) + 1;
      toolTips.add(new ToolTip((int)(xStart - previousCycles * zoom),
                               (int)(yBottom - getSignalHeight()),
                               (int)xStart - 1, (int)yBottom,
                               previousToolTipText));
    }
    if (isLastCycle) {
      // print label as preview for not yet finished value
      final String toolTipText = getToolTipText(cycle);
      if (toolTipText != null) {
        final int cycles = getNotChangedSince(cycle) - 1;
        toolTips.add(new ToolTip((int)(xStart - cycles * zoom),
                                 (int)(yBottom - getSignalHeight()),
                                 (int)xStart - 1, (int)yBottom,
                                 toolTipText));
      }
    }
  }

  @Override
  public void setVisible(final boolean visible)
  {
    this.visible = visible;
  }

  @Override
  public boolean getVisible()
  {
    return visible;
  }

  @Override
  public String toString()
  {
    final StringBuffer values = new StringBuffer();
    for (final SignalRecord<T> record : signalRecords) {
      if (values.length() > 0) values.append(", ");
      values.append(record.value);
    }
    return String.format("Signal[label=%s, values={%s}]", getLabel(), values);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
