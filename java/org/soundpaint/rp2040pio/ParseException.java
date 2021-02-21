/*
 * @(#)ParseException.java 1.00 21/02/16
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

import java.io.IOException;

public class ParseException extends IOException
{
  private static final long serialVersionUID = -3298538004378904681L;

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

  public static ParseException create(final String message,
                                      final String resourcePath,
                                      final int lineIndex,
                                      final Throwable cause)
  {
    final String fullMessage =
      String.format("parse exception in %s, line %d: %s",
                    resourcePath, lineIndex, message);
    return cause != null ?
      new ParseException(fullMessage, cause) :
      new ParseException(fullMessage);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
