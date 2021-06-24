/*
 * @(#)GPIO.java 1.00 21/01/31
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

import java.io.PrintStream;
import java.util.function.Function;

/**
 * General-Purpose Set of 32 Peripheral I/O Terminals
 */
public class GPIO implements Constants
{
  private enum Override
  {
    BYPASS((value) -> value, (value) -> value),
    INVERT((value) -> value.inverse(), (value) -> value.inverse()),
    ALWAYS_LOW((value) -> Bit.LOW, (value) -> Direction.IN),
    ALWAYS_HIGH((value) -> Bit.HIGH, (value) -> Direction.OUT);

    private static final Override[] values = Override.values();

    private final Function<Bit, Bit> fnBit;
    private final Function<Direction, Direction> fnDirection;

    private Override(final Function<Bit, Bit> fnBit,
                     final Function<Direction, Direction> fnDirection)
    {
      if (fnBit == null) throw new NullPointerException("fnBit");
      this.fnBit = fnBit;
      if (fnDirection == null) throw new NullPointerException("fnDirection");
      this.fnDirection = fnDirection;
    }

    public Bit apply(final Bit value)
    {
      return fnBit.apply(value);
    }

    public Direction apply(final Direction value)
    {
      return fnDirection.apply(value);
    }

    public static Override fromValue(final int value)
    {
      if (value < 0) {
        final String message = String.format("value < 0: %d", value);
        throw new IllegalArgumentException(message);
      }
      if (value > values.length) {
        final String message =
          String.format("value >= %d: %d", values.length, value);
        throw new IllegalArgumentException(message);
      }
      return values[value];
    }

    public int getValue() { return ordinal(); }
  }

  private static class Terminal
  {
    private int num;
    private GPIO_Function function;
    private Override irqOverride;
    private Override inputOverride;
    private Override oeOverride;
    private Override outputOverride;
    private Bit externalInput;

    private Terminal()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private Terminal(final int num)
    {
      Constants.checkGpioPin(num, "GPIO port");
      this.num = num;
      reset();
    }

    public void reset()
    {
      function = GPIO_Function.NULL;
      irqOverride = Override.BYPASS;
      inputOverride = Override.BYPASS;
      oeOverride = Override.BYPASS;
      outputOverride = Override.BYPASS;
      externalInput = Bit.LOW;
    }

    private Bit getPadIn(final Bit outBeforeOverride,
                         final Direction oeBeforeOverride)
    {
      /*
       * Loopback GPIO output as pad input, if a PIO drives this GPIO
       * pin as output, while listening to this GPIO pin as input pin.
       *
       * See comment in file pico-examples/pio/spi/spi_loopback.c:
       *
       *   #define PIN_MISO 16 // same as MOSI, so we get loopback
       *
       * Note that, as a result from loopback, a PIO may even observe
       * the other PIO's GPIO pad output.
       */
      final Direction oeAfterOverride = getOeAfterOverride(oeBeforeOverride);
      return
        oeAfterOverride == Direction.OUT ?
        getOutAfterOverride(outBeforeOverride) :
        externalInput;
    }

    private Bit getInputAfterOverride(final Bit outBeforeOverride,
                                      final Direction oeBeforeOverride)
    {
      final Bit padIn = getPadIn(outBeforeOverride, oeBeforeOverride);
      return inputOverride.apply(padIn);
    }

    private Bit getIrqAfterOverride(final Bit outBeforeOverride,
                                    final Direction oeBeforeOverride)
    {
      final Bit padIn = getPadIn(outBeforeOverride, oeBeforeOverride);
      return irqOverride.apply(padIn);
    }

    private Direction getOeAfterOverride(final Direction oeBeforeOverride)
    {
      return oeOverride.apply(oeBeforeOverride);
    }

    private Bit getOutAfterOverride(final Bit outBeforeOverride)
    {
      return outputOverride.apply(outBeforeOverride);
    }
  }

  private final PrintStream console;
  private final PIO pio0;
  private final PIO pio1;
  private final Terminal[] terminals;
  private int regINPUT_SYNC_BYPASS; // bits 0…31 of INPUT_SYNC_BYPASS
                                    // (contents currently ignored)

