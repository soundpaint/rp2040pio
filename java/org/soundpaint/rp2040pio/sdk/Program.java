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
package org.soundpaint.rp2040pio.sdk;

import java.util.Arrays;
import java.io.InputStream;
import org.soundpaint.rp2040pio.Constants;

/**
 * This class is for compatibility with the Raspberry Pi Pico SDK.
 * It represents C struct pio_program_t and all functions that
 * manipulate this struct.
 */
public class Program implements Constants
{
  private final String id;
  private final int origin;
  private final int wrap;
  private final int wrapTarget;
  private final int sideSetCount;
  private final boolean sideSetOpt;
  private final boolean sideSetPinDirs;
  private final short[] instructions;
  private final int allocationMask;

  private Program()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  /**
   * @param origin Either the fixed origin of the program (values
   * 0…31), or -1, if the program is relocatable, such that it does
   * not matter where it will be loaded into memory.
   * @param instructions Array with all instructions of the program.
   * If the array is null or the length of the array is greater than
   * 32, an exception is thrown.
   *
   * The program id is optional.  Set to
   * &lt;code&gt;null&lt;/code&gt;, if program id is undefined.
   * However, if defined, it must be a non-empty string.
   */
  public Program(final String id, final int origin, final int wrap,
                 final int wrapTarget, final int sideSetCount,
                 final boolean sideSetOpt, final boolean sideSetPinDirs,
                 final short[] instructions)
  {
    if (id != null) {
      checkId(id);
    }
    if (origin < -1) {
      throw new IllegalArgumentException("origin < -1: " + origin);
    }
    if (origin > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("origin > " +
                                         (MEMORY_SIZE - 1) + ": " +
                                         origin);
    }
    if (wrap < 0) {
      throw new IllegalArgumentException("wrap < 0: " + wrap);
    }
    if (wrap > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("wrap > " + (MEMORY_SIZE - 1) + ": " +
                                         wrap);
    }
    if (wrapTarget < 0) {
      throw new IllegalArgumentException("wrap_target < 0: " + wrapTarget);
    }
    if (wrapTarget > MEMORY_SIZE - 1) {
      throw new IllegalArgumentException("wrap_target > " +
                                         (MEMORY_SIZE - 1) + ": " +
                                         wrapTarget);
    }
    if (sideSetCount < 0) {
      throw new IllegalArgumentException("side_set count < 0: " + sideSetCount);
    }
    if (sideSetCount > 5) {
      throw new IllegalArgumentException("side_set count > 5: " + sideSetCount);
    }
    if (sideSetOpt && ((sideSetCount > 4))) {
      throw new IllegalArgumentException("max. side-set count is 4, " +
                                         "if opt is set");
    }
    if (instructions == null) {
      throw new NullPointerException("instructions");
    }
    final int length = instructions.length;
    if (length > MEMORY_SIZE) {
      throw new IllegalArgumentException("instructions length > " +
                                         MEMORY_SIZE + ": " +
                                         length);
    }
    this.id = id;
    this.instructions = Arrays.copyOf(instructions, length);
    this.origin = origin;
    this.wrap = wrap;
    this.wrapTarget = wrapTarget;
    this.sideSetCount = sideSetCount;
    this.sideSetOpt = sideSetOpt;
    this.sideSetPinDirs = sideSetPinDirs;
    final int mask = (length < 32 ? (0x1 << length) : 0) - 1;
    allocationMask =
      origin >= 0 ?
      mask << origin | (mask << (origin - MEMORY_SIZE)) :
      mask;
  }

  private void checkId(final String id)
  {
    if (id.isEmpty()) {
      throw new IllegalArgumentException("invalid program id: <empty>");
    }
  }

  /**
   * Optional program identifier, or &lt;code&gt;null&lt;/code&gt;, if
   * undefined.
   */
  public String getId()
  {
    return id;
  }

  public int getLength()
  {
    return instructions.length;
  }

  public short getInstruction(final int index)
  {
    if (index < 0) {
      throw new IllegalArgumentException("index < 0: " + index);
    }
    if (index > instructions.length) {
      throw new IllegalArgumentException("index > instructions length: " +
                                         index);
    }
    return instructions[index];
  }

  /**
   * @return The origin of the program.  Either a fixed values in the
   * range 0…31, or -1, if the program is relocatable, such that it
   * does not matter where it will be loaded into memory.
   */
  public int getOrigin()
  {
    return origin;
  }

  /**
   * Helper function that returns an allocation mask for the program.
   * If the program origin is -1, the allocation mask is 0-based.
   */
  public int getAllocationMask()
  {
    return allocationMask;
  }

  public SMConfig getDefaultConfig(final int offset)
  {
    final SMConfig smConfig = SMConfig.getDefault();
    smConfig.setWrap(wrapTarget, wrap);
    if (sideSetCount > 0) {
      final int nettoSideSetCount = sideSetCount - (sideSetOpt ? 1 : 0);
      smConfig.setSideSet(nettoSideSetCount, sideSetOpt, sideSetPinDirs);
    }
    return smConfig;
  }

  @Override
  public String toString()
  {
    return
      String.format("Program{origin=%02x,length=%02x}",
                    origin, instructions.length);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
