/*
 * @(#)Help.java 1.00 21/03/28
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
package org.soundpaint.rp2040pio.monitor.commands;

import java.io.PrintStream;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.monitor.CommandRegistry;

/**
 * Monitor command "help" prints a list of all available monitor
 * commands.
 */
public class Help extends Command
{
  private static final String fullName = "help";
  private static final String singleLineDescription =
    "list all available monitor commands";

  private final CommandRegistry commands;

  public Help(final PrintStream console, final CommandRegistry commands)
  {
    super(console, fullName, singleLineDescription);
    if (commands == null) {
      throw new NullPointerException("commands");
    }
    this.commands = commands;
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options)
  {
    console.println("Available commands:");
    for (final Command command : commands) {
      console.printf("  %-12s %s%n", command.getFullName(),
                     command.getSingleLineDescription());
    }
    console.println();
    console.printf(helpNotes);
    console.println();
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
