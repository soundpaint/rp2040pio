/*
 * @(#)Main.java 1.00 21/01/31
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

import java.io.IOException;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

public class Main
{
  private final SDK sdk;
  private final RegisterServer registerServer;

  public Main() throws IOException
  {
    sdk = SDK.getDefaultInstance();
    registerServer = new RegisterServer(sdk);
  }

  public static Supplier<Boolean> createDelayFilter(final PIOSDK pioSdk,
                                                    final int smNum)
  {
    final Supplier<Boolean> displayFilter = () -> {
      final PIOEmuRegisters pioEmuRegisters = pioSdk.getEmuRegisters();
      final int smDelayCycleAddress =
      pioEmuRegisters.getSMAddress(PIOEmuRegisters.Regs.SM0_DELAY_CYCLE,
                                   smNum);
      final boolean isDelayCycle =
      pioEmuRegisters.readAddress(smDelayCycleAddress) != 0x0;
      return !isDelayCycle;
    };
    return displayFilter;
  }

  public void run() throws IOException
  {
    final String programResourcePath = "/examples/squarewave.hex";
    //final String programResourcePath = "/examples/ws2812.hex";
    //final Monitor monitor = new Monitor(sdk);
    //monitor.addProgram(programResourcePath);
    //monitor.setSideSetCount(1);
    //monitor.dumpProgram();
    final TimingDiagram diagram = new TimingDiagram(sdk);
    diagram.addProgram(programResourcePath);
    diagram.addSignal(DiagramConfig.createClockSignal("clock"));

    diagram.addSignal(sdk.getPIO0Address(PIOEmuRegisters.Regs.SM0_CLK_ENABLE), 0);
    diagram.addSignal("GPIO 0",
                      sdk.getPIO0Address(PIOEmuRegisters.Regs.GPIO_PINS), 0, 0);
    diagram.addSignal("GPIO 0",
                      sdk.getPIO0Address(PIOEmuRegisters.Regs.GPIO_PINS), 0);
    diagram.addSignal("GPIO 1",
                      sdk.getPIO0Address(PIOEmuRegisters.Regs.GPIO_PINS), 1, 1);
    diagram.addSignal("GPIO 10",
                      sdk.getPIO0Address(PIOEmuRegisters.Regs.GPIO_PINS), 10, 10);
    diagram.addSignal(sdk.getPIO0Address(PIOEmuRegisters.Regs.SM0_PC));
    diagram.addSignal(sdk.getPIO0Address(PIOEmuRegisters.Regs.SM0_PC),
                      createDelayFilter(sdk.getPIO0SDK(), 0));

    final int instrAddr = sdk.getPIO0Address(PIORegisters.Regs.SM0_INSTR);
    final DiagramConfig.Signal instr1 =
      DiagramConfig.createInstructionSignal(sdk, sdk.getPIO0SDK(), instrAddr,
                                            0, null, false, null);
    diagram.addSignal(instr1);
    final DiagramConfig.Signal instr2 =
      DiagramConfig.createInstructionSignal(sdk, sdk.getPIO0SDK(), instrAddr,
                                            0, null, true, null);
    diagram.addSignal(instr2);
    final DiagramConfig.Signal instr3 =
      DiagramConfig.createInstructionSignal(sdk, sdk.getPIO0SDK(), instrAddr,
                                            0, null, true,
                                            createDelayFilter(sdk.getPIO0SDK(), 0));
    diagram.addSignal(instr3);

    //diagram.setSideSetCount(1);
    diagram.create();
  }

  public static void main(final String argv[]) throws IOException
  {
    new Main().run();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
