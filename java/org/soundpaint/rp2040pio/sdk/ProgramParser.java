/*
 * @(#)ProgramParser.java 1.00 21/02/16
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
package org.soundpaint.rp2040pio.sdk;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.function.Function;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.IOUtils;
import org.soundpaint.rp2040pio.ParseException;

public class ProgramParser implements Constants
{
  private static final String DIRECTIVE_PROGRAM = ".program";
  private static final String DIRECTIVE_ORIGIN = ".origin";
  private static final String DIRECTIVE_WRAP = ".wrap";
  private static final String DIRECTIVE_WRAP_TARGET = ".wrap_target";
  private static final String DIRECTIVE_SIDE_SET = ".side_set";
  private static final String DIRECTIVE_WORD = ".word";
  private static final String ARG_OPT = "opt";
  private static final String ARG_PINDIRS = "pindirs";

  private final String resourceId;
  private final BufferedReader reader;
  private final short[] instructions;
  private boolean firstInstructionParsed;
  private int lineIndex;
  private int address;
  private String id;
  private boolean idParsed;
  private int origin;
  private boolean originParsed;
  private int wrap;
  private boolean wrapParsed;
  private int wrapTarget;
  private boolean wrapTargetParsed;
  private int sideSetCount;
  private boolean sideSetCountParsed;
  private boolean sideSetOptParsed;
  private boolean sideSetPinDirsParsed;

  private ProgramParser()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  private ProgramParser(final String resourceId, final BufferedReader reader)
    throws IOException
  {
    if (resourceId == null) {
      throw new NullPointerException("resourceId");
    }
    if (reader == null) {
      throw new NullPointerException("reader");
    }
    this.resourceId = resourceId;
    this.reader = reader;
    address = -1;
    instructions = new short[MEMORY_SIZE];
    lineIndex = 0;
    address = 0;
    id = null;
    origin = -1;
    wrap = MEMORY_SIZE - 1;
    wrapTarget = 0;
    sideSetCount = 0;
  }

  private ParseException parseException(final String message)
  {
    return parseException(message, null);
  }

  private ParseException parseException(final String message,
                                        final Throwable cause)
  {
    return ParseException.create(message, resourceId, lineIndex, cause);
  }

  private int parseDecimalInt(final String decInt) throws ParseException
  {
    try {
      return Integer.parseInt(decInt);
    } catch (final NumberFormatException e) {
      throw parseException("expected decimal integer: " + decInt, e);
    }
  }

  private void parseInstruction(final String word,
                                final Function<String, Integer> wordParser)
    throws ParseException
  {
    if (address >= MEMORY_SIZE) {
      throw parseException("program too large: " +
                           "get more than " + MEMORY_SIZE + " words");
    }
    final int value;
    try {
      value = wordParser.apply(word);
    } catch (final NumberFormatException e) {
      throw parseException("invalid 16 bit word: " + word, e);
    }
    if (value < 0x0000) {
      throw parseException("instruction word < 0x0000: " + value);
    }
    if (value > 0xffff) {
      throw parseException("instruction word > 0xffff: " + value);
    }
    instructions[address++] = (short)value;
    firstInstructionParsed = true;
  }

  private void checkProgramId(final String id) throws ParseException
  {
    final String tail = id.replaceFirst("[_A-Za-z][_A-Za-z0-9]*", "");
    if (!tail.isEmpty()) {
      final int position = id.indexOf(tail.charAt(0));
      throw parseException(DIRECTIVE_PROGRAM + ": " +
                           "id contains invalid character at position " +
                           position + ": " +
                           String.format("%n  %s%n%" + (position + 2) + "s^",
                                         id, ""));
    }
  }

  private void parseProgramDrct(final String unparsed) throws ParseException
  {
    if (idParsed) {
      throw parseException(DIRECTIVE_PROGRAM + " already declared");
    }
    final String id = unparsed.trim();
    if (id.isEmpty()) {
      throw parseException(DIRECTIVE_PROGRAM + ": id expected");
    }
    checkProgramId(id);
    this.id = id;
    idParsed = true;
  }

  private void parseOriginDrct(final String unparsed) throws ParseException
  {
    if (originParsed) {
      throw parseException(DIRECTIVE_ORIGIN + " already declared");
    }
    final int origin = parseDecimalInt(unparsed);
    if (origin < -1) {
      throw parseException(DIRECTIVE_ORIGIN + ": origin < -1: " + origin);
    }
    if (origin > MEMORY_SIZE - 1) {
      throw parseException(DIRECTIVE_ORIGIN + ": " +
                           "origin > " + (MEMORY_SIZE - 1) + ": " + origin);
    }
    this.origin = origin;
    originParsed = true;
  }

  private void parseWrapDrct(final String unparsed) throws ParseException
  {
    if (wrapParsed) {
      throw parseException(DIRECTIVE_WRAP + " already declared");
    }
    final int wrap = parseDecimalInt(unparsed);
    if (wrap < 0) {
      throw parseException(DIRECTIVE_WRAP + ": wrap < 0: " + wrap);
    }
    if (wrap > MEMORY_SIZE - 1) {
      throw parseException(DIRECTIVE_WRAP + ": " +
                           "wrap > " + (MEMORY_SIZE - 1) + ": " + wrap);
    }
    this.wrap = wrap;
    wrapParsed = true;
  }

  private void parseWrapTargetDrct(final String unparsed) throws ParseException
  {
    if (wrapTargetParsed) {
      throw parseException(DIRECTIVE_WRAP_TARGET + " already declared");
    }
    final int wrapTarget = parseDecimalInt(unparsed);
    if (wrapTarget < 0) {
      throw parseException(DIRECTIVE_WRAP_TARGET + ": " +
                           "wrap_target < 0: " + wrapTarget);
    }
    if (wrapTarget > MEMORY_SIZE - 1) {
      throw parseException(DIRECTIVE_WRAP_TARGET + ": " +
                           "wrap_target > " + (MEMORY_SIZE - 1) + ": " +
                           wrapTarget);
    }
    this.wrapTarget = wrapTarget;
    wrapTargetParsed = true;
  }

  private void parseSideSetArg(final String arg) throws ParseException
  {
    if (ARG_OPT.equals(arg)) {
      if (sideSetOptParsed) {
        throw parseException(DIRECTIVE_SIDE_SET + " " + ARG_OPT +
                             " already declared");
      }
      sideSetOptParsed = true;
    }
    if (ARG_PINDIRS.equals(arg)) {
      if (sideSetPinDirsParsed) {
        throw parseException(DIRECTIVE_SIDE_SET + " " + ARG_PINDIRS +
                             " already declared");
      }
      sideSetPinDirsParsed = true;
    }
  }

  private void parseSideSetDrct(final String unparsed) throws ParseException
  {
    if (sideSetCountParsed) {
      throw parseException(DIRECTIVE_SIDE_SET + " already declared");
    }
    if (firstInstructionParsed) {
      throw parseException(DIRECTIVE_SIDE_SET + ": " +
                           "this directive is only valid before the " +
                           "first instruction");
    }
    final String[] tokens = unparsed.split("[\\p{javaWhitespace}]*");
    if (tokens.length == 0) {
      throw parseException(DIRECTIVE_SIDE_SET + ": <count> expected");
    }
    final int sideSetCount = parseDecimalInt(tokens[0]);
    if (sideSetCount < 0) {
      throw parseException(DIRECTIVE_SIDE_SET + ": " +
                           "side_set count < 0: " + sideSetCount);
    }
    if (sideSetCount > 5) {
      throw parseException(DIRECTIVE_SIDE_SET + ": " +
                           "side_set count > 5: " + sideSetCount);
    }
    if (tokens.length >= 2) {
      parseSideSetArg(tokens[1].trim());
    }
    if (tokens.length >= 3) {
      parseSideSetArg(tokens[2].trim());
    }
    if (tokens.length >= 4) {
      throw parseException(DIRECTIVE_SIDE_SET + ": " +
                           "unexpected extra argument: " + tokens[3]);
    }
    if (sideSetOptParsed && ((sideSetCount > 4))) {
      throw parseException("max. side-set count is 4, if opt is set");
    }
    this.sideSetCount = sideSetCount;
    sideSetCountParsed = true;
  }

  private void parseDirective(final String directive) throws ParseException
  {
    if (directive.startsWith(DIRECTIVE_PROGRAM)) {
      parseProgramDrct(directive.substring(DIRECTIVE_PROGRAM.length()));
    } else if (directive.startsWith(DIRECTIVE_ORIGIN)) {
      parseOriginDrct(directive.substring(DIRECTIVE_ORIGIN.length()));
    } else if (directive.startsWith(DIRECTIVE_WRAP)) {
      parseWrapDrct(directive.substring(DIRECTIVE_WRAP.length()));
    } else if (directive.startsWith(DIRECTIVE_WRAP_TARGET)) {
      parseWrapTargetDrct(directive.substring(DIRECTIVE_WRAP_TARGET.length()));
    } else if (directive.startsWith(DIRECTIVE_SIDE_SET)) {
      parseSideSetDrct(directive.substring(DIRECTIVE_SIDE_SET.length()));
    } else if (directive.startsWith(DIRECTIVE_WORD)) {
      parseInstruction(directive.substring(DIRECTIVE_WORD.length()),
                       (unparsed) -> Integer.parseInt(unparsed));
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
        parseInstruction(trimmedLine,
                         (unparsed) -> Integer.parseInt(unparsed, 16));
      } else {
        // ignore empty lines
      }
    }
    reader.close();
    if (address == 0) {
      throw parseException("program does not contain any instruction");
    }
    if (!wrapTargetParsed) {
      wrapTarget = origin >= 0 ? origin : 0;
    }
    if (!wrapParsed) {
      wrapTarget =
        origin >= 0 ? ((origin + address - 1) % MEMORY_SIZE) : address - 1;
    }
    final short[] trimmedInstructions = new short[address];
    System.arraycopy(instructions, 0, trimmedInstructions, 0, address);
    final Program program =
      new Program(id, origin, wrap, wrapTarget, sideSetCount,
                  sideSetOptParsed, sideSetPinDirsParsed, trimmedInstructions);
    final String programDisplay =
      id != null ? "program \"" + id + "\"" : "unnamed program";
    final String message =
      "parsed " + programDisplay + " with " +
      address + " PIO SM instructions" +
      (origin >= 0 ? " @" + origin : "");
    System.out.println(message);
    return program;
  }

  public static Program parse(final String resourceId,
                              final BufferedReader reader)
    throws IOException
  {
    final ProgramParser parser = new ProgramParser(resourceId, reader);
    return parser.parse();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
