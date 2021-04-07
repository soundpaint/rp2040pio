/*
 * @(#)DiagramConfig.java 1.00 21/02/12
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
package org.soundpaint.rp2040pio.diagram;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.Bit;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Configuration of a timing diagram.
 */
public class DiagramConfig implements Constants, Iterable<DiagramConfig.Signal>
{
  public static interface Signal
  {
    void reset();
    String getLabel();
    boolean isClock();
    boolean isBinary();
    boolean isValued();
    int notChangedSince();
    String getRenderedValue();
    String getPreviousRenderedValue();
    String getToolTipText();
    String getPreviousToolTipText();
    void record();
    void rewind();
    void setVisible(final boolean visible);
    boolean getVisible();
  }

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

  private abstract static class AbstractSignal<T> implements Signal
  {
    private List<SignalRecord<T>> signalRecords;
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
      visible = true;
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

    public void rewind()
    {
      replayIndex = 0;
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
      signalRecords.
        add(new SignalRecord<T>(previousValue, value, notChangedSince));
    }

    public boolean update()
    {
      if (replayIndex >= signalRecords.size()) return false;
      final SignalRecord<T> signalRecord = signalRecords.get(replayIndex++);
      this.previousValue = signalRecord.previousValue;
      this.value = signalRecord.value;
      this.notChangedSince = signalRecord.notChangedSince;
      return true;
    }

    @Override
    public int notChangedSince() { return notChangedSince; }

    public boolean changed() { return notChangedSince() == 0; }

    /**
     * Setting the renderer to &lt;code&gt;null&lt;/code&gt; results
     * in reverting to the default behavior of calling method
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
     * results in reverting to the default behavior of not providing
     * any tooltip text.
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

  public static class ClockSignal extends AbstractSignal<Void>
  {
    public ClockSignal() throws IOException
    {
      this("clock");
    }

    public ClockSignal(final String label) throws IOException
    {
      super(label);
    }

    @Override
    public void reset() {}

    @Override
    public void record() {}

    @Override
    public boolean isClock() { return true; }

    @Override
    public int notChangedSince() { return 0; }

    @Override
    protected String renderValue(final Void value) { return null; }

    @Override
    protected String toolTipTextForValue(final Void value) { return null; }
  }

  public static class ValuedSignal<T> extends AbstractSignal<T>
  {
    private Supplier<T> valueGetter;
    private Supplier<Boolean> changeInfoGetter;

    public ValuedSignal(final String label,
                        final Supplier<T> valueGetter)
    {
      this(label, valueGetter, null);
    }

    /**
     * @param changeInfoGetter If set to
     * &lt;code&gt;null&lt;/code&gt;, then a change is assumed only
     * when the updated value changes.
     */
    public ValuedSignal(final String label,
                        final Supplier<T> valueGetter,
                        final Supplier<Boolean> changeInfoGetter)
    {
      super(label);
      if (valueGetter == null) {
        throw new NullPointerException("valueGetter");
      }
      this.valueGetter = valueGetter;
      this.changeInfoGetter = changeInfoGetter;
    }

    @Override
    public void record()
    {
      final boolean enforceChanged =
        changeInfoGetter != null ? changeInfoGetter.get() : false;
      record(valueGetter.get(), enforceChanged);
    }
  }

  public static class BitSignal extends ValuedSignal<Bit>
  {
    public BitSignal(final String label,
                     final Supplier<Bit> valueGetter)
    {
      super(label, valueGetter);
    }

    public BitSignal(final String label,
                     final Supplier<Bit> valueGetter,
                     final Supplier<Boolean> changeInfoGetter)
    {
      super(label, valueGetter, changeInfoGetter);
    }

    @Override
    public boolean isBinary() { return true; }

    public boolean asBoolean()
    {
      return getValue() == Bit.HIGH;
    }

    public boolean previousAsBoolean()
    {
      return getPreviousValue() == Bit.HIGH;
    }
  }

  public static ClockSignal createClockSignal(final String label)
    throws IOException
  {
    return new ClockSignal(label);
  }