  private GPIO()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public GPIO(final PrintStream console, final MasterClock masterClock)
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    if (masterClock == null) {
      throw new NullPointerException("masterClock");
    }
    this.console = console;
    pio0 = new PIO(0, console, masterClock, this);
    pio1 = new PIO(1, console, masterClock, this);
    terminals = new Terminal[GPIO_NUM];
    for (int port = 0; port < terminals.length; port++) {
      terminals[port] = new Terminal(port);
    }
    reset();
  }

  public void reset()
  {
    for (int port = 0; port < terminals.length; port++) {
      terminals[port].reset();
    }
  }

  public PIO getPIO0() { return pio0; }

  public PIO getPIO1() { return pio1; }

  public synchronized int getGPIO_PADIN()
  {
    int status = 0x0;
    for (int port = 0; port < terminals.length; port++) {
      status <<= 0x1;
      status |= terminals[terminals.length - 1 - port].externalInput.getValue();
    }
    return status;
  }

  public synchronized void setGPIO_PADIN(final int bits, final int mask,
                                         final boolean xor)
  {
    final int status = Constants.hwSetBits(getGPIO_PADIN(), bits, mask, xor);
    for (int port = 0; port < terminals.length; port++) {
      terminals[port].externalInput =
        Bit.fromValue((status >>> port) & 0x1);
    }
  }

  /**
   * Set GPIOx_CTRL_FUNCSEL to 6 (for PIO0) or 7 (for PIO1), see
   * Sect. 2.19.2. "Function Select" of RP2040 datasheet for details.
   */
  public void setFunction(final int gpio, final GPIO_Function fn)
  {
    Constants.checkGpioPin(gpio, "GPIO port");
    if (fn == null) {
      throw new NullPointerException("fn");
    }
    terminals[gpio].function = fn;
  }

  private GPIO_Function getFunction(final int gpio)
  {
    Constants.checkGpioPin(gpio, "GPIO port");
    return terminals[gpio].function;
  }

  public void setCTRL(final int gpio, final int value,
                      final int mask, final boolean xor)
  {
    final int ctrl = Constants.hwSetBits(getCTRL(gpio), value, mask, xor);
    final Terminal terminal = terminals[gpio];

    final Override irqOverride =
      Override.fromValue((ctrl & IO_BANK0_GPIO0_CTRL_IRQOVER_BITS) >>
                         IO_BANK0_GPIO0_CTRL_IRQOVER_LSB);
    terminal.irqOverride = irqOverride;

    final Override inputOverride =
      Override.fromValue((ctrl & IO_BANK0_GPIO0_CTRL_INOVER_BITS) >>
                         IO_BANK0_GPIO0_CTRL_INOVER_LSB);
    terminal.inputOverride = inputOverride;

    final Override oeOverride =
      Override.fromValue((ctrl & IO_BANK0_GPIO0_CTRL_OEOVER_BITS) >>
                         IO_BANK0_GPIO0_CTRL_OEOVER_LSB);
    terminal.oeOverride = oeOverride;

    final Override outputOverride =
      Override.fromValue((ctrl & IO_BANK0_GPIO0_CTRL_OUTOVER_BITS) >>
                         IO_BANK0_GPIO0_CTRL_OUTOVER_LSB);
    terminal.outputOverride = outputOverride;

    final GPIO_Function fn =
      GPIO_Function.fromValue((ctrl & IO_BANK0_GPIO0_CTRL_FUNCSEL_BITS) >>
                              IO_BANK0_GPIO0_CTRL_FUNCSEL_LSB,
                              GPIO_Function.NULL);
    terminal.function = fn;
  }

  public int getCTRL(final int gpio)
  {
    Constants.checkGpioPin(gpio, "GPIO port");
    final Terminal terminal = terminals[gpio];
    return
      (terminal.irqOverride.getValue() << IO_BANK0_GPIO0_CTRL_IRQOVER_LSB) |
      (terminal.inputOverride.getValue() << IO_BANK0_GPIO0_CTRL_INOVER_LSB) |
      (terminal.oeOverride.getValue() << IO_BANK0_GPIO0_CTRL_OEOVER_LSB) |
      (terminal.outputOverride.getValue() << IO_BANK0_GPIO0_CTRL_OUTOVER_LSB) |
      (terminal.function.getValue() << IO_BANK0_GPIO0_CTRL_FUNCSEL_LSB);
  }

  public int getSTATUS(final int gpio)
  {
    Constants.checkGpioPin(gpio, "GPIO port");
    return
      (getIrqToProc(gpio).getValue() << IO_BANK0_GPIO0_STATUS_IRQTOPROC_LSB) |
      (getIrqFromPad(gpio).getValue() <<
       IO_BANK0_GPIO0_STATUS_IRQFROMPAD_LSB) |
      (getInToPeri(gpio).getValue() << IO_BANK0_GPIO0_STATUS_INTOPERI_LSB) |
      (getInFromPad(gpio).getValue() << IO_BANK0_GPIO0_STATUS_INFROMPAD_LSB) |
      (getOeToPad(gpio).getValue() << IO_BANK0_GPIO0_STATUS_OETOPAD_LSB) |
      (getOeFromPeripheral(gpio).getValue() <<
       IO_BANK0_GPIO0_STATUS_OEFROMPERI_LSB) |
      (getOutToPad(gpio).getValue() << IO_BANK0_GPIO0_STATUS_OUTTOPAD_LSB) |
      (getOutFromPeripheral(gpio).getValue() <<
       IO_BANK0_GPIO0_STATUS_OUTFROMPERI_LSB);
  }

  private Bit getIrqToProc(final int gpio)
  {
    Constants.checkGpioPin(gpio, "GPIO port");
    final Bit outBeforeOverride = getOutFromPeripheral(gpio);
    final Direction oeBeforeOverride = getOeFromPeripheral(gpio);
    return terminals[gpio].getIrqAfterOverride(outBeforeOverride,
                                               oeBeforeOverride);
  }

  private Bit getIrqFromPad(final int gpio)
  {
    /*
     * TODO: Clarify: How does / should this method differ from method
     * getInFromPad()?  It seems the RP2040 datasheet does not explain
     * the difference between interrupt from pad
     * (IO_BANK0_GPIOx_STATUS_IRQFROMPAD) and input signal from pad
     * (IO_BANK0_GPIOx_STATUS_INFROMPAD).  Maybe, interrupt from pad
     * is the value of an edge-triggered flip-flop (but how is the
     * flip-flop reset again?), while input signal from pad is the
     * pad's current logical value in terms of voltage level?
     */
    Constants.checkGpioPin(gpio, "GPIO port");
    final Bit outBeforeOverride = getOutFromPeripheral(gpio);
    final Direction oeBeforeOverride = getOeFromPeripheral(gpio);
    return terminals[gpio].getPadIn(outBeforeOverride, oeBeforeOverride);
   }

  public int getPinsToPeri(final int base, final int count)
  {
    Constants.checkGpioPin(base, "GPIO pin base");
    Constants.checkGpioPinsCount(count, "GPIO pin count");
    int pins = 0;
    for (int pin = 0; pin < count; pin++) {
      pins = (pins << 0x1) | getInToPeri((base - pin - 1) & 0x1f).getValue();
    }
    return pins;
  }

  public Bit getInToPeri(final int gpio)
  {
    Constants.checkGpioPin(gpio, "GPIO port");
    final Bit outBeforeOverride = getOutFromPeripheral(gpio);
    final Direction oeBeforeOverride = getOeFromPeripheral(gpio);
    return terminals[gpio].getInputAfterOverride(outBeforeOverride,
                                                 oeBeforeOverride);
  }

  private Bit getInFromPad(final int gpio)
  {
    Constants.checkGpioPin(gpio, "GPIO port");
    final Bit outBeforeOverride = getOutFromPeripheral(gpio);
    final Direction oeBeforeOverride = getOeFromPeripheral(gpio);
    return terminals[gpio].getPadIn(outBeforeOverride, oeBeforeOverride);
  }

  private Direction getOeToPad(final int gpio)
  {
    Constants.checkGpioPin(gpio, "GPIO port");
    final Direction oeBeforeOverride = getOeFromPeripheral(gpio);
    return terminals[gpio].getOeAfterOverride(oeBeforeOverride);
  }

  private Direction getOeFromPeripheral(final int gpio)
  {
    switch (getFunction(gpio)) {
    case XIP:
    case SPI:
    case UART:
    case I2C:
    case PWM:
    case SIO:
      // not implemented by this emulator
      return Direction.IN;
    case PIO0:
      return pio0.getDirection(gpio);
    case PIO1:
      return pio1.getDirection(gpio);
    case GPCK:
    case USB:
      // not implemented by this emulator
      return Direction.IN;
    case NULL:
      return Direction.IN;
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }

  private Bit getOutToPad(final int gpio)
  {
    Constants.checkGpioPin(gpio, "GPIO port");
    final Bit outBeforeOverride = getOutFromPeripheral(gpio);
    return terminals[gpio].getOutAfterOverride(outBeforeOverride);
  }

  private Bit getOutFromPeripheral(final int gpio)
  {
    switch (getFunction(gpio)) {
    case XIP:
    case SPI:
    case UART:
    case I2C:
    case PWM:
    case SIO:
      // not implemented by this emulator
      return Bit.LOW;
    case PIO0:
      return pio0.getLevel(gpio);
    case PIO1:
      return pio1.getLevel(gpio);
    case GPCK:
    case USB:
      // not implemented by this emulator
      return Bit.LOW;
    case NULL:
      return Bit.LOW;
    default:
      throw new InternalError("unexpected case fall-through");
    }
  }

  public void setInputSyncByPass(final int bits, final int mask,
                                 final boolean xor)
  {
    regINPUT_SYNC_BYPASS =
      Constants.hwSetBits(regINPUT_SYNC_BYPASS, bits, mask, xor);
  }

  public int getInputSyncByPass()
  {
    return regINPUT_SYNC_BYPASS;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
