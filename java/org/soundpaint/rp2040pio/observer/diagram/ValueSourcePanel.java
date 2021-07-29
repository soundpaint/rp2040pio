/*
 * @(#)ValueSourcePanel.java 1.00 21/06/30
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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.GPIOPadsBank0Registers;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.doctool.RegistersDocs;
import org.soundpaint.rp2040pio.doctool.RegistersDocs.BitsInfo;
import org.soundpaint.rp2040pio.doctool.RegistersDocs.BitsRange;
import org.soundpaint.rp2040pio.doctool.RegistersDocs.BitsType;
import org.soundpaint.rp2040pio.doctool.RegistersDocs.RegisterDetails;
import org.soundpaint.rp2040pio.sdk.SDK;

public class ValueSourcePanel extends JPanel
  implements org.soundpaint.rp2040pio.observer.diagram.Constants
{
  private static final long serialVersionUID = -726756788602613552L;
  private static final String COLUMN_BITS_RANGE = "Bits";
  private static final int COLUMN_BITS_RANGE_IDX = 0;
  private static final String COLUMN_NAME = "Name";
  private static final int COLUMN_NAME_IDX = 1;
  private static final String COLUMN_DESCRIPTION = "Description";
  private static final int COLUMN_DESCRIPTION_IDX = 2;
  private static final String COLUMN_TYPE = "Type";
  private static final int COLUMN_TYPE_IDX = 3;
  private static final String COLUMN_RESET_VALUE = "Reset Value";
  private static final int COLUMN_RESET_VALUE_IDX = 4;

  private static class BitsRangeCellRenderer extends DefaultTableCellRenderer
  {
    private static final long serialVersionUID = 8594096152113741258L;

    @Override
    protected void setValue(final Object value)
    {
      setText((value == null) ? "" : ((BitsRange)value).toShortString());
    }
  }

  private enum RegistersSet
  {
    PIO0_REGS("PIO0 Registers", "PIO0_",
              PIORegisters.Regs.getRegisterSetLabel(),
              PIORegisters.Regs.getRegisterSetDescription(),
              PIORegisters.Regs.values(),
              Constants.PIO0_BASE, 0),
    PIO0_ADD_ON_REGS("PIO0 Add-on Registers", "PIO0_",
                     PIOEmuRegisters.Regs.getRegisterSetLabel(),
                     PIOEmuRegisters.Regs.getRegisterSetDescription(),
                     PIOEmuRegisters.Regs.values(),
                     Constants.PIO0_EMU_BASE, 0),
    PIO1_REGS("PIO1 Registers", "PIO1_",
              PIORegisters.Regs.getRegisterSetLabel(),
              PIORegisters.Regs.getRegisterSetDescription(),
              PIORegisters.Regs.values(),
              Constants.PIO1_BASE, 1),
    PIO1_ADD_ON_REGS("PIO1 Add-on Registers", "PIO1_",
                     PIOEmuRegisters.Regs.getRegisterSetLabel(),
                     PIOEmuRegisters.Regs.getRegisterSetDescription(),
                     PIOEmuRegisters.Regs.values(),
                     Constants.PIO1_EMU_BASE, 1),
    GPIO_IO_BANK0_REGS("GPIO IO Bank0 Registers", "",
                       GPIOIOBank0Registers.Regs.getRegisterSetLabel(),
                       GPIOIOBank0Registers.Regs.getRegisterSetDescription(),
                       GPIOIOBank0Registers.Regs.values(),
                       Constants.IO_BANK0_BASE, -1),
    GPIO_PADS_BANK0_REGS("GPIO Pads Bank0 Registers", "",
                         GPIOPadsBank0Registers.Regs.getRegisterSetLabel(),
                         GPIOPadsBank0Registers.Regs.getRegisterSetDescription(),
                         GPIOPadsBank0Registers.Regs.values(),
                         Constants.PADS_BANK0_BASE, -1),
    PICO_ADD_ON_REGS("Global Add-on Registers", "",
                     PicoEmuRegisters.Regs.getRegisterSetLabel(),
                     PicoEmuRegisters.Regs.getRegisterSetDescription(),
                     PicoEmuRegisters.Regs.values(),
                     Constants.EMULATOR_BASE, -1);

    private static RegistersSet fromAddress(final int address)
    {
      final int baseAddress = address & 0xffffc000;
      for (final RegistersSet registersSet : RegistersSet.values()) {
        if (registersSet.baseAddress == baseAddress)
          return registersSet;
      }
      return null;
    }

    private final String id;
    private final String label;
    private final String description;
    private final RegistersDocs<? extends Enum<?>>[] regs;
    private final int baseAddress;
    private final int pioNum;
    private final String suggestedLabelPrefix;

    private RegistersSet(final String id,
                         final String suggestedLabelPrefix,
                         final String label,
                         final String description,
                         final RegistersDocs<? extends Enum<?>>[] regs,
                         final int baseAddress,
                         final int pioNum)
    {
      this.id = id;
      this.label = label;
      this.description = description;
      this.regs = regs;
      this.baseAddress = baseAddress;
      this.pioNum = pioNum;
      this.suggestedLabelPrefix = suggestedLabelPrefix;
    }

    @Override
    public String toString() { return id; }
  }

  private final Diagram diagram;
  private final SDK sdk;
  private final Consumer<String> suggestedLabelSetter;
  private final Consumer<Void> sourceChangedListener;
  private final JLabel lbRegistersSet;
  private final JComboBox<RegistersSet> cbRegistersSet;
  private final JLabel lbRegistersSetInfo;
  private final JLabel lbRegister;
  private final JComboBox<RegistersDocs<? extends Enum<?>>> cbRegister;
  private final JLabel lbRegisterInfo;
  private final JLabel lbRegisterBitsInfos;
  private final JLabel lbRegisterBits;
  private final DefaultTableModel bitsInfos;
  private final JTable tbBitsInfos;
  private final JScrollPane tbBitsInfosScroll;

  private ValueSourcePanel()
  {
    throw new UnsupportedOperationException("unsupported default constructor");
  }

  public ValueSourcePanel(final Diagram diagram, final SDK sdk,
                          final Consumer<String> suggestedLabelSetter,
                          final Consumer<Void> sourceChangedListener)
  {
    Objects.requireNonNull(diagram);
    Objects.requireNonNull(sdk);
    Objects.requireNonNull(suggestedLabelSetter);
    Objects.requireNonNull(sourceChangedListener);
    this.diagram = diagram;
    this.sdk = sdk;
    this.suggestedLabelSetter = suggestedLabelSetter;
    this.sourceChangedListener = sourceChangedListener;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createTitledBorder("Use Value From Register"));
    bitsInfos = createTableModel();
    tbBitsInfos = new JTable(bitsInfos);
    setupTableColumnRenderers();
    tbBitsInfos.setColumnSelectionAllowed(false);
    tbBitsInfos.getSelectionModel().
      addListSelectionListener((selection) -> selectionChanged(selection));
    tbBitsInfosScroll = new JScrollPane(tbBitsInfos);
    lbRegistersSet = new JLabel("Register Set");
    lbRegistersSetInfo = new JLabel();
    lbRegistersSet.setPreferredSize(PREFERRED_LABEL_SIZE);
    lbRegisterBitsInfos = new JLabel("Bits Range");
    lbRegisterBitsInfos.setPreferredSize(PREFERRED_LABEL_SIZE);
    lbRegisterBits = new JLabel();
    lbRegisterBits.setPreferredSize(PREFERRED_LABEL_SIZE);
    cbRegistersSet = addRegistersSetSelection();
    add(Box.createVerticalStrut(5));
    lbRegister = new JLabel("Register");
    lbRegister.setPreferredSize(PREFERRED_LABEL_SIZE);
    lbRegisterInfo = new JLabel();
    cbRegister = addRegisterSelection();
    add(Box.createVerticalStrut(5));
    addBitsSelection();
    SwingUtils.setPreferredHeightAsMaximum(this);
  }

  public void initRegistersForSelectedRegisterSet()
  {
    registersSetSelected((RegistersSet)cbRegistersSet.getSelectedItem());
  }

  private void setupTableColumnRenderers()
  {
    final TableColumnModel model = tbBitsInfos.getColumnModel();
    final TableColumn bitsRangeColumn = model.getColumn(COLUMN_BITS_RANGE_IDX);
    bitsRangeColumn.setCellRenderer(new BitsRangeCellRenderer());
  }

  private DefaultTableModel createTableModel()
  {
    final DefaultTableModel model = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex)
        {
          return false;
        }
      };
    model.addColumn(COLUMN_BITS_RANGE);
    model.addColumn(COLUMN_NAME);
    model.addColumn(COLUMN_DESCRIPTION);
    model.addColumn(COLUMN_TYPE);
    model.addColumn(COLUMN_RESET_VALUE);
    return model;
  }

  private JComboBox<RegistersSet> addRegistersSetSelection()
  {
    final JPanel registersSetSelection = new JPanel();
    registersSetSelection.
      setLayout(new BoxLayout(registersSetSelection, BoxLayout.LINE_AXIS));
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
    final JPanel registersSetInfo = new JPanel();
    registersSetInfo.
      setLayout(new BoxLayout(registersSetInfo, BoxLayout.LINE_AXIS));
    final JLabel lbFiller = new JLabel();
    lbFiller.setPreferredSize(PREFERRED_LABEL_SIZE);
    registersSetInfo.add(lbFiller);
    registersSetInfo.add(Box.createHorizontalStrut(5));
    registersSetInfo.add(lbRegistersSetInfo);
    registersSetInfo.add(Box.createHorizontalGlue());
    add(registersSetInfo);
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
    final JPanel registerInfo = new JPanel();
    registerInfo.setLayout(new BoxLayout(registerInfo, BoxLayout.LINE_AXIS));
    final JLabel lbFiller = new JLabel();
    lbFiller.setPreferredSize(PREFERRED_LABEL_SIZE);
    registerInfo.add(lbFiller);
    registerInfo.add(Box.createHorizontalStrut(5));
    registerInfo.add(lbRegisterInfo);
    registerInfo.add(Box.createHorizontalGlue());
    add(registerInfo);
    return cbRegister;
  }

  private void addBitsSelection()
  {
    final JPanel bitsSelection = new JPanel();
    bitsSelection.setLayout(new BoxLayout(bitsSelection, BoxLayout.LINE_AXIS));
    final JPanel labelPanel = new JPanel();
    labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.PAGE_AXIS));
    labelPanel.add(lbRegisterBitsInfos);
    labelPanel.add(lbRegisterBits);
    bitsSelection.add(labelPanel);
    bitsSelection.add(Box.createHorizontalStrut(5));
    tbBitsInfos.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    bitsSelection.add(tbBitsInfosScroll);
    bitsSelection.add(Box.createHorizontalGlue());
    add(bitsSelection);
  }

  private void registersSetSelected(final RegistersSet registersSet)
  {
    lbRegistersSetInfo.setText(String.format("%s @ 0x%08x",
                                             registersSet.label,
                                             registersSet.baseAddress));
    cbRegister.removeAllItems();
    for (RegistersDocs<? extends Enum<?>> register : registersSet.regs) {
      cbRegister.addItem(register);
    }
    cbRegister.setMaximumSize(cbRegister.getPreferredSize());
    registerSelected(cbRegister.getItemAt(0));
  }

  private String getSuggestedBitsRange()
  {
    if (bitsInfos.getRowCount() == 0) {
      return null;
    }
    final int minSelectionRow = tbBitsInfos.getSelectedRow();
    final int maxSelectionRow =
      minSelectionRow + tbBitsInfos.getSelectedRowCount() - 1;
    if ((minSelectionRow < 0) || (maxSelectionRow < 0)) {
      return null;
    }
    final int msb =
      ((BitsRange)bitsInfos.getValueAt(minSelectionRow, COLUMN_BITS_RANGE_IDX)).
      getMsb();
    final int lsb =
      ((BitsRange)bitsInfos.getValueAt(maxSelectionRow, COLUMN_BITS_RANGE_IDX)).
      getLsb();
    return String.format("[%s]", msb != lsb ? msb + ":" + lsb : msb);
  }

  private String getSuggestedBitsLabel()
  {
    if (bitsInfos.getRowCount() <= 1) {
      return "";
    }
    boolean haveUnselectedRelevantBits = false;
    for (int row = 0; row < bitsInfos.getRowCount(); row++) {
      if (!tbBitsInfos.isRowSelected(row)) {
        if (((BitsType)bitsInfos.getValueAt(row, COLUMN_TYPE_IDX)).
            isRelevant()) {
          haveUnselectedRelevantBits = true;
          break;
        }
      }
    }
    if (!haveUnselectedRelevantBits) {
      return "";
    }
    final int minSelectionRow = tbBitsInfos.getSelectedRow();
    final int maxSelectionRow =
      minSelectionRow + tbBitsInfos.getSelectedRowCount() - 1;
    if ((minSelectionRow < 0) || (maxSelectionRow < 0)) {
      // empty range
      return null;
    }
    if (minSelectionRow == maxSelectionRow) {
      final String name =
        (String)bitsInfos.getValueAt(minSelectionRow, COLUMN_NAME_IDX);
      if (name != null) {
        return String.format("_%s", name);
      }
    }
    final int msb =
      ((BitsRange)bitsInfos.getValueAt(minSelectionRow, COLUMN_BITS_RANGE_IDX)).
      getMsb();
    final int lsb =
      ((BitsRange)bitsInfos.getValueAt(maxSelectionRow, COLUMN_BITS_RANGE_IDX)).
      getLsb();
    return String.format("[%s]", msb != lsb ? msb + ":" + lsb : msb);
  }

  private RegistersDocs<? extends Enum<?>> getSelectedRegister()
  {
    @SuppressWarnings("unchecked")
    final RegistersDocs<? extends Enum<?>> register =
      (RegistersDocs<? extends Enum<?>>)cbRegister.getSelectedItem();
    return register;
  }

  private String getSuggestedLabel(final String suggestedBitsLabel)
  {
    final RegistersSet registersSet =
      (RegistersSet)cbRegistersSet.getSelectedItem();
    final String suggestedLabelPrefix = registersSet.suggestedLabelPrefix;
    final RegistersDocs<? extends Enum<?>> register = getSelectedRegister();
    return String.format("%s%s%s",
                         suggestedLabelPrefix, register, suggestedBitsLabel);
  }

  private int chooseBitsInfosRow()
  {
    for (int row = bitsInfos.getRowCount() - 1; row >= 0; row--) {
      final BitsType type =
        (BitsType)bitsInfos.getValueAt(row, COLUMN_TYPE_IDX);
      if ((type != BitsType.RESERVED) && (type != BitsType.UNUSED))
        return row;
    }
    return -1;
  }

  public void updateSuggestedLabel()
  {
    final String suggestedBitsRange = getSuggestedBitsRange();
    if (suggestedBitsRange != null) {
      lbRegisterBits.setText(suggestedBitsRange);
    } else {
      lbRegisterBits.setText("");
    }
    final String suggestedBitsLabel = getSuggestedBitsLabel();
    if (suggestedBitsLabel != null) {
      final String suggestedLabel = getSuggestedLabel(suggestedBitsLabel);
      suggestedLabelSetter.accept(suggestedLabel);
    }
  }

  private void sourceChanged()
  {
    updateSuggestedLabel();
    sourceChangedListener.accept(null);
  }

  private void ensureCellIsVisible(final int row, final int column)
  {
    if (tbBitsInfos.getParent() instanceof JViewport) {
      final JViewport viewport = (JViewport)tbBitsInfos.getParent();
      final Rectangle cellRect = tbBitsInfos.getCellRect(row, column, true);
      final Point viewPosition = viewport.getViewPosition();
      cellRect.translate(-viewPosition.x, -viewPosition.y);
      tbBitsInfos.scrollRectToVisible(cellRect);
    }
  }

  private void registerSelected(final RegistersDocs<? extends Enum<?>> register)
  {
    lbRegisterInfo.setText(String.format("%s", register.getInfo()));
    bitsInfos.setRowCount(0);
    final RegisterDetails registerDetails = register.getRegisterDetails();
    for (final BitsInfo bitsInfo : registerDetails.getBitsInfos()) {
      final Object[] rowData = new Object[5];
      rowData[COLUMN_BITS_RANGE_IDX] = bitsInfo.getBitsRange();
      rowData[COLUMN_NAME_IDX] = bitsInfo.getName();
      rowData[COLUMN_DESCRIPTION_IDX] = bitsInfo.getDescription();
      rowData[COLUMN_TYPE_IDX] = bitsInfo.getType();
      rowData[COLUMN_RESET_VALUE_IDX] = bitsInfo.getResetValue();
      bitsInfos.addRow(rowData);
    }
    tbBitsInfosScroll.setMaximumSize(tbBitsInfosScroll.getPreferredSize());
    tbBitsInfosScroll.revalidate();
    final int row = chooseBitsInfosRow();
    if (row >= 0) {
      tbBitsInfos.setRowSelectionInterval(row, row);
      ensureCellIsVisible(row, 0);
    }
    sourceChanged();
  }

  /**
   * @return The number of the associated PIO (0…1), or -1, if the
   * selected register set is not related to any specific PIO.
   */
  public int getSelectedRegisterSetPio()
  {
    return ((RegistersSet)cbRegistersSet.getSelectedItem()).pioNum;
  }

  /**
   * @return The number of the associated SM (0…3), or -1, if the
   * selected register is not related to any specific SM.
   */
  public int getSelectedRegisterSm()
  {
    final RegistersDocs<? extends Enum<?>> register = getSelectedRegister();
    return register.getRegisterDetails().getSmNum();
  }

  public int getSelectedRegisterAddress()
  {
    final int baseAddress =
      ((RegistersSet)cbRegistersSet.getSelectedItem()).baseAddress;
    return baseAddress + 0x4 * cbRegister.getSelectedIndex();
  }

  public int getSelectedRegisterMsb()
  {
    final int minSelectionRow = tbBitsInfos.getSelectedRow();
    if (minSelectionRow < 0) return -1;
    final BitsRange upperMostBitsRange =
      (BitsRange)bitsInfos.getValueAt(minSelectionRow, COLUMN_BITS_RANGE_IDX);
    return upperMostBitsRange.getMsb();
  }

  public int getSelectedRegisterLsb()
  {
    final int maxSelectionRow =
      tbBitsInfos.getSelectedRow() + tbBitsInfos.getSelectedRowCount() - 1;
    if (maxSelectionRow < 0) return -1;
    final BitsRange lowerMostBitsRange =
      (BitsRange)bitsInfos.getValueAt(maxSelectionRow, COLUMN_BITS_RANGE_IDX);
    return lowerMostBitsRange.getLsb();
  }

  @Override
  public void setEnabled(final boolean enabled)
  {
    super.setEnabled(enabled);
    lbRegistersSet.setEnabled(enabled);
    cbRegistersSet.setEnabled(enabled);
    lbRegistersSetInfo.setEnabled(enabled);
    lbRegister.setEnabled(enabled);
    cbRegister.setEnabled(enabled);
    lbRegisterInfo.setEnabled(enabled);
    lbRegisterBitsInfos.setEnabled(enabled);
    lbRegisterBits.setEnabled(enabled);
    tbBitsInfos.setEnabled(enabled);
    tbBitsInfos.setRowSelectionAllowed(enabled);
    tbBitsInfosScroll.getHorizontalScrollBar().setEnabled(enabled);
    tbBitsInfosScroll.getVerticalScrollBar().setEnabled(enabled);
  }

  private void selectionChanged(final ListSelectionEvent selection)
  {
    if (!selection.getValueIsAdjusting()) {
      sourceChanged();
    }
  }

  private void resetInputs()
  {
    cbRegistersSet.setSelectedIndex(0);
    initRegistersForSelectedRegisterSet();
  }

  private void selectRegisterByAddress(final int address)
  {
    final RegistersSet registersSet = RegistersSet.fromAddress(address);
    if (registersSet != null) {
      cbRegistersSet.setSelectedItem(registersSet);
      registersSetSelected(registersSet);
      final int registerIndex = (address - registersSet.baseAddress) >> 2;
      if (registerIndex < cbRegister.getItemCount()) {
        cbRegister.setSelectedIndex(registerIndex);
        registerSelected(cbRegister.getItemAt(registerIndex));
      } else {
        final String message =
          String.format("warning: registers set %s: " +
                        "register not found for signal address 0x%8x",
                        registersSet, address);
        diagram.getConsole().println(message);
      }
    } else {
      final String message =
        String.format("warning: " +
                      "no registers set found for signal address 0x%8x",
                      address);
      diagram.getConsole().println(message);
      resetInputs();
    }
  }

  private void selectBitsRange(final int msb, final int lsb)
  {
    final int rowCount = tbBitsInfos.getRowCount();
    for (int row = 0; row < rowCount; row++) {
      final BitsRange bitsRange =
        (BitsRange)bitsInfos.getValueAt(row, COLUMN_BITS_RANGE_IDX);
      final int rangeMsb = bitsRange.getMsb();
      final int rangeLsb = bitsRange.getLsb();
      if ((rangeMsb <= msb) && (rangeLsb >= lsb)) {
        tbBitsInfos.addRowSelectionInterval(row, row);
      } else if (((rangeMsb <= msb) && (rangeMsb >= lsb)) ||
                 ((rangeLsb <= msb) && (rangeLsb >= lsb))) {
        final String message =
          String.format("warning: signal specifies bit range [%d:%d] not " +
                        "describable by register bit ranges; " +
                        "ignoring bit range [%d:%d]",
                        msb, lsb, rangeMsb, rangeLsb);
        diagram.getConsole().println(message);
      }
    }
  }

  public void load(final ValuedSignal<?> signal)
  {
    if (signal != null) {
      final SignalRendering.SignalParams signalParams =
        signal.getSignalParams();
      selectRegisterByAddress(signalParams.getAddress());
      if (signal instanceof RegisterBitSignal) {
        final int bit = ((RegisterBitSignal)signal).getBit();
        selectBitsRange(bit, bit);
      } else {
        selectBitsRange(signalParams.getMsb(), signalParams.getLsb());
      }
    } else {
      resetInputs();
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