  public static ValuedSignal<PIOSDK.InstructionInfo>
    createInstructionSignal(final SDK sdk,
                            final PIOSDK pioSdk,
                            final int address, final int smNum,
                            final String label,
                            final boolean showAddress,
                            final Supplier<Boolean> displayFilter)
    throws IOException
  {
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    if (pioSdk == null) {
      throw new NullPointerException("pioSdk");
    }
    Constants.checkSmNum(smNum);
    final String signalLabel = createSignalLabel(sdk, label, address, 31, 0);
    final Supplier<PIOSDK.InstructionInfo> valueGetter = () -> {
      if ((displayFilter != null) && (!displayFilter.get())) return null;
      try {
        return pioSdk.getCurrentInstruction(smNum, showAddress, false);
      } catch (final IOException e) {
        return new PIOSDK.InstructionInfo(e);
      }
    };
    final ValuedSignal<PIOSDK.InstructionInfo> instructionSignal =
      new ValuedSignal<PIOSDK.InstructionInfo>(signalLabel, valueGetter);
    instructionSignal.setRenderer((instructionInfo) ->
                                  instructionInfo.toString());
    instructionSignal.setToolTipTexter((instructionInfo) ->
                                       instructionInfo.getToolTipText());
    return instructionSignal;
  }

  private static String createSignalLabel(final SDK sdk, final String label,
                                          final int address, final int bit)
    throws IOException
  {
    return
      (label != null) ? label : sdk.getLabelForAddress(address) + "_" + bit;
  }

  private static String createSignalLabel(final SDK sdk, final String label,
                                          final int address,
                                          final int msb, final int lsb)
    throws IOException
  {
    if (label != null) return label;
    return
      sdk.getLabelForAddress(address) +
      ((lsb == 0) && (msb == 31) ? "" :
       ("_" + msb +
        (lsb != msb ? ":" + lsb : "")));
  }

  public static BitSignal createFromRegister(final SDK sdk, final String label,
                                             final int address, final int bit)
    throws IOException
  {
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    Constants.checkBit(bit);
    final String signalLabel = createSignalLabel(sdk, label, address, bit);
    final Supplier<Bit> supplier = () -> {
      try {
        return Bit.fromValue(sdk.readAddress(address, bit, bit));
      } catch (final IOException e) {
        // TODO: console.println(e.getMessage());
        return null;
      }
    };
    return new BitSignal(signalLabel, supplier);
  }

  public static ValuedSignal<Integer>
    createFromRegister(final SDK sdk, final String label,
                       final int address)
    throws IOException
  {
    return createFromRegister(sdk, label, address, 31, 0);
  }

  public static ValuedSignal<Integer>
    createFromRegister(final SDK sdk, final String label,
                       final int address, final int msb, final int lsb)
    throws IOException
  {
    return createFromRegister(sdk, label, address, msb, lsb, null);
  }

  public static ValuedSignal<Integer>
    createFromRegister(final SDK sdk, final String label,
                       final int address, final int msb, final int lsb,
                       final Supplier<Boolean> displayFilter)
    throws IOException
  {
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    Constants.checkMSBLSB(msb, lsb);
    final String signalLabel = createSignalLabel(sdk, label, address, msb, lsb);
    final Supplier<Integer> supplier = () -> {
      if ((displayFilter != null) && (!displayFilter.get()))
        return null;
      try {
        return sdk.readAddress(address, msb, lsb);
      } catch (final IOException e) {
        // TODO: console.println(e.getMessage());
        return null;
      }
    };
    return new ValuedSignal<Integer>(signalLabel, supplier);
  }

  private final List<Signal> signals;

  public DiagramConfig()
  {
    signals = new ArrayList<Signal>();
  }

  public void addSignal(final Signal signal)
  {
    if (signal == null) {
      throw new NullPointerException("signal");
    }
    signals.add(signal);
  }

  public void clear()
  {
    signals.clear();
  }

  public Iterator<Signal> iterator()
  {
    return signals.iterator();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
