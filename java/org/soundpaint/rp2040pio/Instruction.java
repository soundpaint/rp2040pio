/*
 * @(#)Instruction.java 1.00 21/01/31
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Consumer;

/**
 * Instruction
 */
public abstract class Instruction
{
  private int delay;
  private int sideSet;
  private int sideSetCount;
  private boolean sideSetEnabled;
  private int opCode;

  public enum ResultState
  {
    /**
     * The instruction leaves it to the program control to ordinarily
     * update the program counter (PC) by increasing it by one and
     * perform any delay.
     */
    COMPLETE,
    /**
     * The PC must be kept unmodified (e.g. as the result of a wait
     * instruction keeping unfulfilled or an additional synthetic
     * instruction being inserted), and any delay must not yet be
     * performed.
     */
    STALL,
    /**
     * The instruction has modified the PC by itself.  Delay must be
     * ordinarily performed.
     */
    JUMP
  };

  public Instruction()
  {
    reset();
  }

  public int getDelay()
  {
    return delay;
  }

  private String getDelayDisplayValue()
  {
    return delay > 0 ? "[" + delay + "]" : "";
  }

  public int getOpCode()
  {
    return opCode;
  }

  private String getSideSetDisplayValue()
  {
    final boolean printSideSet = sideSetEnabled && sideSetCount > 0;
    return printSideSet ? "side " + Integer.toString(sideSet) : "";
  }

  public void reset()
  {
    delay = 0;
    sideSet = 0;
    sideSetCount = 0;
    sideSetEnabled = false;
    resetParams();
  }

  abstract protected void resetParams();

  public Instruction decode(final short opCode,
                            final int pinCtrlSidesetCount,
                            final boolean execCtrlSideEn)
    throws Decoder.DecodeException
  {
    final int delayAndSideSet = (opCode >>> 0x8) & 0x1f;
    final int delayMask = (0x1 << (5 - pinCtrlSidesetCount)) - 1;
    this.opCode = opCode;
    delay = delayAndSideSet & delayMask;
    final int delayBitCount = 5 - pinCtrlSidesetCount;
    final boolean haveSideSetEnableBit =
      execCtrlSideEn && (pinCtrlSidesetCount > 0);
    sideSetEnabled = !haveSideSetEnableBit || (delayAndSideSet & 0x10) != 0x0;
    sideSetCount = pinCtrlSidesetCount - (haveSideSetEnableBit ? 1 : 0);
    final int delayAndSideSetWithoutSideEn =
      execCtrlSideEn ? delayAndSideSet & 0xf : delayAndSideSet;
    sideSet = delayAndSideSetWithoutSideEn >>> delayBitCount;
    decodeLSB(opCode & 0xff);
    return this;
  }

  protected int getDelayAndSideSetBits(final int pinCtrlSidesetCount,
                                       final boolean execCtrlSideEn)
  {
    final int delayBitCount = 5 - pinCtrlSidesetCount;
    final boolean haveSideSetEnableBit =
      execCtrlSideEn && (pinCtrlSidesetCount > 0);
    final int delayAndSideSet =
      (haveSideSetEnableBit && (sideSet > 0) ? 0x10 : 0x00) |
      (sideSet << delayBitCount) |
      delay;
    return (delayAndSideSet & 0x1f) << 8;
  }

  /**
   * Updates all internal data of this instruction according to the
   * argument bits of the instruction word.
   */
  abstract void decodeLSB(final int lsb) throws Decoder.DecodeException;

  private void executeSideSet(final SM.Status smStatus)
  {
    final int pinCtrlSidesetBase = smStatus.regPINCTRL_SIDESET_BASE;
    final PIO.PinDir execCtrlSidePinDir = smStatus.regEXECCTRL_SIDE_PINDIR;
    if (sideSetCount > 0) {
      if (execCtrlSidePinDir == PIO.PinDir.GPIO_LEVELS) {
        smStatus.collatePins(sideSet, pinCtrlSidesetBase, sideSetCount, true);
      } else {
        smStatus.collatePinDirs(sideSet, pinCtrlSidesetBase, sideSetCount);
      }
    }
  }

  abstract ResultState executeOperation(final SM sm);

  public ResultState execute(final SM sm)
  {
    final ResultState resultState = executeOperation(sm);
    if (sideSetEnabled) executeSideSet(sm.getStatus());
    return resultState;
  }

