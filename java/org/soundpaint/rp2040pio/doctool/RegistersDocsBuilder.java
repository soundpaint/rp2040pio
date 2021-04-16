/*
 * @(#)RegistersDocsBuilder.java 1.00 21/04/15
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

import java.util.EnumSet;
import org.soundpaint.rp2040pio.PIOEmuRegisters;

/**
 * Automatically create registers documentation from annotations in
 * the code.
 */
public class RegistersDocsBuilder<T extends Enum<T> & RegistersDocs<T>>
{
  private static String csvEncode(final String raw)
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

  private String createDocs(final EnumSet<T> regs)
  {
    final StringBuilder s = new StringBuilder();
    s.append(String.format("List of Registers%n"));
    s.append(String.format("=================%n"));
    s.append(String.format("%n"));
    s.append(String.format(".. csv-table:: Headline"));
    s.append(String.format("   :header: Offset, Name, Info%n"));
    s.append(String.format("   :width: 8, 20, 30%n"));
    s.append(String.format("%n"));
    int address = 0x000;
    for (final T reg : regs) {
      s.append(String.format("   0x%03x, %s, %s%n",
                             address, reg,
                             csvEncode(reg.getInfo().replace("%n", " "))));
      address += 0x004;
    }
    s.append(String.format("%n"));
    return s.toString();
  }

  public static void main(final String argv[])
  {
    final String s =
      new RegistersDocsBuilder<PIOEmuRegisters.Regs>().
      createDocs(EnumSet.allOf(PIOEmuRegisters.Regs.class));
    System.out.println(s);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
