/*
 * @(#)FifoObserver.java 1.00 21/04/28
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
package org.soundpaint.rp2040pio.observer.fifo;

import java.io.IOException;
import java.io.PrintStream;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.observer.GUIObserver;

/**
 * Emulation FIFO Status Observation
 */
public class FifoObserver extends GUIObserver
{
  private static final long serialVersionUID = 5938495055310591092L;
  private static final String APP_TITLE = "FIFO Observer";
  private static final String APP_FULL_NAME =
    "Emulation FIFO Observer Version 0.1";

  private final FifoViewPanel fifoViewPanel;

  private FifoObserver(final PrintStream console, final String[] argv)
    throws IOException
  {
    super(APP_TITLE, APP_FULL_NAME, console, argv);
    add(fifoViewPanel = new FifoViewPanel(console, getSDK(), APP_TITLE));
    pack();
    setVisible(true);
    startUpdating();
  }

  @Override
  protected void updateView()
  {
    fifoViewPanel.updateView();
  }

  public static void main(final String argv[])
  {
    final PrintStream console = System.out;
    try {
      new FifoObserver(console, argv);
    } catch (final IOException e) {
      console.printf("initialization failed: %s%n", e.getMessage());
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