  public abstract String getMnemonic();

  abstract String getParamsDisplay();

  protected void checkIRQIndex(final int irqIndex)
    throws Decoder.DecodeException
  {
    if ((irqIndex & 0x08) != 0) {
      throw new Decoder.DecodeException(this, getOpCode());
    }
  }

  protected static int getIRQNum(final int smNum, final int index)
  {
    final boolean isRel = (index & 0x10) != 0;
    return
      isRel ?
      (index & 0x4) | ((smNum + index) & 0x3) :
      index & 0x7;
  }

  protected static String getIRQNumDisplay(final int index)
  {
    return
      String.format("%1x%s", index & 0x7, (index & 0x10) != 0 ? "_rel" : "");
  }

  @Override
  public String toString()
  {
    final String mnemonic = getMnemonic();
    final String paramsDisplay = getParamsDisplay();
    final String sideSetDisplayValue = getSideSetDisplayValue();
    final String delayDisplayValue = getDelayDisplayValue();
    return
      String.format("%-16s%s",
                    mnemonic +
                    (!paramsDisplay.isEmpty() ? " " + paramsDisplay : ""),
                    sideSetDisplayValue +
                    (!sideSetDisplayValue.isEmpty() &&
                     !delayDisplayValue.isEmpty() ? " " : "") +
                    delayDisplayValue);
  }

  public static class Jmp extends Instruction
  {
    private static final Map<Integer, Condition> code2cond =
      new HashMap<Integer, Condition>();

    public enum Condition
    {
      ALWAYS(0b000, "", (smStatus) -> true),
      NOT_X(0b001, "!x", (smStatus) -> smStatus.regX == 0),
      DEC_X(0b010, "x--", (smStatus) -> smStatus.regX-- != 0),
      NOT_Y(0b011, "!y", (smStatus) -> smStatus.regY == 0),
      DEC_Y(0b100, "y--", (smStatus) -> smStatus.regY-- != 0),
      X_NEQ_Y(0b101, "x!=y", (smStatus) -> smStatus.regX != smStatus.regY),
      PIN(0b110, "pin", (smStatus) -> smStatus.jmpPin() == Bit.HIGH),
      NOT_OSRE(0b111, "!osre",
               (smStatus) -> !smStatus.isOsrCountBeyondThreshold());

      private final int code;
      private final String mnemonic;
      private final Function<SM.Status, Boolean> eval;

      private Condition(final int code, final String mnemonic,
                        final Function<SM.Status, Boolean> eval)
      {
        this.code = code;
        this.mnemonic = mnemonic;
        this.eval = eval;
        code2cond.put(code, this);
      }

      public boolean fulfilled(final SM.Status smStatus)
      {
        return eval.apply(smStatus);
      }

      @Override
      public String toString()
      {
        return mnemonic;
      }
    }

    private int address;
    private Condition condition;

    @Override
    protected void resetParams()
    {
      address = 0;
      condition = Condition.ALWAYS;
    }

    public void setCondition(final Condition condition)
    {
      if (condition == null) {
        throw new NullPointerException("condition");
      }
      this.condition = condition;
    }

    public void setAddress(final int address)
    {
      if (address < 0) {
        throw new IllegalArgumentException("address < 0: " + address);
      }
      if (address > 31) {
        throw new IllegalArgumentException("address > 31: " + address);
      }
      this.address = address;
    }

    public int encode(final int pinCtrlSidesetCount,
                      final boolean execCtrlSideEn)
    {
      return
        0x0000 |
        getDelayAndSideSetBits(pinCtrlSidesetCount, execCtrlSideEn) |
        (condition.ordinal() << 5) |
        (address & 0x1f);
    }

    @Override
    public void decodeLSB(final int lsb)
    {
      address = lsb & 0x1f;
      condition = code2cond.get((lsb >>> 5) & 0x7);
    }

    @Override
    public ResultState executeOperation(final SM sm)
    {
      final SM.Status smStatus = sm.getStatus();
      final boolean doJump = condition.fulfilled(smStatus);
      if (doJump) smStatus.regADDR = address;
      return doJump ? ResultState.JUMP : ResultState.COMPLETE;
    }

    @Override
    public String getMnemonic()
    {
      return "jmp";
    }

