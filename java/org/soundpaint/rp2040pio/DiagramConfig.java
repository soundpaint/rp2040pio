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
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Configuration of a timing diagram.
 */
public class DiagramConfig implements Constants, Iterable<DiagramConfig.Signal>
{
  /**
   * Holds a copy of all info of a specific Instruction during a
   * specific cycle that is relevant for the timing diagram.
   */
  private static class InstructionInfo
  {
    private final String mnemonic;
    private final String fullStatement;
    private final boolean isDelayCycle;
    private final int delay;

    private InstructionInfo()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    public InstructionInfo(final String mnemonic, final String fullStatement,
                           final boolean isDelayCycle, final int delay)
    {
      // instruction & state machine will change, hence save snapshot
      // of relevant info
      this.mnemonic = mnemonic;
      this.fullStatement = fullStatement;
      this.isDelayCycle = isDelayCycle;
      this.delay = delay;
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

  private static final String[] MNEMONIC =
  {"jmp", "wait", "in", "out", "push", "mov", "irq", "set"};

  public static ValuedSignal<InstructionInfo>
    createInstructionSignal(final SDK sdk,
                            final PIOSDK pioSdk,
                            final int address, final int smNum,
                            final String label,
                            final boolean showAddress,
                            final Supplier<Boolean> displayFilter)
  {
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    if (pioSdk == null) {
      throw new NullPointerException("pioSdk");
    }
    Constants.checkSmNum(smNum);
    final Decoder decoder = new Decoder();
    final String signalLabel = createSignalLabel(sdk, label, address, 31, 0);
    final Supplier<InstructionInfo> valueGetter = () -> {
      if ((displayFilter != null) && (!displayFilter.get()))
        return null;
      final PIORegisters pioRegisters = pioSdk.getRegisters();
      final PIOEmuRegisters pioEmuRegisters = pioSdk.getEmuRegisters();

      final int smInstrAddress =
      pioRegisters.getSMAddress(PIORegisters.Regs.SM0_INSTR, smNum);
      final int opCode = pioRegisters.readAddress(smInstrAddress) & 0xffff;

      final int smPCAddress =
      pioEmuRegisters.getSMAddress(PIOEmuRegisters.Regs.SM0_PC, smNum);
      final int pc = pioEmuRegisters.readAddress(smPCAddress);
      final String addressLabel =
      showAddress ? String.format("%02x:", pc) : "";

      final int smDelayAddress =
      pioEmuRegisters.getSMAddress(PIOEmuRegisters.Regs.SM0_DELAY, smNum);
      final int delay = pioEmuRegisters.readAddress(smDelayAddress);

      final int smDelayCycleAddress =
      pioEmuRegisters.getSMAddress(PIOEmuRegisters.Regs.SM0_DELAY_CYCLE, smNum);
      final boolean isDelayCycle =
      pioEmuRegisters.readAddress(smDelayCycleAddress) != 0x0;

      final int smPinCtrlSidesetCountAddress =
      pioRegisters.getSMAddress(PIORegisters.Regs.SM0_PINCTRL, smNum);
      final int pinCtrlSidesetCount =
      (pioRegisters.readAddress(smPinCtrlSidesetCountAddress) &
       SM0_PINCTRL_SIDESET_COUNT_BITS) >>> SM0_PINCTRL_SIDESET_COUNT_LSB;

      final int smExecCtrlSideEnAddress =
      pioRegisters.getSMAddress(PIORegisters.Regs.SM0_EXECCTRL, smNum);
      final boolean execCtrlSideEn =
      (pioRegisters.readAddress(smExecCtrlSideEnAddress) &
       SM0_EXECCTRL_SIDE_EN_BITS) != 0x0;

      try {
        final Instruction instruction =
          decoder.decode((short)opCode, pinCtrlSidesetCount, execCtrlSideEn);
        final String mnemonic = instruction.getMnemonic();
        final String fullStatement =
          addressLabel + instruction.toString().replaceAll("\\s{2,}", " ");
        return new InstructionInfo(mnemonic, fullStatement, isDelayCycle, delay);
      } catch (final Decoder.DecodeException e) {
        // illegal op-code => nothing to show
        return null;
      }
    };
    final ValuedSignal<InstructionInfo> instructionSignal =
      new ValuedSignal<InstructionInfo>(signalLabel, valueGetter);
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

  private static String createSignalLabel(final SDK sdk, final String label,
                                          final int address, final int bit)
  {
    return
      (label != null) ? label : sdk.getLabelForAddress(address) + "_" + bit;
  }

  private static String createSignalLabel(final SDK sdk, final String label,
                                          final int address,
                                          final int msb, final int lsb)
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
  {
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    Constants.checkBit(bit);
    final String signalLabel = createSignalLabel(sdk, label, address, bit);
    final Supplier<Bit> supplier =
      () -> Bit.fromValue(sdk.readAddress(address, bit, bit));
    return new DiagramConfig.BitSignal(signalLabel, supplier);
  }

  public static ValuedSignal<Integer>
    createFromRegister(final SDK sdk, final String label,
                       final int address)
  {
    return createFromRegister(sdk, label, address, 31, 0);
  }

  public static ValuedSignal<Integer>
    createFromRegister(final SDK sdk, final String label,
                       final int address, final int msb, final int lsb)
  {
    return createFromRegister(sdk, label, address, msb, lsb, null);
  }

  public static ValuedSignal<Integer>
    createFromRegister(final SDK sdk, final String label,
                       final int address, final int msb, final int lsb,
                       final Supplier<Boolean> displayFilter)
  {
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    Constants.checkMSBLSB(msb, lsb);
    final String signalLabel = createSignalLabel(sdk, label, address, msb, lsb);
    final Supplier<Integer> supplier = () -> {
      if ((displayFilter != null) && (!displayFilter.get()))
        return null;
      return sdk.readAddress(address, msb, lsb);
    };
    return new DiagramConfig.ValuedSignal<Integer>(signalLabel, supplier);
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
