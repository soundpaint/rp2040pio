/*
 * @(#)SignalDialog.java 1.00 21/06/29
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
import java.util.function.BiFunction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import org.soundpaint.rp2040pio.sdk.SDK;

public class SignalDialog extends JDialog
{
  private static final long serialVersionUID = 4433806198970312268L;

  @FunctionalInterface
  public static interface SignalConsumer
  {
    void accept(final Integer index, final Signal signal,
                final Boolean add);
  }

  private class ActionPanel extends Box
  {
    private static final long serialVersionUID = 7200614607584132864L;

    private final JButton btApply;
    private final JButton btCancel;

    public ActionPanel()
    {
      super(BoxLayout.LINE_AXIS);
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      btApply = new JButton();
      btApply.addActionListener((event) -> {
          if (SignalDialog.this.apply()) {
            SignalDialog.this.setVisible(false);
          }
        });
      add(btApply);
      add(Box.createHorizontalGlue());
      btCancel = new JButton("Cancel");
      btCancel.setMnemonic(KeyEvent.VK_C);
      btCancel.addActionListener((event) -> {
          SignalDialog.this.setVisible(false);
        });
      add(btCancel);
    }
  }

  private final Diagram diagram;
  private final SignalConsumer signalConsumer;
  private final SignalFactoryPanel signalFactoryPanel;
  private final ActionPanel actionPanel;
  private int index;
  private Signal editSignal;

  private SignalDialog()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public SignalDialog(final Diagram diagram, final SDK sdk,
                      final SignalConsumer signalConsumer,
                      final BiFunction<String, Signal, String> labelChecker)
  {
    super(diagram, "Signal", Dialog.ModalityType.DOCUMENT_MODAL);
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(signalConsumer);
    Objects.requireNonNull(labelChecker);
    this.diagram = diagram;
    this.signalConsumer = signalConsumer;
    getContentPane().add(signalFactoryPanel =
                         new SignalFactoryPanel(diagram, sdk, labelChecker));
    getContentPane().add(actionPanel = new ActionPanel(), BorderLayout.SOUTH);
  }

  private boolean apply()
  {
    final Signal signal = signalFactoryPanel.createSignal(editSignal);
    if (signal != null) {
      if (editSignal != null) {
        signalConsumer.accept(index, signal, false);
      } else {
        signal.setVisible(true);
        signalConsumer.accept(index, signal, true);
      }
      return true;
    }
    return false;
  }

  public void open(final int index, final Signal editSignal)
  {
    if (editSignal != null) {
      actionPanel.btApply.setText("Apply");
      actionPanel.btApply.setMnemonic(KeyEvent.VK_A);
      signalFactoryPanel.load(editSignal);
      setTitle(String.format("Edit Signal %s", editSignal.getLabel()));
    } else {
      actionPanel.btApply.setText("Add");
      actionPanel.btApply.setMnemonic(KeyEvent.VK_A);
      signalFactoryPanel.load(null);
      setTitle(String.format("Insert New Signal After Signal #%d", index - 1));
    }
    this.editSignal = editSignal;
    this.index = index;
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
