/*
 * @(#)SignalsDialog.java 1.00 21/04/07
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
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import org.soundpaint.rp2040pio.sdk.SDK;

public class SignalsDialog extends JDialog
{
  private static final long serialVersionUID = 8248679860337463934L;

  private class ActionPanel extends Box
  {
    private static final long serialVersionUID = -4136799373128393432L;

    private final JButton btOk;
    private final JButton btApply;
    private final JButton btCancel;

    public ActionPanel()
    {
      super(BoxLayout.LINE_AXIS);
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      btOk = new JButton("Ok");
      btOk.setMnemonic(KeyEvent.VK_O);
      btOk.addActionListener((event) -> {
          applyChanges();
          SignalsDialog.this.setVisible(false);
        });
      add(btOk);
      add(Box.createHorizontalGlue());
      btApply = new JButton("Apply");
      btApply.setMnemonic(KeyEvent.VK_A);
      btApply.addActionListener((event) -> {
          applyChanges();
        });
      add(btApply);
      add(Box.createHorizontalGlue());
      btCancel = new JButton("Cancel");
      btCancel.setMnemonic(KeyEvent.VK_C);
      btCancel.addActionListener((event) -> {
          SignalsDialog.this.setVisible(false);
        });
      add(btCancel);
    }
  }

  private final SignalsPropertiesPanel signalsPropertiesPanel;

  private SignalsDialog()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public SignalsDialog(final Diagram diagram, final SDK sdk)
  {
    super(diagram, "Signals", Dialog.ModalityType.DOCUMENT_MODAL);
    Objects.requireNonNull(diagram);
    signalsPropertiesPanel = new SignalsPropertiesPanel(diagram, sdk);
    final JScrollPane scrollPane = new JScrollPane(signalsPropertiesPanel);
    final Border loweredEtched =
      BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    final TitledBorder titled =
      BorderFactory.createTitledBorder(loweredEtched, "Signals");
    titled.setTitleJustification(TitledBorder.CENTER);
    scrollPane.setBorder(titled);
    getContentPane().add(scrollPane);
    getContentPane().add(new ActionPanel(), BorderLayout.SOUTH);
    setPreferredSize(new Dimension(400, 400));
  }

  private void applyChanges()
  {
    signalsPropertiesPanel.applyChanges();
  }

  public void open()
  {
    signalsPropertiesPanel.rebuild();
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
