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
import org.soundpaint.rp2040pio.AddressSpace;
import org.soundpaint.rp2040pio.Emulator;
import org.soundpaint.rp2040pio.LocalAddressSpace;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
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
    s.append(String.format(".. index::%n"));
    s.append(String.format("   single: monitor command; %s%n", commandName));
    s.append(String.format("   single: %s%n", commandName));
    s.append(String.format("%n"));
    s.append(String.format(".. _%s-command-label:%n", commandName));
    s.append(String.format("%n"));
    s.append(String.format(commandName + "%n"));
    s.append(String.format(DocsBuilder.fill('-', commandName.length()) + "%n"));
    s.append(String.format("%n"));
    s.append(String.format("**Usage**%n"));
    s.append(String.format("^^^^^^^^^%n"));
    s.append(String.format("%n%s%n%n", command.getUsage()));
    s.append(String.format("**Description**%n"));
    s.append(String.format("^^^^^^^^^^^^^^^%n"));
    s.append(String.format("%n%s%n%n", command.getSingleLineDescription()));
    final String optionsHelp = command.getOptionsHelp();
    if (!optionsHelp.isEmpty()) {
      s.append(String.format("**Options**%n"));
      s.append(String.format("^^^^^^^^^^^%n"));
      s.append(String.format("%n%s%n", optionsHelp));
    }
    final String notes = command.getNotes();
    if ((notes != null) && !notes.isEmpty()) {
      s.append(String.format("**Notes**%n"));
      s.append(String.format("^^^^^^^^^%n"));
      s.append(String.format("%n" + notes + "%n%n"));
    }
    s.append(String.format(":ref:`Back to Overview <commands-overview>`%n"));
    s.append(String.format("%n"));
    return s.toString();
  }

  private String createCommandsOverview(final CommandRegistry commandRegistry)
  {
    final StringBuilder s = new StringBuilder();
    s.append(String.format(".. index::%n"));
    s.append(String.format("   single: monitor; commands overview%n"));
    s.append(String.format("%n"));
    s.append(String.format(".. _commands-overview:%n"));
    s.append(String.format("%n"));
    s.append(String.format("Overview%n"));
    s.append(String.format("--------%n"));
    s.append(String.format("%n"));
    s.append(String.format("The monitor supports all of the commands%n"));
    s.append(String.format("listed below.%n"));
    s.append(String.format("%n"));
    s.append(String.format(".. csv-table::%n"));
    s.append(String.format("   :header: Command, Short Description%n"));
    s.append(String.format("   :widths: 20, 80%n"));
    s.append(String.format("%n"));
    for (final Command command : commandRegistry) {
      final String commandName = command.getFullName();
      final String commandRef =
        String.format(":ref:`%s <%s-command-label>`", commandName, commandName);
      final String commandDescription = command.getSingleLineDescription();
      s.append(String.format("   %s,%s%n",
                             DocsBuilder.csvEncode(commandRef),
                             DocsBuilder.csvEncode(commandDescription)));
    }
    s.append(String.format("%n"));
    return s.toString();
  }

  private String createDocs() throws IOException
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
    final Emulator emulator = new Emulator(console);
    final AddressSpace memory = new LocalAddressSpace(emulator);
    final BufferedReader in =
      new BufferedReader(new InputStreamReader(System.in));
    final SDK sdk = new SDK(console, memory);
    final CommandRegistry commandRegistry =
      new CommandRegistry(console, in, sdk, null);
    s.append(createCommandsOverview(commandRegistry));
    for (final Command command : commandRegistry) {
      s.append(createCommandDocs(command));
    }
    emulator.terminate();
    return s.toString();
  }

  public MonitorCommandsDocsBuilder(final String rstFilePath)
    throws IOException
  {
    final String docs = createDocs();
    DocsBuilder.writeToFile(rstFilePath, docs);
  }

  public static void main(final String argv[])
  {
    try {
      new MonitorCommandsDocsBuilder("monitor-commands.rst");
    } catch (final IOException e) {
      final String message =
        String.format("failed creating monitor commands documentation: %s%n",
                      e.getMessage());
      System.err.printf(message);
      System.exit(-1);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
