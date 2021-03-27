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
import java.io.PrintStream;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.sdk.LocalRegisters;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

public class Main
{
  private final PrintStream console;
  private final SDK sdk;

  private Main()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public Main(final PrintStream console) throws IOException
  {
    this.console = console;
    final Emulator emulator = new Emulator(console);
    final Registers registers = new LocalRegisters(emulator);
    sdk = new SDK(console, registers);
  }

  public static Supplier<Boolean> createDelayFilter(final SDK sdk,
                                                    final int pioNum,
                                                    final int smNum)
  {
    final Supplier<Boolean> displayFilter = () -> {
      final int smDelayCycleAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_DELAY_CYCLE);
      try {
        final boolean isDelayCycle =
          sdk.readAddress(smDelayCycleAddress) != 0x0;
        return !isDelayCycle;
      } catch (final IOException e) {
        // TODO: Maybe log warning that we failed to evaluate delay?
        return false;
      }
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
    diagram.setProgram(programResourcePath);
    diagram.addSignal(DiagramConfig.createClockSignal("clock"));

    diagram.addSignal(PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_CLK_ENABLE), 0);
    diagram.addSignal("GPIO 0",
                      GPIOIOBank0Registers.getAddress(GPIOIOBank0Registers.Regs.GPIO0_STATUS), 8, 8);
    diagram.addSignal("GPIO 1",
                      GPIOIOBank0Registers.getAddress(GPIOIOBank0Registers.Regs.GPIO1_STATUS), 8, 8);
    diagram.addSignal("GPIO 10",
                      GPIOIOBank0Registers.getAddress(GPIOIOBank0Registers.Regs.GPIO10_STATUS), 8, 8);
    diagram.addSignal(PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_PC));
    diagram.addSignal(PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_PC),
                      createDelayFilter(sdk, 0, 0));

    final int instrAddr = PIORegisters.getAddress(0, PIORegisters.Regs.SM0_INSTR);
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
                                            createDelayFilter(sdk, 0, 0));
    diagram.addSignal(instr3);

    //diagram.setSideSetCount(1);
    diagram.create();
  }

  public static void main(final String argv[]) throws IOException
  {
    new Main(System.out).run();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