    @Override
    public String getParamsDisplay()
    {
      final String conditionDisplay = condition.toString();
      return
        (!conditionDisplay.isEmpty() ? conditionDisplay + ", " : "") +
        String.format("%02x", address);
    }
  }

  public static class Wait extends Instruction
  {
    private static final Map<Integer, Source> code2src =
      new HashMap<Integer, Source>();

    private enum Source
    {
      GPIO_(0b00, "gpio", (wait, sm) ->
            sm.getPIOGPIO().getGPIO().getInToPeri(wait.index)),
      PIN(0b01, "pin", (wait, sm) -> {
          final int gpioNum =
            (wait.index + sm.getStatus().regPINCTRL_IN_BASE) &
            (Constants.GPIO_NUM - 1);
          return sm.getPIOGPIO().getGPIO().getInToPeri(gpioNum);
        }),
      IRQ(0b10, "irq", (wait, sm) -> {
          final int irqNum = getIRQNum(sm.getNum(), wait.index);
          final Bit bit = sm.getIRQ(irqNum);
          if ((wait.polarity == Bit.HIGH) && (bit == wait.polarity))
            sm.clearIRQ(irqNum);
          return bit;
        }),
      RESERVED_3(0b11, "???", null);

      private final int code;
      private final String mnemonic;
      private final BiFunction<Wait, SM, Bit> eval;

      private Source(final int code, final String mnemonic,
                     final BiFunction<Wait, SM, Bit> eval)
      {
        this.code = code;
        this.mnemonic = mnemonic;
        this.eval = eval;
        code2src.put(code, this);
      }

      public Bit getBit(final Wait wait, SM sm)
      {
        return eval.apply(wait, sm);
      }

      @Override
      public String toString()
      {
        return mnemonic;
      }
    }

    private Bit polarity;
    private Source src;
    private int index;

    @Override
    protected void resetParams()
    {
      polarity = Bit.LOW;
      src = Source.GPIO_;
      index = 0;
    }

    @Override
    public void decodeLSB(final int lsb)
      throws Decoder.DecodeException
    {
      polarity = (lsb & 0x80) != 0 ? Bit.HIGH : Bit.LOW;
      src = code2src.get((lsb & 0x60) >>> 5);
      if (src == Source.RESERVED_3) {
        throw new Decoder.DecodeException(this, getOpCode());
      }
      index = lsb & 0x1f;
      checkIRQIndex(index);
    }

    @Override
    public ResultState executeOperation(final SM sm)
    {
      final boolean doStall = src.getBit(this, sm) != polarity;
      return doStall ? ResultState.STALL : ResultState.COMPLETE;
    }

    @Override
    public String getMnemonic()
    {
      return "wait";
    }

    @Override
    public String getParamsDisplay()
    {
      final int maskedIndex;
      final String num =
        src == Source.IRQ ?
        getIRQNumDisplay(index) :
        String.format("%02x", index);
      return polarity + " " + src + " " + num;
    }
  }

  public static class In extends Instruction
  {
    private static final Map<Integer, Source> code2src =
      new HashMap<Integer, Source>();

    private enum Source
    {
      PINS(0b000, "pins", (sm) -> {
          final int base = sm.getStatus().regPINCTRL_IN_BASE;
          return
            sm.getPIOGPIO().getGPIO().getPinsToPeri(base, Constants.GPIO_NUM);
        }),
      X(0b001, "x", (sm) -> sm.getX()),
      Y(0b010, "y", (sm) -> sm.getY()),
      NULL(0b011, "null", (sm) -> 0),
      RESERVED_4(0b100, "???", null),
      RESERVED_5(0b101, "???", null),
      ISR(0b110, "ISR", (sm) -> sm.getISRValue()),
      OSR(0b111, "OSR", (sm) -> sm.getOSRValue());

      private final int code;
      private final String mnemonic;
      private final Function<SM, Integer> eval;

      private Source(final int code, final String mnemonic,
                     final Function<SM, Integer> eval)
      {
        this.code = code;
        this.mnemonic = mnemonic;
        this.eval = eval;
        code2src.put(code, this);
      }

      public Integer getData(final SM sm)
      {
        return eval.apply(sm);
      }

      @Override
      public String toString()
      {
        return mnemonic;
      }
    }

    private Source src;
    private int bitCount;

    @Override
    protected void resetParams()
    {
      src = Source.PINS;
      bitCount = 0;
    }

