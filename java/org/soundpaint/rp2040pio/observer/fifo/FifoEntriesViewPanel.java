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
import java.util.function.BiFunction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PIO;
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
  private static final String ARROW_LEFT = "←";
  private static final String ARROW_RIGHT = "→";

  private static enum ColorScheme
  {
    RX, TX;
  }

  private static enum StateColor
  {
    QUEUED(Color.WHITE, Color.RED, Color.WHITE, Color.GREEN),
    QUEUED_READ_PTR(Color.BLACK, Color.RED, Color.BLACK, Color.GREEN),
    READ_PTR(Color.BLACK, Color.GRAY, Color.BLACK, Color.GRAY),
    DEQUEUED(Color.LIGHT_GRAY, Color.GRAY, Color.LIGHT_GRAY, Color.GRAY);

    private final Color fgTX;
    private final Color bgTX;
    private final Color fgRX;
    private final Color bgRX;

    private StateColor(final Color fgTX, final Color bgTX,
                       final Color fgRX, final Color bgRX)
    {
      this.fgTX = fgTX;
      this.bgTX = bgTX;
      this.fgRX = fgRX;
      this.bgRX = bgRX;
    }

    public Color getFgColor(final ColorScheme colorScheme)
    {
      return colorScheme == ColorScheme.RX ? fgRX : fgTX;
    }

    public Color getBgColor(final ColorScheme colorScheme)
    {
      return colorScheme == ColorScheme.RX ? bgRX : bgTX;
    }
  }

  private final PrintStream console;
  private final SDK sdk;
  private final JLabel lbTopLine;
  private final JLabel lbBottomLine;
  private final JLabel[] lbEntries;
  private final JLabel[] lbSeparators;
  private final Integer[] buffer;
  private final JCheckBox cbFDebugTxStall;
  private final JCheckBox cbFDebugTxOver;
  private final JCheckBox cbFDebugRxUnder;
  private final JCheckBox cbFDebugRxStall;
  private final JLabel lbOsrLeftHandArrow;
  private final JLabel lbOsrRightHandArrow;
  private final JLabel lbIsrLeftHandArrow;
  private final JLabel lbIsrRightHandArrow;
  private final JLabel[] lbOsrBits;
  private final JLabel[] lbIsrBits;
  private final JCheckBox cbAutoPull;
  private final JCheckBox cbAutoPush;
  private int pioNum;
  private int smNum;
  private int rxLevel;
  private int txLevel;
  private boolean joinRx;
  private boolean joinTx;
  private boolean autoScroll;

  private FifoEntriesViewPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public FifoEntriesViewPanel(final PrintStream console, final SDK sdk,
                              final boolean initialAutoScroll)
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    this.console = console;
    this.sdk = sdk;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    lbTopLine = new JLabel();
    add(createTopLine());

    buffer = new Integer[2 * Constants.FIFO_DEPTH];
    lbSeparators = new JLabel[2 * Constants.FIFO_DEPTH + 1];
    lbEntries = new JLabel[2 * Constants.FIFO_DEPTH];
    add(createEntriesLine());

    lbBottomLine = new JLabel();
    add(createBottomLine());
    add(Box.createVerticalStrut(10));

    cbFDebugTxStall = new JCheckBox();
    cbFDebugTxOver = new JCheckBox();
    cbFDebugRxUnder = new JCheckBox();
    cbFDebugRxStall = new JCheckBox();
    add(createFDebugLine());
    add(Box.createVerticalStrut(10));

    lbOsrLeftHandArrow = new JLabel();
    lbOsrRightHandArrow = new JLabel();
    lbIsrLeftHandArrow = new JLabel();
    lbIsrRightHandArrow = new JLabel();
    lbOsrBits = new JLabel[33];
    lbIsrBits = new JLabel[33];
    cbAutoPull = new JCheckBox();
    cbAutoPush = new JCheckBox();
    add(createShiftRegisters());

    autoScroll = initialAutoScroll;
    SwingUtils.setPreferredWidthAsMaximum(this);
  }

  private Box createTopLine()
  {
    final Box topLine = new Box(BoxLayout.LINE_AXIS);
    lbTopLine.setFont(codeFont);
    topLine.add(lbTopLine);
    topLine.add(Box.createHorizontalGlue());
    return topLine;
  }

  private Box createEntriesLine()
  {
    final Box entriesLine = new Box(BoxLayout.LINE_AXIS);
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
    return entriesLine;
  }

  private Box createBottomLine()
  {
    final Box bottomLine = new Box(BoxLayout.LINE_AXIS);
    lbBottomLine.setFont(codeFont);
    bottomLine.add(lbBottomLine);
    bottomLine.add(Box.createHorizontalGlue());
    return bottomLine;
  }

  private Box createFDebugLine()
  {
    final Box hBox = new Box(BoxLayout.LINE_AXIS);
    hBox.add(new JLabel("FDEBUG:"));
    hBox.add(Box.createHorizontalStrut(20));
    createFDebugLineCheck(hBox, "TX Stall", cbFDebugTxStall);
    createFDebugLineCheck(hBox, "TX Over", cbFDebugTxOver);
    createFDebugLineCheck(hBox, "RX Under", cbFDebugRxUnder);
    createFDebugLineCheck(hBox, "RX Stall", cbFDebugRxStall);
    return hBox;
  }

  private void createFDebugLineCheck(final Box hBox, final String flagName,
                                     final JCheckBox cbFDebugFlag)
  {
    final JLabel lbFDebugFlag = new JLabel(flagName);
    lbFDebugFlag.setLabelFor(cbFDebugFlag);
    cbFDebugFlag.setEnabled(false);
    hBox.add(cbFDebugFlag);
    hBox.add(lbFDebugFlag);
    hBox.add(Box.createHorizontalGlue());
  }

  private Box createShiftRegisters()
  {
    final Box hBox = new Box(BoxLayout.LINE_AXIS);
    hBox.add(createShiftRegister("OSR Register",
                                 lbOsrLeftHandArrow, lbOsrRightHandArrow,
                                 lbOsrBits, "Pull",
                                 cbAutoPull));
    hBox.add(Box.createHorizontalGlue());
    hBox.add(createShiftRegister("ISR Register",
                                 lbIsrLeftHandArrow, lbIsrRightHandArrow,
                                 lbIsrBits, "Push",
                                 cbAutoPush));
    hBox.add(Box.createHorizontalGlue());
    return hBox;
  }

  private Box createShiftRegister(final String registerName,
                                  final JLabel lbLeftHandArrow,
                                  final JLabel lbRightHandArrow,
                                  final JLabel[] lbBits,
                                  final String actionName,
                                  final JCheckBox cbAuto)
  {
    final Box vBox = new Box(BoxLayout.PAGE_AXIS);
    vBox.setBorder(BorderFactory.createTitledBorder(registerName));
    vBox.add(createShiftRegisterContents(lbLeftHandArrow, lbRightHandArrow,
                                         lbBits));
    vBox.add(createAutoInfo(actionName, cbAuto));
    return vBox;
  }

  private Box createAutoInfo(final String actionName,
                             final JCheckBox cbAuto)
  {
    final Box hBox = new Box(BoxLayout.LINE_AXIS);
    final JLabel lbAuto = new JLabel("Auto " + actionName);
    lbAuto.setLabelFor(cbAuto);
    cbAuto.setEnabled(false);
    hBox.add(cbAuto);
    hBox.add(lbAuto);
    hBox.add(Box.createHorizontalGlue());
    return hBox;
  }

  private Box createShiftRegisterContents(final JLabel lbLeftHandArrow,
                                          final JLabel lbRightHandArrow,
                                          final JLabel[] lbBits)
  {
    final Box hBox = new Box(BoxLayout.LINE_AXIS);
    hBox.add(lbLeftHandArrow);
    for (int index = 0; index < lbBits.length; index++) {
      final JLabel lbBit = new JLabel();
      lbBit.setFont(codeFont);
      lbBits[index] = lbBit;
      hBox.add(lbBit);
    }
    hBox.add(lbRightHandArrow);
    unsetShiftReg(lbLeftHandArrow, lbRightHandArrow, lbBits);
    return hBox;
  }

  private int getShiftCtrl() throws IOException
  {
    final int addressShiftCtrl =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_SHIFTCTRL);
    return sdk.readAddress(addressShiftCtrl);
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

  private void updateEntries(final int entryOffs, final int entryCount,
                             final int readPtr, final int level,
                             final ColorScheme colorScheme)
  {
    int entryPtr = autoScroll ? readPtr : entryOffs;
    for (int displayedEntry = 0; displayedEntry < entryCount; displayedEntry++) {
      final boolean isQueued =
        autoScroll ?
        displayedEntry < level :
        level > 0 &&
        (((entryCount + entryPtr - readPtr) & (entryCount - 1)) < level);
      final boolean isReadPtr =
        autoScroll ?
        (readPtr >= 0) && (displayedEntry == 0) :
        entryPtr == readPtr;
      final Integer data = entryPtr >= 0 ? buffer[entryPtr] : null;
      final JLabel lbEntry = lbEntries[entryOffs + displayedEntry];
      lbEntry.setText(data != null ? String.format("%08x", data) : "  ????  ");
      final StateColor stateColor =
        isQueued ?
        (isReadPtr ? StateColor.QUEUED_READ_PTR : StateColor.QUEUED) :
        (isReadPtr ? StateColor.READ_PTR : StateColor.DEQUEUED);
      lbEntry.setOpaque(isQueued);
      lbEntry.setForeground(stateColor.getFgColor(colorScheme));
      lbEntry.setBackground(stateColor.getBgColor(colorScheme));
      if (entryPtr >= 0) {
        entryPtr = ((entryPtr + 1) & (entryCount - 1)) + entryOffs;
      }
    }
  }

  private void updateEntries(final int shiftCtrl) throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    updateFifoContents();
    final int smJoin =
      (shiftCtrl >>> Constants.SM0_SHIFTCTRL_FJOIN_TX_LSB) & 0x3;
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
        updateEntries(0, 2 * Constants.FIFO_DEPTH,
                      -1, 0, ColorScheme.RX);
      } else {
        updateEntries(0, 2 * Constants.FIFO_DEPTH,
                      txReadPtr, txLevel, ColorScheme.TX);
      }
    } else {
      if (fJoinRX) {
        updateEntries(0, 2 * Constants.FIFO_DEPTH,
                      rxReadPtr, rxLevel, ColorScheme.RX);
      } else {
        updateEntries(0, Constants.FIFO_DEPTH,
                      txReadPtr, txLevel, ColorScheme.TX);
        updateEntries(Constants.FIFO_DEPTH, Constants.FIFO_DEPTH,
                      rxReadPtr, rxLevel, ColorScheme.RX);
      }
    }
  }

  private boolean getFDebug(final int smNum, final int fDebugValue,
                            final int lsb, final int bits)
  {
    return (((fDebugValue & bits) >>> (lsb + smNum)) & 0x01) != 0x0;
  }

  private void updateFDebugStatus() throws IOException
  {
    final int addressFDebug =
      PIORegisters.getAddress(pioNum, PIORegisters.Regs.FDEBUG);
    final int fDebugValue = sdk.readAddress(addressFDebug);
    cbFDebugTxStall.
      setSelected(getFDebug(smNum, fDebugValue,
                            Constants.FDEBUG_TXSTALL_LSB,
                            Constants.FDEBUG_TXSTALL_BITS));
    cbFDebugTxOver.
      setSelected(getFDebug(smNum, fDebugValue,
                            Constants.FDEBUG_TXOVER_LSB,
                            Constants.FDEBUG_TXOVER_BITS));
    cbFDebugRxUnder.
      setSelected(getFDebug(smNum, fDebugValue,
                            Constants.FDEBUG_RXUNDER_LSB,
                            Constants.FDEBUG_RXUNDER_BITS));
    cbFDebugRxStall.
      setSelected(getFDebug(smNum, fDebugValue,
                            Constants.FDEBUG_RXSTALL_LSB,
                            Constants.FDEBUG_RXSTALL_BITS));
  }

  private void unsetShiftReg(final JLabel lbLeftHandArrow,
                             final JLabel lbRightHandArrow,
                             final JLabel[] lbBits)
  {
    lbLeftHandArrow.setText("?");
    lbRightHandArrow.setText("?");
    for (int index = 0; index < lbBits.length; index++) {
      final JLabel lbBit = lbBits[index];
      lbBit.setText("?");
      lbBit.setForeground(Color.LIGHT_GRAY);
      lbBit.setBackground(Color.GRAY);
      lbBit.setOpaque(false);
    }
  }

  private void updateShiftReg(final BiFunction<Integer, Integer, Boolean>
                              levelComparator,
                              final Color activeColor,
                              final JLabel lbLeftHandArrow,
                              final JLabel lbRightHandArrow,
                              final JLabel[] lbBits,
                              final PIOEmuRegisters.Regs regShiftReg,
                              final PIOEmuRegisters.Regs levelReg,
                              final int threshold,
                              final PIO.ShiftDir shiftDir)
    throws IOException
  {
    final int addressShiftReg =
      PIOEmuRegisters.getSMAddress(pioNum, smNum, regShiftReg);
    final int value = sdk.readAddress(addressShiftReg);
    final int addressLevel =
      PIOEmuRegisters.getSMAddress(pioNum, smNum, levelReg);
    final int level = sdk.readAddress(addressLevel);
    final int labelNum = lbBits.length;
    final int bitNum = labelNum - 1;
    for (int bitIndex = 0; bitIndex < bitNum; bitIndex++) {
      final int bitValue = (value >>> bitIndex) & 0x1;
      final int bitIndexAfterShiftDir;
      final int thresholdAfterShiftDir;
      switch (shiftDir) {
      case SHIFT_LEFT:
        thresholdAfterShiftDir = threshold;
        bitIndexAfterShiftDir = bitIndex;
        break;
      default:
        thresholdAfterShiftDir = bitNum -threshold;
        bitIndexAfterShiftDir = bitNum - bitIndex - 1;
        break;
      }
      final JLabel lbBit =
        lbBits[bitNum - bitIndex -
               (bitIndex >= thresholdAfterShiftDir ? 1 : 0)];
      lbBit.setText(bitValue == 0x1 ? "1" : "0");
      if (levelComparator.apply(bitIndexAfterShiftDir, level)) {
        lbBit.setForeground(Color.BLACK);
        lbBit.setBackground(activeColor);
        lbBit.setOpaque(true);
      } else {
        lbBit.setForeground(Color.LIGHT_GRAY);
        lbBit.setBackground(Color.GRAY);
        lbBit.setOpaque(false);
      }
    }
    final JLabel lbThreshold;
    switch (shiftDir) {
    case SHIFT_LEFT:
      lbThreshold = lbBits[labelNum - threshold - 1];
      break;
    default:
      lbThreshold = lbBits[threshold];
      break;
    }
    lbThreshold.setText("|");
    lbThreshold.setForeground(Color.BLACK);
    lbThreshold.setBackground(Color.GRAY);
    lbThreshold.setOpaque(false);
    switch (shiftDir) {
    case SHIFT_LEFT:
      lbLeftHandArrow.setText(ARROW_LEFT);
      lbRightHandArrow.setText(ARROW_LEFT);
      break;
    case SHIFT_RIGHT:
      lbLeftHandArrow.setText(ARROW_RIGHT);
      lbRightHandArrow.setText(ARROW_RIGHT);
      break;
    default:
      lbLeftHandArrow.setText("?");
      lbRightHandArrow.setText("?");
      break;
    }
  }

  private int getThreshold(final int shiftCtrl, final int bits, final int lsb)
  {
    final int thresholdValue = (shiftCtrl & bits) >>> lsb;
    return thresholdValue == 0 ? 32 : thresholdValue;
  }

  private void updateShiftRegs(final int shiftCtrl) throws IOException
  {
    final int outShiftDir =
      (shiftCtrl & Constants.SM0_SHIFTCTRL_OUT_SHIFTDIR_BITS) >>>
      Constants.SM0_SHIFTCTRL_OUT_SHIFTDIR_LSB;
    updateShiftReg((bit, level) -> bit >= level, Color.RED,
                   lbOsrLeftHandArrow, lbOsrRightHandArrow, lbOsrBits,
                   PIOEmuRegisters.Regs.SM0_OSR,
                   PIOEmuRegisters.Regs.SM0_OSR_SHIFT_COUNT,
                   getThreshold(shiftCtrl,
                                Constants.SM0_SHIFTCTRL_PULL_THRESH_BITS,
                                Constants.SM0_SHIFTCTRL_PULL_THRESH_LSB),
                   PIO.ShiftDir.fromValue(outShiftDir));
    cbAutoPull.setSelected((shiftCtrl &
                            Constants.SM0_SHIFTCTRL_AUTOPULL_BITS) != 0x0);
    final int inShiftDir =
      (shiftCtrl & Constants.SM0_SHIFTCTRL_IN_SHIFTDIR_BITS) >>>
      Constants.SM0_SHIFTCTRL_IN_SHIFTDIR_LSB;
    updateShiftReg((bit, level) -> bit < level, Color.GREEN,
                   lbIsrLeftHandArrow, lbIsrRightHandArrow, lbIsrBits,
                   PIOEmuRegisters.Regs.SM0_ISR,
                   PIOEmuRegisters.Regs.SM0_ISR_SHIFT_COUNT,
                   getThreshold(shiftCtrl,
                                Constants.SM0_SHIFTCTRL_PUSH_THRESH_BITS,
                                Constants.SM0_SHIFTCTRL_PUSH_THRESH_LSB),
                   PIO.ShiftDir.fromValue(inShiftDir));
    cbAutoPush.setSelected((shiftCtrl &
                            Constants.SM0_SHIFTCTRL_AUTOPUSH_BITS) != 0x0);
  }

  private void checkedUpdate()
  {
    try {
      final int shiftCtrl = getShiftCtrl();
      updateEntries(shiftCtrl);
      updateFDebugStatus();
      updateShiftRegs(shiftCtrl);
    } catch (final IOException e) {
      for (int entryNum = 0; entryNum < Constants.SM_COUNT; entryNum++) {
        buffer[entryNum] = null;
      }
      unsetShiftReg(lbOsrLeftHandArrow, lbOsrRightHandArrow, lbOsrBits);
      unsetShiftReg(lbIsrLeftHandArrow, lbIsrRightHandArrow, lbIsrBits);
    }
  }

  public void smChanged(final int pioNum, final int smNum)
  {
    this.pioNum = pioNum;
    this.smNum = smNum;
    final String toolTipText =
      String.format("FIFO registers view for PIO%d, SM%d", pioNum, smNum);
    setToolTipText(toolTipText);
    checkedUpdate();
  }

  public void setAutoScroll(final boolean autoScroll)
  {
    this.autoScroll = autoScroll;
    checkedUpdate();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
