/*
 * @(#)SignalFilter.java 1.00 21/07/26
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
import java.util.Objects;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.sdk.SDK;

public enum SignalFilter
{
  NO_DELAY("Cycle is a delay cycle on below target state machine.",
           (sdk, pioNum, smNum) -> {
             final PIOEmuRegisters.Regs sm0DelayCycle =
               PIOEmuRegisters.Regs.SM0_DELAY_CYCLE;
             final int smDelayCycleAddress =
               PIOEmuRegisters.getSMAddress(pioNum, smNum, sm0DelayCycle);
             final boolean isDelayCycle =
               sdk.readAddress(smDelayCycleAddress) != 0x0;
             return !isDelayCycle;
           }),
  CLK_ENABLED("CLK enable signal is false for below target state machine.",
              (sdk, pioNum, smNum) -> {
                final PIOEmuRegisters.Regs sm0ClkEnable =
                  PIOEmuRegisters.Regs.SM0_CLK_ENABLE;
                final int clkEnableAddress =
                  PIOEmuRegisters.getSMAddress(pioNum, smNum, sm0ClkEnable);
                final int clkEnable = sdk.readAddress(clkEnableAddress) & 0x1;
                return clkEnable != 0x0;
              });

  @FunctionalInterface
  private static interface FilterFunction
  {
    boolean acceptCurrentSignalValue(final SDK sdk,
                                     final int pioNum, final int smNum)
      throws IOException;
  }

  private final String description;
  private final FilterFunction filterFunction;

  private SignalFilter(final String description,
                       final FilterFunction filterFunction)
  {
    Objects.requireNonNull(description);
    this.description = description;
    this.filterFunction = filterFunction;
  }

  public boolean acceptCurrentSignalValue(final SDK sdk,
                                          final int pioNum, final int smNum)
    throws IOException
  {
    return filterFunction.acceptCurrentSignalValue(sdk, pioNum, smNum);
  }

  @Override
  public String toString() { return description; }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
