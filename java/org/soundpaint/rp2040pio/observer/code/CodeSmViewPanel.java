/*
 * @(#)CodeSmViewPanel.java 1.00 21/04/24
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

public class CodeSmViewPanel extends JPanel
{
  private static final long serialVersionUID = 1576266124601249894L;

  private static final String lockedSymbol = ""; //"🔒 ";
  private static final String unlockedSymbol = ""; //"   ";
  private static final String wrapSymbol = "← ";
  private static final String wrapTargetSymbol = "→ ";
  private static final String selfWrapSymbol = "↔ ";
  private static final String noWrapSymbol = "  ";
  private static final String breakPointSymbol = ""; //"🛑";
  private static final String noBreakPointSymbol = ""; //" ";
  private static final String errorText = "error: connection to server lost";

  private static final Color fgDefault = Color.BLACK;
  private static final Color bgDefault = Color.WHITE;
  private static final Color fgCurrent = Color.WHITE;
  private static final Color bgCurrent = Color.RED;
  private static final Color fgCurrentInactive = new Color(0x9f9f9f);
  private static final Color bgCurrentInactive = new Color(0xb04f4f);

  public static final Font codeFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);

  private static class EmptySelectionModel extends DefaultListSelectionModel
  {
    private static final long serialVersionUID = -2981078454514535370L;

    private EmptySelectionModel()
    {
      setSelectionMode(SINGLE_SELECTION);
    }

    @Override
    public void setAnchorSelectionIndex(final int anchorIndex) {}

    @Override
    public void setLeadAnchorNotificationEnabled(final boolean flag) {}

    @Override
    public void setLeadSelectionIndex(final int leadIndex) {}

    @Override
    public void setSelectionInterval(final int index0, final int index1) {}
  }

  private static class Instruction
  {
    public boolean isCurrentAddress;
    public boolean isActive;
    public String text;
  }

  private static class InstructionRenderer extends DefaultListCellRenderer
  {
    private static final long serialVersionUID = -1347539595980280795L;

    public InstructionRenderer()
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
      final Instruction instruction = (Instruction)value;
      super.getListCellRendererComponent(list, value, index,
                                         isSelected, cellHasFocus);
      setText(instruction.text);
      if (instruction.isCurrentAddress) {
        if (instruction.isActive && list.isEnabled()) {
          setForeground(fgCurrent);
          setBackground(bgCurrent);
        } else {
          setForeground(fgCurrentInactive);
          setBackground(bgCurrentInactive);
        }
      }
      setFont(codeFont);
      return this;
    }
  }

  private final PrintStream console;
  private final SDK sdk;
  private final JProgressBar pbDelay;
  private final JTextField taForcedOrExecdInstruction;
  private final DefaultListModel<Instruction> instructions;
  private final JList<Instruction> lsInstructions;
  private int pioNum;
  private int smNum;
  private int lastPC;

  private CodeSmViewPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public CodeSmViewPanel(final PrintStream console, final SDK sdk,
                         final JProgressBar pbDelay,
                         final JTextField taForcedOrExecdInstruction)
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    Objects.requireNonNull(pbDelay);
    Objects.requireNonNull(taForcedOrExecdInstruction);
    this.console = console;
    this.sdk = sdk;
    this.pbDelay = pbDelay;
    this.taForcedOrExecdInstruction = taForcedOrExecdInstruction;
    instructions = new DefaultListModel<Instruction>();
    for (int address = 0; address < Constants.MEMORY_SIZE; address++) {
      instructions.addElement(new Instruction());
    }
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    lsInstructions = new JList<Instruction>(instructions);
    lsInstructions.setSelectionModel(new EmptySelectionModel());
    lsInstructions.setCellRenderer(new InstructionRenderer());
    add(new JScrollPane(lsInstructions));
    setPreferredSize(new Dimension(300, 200));
    lastPC = -1;
  }

  private boolean isClkEnabled() throws IOException
  {
    final int clkEnableAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_NEXT_CLK_ENABLE);
    final int clkEnable = sdk.readAddress(clkEnableAddress) & 0x1;
    return clkEnable != 0x0;
  }

  private int getPC() throws IOException
  {
    final int addressAddr =
      PIORegisters.getSMAddress(pioNum, smNum,
                                PIORegisters.Regs.SM0_ADDR);
    return
      sdk.readAddress(addressAddr) & (Constants.MEMORY_SIZE - 1);
  }

  private int getBreakPoints() throws IOException
  {
    final int addressBreakPoints =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_BREAKPOINTS);
    return sdk.readAddress(addressBreakPoints);
  }

  private int getPendingDelay() throws IOException
  {
    final int addressPendingDelay =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_PENDING_DELAY);
    final int pendingDelay = sdk.readAddress(addressPendingDelay);
    return pendingDelay & 0x1f;
  }

  private void updateInstructions() throws IOException
  {
    final PIOSDK pioSdk = pioNum == 0 ? sdk.getPIO0SDK() : sdk.getPIO1SDK();
    final int pc = getPC();
    final int memoryAllocation = pioSdk.getMemoryAllocation();

    final int addressExecCtrl =
      PIORegisters.getSMAddress(pioNum, smNum, PIORegisters.Regs.SM0_EXECCTRL);
    final int execCtrl = sdk.readAddress(addressExecCtrl);
    final int wrap =
      (execCtrl & Constants.SM0_EXECCTRL_WRAP_TOP_BITS) >>>
      Constants.SM0_EXECCTRL_WRAP_TOP_LSB;
    final int wrapTarget =
      (execCtrl & Constants.SM0_EXECCTRL_WRAP_BOTTOM_BITS) >>>
      Constants.SM0_EXECCTRL_WRAP_BOTTOM_LSB;

    final int breakPoints = getBreakPoints();
    final int pendingDelay = getPendingDelay();

    final int forcedInstrAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_FORCED_INSTR);
    final int forcedInstr = sdk.readAddress(forcedInstrAddress);
    final boolean haveForced = (forcedInstr & 0x00010000) != 0x0;
    final int forcedOpCode = haveForced ? forcedInstr & 0xffff : 0x0;

    final int execdInstrAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_EXECD_INSTR);
    final int execdInstr = sdk.readAddress(execdInstrAddress);
    final boolean haveExecd = (execdInstr & 0x00010000) != 0x0;
    final int execdOpCode = haveExecd ? execdInstr & 0xffff : 0x0;

    for (int address = 0; address < Constants.MEMORY_SIZE; address++) {
      final boolean isCurrentAddress = address == pc;
      final PIOSDK.InstructionInfo instructionInfo =
        pioSdk.getMemoryInstruction(smNum, address, true, true);
      final boolean isAllocated = ((memoryAllocation >>> address) & 0x1) != 0x0;
      final boolean isWrap = address == wrap;
      final boolean isWrapTarget = address == wrapTarget;
      final String displayWrap =
        isWrap ?
        (isWrapTarget ? selfWrapSymbol : wrapSymbol) :
        (isWrapTarget ? wrapTargetSymbol : noWrapSymbol);
      final boolean isBreakPoint = ((breakPoints >>> address) & 0x1) != 0x0;
      final String instructionText =
        String.format("%s%s%s%s%n",
                      (isBreakPoint ? breakPointSymbol : noBreakPointSymbol),
                      (isAllocated ? lockedSymbol : unlockedSymbol),
                      displayWrap,
                      instructionInfo.getFullStatement());
      final Instruction instruction = instructions.getElementAt(address);
      instruction.text = instructionText;
      instruction.isCurrentAddress = isCurrentAddress;
      instruction.isActive = (pendingDelay == 0) && !haveForced && !haveExecd;
    }
    final PIOSDK.InstructionInfo currentInstructionInfo =
      pioSdk.getCurrentInstruction(smNum, true, true);
    updateDelayDisplay(currentInstructionInfo, pendingDelay);
    final boolean isActive = pioSdk.smGetEnabled(smNum) && isClkEnabled();
    updateForcedOrExecdInstructionDisplay(pioSdk, haveForced, forcedOpCode,
                                          haveExecd, execdOpCode, isActive);
    lsInstructions.setEnabled(isActive);
    if (pc != lastPC) {
      lsInstructions.ensureIndexIsVisible(pc);
      lastPC = pc;
    }
  }

  private void updateForcedOrExecdInstructionDisplay(final PIOSDK pioSdk,
                                                     final boolean haveForced,
                                                     final int forcedOpCode,
                                                     final boolean haveExecd,
                                                     final int execdOpCode,
                                                     final boolean isActive)
    throws IOException
  {
    if (haveForced) {
      final PIOSDK.InstructionInfo forcedInstrInfo =
        pioSdk.getInstructionFromOpCode(smNum,
                                        Constants.INSTR_ORIGIN_FORCED,
                                        "", forcedOpCode, true, false, 0);
      taForcedOrExecdInstruction.setForeground(fgCurrent);
      taForcedOrExecdInstruction.setBackground(bgCurrent);
      taForcedOrExecdInstruction.setOpaque(true);
      final String displayText =
        String.format("  [f] %04x %s",
                      forcedOpCode, forcedInstrInfo.getFullStatement());
      taForcedOrExecdInstruction.setText(displayText);
    } else if (haveExecd) {
      final PIOSDK.InstructionInfo execdInstrInfo =
        pioSdk.getInstructionFromOpCode(smNum,
                                        Constants.INSTR_ORIGIN_EXECD,
                                        "", execdOpCode, true, false, 0);
      if (isActive) {
        taForcedOrExecdInstruction.setForeground(fgCurrent);
        taForcedOrExecdInstruction.setBackground(bgCurrent);
      } else {
        taForcedOrExecdInstruction.setForeground(fgCurrentInactive);
        taForcedOrExecdInstruction.setBackground(bgCurrentInactive);
      }
      taForcedOrExecdInstruction.setOpaque(true);
      final String displayText =
        String.format("  [x] %04x %s",
                      execdOpCode, execdInstrInfo.getFullStatement());
      taForcedOrExecdInstruction.setText(displayText);
    } else {
      taForcedOrExecdInstruction.setForeground(fgDefault);
      taForcedOrExecdInstruction.setBackground(bgDefault);
      taForcedOrExecdInstruction.setOpaque(false);
      taForcedOrExecdInstruction.setText("");
    }
  }

  private void updateDelayDisplay(final PIOSDK.InstructionInfo instructionInfo,
                                  final int pendingDelay)
    throws IOException
  {
    final int totalDelay = instructionInfo.getDelay();
    final int completedDelay = totalDelay - pendingDelay;
    final float progress;
    final String progressText;
    if (totalDelay == 0) {
      progress = 0.0f;
      progressText = "";
    } else {
      progress = ((float)completedDelay) / totalDelay;
      progressText =
        String.format("delay: %d of %d %s done", completedDelay, totalDelay,
                      totalDelay == 1 ? "cycle" : "cycles");
    }
    final int progressValue = Math.round(progress * 1000.0f);
    pbDelay.setString(progressText);
    pbDelay.setValue(progressValue);
  }

  private void checkedUpdateInstructions()
  {
    try {
      updateInstructions();
    } catch (final IOException e) {
      for (int address = 0; address < Constants.MEMORY_SIZE; address++) {
        instructions.getElementAt(address).text = errorText;
      }
    }
  }

  public void smChanged(final int pioNum, final int smNum)
  {
    this.pioNum = pioNum;
    this.smNum = smNum;
    final String toolTipText =
      String.format("code view for PIO%d, SM%d", pioNum, smNum);
    lsInstructions.setToolTipText(toolTipText);
    checkedUpdateInstructions();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
