/*
 * @(#)ValuedSignalPropertiesPanel.java 1.00 21/06/30
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.GPIOPadsBank0Registers;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.RegisterSet;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.doctool.RegistersDocs;
import org.soundpaint.rp2040pio.doctool.RegistersDocs.BitsInfo;
import org.soundpaint.rp2040pio.doctool.RegistersDocs.RegisterDetails;
import org.soundpaint.rp2040pio.sdk.SDK;

public class ValuedSignalPropertiesPanel extends JPanel
{
  private static final long serialVersionUID = -726756788602613552L;

  private static final Dimension PREFERRED_LABEL_SIZE = new Dimension(120, 32);

  private enum RegistersSet {
    /* TODO:
    PIO0_REGS("PIO0 Registers",
              PIORegisters.Regs.getRegisterSetLabel(),
              PIORegisters.Regs.getRegisterSetDescription(),
              PIORegisters.Regs.values(),
              Constants.PIO0_BASE),
    PIO1_REGS("PIO1 Registers",
              PIORegisters.Regs.getRegisterSetLabel(),
              PIORegisters.Regs.getRegisterSetDescription(),
              PIORegisters.Regs.values(),
              Constants.PIO1_BASE),
    */
    PIO0_ADD_ON_REGS("PIO0 Add-on Registers",
                     PIOEmuRegisters.Regs.getRegisterSetLabel(),
                     PIOEmuRegisters.Regs.getRegisterSetDescription(),
                     PIOEmuRegisters.Regs.values(),
                     Constants.PIO0_EMU_BASE),
    PIO1_ADD_ON_REGS("PIO1 Add-on Registers",
                     PIOEmuRegisters.Regs.getRegisterSetLabel(),
                     PIOEmuRegisters.Regs.getRegisterSetDescription(),
                     PIOEmuRegisters.Regs.values(),
                     Constants.PIO1_EMU_BASE),
    /* TODO:
    GPIO_IO_BANK0_REGS("GPIO IO Bank0 Registers",
                       GPIOIOBank0Registers.Regs.getRegisterSetLabel(),
                       GPIOIOBank0Registers.Regs.getRegisterSetDescription(),
                       GPIOIOBank0Registers.Regs.values(),
                       Constants.IO_BANK0_BASE),
    GPIO_PADS_BANK0_REGS("GPIO Pads Bank0 Registers",
                         GPIOPadsBank0Registers.Regs.getRegisterSetLabel(),
                         GPIOPadsBank0Registers.Regs.getRegisterSetDescription(),
                         GPIOPadsBank0Registers.Regs.values(),
                         Constants.PADS_BANK0_BASE),
    */
    PICO_ADD_ON_REGS("Global Add-on Registers",
                     PicoEmuRegisters.Regs.getRegisterSetLabel(),
                     PicoEmuRegisters.Regs.getRegisterSetDescription(),
                     PicoEmuRegisters.Regs.values(),
                     Constants.EMULATOR_BASE);

    private final String id;
    private final String label;
    private final String description;
    private final RegistersDocs<? extends Enum<?>>[] regs;
    private final int baseAddress;

    private RegistersSet(final String id,
                         final String label,
                         final String description,
                         final RegistersDocs<? extends Enum<?>>[] regs,
                         final int baseAddress)
    {
      this.id = id;
      this.label = label;
      this.description = description;
      this.regs = regs;
      this.baseAddress = baseAddress;
    }

    @Override
    public String toString() { return id; }
  }

  private class BitsInfoRenderer extends DefaultListCellRenderer
  {
    private static final long serialVersionUID = -4831476178158333446L;

    public BitsInfoRenderer()
    {
      setHorizontalAlignment(SwingConstants.LEFT);
    }

    @Override
    public Component
      getListCellRendererComponent(final JList<?> list,
                                   final Object value,
                                   final int index,
                                   final boolean isSelected,
                                   final boolean cellHasFocus)
    {
      super.getListCellRendererComponent(list, value, index,
                                         isSelected, cellHasFocus);
      final BitsInfo bitsInfo = (BitsInfo)value;
      setText(bitsInfo.toString(String.valueOf(cbRegister.getSelectedItem())));
      return this;
    }
  }

  private final Diagram diagram;
  private final SDK sdk;
  private final JLabel lbRegistersSet;
  private final JComboBox<RegistersSet> cbRegistersSet;
  private final JLabel lbRegister;
  private final JComboBox<RegistersDocs<? extends Enum<?>>> cbRegister;
  private final JLabel lbRegisterBitsInfo;
  private final DefaultListModel<BitsInfo> bitsInfos;
  private final JList<BitsInfo> lsBitsInfo;
  private final JScrollPane lsBitsInfoScroll;

  private ValuedSignalPropertiesPanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public ValuedSignalPropertiesPanel(final Diagram diagram, final SDK sdk)
  {
    Objects.requireNonNull(diagram);
    this.diagram = diagram;
    Objects.requireNonNull(sdk);
    this.sdk = sdk;

    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createTitledBorder("Valued Signal Properties"));
    bitsInfos = new DefaultListModel<BitsInfo>();
    lsBitsInfo = new JList<BitsInfo>(bitsInfos);
    lsBitsInfoScroll = new JScrollPane(lsBitsInfo);
    lbRegistersSet = new JLabel("Register Set");
    lbRegistersSet.setPreferredSize(PREFERRED_LABEL_SIZE);
    cbRegistersSet = addRegistersSetSelection();
    add(Box.createVerticalStrut(5));
    lbRegister = new JLabel("Register");
    lbRegister.setPreferredSize(PREFERRED_LABEL_SIZE);
    cbRegister = addRegisterSelection();
    registersSetSelected((RegistersSet)cbRegistersSet.getSelectedItem());
    add(Box.createVerticalStrut(5));
    lbRegisterBitsInfo = new JLabel("Register Bits");
    lbRegisterBitsInfo.setPreferredSize(PREFERRED_LABEL_SIZE);
    addBitsSelection();
    SwingUtils.setPreferredHeightAsMaximum(this);
  }

  private JComboBox<RegistersSet> addRegistersSetSelection()
  {
    final JPanel registersSetSelection = new JPanel();
    registersSetSelection.
      setLayout(new BoxLayout(registersSetSelection, BoxLayout.LINE_AXIS));
    lbRegistersSet.setAlignmentX(1.0f);
    registersSetSelection.add(lbRegistersSet);
    registersSetSelection.add(Box.createHorizontalStrut(5));
    final JComboBox<RegistersSet> cbRegistersSet =
      new JComboBox<RegistersSet>(RegistersSet.values());
    cbRegistersSet.addItemListener((event) -> {
        if (event.getStateChange() == ItemEvent.SELECTED) {
          registersSetSelected((RegistersSet)event.getItem());
        }
      });
    cbRegistersSet.setMaximumSize(cbRegistersSet.getPreferredSize());
    registersSetSelection.add(cbRegistersSet);
    registersSetSelection.add(Box.createHorizontalGlue());
    add(registersSetSelection);
    return cbRegistersSet;
  }

  private JComboBox<RegistersDocs<? extends Enum<?>>> addRegisterSelection()
  {
    final JPanel registerSelection = new JPanel();
    registerSelection.
      setLayout(new BoxLayout(registerSelection, BoxLayout.LINE_AXIS));
    registerSelection.add(lbRegister);
    registerSelection.add(Box.createHorizontalStrut(5));
    final JComboBox<RegistersDocs<? extends Enum<?>>> cbRegister =
      new JComboBox<RegistersDocs<? extends Enum<?>>>();
    cbRegister.addItemListener((event) -> {
        if (event.getStateChange() == ItemEvent.SELECTED) {
          @SuppressWarnings("unchecked")
          final RegistersDocs<? extends Enum<?>> docs =
            (RegistersDocs<? extends Enum<?>>)event.getItem();
          registerSelected(docs);
        }
      });
    registerSelection.add(cbRegister);
    registerSelection.add(Box.createHorizontalGlue());
    add(registerSelection);
    return cbRegister;
  }

  private void addBitsSelection()
  {
    final JPanel bitsSelection = new JPanel();
    bitsSelection.setLayout(new BoxLayout(bitsSelection, BoxLayout.LINE_AXIS));
    bitsSelection.add(lbRegisterBitsInfo);
    bitsSelection.add(Box.createHorizontalStrut(5));
    lsBitsInfo.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    lsBitsInfo.setCellRenderer(new BitsInfoRenderer());
    bitsSelection.add(lsBitsInfoScroll);
    bitsSelection.add(Box.createHorizontalGlue());
    add(bitsSelection);
  }

  private void registersSetSelected(final RegistersSet registersSet)
  {
    cbRegister.removeAllItems();
    for (RegistersDocs<? extends Enum<?>> register : registersSet.regs) {
      cbRegister.addItem(register);
    }
    cbRegister.setMaximumSize(cbRegister.getPreferredSize());
    registerSelected(cbRegister.getItemAt(0));
  }

  private void registerSelected(final RegistersDocs<? extends Enum<?>> register)
  {
    bitsInfos.clear();
    final RegisterDetails registerDetails = register.getRegisterDetails();
    for (final BitsInfo bitsInfo : registerDetails.getBitsInfos()) {
      bitsInfos.addElement(bitsInfo);
    }
    lsBitsInfoScroll.setMaximumSize(lsBitsInfoScroll.getPreferredSize());
  }

  public Signal createSignal(final String label)
  {
    final int baseAddress =
      ((RegistersSet)cbRegistersSet.getSelectedItem()).baseAddress;
    final int address = baseAddress + 0x4 * cbRegister.getSelectedIndex();
    final int minSelectionIndex = lsBitsInfo.getMinSelectionIndex();
    final int maxSelectionIndex = lsBitsInfo.getMaxSelectionIndex();
    if ((minSelectionIndex < 0) || (maxSelectionIndex < 0)) {
      JOptionPane.showMessageDialog(this,
                                    "Please select a contiguous range of bits.",
                                    "No Bit Range Selected",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
    final int msb =
      lsBitsInfo.getModel().getElementAt(minSelectionIndex).getMsb();
    final int lsb =
      lsBitsInfo.getModel().getElementAt(maxSelectionIndex).getLsb();
    final Supplier<Boolean> displayFilter = null;
    try {
      return
        SignalFactory.createFromRegister(sdk, label, address, msb, lsb,
                                         displayFilter);
    } catch (final IOException e) {
      JOptionPane.showMessageDialog(this, e.getMessage(),
                                    "I/O Exception",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
  }

  @Override
  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    lbRegistersSet.setEnabled(enabled);
    cbRegistersSet.setEnabled(enabled);
    lbRegister.setEnabled(enabled);
    cbRegister.setEnabled(enabled);
    lbRegisterBitsInfo.setEnabled(enabled);
    lsBitsInfo.setEnabled(enabled);
    lsBitsInfoScroll.getHorizontalScrollBar().setEnabled(enabled);
    lsBitsInfoScroll.getVerticalScrollBar().setEnabled(enabled);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
