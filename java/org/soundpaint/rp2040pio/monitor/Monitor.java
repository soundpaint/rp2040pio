/*
 * @(#)Monitor.java 1.00 21/02/02
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
package org.soundpaint.rp2040pio.monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.RegisterClient;
import org.soundpaint.rp2040pio.Registers;
import org.soundpaint.rp2040pio.monitor.commands.BreakPoints;
import org.soundpaint.rp2040pio.monitor.commands.Enable;
import org.soundpaint.rp2040pio.monitor.commands.Enter;
import org.soundpaint.rp2040pio.monitor.commands.Execute;
import org.soundpaint.rp2040pio.monitor.commands.Help;
import org.soundpaint.rp2040pio.monitor.commands.Label;
import org.soundpaint.rp2040pio.monitor.commands.Load;
import org.soundpaint.rp2040pio.monitor.commands.Quit;
import org.soundpaint.rp2040pio.monitor.commands.Read;
import org.soundpaint.rp2040pio.monitor.commands.Reset;
import org.soundpaint.rp2040pio.monitor.commands.SideSet;
import org.soundpaint.rp2040pio.monitor.commands.Trace;
import org.soundpaint.rp2040pio.monitor.commands.Unassemble;
import org.soundpaint.rp2040pio.monitor.commands.Unload;
import org.soundpaint.rp2040pio.monitor.commands.Version;
import org.soundpaint.rp2040pio.monitor.commands.Wait;
import org.soundpaint.rp2040pio.monitor.commands.Wrap;
import org.soundpaint.rp2040pio.monitor.commands.Write;
import org.soundpaint.rp2040pio.sdk.GPIOSDK;
import org.soundpaint.rp2040pio.sdk.Panic;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;
import org.soundpaint.rp2040pio.sdk.Program;
import org.soundpaint.rp2040pio.sdk.ProgramParser;

/**
 * Program Execution Monitor And Control
 */
public class Monitor
{
  private static final String PRG_NAME = "Monitor";
  private static final String PRG_ID_AND_VERSION =
    "Emulation Monitor Version 0.1 for " + Constants.getProgramAndVersion();
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
  private static final String panicInfo =
    "The system may be now in a corrupted state.%n" +
    "You may consider to fully reset the emulator with%n" +
    "the \"reset\" commmand, if unexpected behavior shows up.%n";
  private static final String commandHint =
    "For a list of available commands, enter 'help'.";

  private final BufferedReader in;
  private final PrintStream console;
  private final SDK sdk;
  private final PIOSDK pioSdk;
  private final GPIOSDK gpioSdk;
  private final CmdOptions options;
  private final CommandRegistry commands;
  private final Command quit;

  private Monitor()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public Monitor(final BufferedReader in, final PrintStream console,
                 final String[] argv)
  {
    if (in == null) {
      throw new NullPointerException("in");
    }
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.in = in;
    this.console = console;
    options = parseArgs(argv);
    printAbout();
    final Registers registers = connect();
    sdk = new SDK(console, registers);
    pioSdk = sdk.getPIO0SDK();
    gpioSdk = sdk.getGPIOSDK();
    commands = installCommands(console, in, sdk, quit = new Quit(console));
  }

  private CommandRegistry installCommands(final PrintStream console,
                                          final BufferedReader in,
                                          final SDK sdk,
                                          final Command quit)
  {
    final CommandRegistry commands = new CommandRegistry();
    commands.add(new BreakPoints(console, sdk));
    commands.add(new Enable(console, sdk));
    commands.add(new Enter(console, sdk, in));
    commands.add(new Execute(console, sdk));
    commands.add(new Help(console, commands));
    commands.add(new Label(console, sdk));
    commands.add(new Load(console, sdk));
    commands.add(quit);
    commands.add(new Read(console, sdk));
    commands.add(new Reset(console, sdk));
    commands.add(new SideSet(console, sdk));
    commands.add(new Trace(console, sdk));
    commands.add(new Unassemble(console, sdk));
    commands.add(new Unload(console, sdk));
    commands.add(new Version(console, sdk));
    commands.add(new Wait(console, sdk));
    commands.add(new Wrap(console, sdk));
    commands.add(new Write(console, sdk));
    return commands;
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
    console.println("Monitor Control Program");
    console.println(Constants.getAbout());
    console.println();
    console.println(commandHint);
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

  private void run()
  {
    try {
      boolean quit = false;
      while (!quit) {
        console.print("> ");
        final String commandLine = in.readLine();
        if (!commandLine.isEmpty()) {
          try {
            quit = parseAndExecute(commandLine);
          } catch (final Panic e) {
            console.println(e.getMessage());
            console.printf(panicInfo);
          }
        }
      }
      console.println("bye");
      System.exit(0);
    } catch (final IOException e) {
      console.println(e.getMessage());
      System.exit(-1);
    }
  }

  private boolean parseAndExecute(final String commandLine)
  {
    final int spacePos = commandLine.indexOf(' ');
    final String commandToken =
      (spacePos < 0 ? commandLine : commandLine.substring(0, spacePos)).trim();
    final List<Command> matchingCommands = commands.lookup(commandToken);
    if ((matchingCommands == null) || (matchingCommands.size() == 0)) {
      console.println("unknown command: " + commandToken);
      console.println(commandHint);
      return false;
    }
    if (matchingCommands.size() > 1) {
      console.println("ambiguous command: " + commandToken);
      console.println("possible resolutions: " + matchingCommands);
      return false;
    }
    final Command command = matchingCommands.get(0);
    final String args = spacePos < 0 ? "" : commandLine.substring(spacePos + 1);
    final boolean executed = command.parseAndExecute(args);
    return (command == quit) && executed;
  }

  public static void main(final String argv[])
  {
    final BufferedReader in =
      new BufferedReader(new InputStreamReader(System.in));
    new Monitor(in, System.out, argv).run();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
