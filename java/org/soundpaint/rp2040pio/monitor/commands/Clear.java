/*
 * @(#)Clear.java 1.00 21/04/22
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
package org.soundpaint.rp2040pio.monitor.commands;

import java.io.IOException;
import java.io.PrintStream;
import org.soundpaint.rp2040pio.CmdOptions;
import org.soundpaint.rp2040pio.monitor.Command;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Monitor command "clear" clears the screen and, optionally,
 * scrollback buffer.
 */
public class Clear extends Command
{
  private static final String fullName = "clear";
  private static final String singleLineDescription =
    "clear screen and optionally scrollback buffer";

  private static final CmdOptions.FlagOptionDeclaration optBuffer =
    CmdOptions.createFlagOption(false, 'b', "buffer", CmdOptions.Flag.OFF,
                                "also clear scrollback buffer");

  public Clear(final PrintStream console)
  {
    super(console, fullName, singleLineDescription,
          new CmdOptions.OptionDeclaration<?>[] { optBuffer });
  }

  /**
   * Returns true if no error occurred and the command has been
   * executed.
   */
  @Override
  protected boolean execute(final CmdOptions options) throws IOException
  {
    if (options.getValue(optBuffer).isOn()) {
      console.printf("\u001b[3J");
    }
    console.printf("\u001b[2J");
    console.printf("\u001b[H");
    return true;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
