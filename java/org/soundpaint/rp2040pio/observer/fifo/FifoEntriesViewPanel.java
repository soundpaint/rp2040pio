/*
 * @(#)FifoEntriesViewPanel.java 1.00 21/04/28
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

public class FifoEntriesViewPanel extends JPanel
{
  private static final long serialVersionUID = 2577996075333519551L;
  private static final Font codeFont = new Font(Font.MONOSPACED,
                                                Font.PLAIN, 12);
  private static final String errorText = "error: connection to server lost";
  private static final String
    txtFJoinNone =
    "┌───────────────── TX ─────────────────┐┌───────────────── RX ─────────────────┐";
  private static final String
    txtFJoinTX =
    "┌───────────────────────────────────── TX ─────────────────────────────────────┐";
  private static final String
    txtFJoinRX =
    "┌───────────────────────────────────── RX ─────────────────────────────────────┐";
  private static final String
    txtFJoinBoth =
    "┌─────────────────────────────────── (NONE) ───────────────────────────────────┐";
  private static final String
    txtBottomUnjoined =
    "└──────────────────────────────────────┘└──────────────────────────────────────┘";
  private static final String
    txtBottomJoined =
    "└──────────────────────────────────────────────────────────────────────────────┘";

  private final PrintStream console;
  private final SDK sdk;
  private JLabel lbTopLine;
  private JLabel lbBottomLine;
  private final JLabel[] lbEntries;
  private final JLabel[] lbSeparators;
  private final Integer[] buffer;
  private int pioNum;
  private int smNum;
  private int rxLevel;
  private int txLevel;
  private boolean joinRx;
  private boolean joinTx;
  private boolean autoRotate;

  private FifoEntriesViewPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public FifoEntriesViewPanel(final PrintStream console, final SDK sdk)
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    this.console = console;
    this.sdk = sdk;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    final Box topLine = new Box(BoxLayout.X_AXIS);
    add(topLine);
    lbTopLine = new JLabel();
    lbTopLine.setFont(codeFont);
    topLine.add(lbTopLine);
    topLine.add(Box.createHorizontalGlue());
    final Box entriesLine = new Box(BoxLayout.X_AXIS);
    add(entriesLine);
    buffer = new Integer[2 * Constants.FIFO_DEPTH];
    lbSeparators = new JLabel[2 * Constants.FIFO_DEPTH + 1];
    lbEntries = new JLabel[2 * Constants.FIFO_DEPTH];
    for (int entryNum = 0; entryNum < 2 * Constants.FIFO_DEPTH; entryNum++) {
      final JLabel lbSeparator = new JLabel(entryNum == 0 ? "│" : "  ");
      lbSeparator.setOpaque(true);
      lbSeparator.setFont(codeFont);
      lbSeparators[entryNum] = lbSeparator;
      entriesLine.add(lbSeparator);
      final JLabel lbEntry = new JLabel();
      lbEntry.setOpaque(true);
      lbEntry.setFont(codeFont);
      lbEntries[entryNum] = lbEntry;
      entriesLine.add(lbEntry);
    }
    final JLabel lbSeparator = new JLabel("│");
    lbSeparator.setOpaque(true);
    lbSeparator.setFont(codeFont);
    lbSeparators[2 * Constants.FIFO_DEPTH] = lbSeparator;
    entriesLine.add(lbSeparator);
    entriesLine.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(entriesLine);
    final Box bottomLine = new Box(BoxLayout.X_AXIS);
    add(bottomLine);
    lbBottomLine = new JLabel();
    lbBottomLine.setFont(codeFont);
    bottomLine.add(lbBottomLine);
    bottomLine.add(Box.createHorizontalGlue());
    repaintLater();
  }

  private int getSMJoin() throws IOException
  {
    final int addressShiftCtrl =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_SHIFTCTRL);
    final int shiftCtrl = sdk.readAddress(addressShiftCtrl);
    return (shiftCtrl >>> 30) & 0x3;
  }

  private int getSMFReadPtr() throws IOException
  {
    final int addressFReadPtr =
      PIOEmuRegisters.getAddress(pioNum, PIOEmuRegisters.Regs.FREAD_PTR);
    final int fReadPtr = sdk.readAddress(addressFReadPtr);
    return (fReadPtr >>> (smNum << 3)) & 0xff;
  }

  private int getSMFLevel() throws IOException
  {
    final int addressFLevel =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.FLEVEL);
    final int fLevel = sdk.readAddress(addressFLevel);
    return (fLevel >>> (smNum << 3)) & 0xff;
  }

  private int getSMFStat() throws IOException
  {
    final int addressFStat =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.FSTAT);
    final int fStat = sdk.readAddress(addressFStat);
    return fStat >>> smNum;
  }

  private void updateFifoContents() throws IOException
  {
    final int addressFifo =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_FIFO_MEM0);
    for (int entryNum = 0; entryNum < 2 * Constants.FIFO_DEPTH; entryNum++) {
      buffer[entryNum] = sdk.readAddress(addressFifo + (entryNum << 2));
    }
  }

  private void updateEntries(final int startEntryNum, final int entryCount,
                             final int readPtr, final int level)
  {
    final int stopEntryNum = startEntryNum + entryCount;
    for (int entryNum = startEntryNum;
         entryNum < 2 * Constants.FIFO_DEPTH; entryNum++) {
      final boolean isFirst;
      final boolean isLast;
      if (autoRotate) {
        isFirst = entryNum == startEntryNum;
        isLast = entryNum == startEntryNum + level - 1;
      } else {
        isFirst = entryNum == startEntryNum + readPtr;
        isLast = entryNum == startEntryNum + readPtr + level - 1;
      }
      final Integer data = buffer[entryNum];
      final JLabel lbEntry = lbEntries[entryNum];
      lbEntry.setText(data != null ? String.format("%08x", data) : "     ???");
      if (isFirst && isLast) {
        lbEntry.setBackground(Color.DARK_GRAY);
        lbEntry.setForeground(Color.LIGHT_GRAY);
      } else if (isFirst) {
        lbEntry.setBackground(Color.GREEN);
        lbEntry.setForeground(Color.BLACK);
      } else if (isLast) {
        lbEntry.setBackground(Color.RED);
        lbEntry.setForeground(Color.WHITE);
      } else {
        lbEntry.setBackground(Color.WHITE);
        lbEntry.setForeground(Color.BLACK);
      }
    }
  }

  private void updateEntries() throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    updateFifoContents();
    final int smJoin = getSMJoin();
    final boolean fJoinTX = (smJoin & 0x1) != 0x0;
    final boolean fJoinRX = (smJoin & 0x2) != 0x0;
    final int smfReadPtr = getSMFReadPtr();
    final int txReadPtr = smfReadPtr & 0xf;
    final int rxReadPtr = (smfReadPtr & 0xf0) >> 4;
    final int smfLevel = getSMFLevel();
    final int txLevel = smfLevel & 0xf;
    final int rxLevel = (smfLevel >>> 4) & 0xf;
    lbBottomLine.setText(fJoinTX || fJoinRX ?
                         txtBottomJoined : txtBottomUnjoined);
    lbSeparators[Constants.FIFO_DEPTH].setText(fJoinTX || fJoinRX ? "  " : "||");
    if (fJoinTX) {
      lbTopLine.setText(fJoinRX ? txtFJoinBoth : txtFJoinTX);
    } else {
      lbTopLine.setText(fJoinRX ? txtFJoinRX : txtFJoinNone);
    }
    if (fJoinTX) {
      if (fJoinRX) {
        updateEntries(0, 2 * Constants.FIFO_DEPTH, -1, 0);
      } else {
        updateEntries(0, 2 * Constants.FIFO_DEPTH, txReadPtr, txLevel);
      }
    } else {
      if (fJoinRX) {
        updateEntries(0, 2 * Constants.FIFO_DEPTH, rxReadPtr, rxLevel);
      } else {
        updateEntries(0, Constants.FIFO_DEPTH, txReadPtr, txLevel);
        updateEntries(Constants.FIFO_DEPTH, Constants.FIFO_DEPTH,
                      rxReadPtr, txLevel);
      }
    }
  }

  private void checkedUpdateEntries()
  {
    try {
      updateEntries();
    } catch (final IOException e) {
      for (int entryNum = 0; entryNum < Constants.MEMORY_SIZE; entryNum++) {
        buffer[entryNum] = null;
      }
    }
  }

  public void smChanged(final int pioNum, final int smNum)
  {
    this.pioNum = pioNum;
    this.smNum = smNum;
    checkedUpdateEntries();
  }

  public void setAutoRotate(final boolean autoRotate)
  {
    this.autoRotate = autoRotate;
  }

  public void repaintLater()
  {
    SwingUtilities.invokeLater(() -> {
        final String toolTipText =
          String.format("FIFO registers view for PIO%d, SM%d", pioNum, smNum);
        setToolTipText(toolTipText);
      });
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