    @Override
    public void decodeLSB(final int lsb)
      throws Decoder.DecodeException
    {
      src = code2src.get((lsb & 0xe0) >>> 5);
      if ((src == Source.RESERVED_4) ||
          (src == Source.RESERVED_5)) {
        throw new Decoder.DecodeException(this, getOpCode());
      }
      bitCount = lsb & 0x1f;
    }

    private void shiftIn(final SM sm, final SM.Status smStatus, final int data,
                         final int bitsToShift)
    {
      if (bitsToShift < 32) {
        if (sm.getInShiftDir() == PIO.ShiftDir.SHIFT_LEFT) {
          smStatus.isrValue <<= bitsToShift;
          smStatus.isrValue |= data & ((0x1 << bitsToShift) - 1);
        } else /* SHIFT RIGHT */ {
          smStatus.isrValue >>>= bitsToShift;
          smStatus.isrValue |=
            (data & ((0x1 << bitsToShift) - 1)) << (32 - bitsToShift);
        }
      } else {
        smStatus.isrValue = data;
      }
    }

    private void saturate(final SM sm, final SM.Status smStatus,
                          final int bitsToShift)
    {
      smStatus.isrShiftCount =
        SM.saturate(smStatus.isrShiftCount, bitsToShift, 32);
    }

    @Override
    public ResultState executeOperation(final SM sm)
    {
      final int bitsToShift =
        Constants.checkBitCount(bitCount, "shift ISR bitCount");
      final SM.Status smStatus = sm.getStatus();
      /*
       * TODO: Clarify: Do we need to stall if ISR and RX FIFO are
       * both full, prior to call shiftIn() (see built-in example
       * "logic-analyser" as test case)?  The spec does not say so
       * (see Sect. 3.5.4.1.), but I would expect it.  In the latter
       * case, we need the following additional code te be executed
       * first, prior to the call to shiftIn():
       */
      /*
      if (smStatus.isIsrCountBeyondThreshold()) {
        if (sm.isRXFIFOFull()) {
          return ResultState.STALL;
        }
      }
      */
      shiftIn(sm, smStatus, src.getData(sm), bitsToShift);
      saturate(sm, smStatus, bitsToShift);
      final boolean stall;
      if (smStatus.regSHIFTCTRL_AUTOPUSH) {
        // Cp. pseudocode sequence for "IN" cycle in RP2040 datasheet,
        // Sect. 3.5.4.1. "Autopush Details".
        if (smStatus.isIsrCountBeyondThreshold()) {
          if (sm.isRXFIFOFull()) {
            stall = true;
          } else {
            stall = sm.rxPush(false, true);
          }
        } else {
          stall = false;
        }
      } else {
        stall = false;
      }
      return stall ? ResultState.STALL : ResultState.COMPLETE;
    }

    @Override
    public String getMnemonic()
    {
      return "in";
    }

    @Override
    public String getParamsDisplay()
    {
      return src + ", " + String.format("%02x", bitCount != 0 ? bitCount : 32);
    }
  }

  public static class Out extends Instruction
  {
    private static final Map<Integer, Destination> code2dst =
      new HashMap<Integer, Destination>();

    private class DestinationData {
      public final int shiftOutBits;
      public final int bitsToShift;
      public DestinationData(int shiftOutBits, int bitsToShift) {
        this.shiftOutBits = shiftOutBits;
        this.bitsToShift = bitsToShift;
      }
    }

    public enum Destination
    {
      PINS(0b000, "pins", (sm, data) -> {
          SM.IOMapping.OUT.collatePins(sm, data.shiftOutBits);
          return null;
        }),
      X(0b001, "x", (sm, data) -> {
          sm.setX(data.shiftOutBits);
          return null;
        }),
      Y(0b010, "y", (sm, data) -> {
          sm.setY(data.shiftOutBits);
          return null;
        }),
      NULL(0b011, "null", (sm, data) -> {
          return null;
        }),
      PINDIRS(0b100, "pindirs", (sm, data) -> {
          SM.IOMapping.OUT.collatePinDirs(sm, data.shiftOutBits);
          return null;
        }),
      PC(0b101, "pc", (sm, data) -> {
          sm.setPC(data.shiftOutBits & 0x1f);
          return null;
        }),
      ISR(0b110, "isr", (sm, data) -> {
          sm.setISRValue(data.shiftOutBits);
          sm.setISRShiftCount(data.bitsToShift);
          return null;
        }),
      EXEC(0b111, "exec", (sm, data) -> {
          sm.execInstruction(data.shiftOutBits);
          return null;
        });

