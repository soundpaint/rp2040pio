/*
 * @(#)CommandRegistry.java 1.00 21/03/28
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
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.ParseException;
import org.soundpaint.rp2040pio.monitor.commands.BreakPoints;
import org.soundpaint.rp2040pio.monitor.commands.Clear;
import org.soundpaint.rp2040pio.monitor.commands.Enable;
import org.soundpaint.rp2040pio.monitor.commands.Enter;
import org.soundpaint.rp2040pio.monitor.commands.Execute;
import org.soundpaint.rp2040pio.monitor.commands.Fifo;
import org.soundpaint.rp2040pio.monitor.commands.Gpio;
import org.soundpaint.rp2040pio.monitor.commands.Help;
import org.soundpaint.rp2040pio.monitor.commands.Label;
import org.soundpaint.rp2040pio.monitor.commands.Load;
import org.soundpaint.rp2040pio.monitor.commands.Quit;
import org.soundpaint.rp2040pio.monitor.commands.Read;
import org.soundpaint.rp2040pio.monitor.commands.Registers;
import org.soundpaint.rp2040pio.monitor.commands.Reset;
import org.soundpaint.rp2040pio.monitor.commands.Save;
import org.soundpaint.rp2040pio.monitor.commands.Script;
import org.soundpaint.rp2040pio.monitor.commands.SideSet;
import org.soundpaint.rp2040pio.monitor.commands.Trace;
import org.soundpaint.rp2040pio.monitor.commands.Unassemble;
import org.soundpaint.rp2040pio.monitor.commands.Unload;
import org.soundpaint.rp2040pio.monitor.commands.Version;
import org.soundpaint.rp2040pio.monitor.commands.Wait;
import org.soundpaint.rp2040pio.monitor.commands.Wrap;
import org.soundpaint.rp2040pio.monitor.commands.Write;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Used for command dispatching.
 */
public class CommandRegistry implements Iterable<Command>
{
  private final PrintStream console;
  private final Set<Command> commands;
  private final HashMap<String, List<Command>> token2commands;
  private final Command quit;

  private CommandRegistry()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public CommandRegistry(final PrintStream console,
                         final BufferedReader in,
                         final SDK sdk)
  {
    if (console == null) {
      throw new NullPointerException("console");
    }
    this.console = console;
    commands =
      new TreeSet<Command>((cmd1, cmd2) ->
                           cmd1.getFullName().compareTo(cmd2.getFullName()));
    token2commands = new HashMap<String, List<Command>>();
    quit = installCommands(in, sdk);
  }

  private Quit installCommands(final BufferedReader in, final SDK sdk)
  {
    final Quit quit;
    add(new BreakPoints(console, sdk));
    add(new Clear(console));
    add(new Enable(console, sdk));
    add(new Enter(console, sdk, in));
    add(new Execute(console, sdk));
    add(new Fifo(console, sdk));
    add(new Gpio(console, sdk));
    add(new Help(console, this));
    add(new Label(console, sdk));
    add(new Load(console, sdk));
    add(quit = new Quit(console));
    add(new Read(console, sdk));
    add(new Registers(console, sdk));
    add(new Reset(console, sdk));
    add(new Save(console, sdk));
    add(new Script(console, this));
    add(new SideSet(console, sdk));
    add(new Trace(console, sdk));
    add(new Unassemble(console, sdk));
    add(new Unload(console, sdk));
    add(new Version(console, sdk));
    add(new Wait(console, sdk));
    add(new Wrap(console, sdk));
    add(new Write(console, sdk));
    return quit;
  }

  private void add(final Command command)
  {
    if (commands.contains(command)) {
      throw new IllegalArgumentException("command already registered");
    }
    final String fullName = command.getFullName();
    for (final Command otherCommand : commands) {
      final String otherFullName = otherCommand.getFullName();
      if (fullName.startsWith(otherFullName)) {
        throw new IllegalArgumentException("name clash with other command: " +
                                           otherCommand);
      }
      if (otherFullName.startsWith(fullName)) {
        throw new IllegalArgumentException("name clash with other command: " +
                                           otherCommand);
      }
    }
    commands.add(command);
    updateTokenHashes(command);
  }

  private void updateTokenHashes(final Command command)
  {
    final String fullName = command.getFullName();
    for (int i = 1; i <= fullName.length(); i++) {
      final String partialName = fullName.substring(0, i);
      final List<Command> commands;
      if (token2commands.containsKey(partialName)) {
        commands = token2commands.get(partialName);
      } else {
        commands = new ArrayList<Command>();
        token2commands.put(partialName, commands);
      }
      commands.add(command);
    }
  }

  public void remove(final Command command)
  {
    if (!commands.contains(command)) {
      throw new IllegalArgumentException("no such command registered");
    }
    commands.remove(command);
    final String fullName = command.getFullName();
    for (int i = 1; i < fullName.length(); i++) {
      final String partialName = fullName.substring(0, i);
      final List<Command> commands = token2commands.get(partialName);
      commands.remove(command);
    }
  }

  public List<Command> lookup(final String partialName)
  {
    if (token2commands.containsKey(partialName)) {
      return token2commands.get(partialName);
    }
    return null;
  }

  @Override
  public Iterator<Command> iterator()
  {
    return commands.iterator();
  }

  private ParseException createCommandParseException(final Command command,
                                                     final Exception e)
  {
    final String helpNotes = String.format(Command.helpNotes);
    final String message =
      String.format("%s:%n%s%n%s", command, e.getMessage(), helpNotes);
    return new ParseException(message);
  }

  /**
   * @return &lt;code&gt;true&lt;/code&gt;, if and only if command
   * "quit" is to be successfully be executed (the command itself, not
   * showing help with "-h" option).
   * @throws &lt;code&gt;ParseException&lt;/code&gt;, if the command
   * can not be executed due to a parse exception.  In dry-run mode,
   * checking for this exception can be used to check the syntax of a
   * command without actually executing it.
   */
  public boolean parseAndExecute(final String commandLine, boolean dryRun)
    throws ParseException
  {
    final String[] argv;
    try {
      argv = CmdOptions.splitArgs(commandLine);
    } catch (final CmdOptions.ParseException e) {
      final String message =
        String.format("failed tokenizing command line: %s%n", e.getMessage());
      throw new ParseException(message, e);
    }
    if (argv.length == 0) {
      return false;
    }
    final String commandToken = argv[0];
    final List<Command> matchingCommands = lookup(commandToken);
    if ((matchingCommands == null) || (matchingCommands.size() == 0)) {
      final String message =
        String.format("unknown command: %s%n%s%n",
                      commandToken, String.format(Command.commandHint));
      throw new ParseException(message);
    }
    if (matchingCommands.size() > 1) {
      final String message =
        String.format("ambiguous command: %s%npossible resolutions: %s%n",
                      commandToken, matchingCommands);
      throw new ParseException(message);
    }
    final Command command = matchingCommands.get(0);
    try {
      command.parse(argv);
    } catch (final CmdOptions.ParseException e) {
      throw createCommandParseException(command, e);
    }
    if (dryRun) {
      return false;
    }
    final boolean executed;
    try {
      executed = command.execute();
    } catch (final IOException e) {
      throw createCommandParseException(command, e);
    }
    return (command == quit) && executed;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
