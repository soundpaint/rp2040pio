/*
 * @(#)MonitorUtils.java 1.00 21/04/14
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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;
import org.soundpaint.rp2040pio.IOUtils;

/**
 * Utility methods that are used by multiple monitor commands.
 */
public class MonitorUtils
{
  public static boolean listExampleHexDumps(final PrintStream console)
    throws IOException
  {
    final String suffix = ".hex";
    final List<String> examples =
      IOUtils.list("examples").stream().
      filter(s -> s.endsWith(suffix)).
      map(s -> { return s.substring(0, s.length() - suffix.length()); }).
      collect(Collectors.toList());
    for (final String example : examples) {
      console.printf("(pio*:sm*) %s%n", example);
    }
    return true;
  }

  public static boolean showExampleHexDump(final PrintStream console,
                                           final String hexDumpId)
    throws IOException
  {
    final String resourcePath = String.format("/examples/%s.hex", hexDumpId);
    final LineNumberReader reader =
      IOUtils.getReaderForResourcePath(resourcePath);
    console.printf("(pio*:sm*) [hex dump %s]%n", hexDumpId);
    while (true) {
      final String line = reader.readLine();
      if (line == null) break;
      console.printf("(pio*:sm*) %3d: %s%n", reader.getLineNumber(), line);
    }
    console.printf("(pio*:sm*) [end of hex dump %s]%n", hexDumpId);
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