      private final int code;
      private final String mnemonic;
      private final BiFunction<SM, DestinationData, Void> eval;

      private Destination(final int code, final String mnemonic,
                          final BiFunction<SM, DestinationData, Void> eval)
      {
        this.code = code;
        this.mnemonic = mnemonic;
        this.eval = eval;
        code2dst.put(code, this);
      }

      public Consumer<DestinationData> getConsumer(final SM sm)
      {
        return (data) -> {
          eval.apply(sm, data);
        };
      }

      @Override
      public String toString()
      {
        return mnemonic;
      }
    }

    private Destination dst;
    private int bitCount;

    @Override
    protected void resetParams()
    {
      dst = Destination.PINS;
      bitCount = 0;
    }

    public void setDestination(final Destination dst)
    {
      if (dst == null) {
        throw new NullPointerException("dst");
      }
      this.dst = dst;
    }

    public void setBitCount(final int bitCount)
    {
      if (bitCount < 0) {
        throw new IllegalArgumentException("bit count < 0: " + bitCount);
      }
      if (bitCount > 31) {
        throw new IllegalArgumentException("bit count > 31: " + bitCount);
      }
      this.bitCount = bitCount;
    }

    public int encode(final int pinCtrlSidesetCount,
                      final boolean execCtrlSideEn)
    {
      return
        0x6000 |
        getDelayAndSideSetBits(pinCtrlSidesetCount, execCtrlSideEn) |
        (dst.ordinal() << 5) |
        bitCount;
    }

    @Override
    public void decodeLSB(final int lsb)
    {
      dst = code2dst.get((lsb & 0xe0) >>> 5);
      bitCount = lsb & 0x1f;
    }

    private void outputOsr(final SM sm, final SM.Status smStatus,
                           final int bitsToShift)
    {
      final int shiftOutBits;
      if (bitsToShift < 32) {
        if (sm.getOutShiftDir() == PIO.ShiftDir.SHIFT_LEFT) {
          shiftOutBits =
            (smStatus.osrValue >>> (32 - bitsToShift)) &
            ((0x1 << bitsToShift) - 1);
        } else /* SHIFT_RIGHT */ {
          shiftOutBits = smStatus.osrValue & ((0x1 << bitsToShift) - 1);
        }
      } else {
        shiftOutBits = smStatus.osrValue;
      }
      dst.getConsumer(sm).accept(new DestinationData(shiftOutBits, bitsToShift));
    }

    private void shiftOsr(final SM sm, final SM.Status smStatus,
                          final int bitsToShift)
    {
      if (bitsToShift < 32) {
        if (sm.getOutShiftDir() == PIO.ShiftDir.SHIFT_LEFT) {
          smStatus.osrValue <<= bitsToShift;
        } else /* SHIFT_RIGHT */ {
          smStatus.osrValue >>>= bitsToShift;
        }
      } else {
        smStatus.osrValue = 0;
      }
    }

    private void saturate(final SM sm, final SM.Status smStatus,
                          final int bitsToShift)
    {
      smStatus.osrShiftCount =
        SM.saturate(smStatus.osrShiftCount, bitsToShift, 32);
    }

    @Override
    public ResultState executeOperation(final SM sm)
    {
      // Cp. pseudocode sequence for "OUT" cycles in RP2040 datasheet,
      // Sect. 3.5.4.2. "Autopull Details".
      final SM.Status smStatus = sm.getStatus();
      final boolean stall;
      if (smStatus.regSHIFTCTRL_AUTOPULL &&
          smStatus.isOsrCountBeyondThreshold()) {
        // block=true to avoid loading regX
        sm.txPull(false, true); // also sets osr count = 0

        // RP2040 cannot fill empty OSR and "OUT" in same cycle =>
        // always stall regardless of TX state
        stall = true;
      } else {
        final int bitsToShift =
          Constants.checkBitCount(bitCount, "shift OSR bitCount");
        outputOsr(sm, smStatus, bitsToShift);
        shiftOsr(sm, smStatus, bitsToShift);
        saturate(sm, smStatus, bitsToShift);
        if (smStatus.regSHIFTCTRL_AUTOPULL) {
          if (smStatus.isOsrCountBeyondThreshold()) {
            // block=true to avoid loading regX
            sm.txPull(false, true); // also sets osr count = 0
          }
        }
        // stall always false, since we *did* output from OSR
        stall = false;
      }
      return (stall || dst == Destination.EXEC) ?
        ResultState.STALL :
        (dst == Destination.PC ?
         ResultState.JUMP :
         ResultState.COMPLETE);
    }

