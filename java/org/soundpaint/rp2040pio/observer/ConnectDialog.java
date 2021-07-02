/*
 * @(#)ConnectDialog.java 1.00 21/05/30
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
package org.soundpaint.rp2040pio.observer;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class ConnectDialog extends JDialog
{
  private static final long serialVersionUID = -8971812083742002912L;

  private class ActionPanel extends Box
  {
    private static final long serialVersionUID = 2187278488162670119L;

    private final JButton btOk;
    private final JButton btCancel;

    public ActionPanel()
    {
      super(BoxLayout.LINE_AXIS);
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      btOk = new JButton("Ok");
      btOk.setMnemonic(KeyEvent.VK_O);
      btOk.addActionListener((event) -> {
          if (apply()) {
            ConnectDialog.this.setVisible(false);
          }
        });
      add(btOk);
      add(Box.createHorizontalGlue());
      btCancel = new JButton("Cancel");
      btCancel.setMnemonic(KeyEvent.VK_C);
      btCancel.addActionListener((event) ->
                                 ConnectDialog.this.setVisible(false));
      add(btCancel);
    }
  }

  private final GUIObserver observer;
  private final JTextField tfPort;
  private int savedPort;

  private ConnectDialog()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public ConnectDialog(final GUIObserver observer, final int defaultPort)
  {
    super(observer, "Connect to Emulation Server");
    Objects.requireNonNull(observer);
    this.observer = observer;
    savedPort = defaultPort;
    tfPort = new JTextField(String.valueOf(savedPort));
    getContentPane().add(createConnectionDetails());
    getContentPane().add(new ActionPanel(), BorderLayout.SOUTH);
    pack();
  }

  public void makeVisible()
  {
    tfPort.setText(String.valueOf(savedPort));
    setVisible(true);
  }

  private Box createConnectionDetails()
  {
    final Box vBox = new Box(BoxLayout.PAGE_AXIS);
    final Border loweredEtched =
      BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    final TitledBorder titled =
      BorderFactory.createTitledBorder(loweredEtched, "Connection Details");
    titled.setTitleJustification(TitledBorder.CENTER);
    vBox.setBorder(titled);
    vBox.add(createServerPortLine());
    vBox.add(Box.createVerticalGlue());
    return vBox;
  }

  private Box createServerPortLine()
  {
    final Box hBox = new Box(BoxLayout.LINE_AXIS);
    hBox.add(new JLabel("Server"));
    final JTextField tfServer = new JTextField("localhost");
    tfServer.setEnabled(false);
    hBox.add(tfServer);
    hBox.add(Box.createHorizontalStrut(20));
    final JLabel lbPort = new JLabel("Port");
    hBox.add(lbPort);
    hBox.add(tfPort);
    hBox.add(Box.createHorizontalGlue());
    return hBox;
  }

  private boolean apply()
  {
    try {
      final int port = Integer.parseInt(tfPort.getText().trim());
      if ((port <= 0) || (port > 65535)) {
        JOptionPane.
          showMessageDialog(this,
                            "Port number must be in the range 1…65535.",
                            "Invalid Port Number",
                            JOptionPane.ERROR_MESSAGE);
        return false;
      }
      observer.connect(port);
      savedPort = port;
      return true;
    } catch (final NumberFormatException e) {
      JOptionPane.
        showMessageDialog(this,
                          "Port is not a valid integer number.",
                          "Invalid Port",
                          JOptionPane.ERROR_MESSAGE);
      return false;
    } catch (final IOException e) {
      JOptionPane.
        showMessageDialog(this,
                          e.getMessage(),
                          "Failed Connecting",
                          JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
