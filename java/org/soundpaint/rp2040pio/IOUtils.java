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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class IOUtils
{
  /**
   * @param resourcePath absolute Path within root package, i.e. with
   * leading "/".
   */
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

  /**
   * @param resourcePath Path relative to root package, i.e. without
   * leading "/".
   */
  public static List<String> list(final String resourcePath) throws IOException
  {
    final List<String> paths = new ArrayList<String>();
    final File resourceFile =
      new File(IOUtils.class.getProtectionDomain().
               getCodeSource().getLocation().getPath());
    if (resourceFile.isFile()) {
      final JarFile jarFile = new JarFile(resourceFile);
      final Enumeration<JarEntry> entries = jarFile.entries();
      final String prefix = resourcePath + "/";
      while (entries.hasMoreElements()) {
        final String path = entries.nextElement().getName();
        if (path.startsWith(prefix) && (path.length() > prefix.length())) {
          paths.add(path.substring(prefix.length()));
        }
      }
      jarFile.close();
    } else {
      final URL url = IOUtils.class.getResource("/" + resourcePath);
      if (url != null) {
        final File directoryPath;
        try {
          directoryPath = new File(url.toURI());
        } catch (final URISyntaxException e) {
          throw new InternalError("unexpected exception", e);
        }
        final String prefix = directoryPath + "/";
        for (final File file : directoryPath.listFiles()) {
          final String path = file.getPath();
          if (path.startsWith(prefix) && (path.length() > prefix.length())) {
            paths.add(path.substring(prefix.length()));
          }
        }
      }
    }
    return paths;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
