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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.soundpaint.rp2040pio.Emulator;
import org.soundpaint.rp2040pio.IOUtils;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.Registers;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.monitor.CommandRegistry;
import org.soundpaint.rp2040pio.sdk.LocalRegisters;
import org.soundpaint.rp2040pio.sdk.SDK;

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
                                 final LineNumberReader reader,
                                 final String scriptId)
    throws IOException
  {
    s.append(String.format(".. index::%n"));
    s.append(String.format("   single: script; %s%n", scriptId));
    s.append(String.format("   single: %s script%n", scriptId));
    s.append(String.format("%n"));
    s.append(String.format(".. _%s-example-script:%n", scriptId));
    s.append(String.format("%n"));
    s.append(String.format(scriptId + "%n"));
    s.append(String.format("%s%n", "-".repeat(scriptId.length())));
    s.append(String.format("%n"));
    while (true) {
      final String line = reader.readLine();
      if (line == null) break;
      if (!(line.startsWith("#"))) break;
      s.append(String.format("%s%n", line.substring(1).trim()));
    }
    s.append(String.format("%n"));
  }

  private void listExampleScript(final StringBuilder s, final String scriptId)
    throws IOException
  {
    final String resourcePath = String.format("/examples/%s.mon", scriptId);
    final LineNumberReader reader =
      IOUtils.getReaderForResourcePath(resourcePath);
    listExampleScript(s, reader, scriptId);
  }

  private void listExampleScripts(final StringBuilder s) throws IOException
  {
    final String suffix = ".mon";
    final List<String> examples =
      IOUtils.list("examples").stream().
      filter(t -> t.endsWith(suffix)).
      map(t -> { return t.substring(0, t.length() - suffix.length()); }).
      collect(Collectors.toList());
    for (final String example : examples) {
      listExampleScript(s, example);
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
    listExampleScripts(s);
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