    @Override
    public String getMnemonic()
    {
      return "out";
    }

    @Override
    public String getParamsDisplay()
    {
      return dst + ", " + String.format("%02x", bitCount != 0 ? bitCount : 32);
    }
  }

  public static class Push extends Instruction
  {
    private boolean ifFull;
    private boolean block;

    @Override
    protected void resetParams()
    {
      ifFull = false;
      block = false;
    }

    @Override
    public void decodeLSB(final int lsb)
      throws Decoder.DecodeException
    {
      ifFull = (lsb & 0x40) != 0;
      block = (lsb & 0x20) != 0;
      if ((lsb & 0x1f) != 0) {
        throw new Decoder.DecodeException(this, getOpCode());
      }
    }

    @Override
    public ResultState executeOperation(final SM sm)
    {
      return sm.rxPush(ifFull, block) ?
        ResultState.STALL :
        ResultState.COMPLETE;
    }

    @Override
    public String getMnemonic()
    {
      return "push";
    }

    @Override
    public String getParamsDisplay()
    {
      return
        (ifFull ? "iffull" : "") +
        (ifFull && !block ? " " : "") +
        (block ? "" : "noblock");
    }
  }

  public static class Pull extends Instruction
  {
    private boolean ifEmpty;
    private boolean block;

    @Override
    protected void resetParams()
    {
      ifEmpty = false;
      block = false;
    }

    public void setIfEmpty(final boolean ifEmpty)
    {
      this.ifEmpty = ifEmpty;
    }

    public void setBlock(final boolean block)
    {
      this.block = block;
    }

    public int encode(final int pinCtrlSidesetCount,
                      final boolean execCtrlSideEn)
    {
      return
        0x8080 |
        getDelayAndSideSetBits(pinCtrlSidesetCount, execCtrlSideEn) |
        (ifEmpty ? 0x1 << 6 : 0) |
        (block ? 0x1 << 5 : 0);
    }

    @Override
    public void decodeLSB(final int lsb)
      throws Decoder.DecodeException
    {
      ifEmpty = (lsb & 0x40) != 0;
      block = (lsb & 0x20) != 0;
      if ((lsb & 0x1f) != 0) {
        throw new Decoder.DecodeException(this, getOpCode());
      }
    }

    @Override
    public ResultState executeOperation(final SM sm)
    {
      return sm.txPull(ifEmpty, block) ?
        ResultState.STALL :
        ResultState.COMPLETE;
    }

    @Override
    public String getMnemonic()
    {
      return "pull";
    }

    @Override
    public String getParamsDisplay()
    {
      return
        (ifEmpty ? "ifempty" : "") +
        (ifEmpty && !block ? " " : "") +
        (block ? "" : "noblock");
    }
  }

  public static class Mov extends Instruction
  {
    private static final Map<Integer, Source> code2src =
      new HashMap<Integer, Source>();
    private static final Map<Integer, Destination> code2dst =
      new HashMap<Integer, Destination>();
    private static final Map<Integer, Operation> code2op =
      new HashMap<Integer, Operation>();

    private enum Source
    {
      PINS(0b000, "pins", (sm) -> {
          final int base = sm.getStatus().regPINCTRL_IN_BASE;
          return
            sm.getPIOGPIO().getGPIO().getPinsToPeri(base, Constants.GPIO_NUM);
        }),
      X(0b001, "x", (sm) -> sm.getX()),
      Y(0b010, "y", (sm) -> sm.getY()),
      NULL(0b011, "null", (sm) -> 0),
      RESERVED_4(0b100, "???", null),
      STATUS(0b101, "status",
             (sm) -> (sm.getStatus().getFIFOStatus())),
      ISR(0b110, "isr", (sm) -> sm.getISRValue()),
      OSR(0b111, "osr", (sm) -> sm.getOSRValue());

      private final int code;
      private final String mnemonic;
      private final Function<SM, Integer> eval;

