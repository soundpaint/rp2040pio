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
import java.util.List;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.RegisterClient;
import org.soundpaint.rp2040pio.Registers;
import org.soundpaint.rp2040pio.monitor.commands.Help;
import org.soundpaint.rp2040pio.monitor.commands.Quit;
import org.soundpaint.rp2040pio.monitor.commands.Read;
import org.soundpaint.rp2040pio.monitor.commands.Trace;
import org.soundpaint.rp2040pio.monitor.commands.Unassemble;
import org.soundpaint.rp2040pio.sdk.GPIOSDK;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;
import org.soundpaint.rp2040pio.sdk.Program;
import org.soundpaint.rp2040pio.sdk.ProgramParser;

/**
 * Program Execution Monitor And Control
 */
public class Monitor
{
  private final BufferedReader in;
  private final PrintStream out;
  private final SDK sdk;
  private final PIOSDK pioSdk;
  private final GPIOSDK gpioSdk;
  private final CommandRegistry commands;
  private final Command quit;

  private Monitor()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public Monitor(final BufferedReader in, final PrintStream out)
    throws IOException
  {
    if (in == null) {
      throw new NullPointerException("in");
    }
    if (out == null) {
      throw new NullPointerException("out");
    }
    this.in = in;
    this.out = out;
    final Registers registers = new RegisterClient();
    sdk = new SDK(out, registers);
    pioSdk = sdk.getPIO0SDK();
    gpioSdk = sdk.getGPIOSDK();
    commands = new CommandRegistry();
    commands.add(new Help(out, commands));
    commands.add(quit = new Quit(out));
    commands.add(new Unassemble(out, pioSdk));
    commands.add(new Trace(out, sdk));
    commands.add(new Read(out, sdk));
    printAbout();
  }

  private void run() throws IOException
  {
    boolean quit = false;
    while (!quit) {
      out.print("> ");
      final String commandLine = in.readLine().trim();
      if (!commandLine.isEmpty()) {
        quit = parseAndExecute(commandLine);
      }
    }
    out.println("bye");
    System.exit(0);
  }

  private boolean parseAndExecute(final String commandLine)
  {
    final int spacePos = commandLine.indexOf(' ');
    final String commandToken =
      spacePos < 0 ?
      commandLine.trim() :
      commandLine.substring(0, spacePos).trim();
    final List<Command> matchingCommands = commands.lookup(commandToken);
    if ((matchingCommands == null) || (matchingCommands.size() == 0)) {
      out.println("unknown command: " + commandToken);
      return false;
    }
    if (matchingCommands.size() > 1) {
      out.println("ambiguous command: " + commandToken);
      out.println("possible resolutions: " + matchingCommands);
      return false;
    }
    final Command command = matchingCommands.get(0);
    final String args =
      spacePos < 0 ? "" : commandLine.substring(spacePos + 1).trim();
    final String[] argv = args.split(" ");
    final boolean executed = command.parseAndExecute(argv);
    return (command == quit) && executed;
  }

  private void printAbout()
  {
    out.println(Constants.getAbout());
    out.println();
    out.println("For a list of available commands, enter 'help'.");
  }

  public void addProgram(final String programResourcePath)
    throws IOException
  {
    final Program program = ProgramParser.parse(programResourcePath);
    pioSdk.addProgram(program);
  }

  public void runCycles(final int cycles) throws IOException
  {
    pioSdk.smSetEnabled(0, true);
    final int address =
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_PC);
    for (int i = 0; i < cycles; i++) {
      sdk.triggerCyclePhase0(true);
      sdk.triggerCyclePhase1(true);
      System.out.println(sdk.readAddress(address) + " " +
                         gpioSdk.asBitArrayDisplay());
    }
    pioSdk.smSetEnabled(0, false);
  }

  public static void main(final String argv[]) throws IOException
  {
    final BufferedReader in =
      new BufferedReader(new InputStreamReader(System.in));
    new Monitor(in, System.out).run();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
