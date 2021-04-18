/*
 * @(#)MonitorCommandsDocsBuilder.java 1.00 21/04/18
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
package org.soundpaint.rp2040pio.doctool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.RegisterClient;
import org.soundpaint.rp2040pio.Registers;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.monitor.CommandRegistry;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Automatically create Sphinx documentation for all Monitor commands,
 * using its integrated help functionality.
 */
public class MonitorCommandsDocsBuilder
{
  private MonitorCommandsDocsBuilder()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  private String createCommandDocs(final Command command)
  {
    final StringBuilder s = new StringBuilder();
    final String commandName = command.getFullName();
    s.append(String.format(commandName + "%n"));
    s.append(String.format(DocsBuilder.fill('-', commandName.length()) + "%n"));
    s.append(String.format("%n"));
    s.append(command.getHelp());
    s.append(String.format("%n"));
    return s.toString();
  }

  private String createDocs()
  {
    final StringBuilder s = new StringBuilder();
    s.append(String.format(DocsBuilder.leadinComment, Instant.now()));
    s.append(String.format("Monitor & Control Program Commands Reference%n"));
    s.append(String.format("============================================%n"));
    s.append(String.format("%n"));
    s.append(String.format("The *Monitor & Control Program*, or in short,%n"));
    s.append(String.format("just *monitor*, features a set of built-in%n"));
    s.append(String.format("commands with integrated, self-documenting%n"));
    s.append(String.format("help.  The following reference documentation%n"));
    s.append(String.format("has been compiled from these sources.%n"));
    s.append(String.format("%n"));
    final PrintStream console = System.out;
    final Registers registers;
    try {
      registers = new RegisterClient(console);
    } catch (final IOException e) {
      final String message =
        String.format("failed creating monitor commands documentation: %s%n",
                      e.getMessage());
      System.err.printf(message);
      System.exit(-1);
      throw new InternalError(message);
    }
    final BufferedReader in =
      new BufferedReader(new InputStreamReader(System.in));
    final SDK sdk = new SDK(console, registers);
    final CommandRegistry commandRegistry =
      new CommandRegistry(console, in, sdk);
    for (final Command command : commandRegistry) {
      s.append(createCommandDocs(command));
    }
    return s.toString();
  }

  public MonitorCommandsDocsBuilder(final String rstFilePath)
  {
    final String docs = createDocs();
    DocsBuilder.writeToFile(rstFilePath, docs);
  }

  public static void main(final String argv[])
  {
    new MonitorCommandsDocsBuilder("monitor-commands.rst");
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
