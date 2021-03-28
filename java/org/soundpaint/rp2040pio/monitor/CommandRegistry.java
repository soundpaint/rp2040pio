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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Used for command dispatching.
 */
public class CommandRegistry implements Iterable<Command>
{
  private final Set<Command> commands;
  private final HashMap<String, List<Command>> token2commands;

  public CommandRegistry()
  {
    commands =
      new TreeSet<Command>((cmd1, cmd2) ->
                           cmd1.getFullName().compareTo(cmd2.getFullName()));
    token2commands = new HashMap<String, List<Command>>();
  }

  public void add(final Command command)
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
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
