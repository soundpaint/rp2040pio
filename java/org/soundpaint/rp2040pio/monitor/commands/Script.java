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
import java.util.Map;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.IOUtils;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.monitor.CommandRegistry;
import org.soundpaint.rp2040pio.monitor.ScriptInfo;
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
    "By convention, monitor scripts files have \".mon\" file name suffix.%n" +
    "They contain commands to be executed verbatim as if they were%n" +
    "manually entered in exactly the same way." +
    "%n" +
    "For safety reasons as well as for providing for future extensions,%n" +
    "an additional flag \"+d\" is by default set to dry-run the script.%n" +
    "To actually run the script, you need to explicitly spcify \"-d\" to%n" +
    "override dry-run mode.%n" +
    "%n" +
    "Some built-in example scripts are available that can be listed with%n" +
    "the \"-l\" option.  To execute a built-in script, use the \"-e\"%n" +
    "option and pass to this option the script's name as shown in the%n" +
    "list of available built-in scripts.%n" +
    "For user-provided script files, use the \"-f\" option to specify the%n" +
    "file path of the script, including the \".mon\" file name suffix.";

  private final CommandRegistry commands;

  private static final CmdOptions.FlagOptionDeclaration optList =
    CmdOptions.createFlagOption(false, 'l', "list", CmdOptions.Flag.OFF,
                                "list names of available example scripts");
  private static final CmdOptions.StringOptionDeclaration optShow =
    CmdOptions.createStringOption("NAME", false, 's', "show", null,
                                  "name of built-in example script to show");
  private static final CmdOptions.StringOptionDeclaration optExample =
    CmdOptions.createStringOption("NAME", false, 'e', "example", null,
                                  "name of built-in example script to execute");
  private static final CmdOptions.StringOptionDeclaration optFile =
    CmdOptions.createStringOption("PATH", false, 'f', "file", null,
                                  "path of monitor script file to execute");
  private static final CmdOptions.BooleanOptionDeclaration optDryRun =
    CmdOptions.createBooleanOption(false, 'd', "dry-run", true,
                                   "dry-run the script commands rather than " +
                                   "actually executing them");

  public Script(final PrintStream console, final CommandRegistry commands)
  {
    super(console, fullName, singleLineDescription, notes,
          new CmdOptions.OptionDeclaration<?>[]
          { optList, optShow, optExample, optFile, optDryRun });
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
    final boolean optListValue =
      options.getValue(optList) == CmdOptions.Flag.ON;
    final String optShowValue = options.getValue(optShow);
    final String optExampleValue = options.getValue(optExample);
    final String optFileValue = options.getValue(optFile);
    int count = 0;
    if (optListValue) count++;
    if (optShowValue != null) count++;
    if (optExampleValue != null) count++;
    if (optFileValue != null) count++;
    if (options.getValue(optHelp) != CmdOptions.Flag.ON) {
      if (count == 0) {
        throw new CmdOptions.
          ParseException("at least one of options \"-l\", \"-s\", \"-e\" " +
                         "and \"-f\" must be specified");
      }
    }
    if (count > 1) {
      throw new CmdOptions.
        ParseException("at most one of options \"-l\", \"-s\", \"-e\" " +
                       "and \"-f\" may be specified at the same time");
    }
  }




  private void listExampleScriptGroup(final Map<String, ScriptInfo>
                                      scriptsGroupInfo,
                                      final String groupName)
  {
    final String groupTitle = String.format("%s:", groupName);
    console.printf("(pio*:sm*) %s%n", groupTitle);
    for (final String scriptId : scriptsGroupInfo.keySet()) {
      final ScriptInfo scriptInfo = scriptsGroupInfo.get(scriptId);
      console.printf("(pio*:sm*)     %s%n", scriptInfo.getScriptId());
    }
  }

  private void listExampleScripts() throws IOException
  {
    final Map<String, Map<String, ScriptInfo>> scriptsInfo =
      ScriptInfo.createScriptsInfo();
    Map<String, ScriptInfo> defaultScriptsGroupInfo = null;
    for (final String groupName : scriptsInfo.keySet()) {
      final Map<String, ScriptInfo> scriptsGroupInfo =
        scriptsInfo.get(groupName);
      if (groupName != ScriptInfo.DEFAULT_GROUP_NAME) {
        listExampleScriptGroup(scriptsGroupInfo, groupName);
      } else {
        // defer default group to end
        defaultScriptsGroupInfo = scriptsGroupInfo;
      }
    }
    if (defaultScriptsGroupInfo != null) {
      listExampleScriptGroup(defaultScriptsGroupInfo,
                             ScriptInfo.DEFAULT_GROUP_NAME);
    } else {
      throw new IOException("default script group not found");
    }
  }

  private boolean showScript(final LineNumberReader in, final String scriptId)
    throws IOException
  {
    console.printf("(pio*:sm*) [script %s]%n", scriptId);
    while (true) {
      final String line = in.readLine();
      if (line == null) break;
      console.printf("(pio*:sm*) %3d: %s%n", in.getLineNumber(), line);
    }
    console.printf("(pio*:sm*) [end of script %s]%n", scriptId);
    return true;
  }

  private int executeScript(final LineNumberReader in,
                            final String scriptId,
                            final boolean dryRun,
                            final boolean localEcho,
                            final String prompt)
  {
    final String action = dryRun ? "dry-running" : "running";
    console.printf("(pio*:sm*) %s script %s%n", action, scriptId);
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
        return -1;
      }
    }
    return 0;
  }

  private boolean executeScript(final LineNumberReader in,
                                final String scriptId, final boolean dryRun)
    throws IOException
  {
    final int exitStatus =
      executeScript(in, scriptId, dryRun, true, "script> ");
    console.printf("(pio*:sm*) script %s exited with status %d%n",
                   scriptId, exitStatus);
    return true;
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    final boolean optListValue =
      options.getValue(optList) == CmdOptions.Flag.ON;
    final String optShowValue = options.getValue(optShow);
    final String optExampleValue = options.getValue(optExample);
    final String optFileValue = options.getValue(optFile);
    final boolean dryRun = options.getValue(optDryRun);
    if (optListValue) {
      listExampleScripts();
    } else if (optShowValue != null) {
      final String resourcePath =
        String.format("/examples/%s.mon", optShowValue);
      final LineNumberReader reader =
        IOUtils.getReaderForResourcePath(resourcePath);
      return showScript(reader, optShowValue);
    } else if (optExampleValue != null) {
      final String resourcePath =
        String.format("/examples/%s.mon", optExampleValue);
      final LineNumberReader reader =
        IOUtils.getReaderForResourcePath(resourcePath);
      return executeScript(reader, optExampleValue, dryRun);
    } else if (optFileValue != null) {
      final LineNumberReader reader =
        IOUtils.getReaderForResourcePath(optFileValue);
      return executeScript(reader, optFileValue, dryRun);
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
