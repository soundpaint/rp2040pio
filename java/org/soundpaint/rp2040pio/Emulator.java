/*
 * @(#)Emulator.java 1.00 21/03/19
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

/**
 * Holds internal subsystems of core emulator.
 */
public class Emulator
{
  private final PrintStream console;
  private final MasterClock masterClock;
  private final GPIO gpio;
  private final PIO pio0;
  private final PIO pio1;

  private Emulator()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public Emulator(final PrintStream console)
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.console = console;
    masterClock = new MasterClock(console);
    gpio = new GPIO(console, masterClock);
    pio0 = gpio.getPIO0();
    pio1 = gpio.getPIO1();
  }

  public PrintStream getConsole()
  {
    return console;
  }

  public MasterClock getMasterClock()
  {
    return masterClock;
  }

  public GPIO getGPIO()
  {
    return gpio;
  }

  public PIO getPIO0()
  {
    return pio0;
  }

  public PIO getPIO1()
  {
    return pio1;
  }

  public void reset()
  {
    masterClock.reset();
    gpio.reset();
    pio0.reset();
    pio1.reset();
  }

  public void terminate()
  {
    masterClock.terminate();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
