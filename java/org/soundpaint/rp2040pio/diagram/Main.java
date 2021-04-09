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
package org.soundpaint.rp2040pio.diagram;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.Emulator;
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.RegisterClient;
import org.soundpaint.rp2040pio.Registers;
import org.soundpaint.rp2040pio.sdk.LocalRegisters;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Draws a timing diagram of a selected set of PIO signals and
 * generated from a specific PIO program.  By now, most of the
 * configuration is hard-wired in order to make it running out of the
 * box.
 */
public class Main
{
  private static final String PRG_NAME = "TimingDiagram";
  private static final String PRG_ID_AND_VERSION =
    "TimingDiagram Version 0.1 for " + Constants.getProgramAndVersion();
  private static final CmdOptions.FlagOptionDeclaration optVersion =
    CmdOptions.createFlagOption(false, 'V', "version", CmdOptions.Flag.OFF,
                                "display version information and exit");
  private static final CmdOptions.FlagOptionDeclaration optHelp =
    CmdOptions.createFlagOption(false, 'h', "help", CmdOptions.Flag.OFF,
                                "display this help text and exit");
  private static final CmdOptions.IntegerOptionDeclaration optPort =
    CmdOptions.createIntegerOption("PORT", false, 'p', "port",
                                   Constants.
                                   REGISTER_SERVER_DEFAULT_PORT_NUMBER,
                                   "use PORT as server port number");
  private static final List<CmdOptions.OptionDeclaration<?>>
    optionDeclarations =
    Arrays.asList(new CmdOptions.OptionDeclaration<?>[]
                  { optVersion, optHelp, optPort });
  private final static boolean RUN_REMOTELY = true;

  private final PrintStream console;
  private final CmdOptions options;
  private final SDK sdk;
  private final TimingDiagram diagram;

  private Main()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public Main(final PrintStream console, final String[] argv)
    throws IOException
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.console = console;
    options = parseArgs(argv);
    printAbout();
    final Registers registers;
    if (RUN_REMOTELY) {
      // connect to emulator running in another JVM
      registers = connect();
    } else {
      // create and connect to emulator running within this JVM
      final Emulator emulator = new Emulator(console);
      registers = new LocalRegisters(emulator);
    }
    sdk = new SDK(console, registers);
    diagram = new TimingDiagram(console, sdk);
    createDiagram();
  }

  private CmdOptions parseArgs(final String argv[])
  {
    final CmdOptions options;
    try {
      options = new CmdOptions(PRG_NAME, PRG_ID_AND_VERSION, null,
                               optionDeclarations);
      options.parse(argv);
      checkValidity(options);
    } catch (final CmdOptions.ParseException e) {
      console.println(e.getMessage());
      System.exit(-1);
      throw new InternalError();
    }
    if (options.getValue(optVersion) == CmdOptions.Flag.ON) {
      console.println(PRG_ID_AND_VERSION);
      System.exit(0);
      throw new InternalError();
    }
    if (options.getValue(optHelp) == CmdOptions.Flag.ON) {
      console.println(options.getFullInfo());
      System.exit(0);
      throw new InternalError();
    }
    return options;
  }

  private void checkValidity(final CmdOptions options)
    throws CmdOptions.ParseException
  {
    final int port = options.getValue(optPort);
    if ((port < 0) || (port > 65535)) {
      throw new CmdOptions.
        ParseException("PORT must be in the range 0..65535");
    }
  }

  private void printAbout()
  {
    console.printf(TimingDiagram.getAboutText());
  }

  private Registers connect()
  {
    final int port = options.getValue(optPort);
    try {
      console.printf("connecting to emulation server at port %d...%n", port);
      return new RegisterClient(console, port);
    } catch (final IOException e) {
      console.println("failed to connect to emulation server: " +
                      e.getMessage());
      console.println("check that emulation server runs at port address " +
                      port);
      System.exit(-1);
      throw new InternalError();
    }
  }

  private static Supplier<Boolean> createDelayFilter(final SDK sdk,
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

  private void createDiagram() throws IOException
  {
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

  public static void main(final String argv[])
  {
    final PrintStream console = System.out;
    try {
      new Main(System.out, argv);
    } catch (final IOException e) {
      console.println(e.getMessage());
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
