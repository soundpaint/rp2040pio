/*
 * @(#)DocsBuilder.java 1.00 21/04/18
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

import java.io.FileWriter;
import java.io.IOException;

/**
 * Automatically create Sphinx documentation for all Monitor commands,
 * using its integrated help functionality.
 */
public class DocsBuilder
{
  public static String fill(final char ch, final int length)
  {
    final StringBuilder s = new StringBuilder();
    s.setLength(length);
    for (int pos = 0; pos < length; pos++) {
      s.setCharAt(pos, ch);
    }
    return s.toString();
  }

  public static String csvEncode(final String raw)
  {
    final StringBuilder s = new StringBuilder();
    s.append("\"");
    boolean escaped = false;
    for (final char ch : raw.toCharArray()) {
      if (escaped) {
        s.append(ch);
        escaped = false;
      } else if (ch == '\\') {
        escaped = true;
      } else if (ch == '"') {
        s.append("\\\"");
      } else {
        s.append(ch);
      }
    }
    s.append("\"");
    return s.toString();
  }

  /**
   * Replaces all characters that could have special meaning for
   * Sphinx.
   */
  public static String createIdFromLabel(final String registersSetLabel)
  {
    return
      registersSetLabel
      .trim()
      .toLowerCase()
      .replace(" ", "_")
      .replace("\"", "")
      .replace("'", "")
      .replace("`", "")
      .replace(":", "");
  }

  public static final String leadinComment =
    ".. # WARNING: This sphinx documentation file was automatically%n" +
    ".. # created directly from documentation info in the source code.%n" +
    ".. # DO NOT CHANGE THIS FILE, since changes will be lost upon%n" +
    ".. # its next update.  Instead, change the info in the source code.%n" +
    ".. # This file was automatically created on:%n" +
    ".. # %s%n" +
    "%n";

  public static void writeToFile(final String rstFilePath,
                                 final String docs)
    throws IOException
  {
    try {
      final FileWriter writer = new FileWriter(rstFilePath);
      writer.write(docs);
      writer.close();
    } catch (final IOException e) {
      final String message =
        String.format("failed creating documentation file %s: %s%n",
                      rstFilePath, e.getMessage());
      throw new IOException(message, e);
    }
  }

  public static void main(final String argv[])
  {
    System.out.println("building monitor commands documentation...");
    MonitorCommandsDocsBuilder.main(argv);
    System.out.println("building registers documentation...");
    RegistersDocsBuilder.main(argv);
    System.out.println("building example scripts documentation...");
    ExampleScriptsDocsBuilder.main(argv);
    System.out.println("documentation successfully built");
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
