/*
 * @(#)Program.java 1.00 21/02/06
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
package org.soundpaint.rp2040pio;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class ProgramParser
{
  private static final String DIRECTIVE_ORIGIN = ".origin";

  private final String resourcePath;
  private final BufferedReader reader;
  private final short[] code;
  private int lineIndex;
  private int address;
  private int origin;

  private static class ParseException extends IOException
  {
    private static final long serialVersionUID = 378126510396027655L;

    private ParseException()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private ParseException(final String message)
    {
      super(message);
    }

    private ParseException(final String message, final Throwable cause)
    {
      super(message, cause);
    }
  }

  private ProgramParser()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  private ProgramParser(final String resourcePath)
    throws IOException
  {
    if (resourcePath == null) {
      throw new NullPointerException("resourcePath");
    }
    this.resourcePath = resourcePath;
    final InputStream in = Main.class.getResourceAsStream(resourcePath);
    if (in == null) {
      throw new IOException("failed loading code: resource not found: " +
                            resourcePath);
    }
    reader = new BufferedReader(new InputStreamReader(in));
    code = new short[Memory.SIZE];
    lineIndex = 0;
    address = 0;
    origin = -1;
  }

  private ParseException parseException(final String message)
  {
    return parseException(message, null);
  }

  private ParseException parseException(final String message,
                                        final Throwable cause)
  {
    final String fullMessage =
      String.format("parse exception in %s, line %d: %s",
                    resourcePath, lineIndex, message);
    return cause != null ?
      new ParseException(fullMessage, cause) :
      new ParseException(fullMessage);
  }

  private int parseDecimalInt(final String decInt) throws ParseException
  {
    try {
      final int value = Integer.parseInt(decInt);
      return value;
    } catch (final NumberFormatException e) {
      throw parseException("expected decimal integer: " + decInt, e);
    }
  }

  private int parseHexInt(final String hexInt) throws ParseException
  {
    try {
      final int value = Integer.parseInt(hexInt, 16);
      return value;
    } catch (final NumberFormatException e) {
      throw parseException("expected hexadecimal integer: " + hexInt, e);
    }
  }

  private void parseCodeWord(final String line) throws ParseException
  {
    if (address >= Memory.SIZE) {
      throw parseException("failed loading code: size too large: " +
                           "get more than " + Memory.SIZE + " words");
    }
    final int value = parseHexInt(line);
    if (value < 0x0000) {
      throw parseException("code word < 0x0000:" + value);
    }
    if (value > 0xffff) {
      throw parseException("code word > 0xffff:" + value);
    }
    code[address++] = (short)value;
  }

  private void parseOrigin(final String unparsed) throws ParseException
  {
    final int origin = parseDecimalInt(unparsed);
    if (origin < -1) {
      throw parseException(DIRECTIVE_ORIGIN + ": origin < -1: " + origin);
    }
    if (origin > Memory.SIZE - 1) {
      throw parseException(DIRECTIVE_ORIGIN +
                           ": origin > " + (Memory.SIZE - 1) + ": " + origin);
    }
    this.origin = origin;
  }

  private void parseDirective(final String directive) throws ParseException
  {
    if (directive.startsWith(DIRECTIVE_ORIGIN)) {
      parseOrigin(directive.substring(DIRECTIVE_ORIGIN.length()));
    } else {
      throw parseException("unsupported directive: " + directive);
    }
  }

  private void parseComment(final String comment) throws ParseException
  {
    if (comment.startsWith(";")) {
      // ignore user comments
      return;
    } else {
      parseDirective(comment);
    }
  }

  private Program parse() throws IOException
  {
    String line;
    while ((line = reader.readLine()) != null) {
      lineIndex++;
      final String trimmedLine = line.trim();
      if (trimmedLine.startsWith("#")) {
        parseComment(trimmedLine.substring(1).trim());
      } else if (!trimmedLine.isEmpty()) {
        parseCodeWord(trimmedLine);
      } else {
        // ignore empty lines
      }
    }
    reader.close();
    final Program program = new Program(code, origin);
    final String message =
      "loaded " + address + " PIO SM instructions" +
      (origin >= 0 ? " @" + origin : "");
    System.out.println(message);
    return program;
  }

  public static Program parse(final String resourcePath) throws IOException
  {
    final ProgramParser parser = new ProgramParser(resourcePath);
    return parser.parse();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
