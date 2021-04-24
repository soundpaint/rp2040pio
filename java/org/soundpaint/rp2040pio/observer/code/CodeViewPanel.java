/*
 * @(#)CodeViewPanel.java 1.00 21/04/24
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

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class CodeViewPanel extends JPanel
{
  private static final long serialVersionUID = -2394791187467829359L;

  private final PrintStream console;
  private final SDK sdk;
  private final int refresh;
  private final JTabbedPane tabbedPane;

  private CodeViewPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public CodeViewPanel(final PrintStream console, final SDK sdk,
                       final int refresh)
    throws IOException
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    this.console = console;
    this.sdk = sdk;
    this.refresh = refresh;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createTitledBorder("Code View"));
    tabbedPane = new JTabbedPane();
    add(tabbedPane);
    for (int smNum = 0; smNum < Constants.SM_COUNT; smNum++) {
      final CodeSmViewPanel codeSmViewPanel =
        new CodeSmViewPanel(console, sdk, smNum);
      tabbedPane.addTab("SM" + smNum, null, codeSmViewPanel,
                        "Code View Panel for State Machine #" + smNum);
      tabbedPane.setMnemonicAt(smNum, KeyEvent.VK_0 + smNum);
    }
    SwingUtils.setPreferredHeightAsMaximum(tabbedPane);
    add(Box.createVerticalGlue());
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
