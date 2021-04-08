/*
 * @(#)Script.java 1.00 21/03/31
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.IOUtils;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.monitor.CommandRegistry;
import org.soundpaint.rp2040pio.sdk.Panic;

/**
 * Monitor command "script" loads a monitor script from a file and
 * executes it.
 */
public class Script extends Command
{
  private static final String fullName = "script";
  private static final String singleLineDescription =
    "load monitor script from file and execute it";
  private static final String notes =
    "By convention, monitor scripts have \".mon\" file name suffix.%n" +
    "They contain commands to be executed verbatim as if they were%n" +
    "manually entered in exactly the same way." +
    "For safety reasons as well as for providing for future extensions,%n" +
    "an additional flag \"-e\" must be specified to execute the script.%n" +
    "Without the \"-e\" flag, the command just checks for existence and%n" +
    "readability of the specified file.";
  private static final String defaultPath = "/examples/squarewave.mon";

  private final CommandRegistry commands;

  private static final CmdOptions.StringOptionDeclaration optFile =
    CmdOptions.createStringOption("PATH", false, 'f', "file", defaultPath,
                                  "path of hex dump file to load");
  private static final CmdOptions.FlagOptionDeclaration optExecute =
    CmdOptions.createFlagOption(false, 'e', "execute", CmdOptions.Flag.OFF,
                                "actually execute the script rather than " +
                                "just checking for existence and readability");

  public Script(final PrintStream console, final CommandRegistry commands)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[] { optFile, optExecute });
    if (commands == null) {
      throw new NullPointerException("commands");
    }
    this.commands = commands;
  }

  private LineNumberReader getReaderForResourcePath(final String resourcePath)
    throws IOException
  {
    final InputStream in = IOUtils.getStreamForResourcePath(resourcePath);
    return new LineNumberReader(new InputStreamReader(in));
  }

  private boolean executeScript(final LineNumberReader in,
                                final String resourcePath,
                                final boolean dryRun)
    throws IOException
  {
    final String action = dryRun ? "dry-running" : "running";
    console.printf("(pio*:sm*) %s script from resource %s%n",
                   action, resourcePath);
    while (true) {
      console.print("script> ");
      try {
        final String line = in.readLine();
        if (line == null) break;
        console.printf("%s%n", line);
        if (commands.parseAndExecute(line, dryRun)) break;
      } catch (final Panic | IOException e) {
        console.println(e.getMessage());
        if (e instanceof Panic) {
          console.printf(Command.panicNotes);
          console.println();
        }
      }
    }
    final int exitStatus = 0;
    console.printf("(pio*:sm*) script exited with status %d%n", exitStatus);
    return true;
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final String resourcePath = options.getValue(optFile);
    if (resourcePath != null) {
      final LineNumberReader reader =
        IOUtils.getReaderForResourcePath(resourcePath);
      final boolean dryRun = options.getValue(optExecute) != CmdOptions.Flag.ON;
      return executeScript(reader, resourcePath, dryRun);
    }
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
