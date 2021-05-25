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

import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;

public abstract class AbstractSignal<T> implements Signal
{
  private static class SignalRecord<T>
  {
    private T previousValue;
    private T value;
    private int notChangedSince;

    private SignalRecord()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private SignalRecord(final T previousValue, final T value,
                         final int notChangedSince)
    {
      this.previousValue = previousValue;
      this.value = value;
      this.notChangedSince = notChangedSince;
    }

    @Override
    public String toString()
    {
      return String.format("SignalRecord(prev=%s,curr=%s,notChangedSince=%d",
                           previousValue, value, notChangedSince);
    }
  }

  private final List<SignalRecord<T>> signalRecords;
  private final String label;
  private T previousValue;
  private T value;
  private int notChangedSince;
  private Function<T, String> renderer;
  private Function<T, String> toolTipTexter;
  private int replayIndex;
  private boolean visible;

  private AbstractSignal()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public AbstractSignal(final String label)
  {
    if (label == null) {
      throw new NullPointerException("label");
    }
    this.signalRecords = new ArrayList<SignalRecord<T>>();
    this.label = label;
    this.renderer = null;
    this.toolTipTexter = null;
    visible = false;
    reset();
  }

  @Override
  public void reset() {
    previousValue = null;
    value = null;
    notChangedSince = 0;
    signalRecords.clear();
    // keep visibility unmodified
  }

  @Override
  public void rewind(final int index)
  {
    if (index < 0) {
      throw new IllegalArgumentException("index < 0: " + index);
    }
    if (index > signalRecords.size()) {
      final String message =
        String.format("index > size: %d > %d", index, signalRecords.size());
      throw new IllegalArgumentException(message);
    }
    replayIndex = index;
  }

  @Override
  public String getLabel() { return label; }

  public T getPreviousValue() { return previousValue; }

  public T getValue() { return value; }

  @Override
  public boolean isClock() { return false; }

  @Override
  public boolean isBinary() { return false; }

  @Override
  public boolean isValued()
  {
    return !isClock() && !isBinary();
  }

  protected void record(final T value, final boolean enforceChanged)
  {
    this.previousValue = this.value;
    this.value = value;
    if (enforceChanged ||
        ((value == null) && (previousValue != null)) ||
        ((value != null) && !value.equals(previousValue))) {
      notChangedSince = 0;
    } else {
      notChangedSince++;
    }
    final SignalRecord<T> signalRecord =
      new SignalRecord<T>(previousValue, value, notChangedSince);
    signalRecords.add(signalRecord);
  }

  @Override
  public int size()
  {
    return signalRecords.size();
  }

  public boolean update()
  {
    if (replayIndex >= signalRecords.size()) return false;
    final SignalRecord<T> signalRecord = signalRecords.get(replayIndex++);
    previousValue = signalRecord.previousValue;
    value = signalRecord.value;
    notChangedSince = signalRecord.notChangedSince;
    return true;
  }

  @Override
  public int notChangedSince() { return notChangedSince; }

  public boolean changed() { return notChangedSince() == 0; }

  /**
   * Setting the renderer to &lt;code&gt;null&lt;/code&gt; results in
   * reverting to the default behavior of calling method
   * String.valueOf(value) for rendering.
   */
  public void setRenderer(final Function<T, String> renderer)
  {
    this.renderer = renderer;
  }

  protected String renderValue(final T value)
  {
    if (value == null)
      return null;
    else if (renderer != null)
      return renderer.apply(value);
    else
      return String.valueOf(value);
  }

  @Override
  public String getRenderedValue()
  {
    return renderValue(value);
  }

  @Override
  public String getPreviousRenderedValue()
  {
    return renderValue(previousValue);
  }

  /**
   * Setting the tooltip texter to &lt;code&gt;null&lt;/code&gt;
   * results in reverting to the default behavior of not providing any
   * tooltip text.
   */
  public void setToolTipTexter(final Function<T, String> toolTipTexter)
  {
    this.toolTipTexter = toolTipTexter;
  }

  protected String toolTipTextForValue(final T value)
  {
    if (value == null)
      return null;
    else if (toolTipTexter != null)
      return toolTipTexter.apply(value);
    else
      return null;
  }

  @Override
  public String getToolTipText()
  {
    return toolTipTextForValue(value);
  }

  @Override
  public String getPreviousToolTipText()
  {
    return toolTipTextForValue(previousValue);
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
    return "Signal[" + label + "]";
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
