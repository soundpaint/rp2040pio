/*
 * @(#)ExampleScriptsDocsBuilder.java 1.00 21/06/10
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

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.soundpaint.rp2040pio.monitor.ScriptInfo;

/**
 * Automatically create Sphinx documentation for all example scripts,
 * using the script file name and extracting information from the
 * comments in the script file header.
 */
public class ExampleScriptsDocsBuilder
{
  private ExampleScriptsDocsBuilder()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  private void listExampleScript(final StringBuilder s,
                                 final ScriptInfo scriptInfo)
  {
    final String scriptId = scriptInfo.getScriptId();
    final String scriptName = scriptInfo.getScriptName();
    final String description = scriptInfo.getDescription();
    s.append(String.format(".. index::%n"));
    s.append(String.format("   single: script; %s%n", scriptId));
    s.append(String.format("   single: %s script%n", scriptId));
    s.append(String.format("%n"));
    s.append(String.format(".. _%s-example-script:%n", scriptId));
    s.append(String.format("%n"));
    final String scriptTitle =
      String.format("%s (``%s``)", scriptName, scriptId);
    s.append(String.format(scriptTitle + "%n"));
    s.append(String.format("%s%n", "^".repeat(scriptTitle.length())));
    s.append(String.format("%n"));
    s.append(description);
    s.append(String.format("%n"));
  }

  private void listExampleScriptGroup(final StringBuilder s,
                                      final Map<String, ScriptInfo>
                                      scriptsGroupInfo,
                                      final String groupName)
  {
    s.append(String.format(".. index::%n"));
    s.append(String.format("   single: script group; %s%n", groupName));
    s.append(String.format("   single: %s script group%n", groupName));
    s.append(String.format("%n"));
    s.append(String.format(".. _%s-example-script-group:%n", groupName));
    s.append(String.format("%n"));
    final String groupTitle = String.format("%s", groupName);
    s.append(String.format(groupTitle + "%n"));
    s.append(String.format("%s%n", "-".repeat(groupTitle.length())));
    s.append(String.format("%n"));
    for (final String scriptId : scriptsGroupInfo.keySet()) {
      final ScriptInfo scriptInfo = scriptsGroupInfo.get(scriptId);
      listExampleScript(s, scriptInfo);
    }
  }

  private void listExampleScripts(final Map<String, Map<String, ScriptInfo>>
                                  scriptsInfo,
                                  final StringBuilder s)
    throws IOException
  {
    Map<String, ScriptInfo> defaultScriptsGroupInfo = null;
    for (final String groupName : scriptsInfo.keySet()) {
      final Map<String, ScriptInfo> scriptsGroupInfo =
        scriptsInfo.get(groupName);
      if (groupName != ScriptInfo.DEFAULT_GROUP_NAME) {
        listExampleScriptGroup(s, scriptsGroupInfo, groupName);
      } else {
        // defer default group to end
        defaultScriptsGroupInfo = scriptsGroupInfo;
      }
    }
    if (defaultScriptsGroupInfo != null) {
      listExampleScriptGroup(s, defaultScriptsGroupInfo,
                             ScriptInfo.DEFAULT_GROUP_NAME);
    } else {
      throw new IOException("default script group not found");
    }
  }

  private String createDocs() throws IOException
  {
    final StringBuilder s = new StringBuilder();
    s.append(String.format(DocsBuilder.leadinComment, Instant.now()));
    s.append(String.format(".. index::%n"));
    s.append(String.format("   single: scripts; examples%n"));
    s.append(String.format("   single: example scripts%n"));
    s.append(String.format("   single: reference; example scripts%n"));
    s.append(String.format("%n"));
    s.append(String.format(".. _example-scripts-reference:%n"));
    s.append(String.format("%n"));
    s.append(String.format("Example Scripts Reference%n"));
    s.append(String.format("=========================%n"));
    s.append(String.format("%n"));
    s.append(String.format("The RP2040 PIO Emulator and client%n"));
    s.append(String.format("applications are bundled with a set of%n"));
    s.append(String.format("built-in example scripts.  These scripts%n"));
    s.append(String.format("loosely follow some of the PIO code examples%n"));
    s.append(String.format("in the RP2040 datasheet, but are adapted to%n"));
    s.append(String.format("run as Monitor scripts, using the Monitor%n"));
    s.append(String.format("specific syntax of commands.%n"));
    s.append(String.format("%n"));
    s.append(String.format("In the Monitor application, the set of these"));
    s.append(String.format("built-in example scripts can be listed with%n"));
    s.append(String.format("the Monitor command ``script --list``.%n"));
    s.append(String.format("%n"));
    final Map<String, Map<String, ScriptInfo>> scriptsInfo =
      ScriptInfo.createScriptsInfo();
    listExampleScripts(scriptsInfo, s);
    return s.toString();
  }

  public ExampleScriptsDocsBuilder(final String rstFilePath)
    throws IOException
  {
    final String docs = createDocs();
    DocsBuilder.writeToFile(rstFilePath, docs);
  }

  public static void main(final String argv[])
  {
    try {
      new ExampleScriptsDocsBuilder("example-scripts.rst");
    } catch (final IOException e) {
      final String message =
        String.format("failed creating example scripts documentation: %s%n",
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