      private Source(final int code, final String mnemonic,
                     final Function<SM, Integer> eval)
      {
        this.code = code;
        this.mnemonic = mnemonic;
        this.eval = eval;
        code2src.put(code, this);
      }

      public Integer read(final SM sm)
      {
        return eval.apply(sm);
      }

      @Override
      public String toString()
      {
        return mnemonic;
      }
    }

    private enum Destination
    {
      PINS(0b000, "pins", (sm, data) -> {
          SM.IOMapping.OUT.collatePins(sm, data);
          return null;
        }),
      X(0b001, "x", (sm, data) -> {
          sm.setX(data);
          return null;
        }),
      Y(0b010, "y", (sm, data) -> {
          sm.setY(data);
          return null;
        }),
      RESERVED_3(0b011, "???", null),
      EXEC(0b100, "exec", (sm, data) -> {
          sm.execInstruction(data);
          return null;
        }),
      PC(0b101, "pc", (sm, data) -> {
          sm.setPC(data & 0x1f);
          return null;
        }),
      ISR(0b110, "isr", (sm, data) -> {
          sm.setISRValue(data);
          sm.setISRShiftCount(0);
          return null;
        }),
      OSR(0b111, "osr", (sm, data) -> {
          sm.setOSRValue(data);
          sm.setOSRShiftCount(0);
          return null;
        });

      private final int code;
      private final String mnemonic;
      private final BiFunction<SM, Integer, Void> eval;

      private Destination(final int code, final String mnemonic,
                          final BiFunction<SM, Integer, Void> eval)
      {
        this.code = code;
        this.mnemonic = mnemonic;
        this.eval = eval;
        code2dst.put(code, this);
      }

      public void write(final SM sm, final int data)
      {
        eval.apply(sm, data);
      }

      @Override
      public String toString()
      {
        return mnemonic;
      }
    }

    private enum Operation
    {
      NONE(0b00, "", (data) -> data),
      INVERT(0b01, "~", (data) -> ~data),
      BIT_REVERSE(0b10, "::", (data) -> Integer.reverse(data)),
      RESERVED_3(0b11, "???", null);

      private final int code;
      private final String mnemonic;
      private final Function<Integer, Integer> eval;

      private Operation(final int code, final String mnemonic,
                     final Function<Integer, Integer> eval)
      {
        this.code = code;
        this.mnemonic = mnemonic;
        this.eval = eval;
        code2op.put(code, this);
      }

      private int apply(final int data)
      {
        return eval.apply(data);
      }

      @Override
      public String toString()
      {
        return mnemonic;
      }
    }

    private Source src;
    private Destination dst;
    private Operation op;

    @Override
    protected void resetParams()
    {
      src = Source.PINS;
      dst = Destination.PINS;
      op = Operation.NONE;
    }

    private boolean isNop()
    {
      return
        (src == Source.Y) &&
        (dst == Destination.Y) &&
        (op == Operation.NONE);
    }

    @Override
    public void decodeLSB(final int lsb)
      throws Decoder.DecodeException
    {
      src = code2src.get(lsb & 0x7);
      if (src == Source.RESERVED_4) {
        throw new Decoder.DecodeException(this, getOpCode());
      }
      dst = code2dst.get((lsb & 0xe0) >>> 5);
      if (dst == Destination.RESERVED_3) {
        throw new Decoder.DecodeException(this, getOpCode());
      }
      op = code2op.get((lsb & 0x18) >>> 3);
      if (op == Operation.RESERVED_3) {
        throw new Decoder.DecodeException(this, getOpCode());
      }
    }

    @Override
    public ResultState executeOperation(final SM sm)
    {
      dst.write(sm, op.apply(src.read(sm)));
      return
        dst == Destination.EXEC ?
        ResultState.STALL :
        (dst == Destination.PC ?
         ResultState.JUMP :
         ResultState.COMPLETE);
    }

    @Override
    public String getMnemonic()
    {
      return isNop() ? "nop" : "mov";
    }

    @Override
    public String getParamsDisplay()
    {
      if (isNop()) return "";
      final String strOp = op.toString();
      return dst + ", " + (!strOp.isEmpty() ? strOp : "") + src;
    }
  }

  public static class Irq extends Instruction
  {
    private boolean clr;
    private boolean wait;
    private int index;

