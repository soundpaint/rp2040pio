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
import java.util.List;
import java.util.stream.Collectors;
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
    "readability of the specified file." +
    "Some built-in example scripts are provided that can be listed with%n" +
    "the \"-l\" option.  If the specified path matches one of the example%n" +
    "scripts, it is executed.  Otherwise, the path is interpreted as%n" +
    "ordinary file system path.";

  private final CommandRegistry commands;

  private static final CmdOptions.StringOptionDeclaration optFile =
    CmdOptions.createStringOption("PATH", false, 'f', "file", null,
                                  "path of hex dump file to load");
  private static final CmdOptions.FlagOptionDeclaration optList =
    CmdOptions.createFlagOption(false, 'l', "list", CmdOptions.Flag.OFF,
                                "list available example script files");
  private static final CmdOptions.FlagOptionDeclaration optExecute =
    CmdOptions.createFlagOption(false, 'e', "execute", CmdOptions.Flag.OFF,
                                "actually execute the script rather than " +
                                "just checking for existence and readability");

  public Script(final PrintStream console, final CommandRegistry commands)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optList, optFile, optExecute });
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

  @Override
  protected void checkValidity(final CmdOptions options)
    throws CmdOptions.ParseException
  {
    final String optFileValue = options.getValue(optFile);
    final boolean optListValue =
      options.getValue(optList) == CmdOptions.Flag.ON;
    if (optListValue && (optFileValue != null)) {
      throw new CmdOptions.
        ParseException("either option \"-l\" or option \"-f\" may be " +
                       "specified, but not both at the same time");
    }
    if (options.getValue(optHelp) != CmdOptions.Flag.ON) {
      if (!optListValue && (optFileValue == null)) {
        throw new CmdOptions.
          ParseException("at least one of options \"-l\" and \"-f\" " +
                         "must be specified");
      }
    }
  }

  private void listExampleScripts() throws IOException
  {
    final List<String> examples =
      IOUtils.list("examples").stream().
      filter((name) -> name.endsWith(".mon")).collect(Collectors.toList());
    for (final String example : examples) {
      console.printf("(pio*:sm*) /examples/%s%n", example);
    }
  }

  private boolean executeScript(final LineNumberReader in,
                                final String resourcePath,
                                final boolean dryRun,
                                final boolean localEcho,
                                final String prompt)
    throws IOException
  {
    final String action = dryRun ? "dry-running" : "running";
    console.printf("(pio*:sm*) %s script from resource %s%n",
                   action, resourcePath);
    while (true) {
      console.print(prompt);
      try {
        final String line = in.readLine();
        if (line == null) break;
        if (localEcho) console.println(line);
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
    final String optFileValue = options.getValue(optFile);
    final boolean optListValue =
      options.getValue(optList) == CmdOptions.Flag.ON;
    if (optListValue) {
      listExampleScripts();
    }
    if (optFileValue != null) {
      final LineNumberReader reader =
        IOUtils.getReaderForResourcePath(optFileValue);
      final boolean dryRun = options.getValue(optExecute) != CmdOptions.Flag.ON;
      return executeScript(reader, optFileValue, dryRun, true, "script> ");
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
