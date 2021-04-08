/*
 * @(#)IOUtils.java 1.00 21/04/08
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

import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;

public class IOUtils
{
  public static InputStream getStreamForResourcePath(final String resourcePath)
    throws IOException
  {
    final InputStream fromFile;
    try {
      fromFile = new FileInputStream(resourcePath);
    } catch (final FileNotFoundException e) {
      final InputStream fromResource =
        Constants.class.getResourceAsStream(resourcePath);
      if (fromResource == null) {
        throw new IOException("resource not found: " + resourcePath);
      }
      return fromResource;
    }
    return fromFile;
  }

  public static LineNumberReader
    getReaderForResourcePath(final String resourcePath)
    throws IOException
  {
    final InputStream in = getStreamForResourcePath(resourcePath);
    return new LineNumberReader(new InputStreamReader(in));
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