    @Override
    protected void resetParams()
    {
      clr = false;
      wait = false;
      index = 0;
    }

    @Override
    public void decodeLSB(final int lsb)
      throws Decoder.DecodeException
    {
      if ((lsb & 0x80) != 0) {
        throw new Decoder.DecodeException(this, getOpCode());
      }
      clr = (lsb & 0x40) != 0;
      wait = (lsb & 0x20) != 0;
      index = lsb & 0x1f;
      checkIRQIndex(index);
    }

    @Override
    public ResultState executeOperation(final SM sm)
    {
      final boolean stall;
      final int irqNum = getIRQNum(sm.getNum(), index);
      if (clr) {
        sm.clearIRQ(irqNum);
        stall = false;
      } else {
        sm.setIRQ(irqNum);
        stall = wait;
      }
      return stall ? ResultState.STALL : ResultState.COMPLETE;
    }

    @Override
    public String getMnemonic()
    {
      return "irq";
    }

    @Override
    public String getParamsDisplay()
    {
      /*
       * Note: Modes "", "set" and "nowait" are all synonyms for the
       * same thing, namely that both flags (clr, wait) are not set.
       * For display, we deliberately choose "".
       */
      final String mode = clr ? "clear " : (wait ? "wait " : "");
      return mode + getIRQNumDisplay(index);
    }
  }

  public static class Set extends Instruction
  {
    private static final Map<Integer, Destination> code2dst =
      new HashMap<Integer, Destination>();

    public enum Destination
    {
      PINS(0b000, "pins", (sm, data) -> {
          SM.IOMapping.SET.collatePins(sm, data);
          return null;
        }),
      X(0b001, "x", (sm, data) -> {
          sm.setX(data);
          return null;
        }),
      Y(0b010, "y", (sm, data) -> {
          sm.setY(data);
          return null;
        }),
      RESERVED_3(0b011, "???", null),
      PINDIRS(0b100, "pindirs", (sm, data) -> {
          SM.IOMapping.SET.collatePinDirs(sm, data);
          return null;
        }),
      RESERVED_5(0b101, "???", null),
      RESERVED_6(0b110, "???", null),
      RESERVED_7(0b111, "???", null);

      private final int code;
      private final String mnemonic;
      private final BiFunction<SM, Integer, Void> eval;

      private Destination(final int code, final String mnemonic,
                          final BiFunction<SM, Integer, Void> eval)
      {
        this.code = code;
        this.mnemonic = mnemonic;
        this.eval = eval;
        code2dst.put(code, this);
      }

      public void write(final SM sm, final int data)
      {
        eval.apply(sm, data);
      }

      @Override
      public String toString()
      {
        return mnemonic;
      }
    }

    private Destination dst;
    private int data;

    @Override
    protected void resetParams()
    {
      dst = Destination.PINS;
      data = 0;
    }

    public void setDestination(final Destination dst)
    {
      if (dst == null) {
        throw new NullPointerException("dst");
      }
      this.dst = dst;
    }

    public void setData(final int data)
    {
      if (data < 0) {
        throw new IllegalArgumentException("data < 0: " + data);
      }
      if (data > 31) {
        throw new IllegalArgumentException("data > 31: " + data);
      }
      this.data = data;
    }

    public int encode(final int pinCtrlSidesetCount,
                      final boolean execCtrlSideEn)
    {
      return
        0xe000 |
        getDelayAndSideSetBits(pinCtrlSidesetCount, execCtrlSideEn) |
        (dst.ordinal() << 5) |
        (data & 0x1f);
    }

    @Override
    public void decodeLSB(final int lsb)
      throws Decoder.DecodeException
    {
      dst = code2dst.get((lsb & 0xe0) >>> 5);
      if ((dst == Destination.RESERVED_3) ||
          (dst == Destination.RESERVED_5) ||
          (dst == Destination.RESERVED_6) ||
          (dst == Destination.RESERVED_7)) {
        throw new Decoder.DecodeException(this, getOpCode());
      }
      data = lsb & 0x1f;
    }

    @Override
    public ResultState executeOperation(final SM sm)
    {
      dst.write(sm, data);
      return ResultState.COMPLETE;
    }

    @Override
    public String getMnemonic()
    {
      return "set";
    }

    @Override
    public String getParamsDisplay()
    {
      return dst + ", " + String.format("%02x", data);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
