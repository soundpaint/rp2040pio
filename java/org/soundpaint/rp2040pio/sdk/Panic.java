/*
 * @(#)Panic.java 1.00 21/03/31
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

public class Panic extends RuntimeException
{
  private static final long serialVersionUID = 1796831772857837603L;
  private static final String PANIC = "*** PANIC ***";

  public Panic(final String message)
  {
    super(message);
  }

  public Panic(final String message, final Throwable cause)
  {
    super(message, cause);
  }

  public Panic(final String message, final Throwable cause,
               final boolean enableSupression, final boolean writableStackTrace)
  {
    super(message, cause, enableSupression, writableStackTrace);
  }

  public Panic(final Throwable cause)
  {
    super(cause);
  }

  @Override
  public String getMessage()
  {
    final String message = super.getMessage();
    if (message != null) {
      return String.format("%s%n%s%n", PANIC, message);
    } else {
      return String.format("%s%n", PANIC);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
