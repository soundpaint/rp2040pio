/*
 * @(#)Memory.java 1.00 21/01/31
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
import java.net.URL;

/**
 * 32 32-Bit Words of Shared Instruction Memory
 */
public class Memory
{
  public static final int SIZE = 32;

  private final short[] code;

  public Memory()
  {
    code = new short[SIZE];
  }

  public void set(final int address, final short value)
  {
    if ((address < 0) || (address >= SIZE)) {
      throw new IllegalArgumentException("address out of range: " + address);
    }
    code[address] = value;
  }

  public short get(final int address)
  {
    if ((address < 0) || (address >= SIZE)) {
      throw new IllegalArgumentException("address out of range: " + address);
    }
    return code[address];
  }

  public void loadFromBinResource(final String resourcePath)
    throws IOException
  {
    final InputStream in = Main.class.getResourceAsStream(resourcePath);
    if (in == null) {
      throw new IOException("failed loading code: resource not found: " +
                            resourcePath);
    }
    final int available = in.available();
    if (available > SIZE * 2) {
      throw new IOException("failed loading code: size too large: " +
                            available + " > " + SIZE * 2);
    }
    if ((available & 0x3) != 0) {
      throw new IOException("failed loading code: " +
                            "size must be multiple of 4: " + available);
    }
    final short[] code = new short[SIZE];
    for (int address = 0; address < available / 4; address ++) {
      short value = 0;
      for (int byteCount = 0; byteCount < 2; byteCount++) {
        value <<= 0x8;
        value |= (in.read() & 0xff);
      }
      code[address] = value;
    }
    System.arraycopy(code, 0, this.code, 0, SIZE);
    System.out.println("loaded " + (available / 4) + " instructions into PIO");
  }

  public void loadFromHexResource(final String resourcePath)
    throws IOException
  {
    final InputStream in = Main.class.getResourceAsStream(resourcePath);
    if (in == null) {
      throw new IOException("failed loading code: resource not found: " +
                            resourcePath);
    }
    final BufferedReader reader
      = new BufferedReader(new InputStreamReader(in));
    int address = 0;
    final short[] code = new short[SIZE];
    String line;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith("#")) continue;
      if (address >= SIZE) {
        throw new IOException("failed loading code: size too large: " +
                              "get more than " + SIZE + " words");
      }
      try {
        final short value = (short)(Integer.parseInt(line, 16));
        code[address++] = value;
      } catch (final NumberFormatException e) {
        throw new IOException("failed loading code: parse error: " +
                              "not a valid 16 bit hex word: " + line);
      }
    }
    reader.close();
    System.arraycopy(code, 0, this.code, 0, SIZE);
    System.out.println("loaded " + address + " instructions into PIO");
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
