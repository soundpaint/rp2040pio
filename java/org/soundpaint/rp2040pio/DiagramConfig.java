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
package org.soundpaint.rp2040pio;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Configuration of a timing diagram.
 */
public class DiagramConfig implements Constants, Iterable<DiagramConfig.Signal>
{
  /**
   * Holds a copy of all info of a specific Instruction during a
   * specific cycle that is relevant for the timing diagram.
   */
  public static class InstructionInfo
  {
    private final String mnemonic;
    private final String fullStatement;
    private final boolean isDelayCycle;
    private final int delay;

    private InstructionInfo()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    public InstructionInfo(final SM sm, final boolean hideDelayCycle,
                           final boolean showAddress)
    {
      // instruction & state machine will change, hence save snapshot
      // of relevant info
      final Instruction instruction = sm.getCurrentInstruction();
      if (instruction == null) {
        throw new NullPointerException("instruction");
      }
      final String addressLabel = showAddress ? sm.getPC() + ": " : "";
      mnemonic = instruction.getMnemonic().toUpperCase();
      fullStatement =
        addressLabel + instruction.toString().replaceAll("\\s{2,}", " ");
      isDelayCycle = !hideDelayCycle && sm.isDelayCycle();
      delay = instruction.getDelay();
    }

    @Override
    public boolean equals(final Object obj)
    {
      if (!(obj instanceof InstructionInfo)) return false;
      final InstructionInfo other = (InstructionInfo)obj;
      if (isDelayCycle && other.isDelayCycle) return true;
      return this == other;
    }

    @Override
    public int hashCode()
    {
      return isDelayCycle ? 0 : super.hashCode();
    }

    public String getToolTipText()
    {
      return isDelayCycle ? "[delay]" : fullStatement;
    }

    @Override
    public String toString()
    {
      return isDelayCycle ? "[" + delay + "]" : mnemonic;
    }
  }

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
  }

  private abstract static class AbstractSignal<T> implements Signal
  {
    private final String label;
    private T previousValue;
    private T value;
    private int notChangedSince;
    private Function<T, String> renderer;
    private Function<T, String> toolTipTexter;

    private AbstractSignal()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    public AbstractSignal(final String label)
    {
      if (label == null) {
        throw new NullPointerException("label");
      }
      this.label = label;
      this.renderer = null;
      this.toolTipTexter = null;
      reset();
    }

    @Override
    public void reset() {
      previousValue = null;
      value = null;
      notChangedSince = 0;
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

    protected void update(final T value, final boolean enforceChanged)
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
  }

  public static class ClockSignal extends AbstractSignal<Void>
  {
    public ClockSignal()
    {
      this("clock");
    }

    public ClockSignal(final String label)
    {
      super(label);
    }

    @Override
    public void reset() {}

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

    public void update()
    {
      final boolean enforceChanged =
        changeInfoGetter != null ? changeInfoGetter.get() : false;
      update(valueGetter.get(), enforceChanged);
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

  public static DiagramConfig.ClockSignal createClockSignal(final String label)
  {
    return new DiagramConfig.ClockSignal(label);
  }

  public static ValuedSignal<String>
    createPCStateSignal(final String label, final PIO pio, final int smNum,
                        final boolean hideDelayCycles)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("smNum < 0: " + smNum);
    }
    if (smNum > SM_COUNT - 1) {
      throw new IllegalArgumentException("smNum > " +
                                         (SM_COUNT - 1) + ": " +
                                         smNum);
    }
    final SM sm = pio.getSM(smNum);
    final String signalLabel = label != null ? label : "SM" + smNum + "_PC";
    final Supplier<String> valueGetter = () -> {
      if (hideDelayCycles && sm.isDelayCycle())
        return null;
      else
        return String.format("%02x", sm.getPC());
    };
    return new DiagramConfig.ValuedSignal<String>(signalLabel, valueGetter);
  }

  public static ValuedSignal<InstructionInfo>
    createInstructionSignal(final String label, final PIO pio, final int smNum,
                            final boolean hideDelayCycles,
                            final boolean showAddress)
  {
    if (smNum < 0) {
      throw new IllegalArgumentException("smNum < 0: " + smNum);
    }
    if (smNum > SM_COUNT - 1) {
      throw new IllegalArgumentException("smNum > " +
                                         (SM_COUNT - 1) + ": " +
                                         smNum);
    }
    final String signalLabel = label != null ? label : "SM" + smNum + "_INSTR";
    final Supplier<InstructionInfo> valueGetter =
      () -> new InstructionInfo(pio.getSM(smNum), hideDelayCycles, showAddress);
    final Supplier<Boolean> changeInfoGetter =
      () -> !pio.getSM(smNum).isStalled() && !pio.getSM(smNum).isDelayCycle();
    final ValuedSignal<InstructionInfo> instructionSignal =
      new ValuedSignal<InstructionInfo>(signalLabel,
                                        valueGetter/*, changeInfoGetter*/);
    instructionSignal.setRenderer((instructionInfo) ->
                                  instructionInfo.toString());
    instructionSignal.setToolTipTexter((instructionInfo) ->
                                       instructionInfo.getToolTipText());
    return instructionSignal;
  }

  public static ValuedSignal<Bit>
    createGPIOValueSignal(final String label, final GPIO gpio, final int pin)
  {
    if (pin < 0) {
      throw new IllegalArgumentException("pin < 0: " + pin);
    }
    if (pin > GPIO_NUM - 1) {
      throw new IllegalArgumentException("pin > " +
                                         (GPIO_NUM - 1) + ": " + pin);
    }
    final String signalLabel = label != null ? label : "GPIO " + pin;
    return
      new DiagramConfig.ValuedSignal<Bit>(signalLabel, () -> gpio.getBit(pin));
  }

  public static BitSignal
    createGPIOBitSignal(final String label, final GPIO gpio, final int pin)
  {
    if (pin < 0) {
      throw new IllegalArgumentException("pin < 0: " + pin);
    }
    if (pin > GPIO_NUM - 1) {
      throw new IllegalArgumentException("pin > " +
                                         (GPIO_NUM - 1) + ": " + pin);
    }
    final String signalLabel = label != null ? label : "GPIO " + pin;
    return
      new DiagramConfig.BitSignal(signalLabel, () -> gpio.getBit(pin));
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
