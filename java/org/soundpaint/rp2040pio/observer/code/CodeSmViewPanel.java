/*
 * @(#)CodeSmViewPanel.java 1.00 21/04/24
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
package org.soundpaint.rp2040pio.observer.code;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.soundpaint.rp2040pio.Bit;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.sdk.SDK;

public class CodeSmViewPanel extends JPanel
{
  private static final long serialVersionUID = 1576266124601249894L;

  private final PrintStream console;
  private final SDK sdk;
  private final int smNum;

  private CodeSmViewPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public CodeSmViewPanel(final PrintStream console, final SDK sdk,
                         final int smNum)
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    this.console = console;
    this.sdk = sdk;
    this.smNum = smNum;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
