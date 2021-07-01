/*
 * @(#)AddSignalDialog.java 1.00 21/06/29
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
package org.soundpaint.rp2040pio.observer.diagram;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import org.soundpaint.rp2040pio.sdk.SDK;

public class AddSignalDialog extends JDialog
{
  private static final long serialVersionUID = 4433806198970312268L;

  private class ActionPanel extends Box
  {
    private static final long serialVersionUID = 7200614607584132864L;

    private final JButton btAdd;
    private final JButton btCancel;

    public ActionPanel()
    {
      super(BoxLayout.X_AXIS);
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      btAdd = new JButton("Add");
      btAdd.setMnemonic(KeyEvent.VK_A);
      btAdd.addActionListener((event) -> {
          if (AddSignalDialog.this.add()) {
            AddSignalDialog.this.setVisible(false);
          }
        });
      add(btAdd);
      add(Box.createHorizontalGlue());
      btCancel = new JButton("Cancel");
      btCancel.setMnemonic(KeyEvent.VK_C);
      btCancel.addActionListener((event) -> {
          AddSignalDialog.this.setVisible(false);
        });
      add(btCancel);
    }
  }

  private final Diagram diagram;
  private final BiConsumer<Integer, Signal> signalAdder;
  private final SignalFactoryPanel signalFactoryPanel;
  private int addIndex;

  private AddSignalDialog()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public AddSignalDialog(final Diagram diagram, final SDK sdk,
                         final BiConsumer<Integer, Signal> signalAdder)
  {
    super(diagram, "Add Signal", Dialog.ModalityType.DOCUMENT_MODAL);
    Objects.requireNonNull(diagram);
    this.diagram = diagram;
    this.signalAdder = signalAdder;
    getContentPane().add(signalFactoryPanel =
                         new SignalFactoryPanel(diagram, sdk));
    getContentPane().add(new ActionPanel(), BorderLayout.SOUTH);
  }

  private boolean add()
  {
    final Signal signal = signalFactoryPanel.createSignal();
    if (signal != null) {
      signal.setVisible(true);
      signalAdder.accept(addIndex, signal);
      return true;
    }
    return false;
  }

  public void open(final int addIndex)
  {
    this.addIndex = addIndex;
    setTitle(String.format("Insert New Signal Before Signal #%d", addIndex));
    pack();
    setVisible(true);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
