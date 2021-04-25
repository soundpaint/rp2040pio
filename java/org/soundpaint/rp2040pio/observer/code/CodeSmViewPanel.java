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
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
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
  private static final Font codeFont = new Font(Font.MONOSPACED,
                                                Font.PLAIN, 12);
  private static final String errorText = "error: connection to server lost";

  private static class Instruction
  {
    public boolean isCurrentAddress;
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
        setBackground(Color.RED);
        setForeground(Color.WHITE);
      }
      setFont(codeFont);
      return this;
    }
  }

  private final PrintStream console;
  private final SDK sdk;
  private final JProgressBar pbDelay;
  private final DefaultListModel<Instruction> instructions;
  private final JList<Instruction> lsInstructions;
  private int pioNum;
  private int smNum;
  private int totalDelay;
  private int pendingDelay;
  private String progressText;
  private int progressValue;

  private CodeSmViewPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public CodeSmViewPanel(final PrintStream console, final SDK sdk,
                         final JProgressBar pbDelay)
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    Objects.requireNonNull(pbDelay);
    this.console = console;
    this.sdk = sdk;
    this.pbDelay = pbDelay;
    instructions = new DefaultListModel<Instruction>();
    for (int address = 0; address < Constants.MEMORY_SIZE; address++) {
      instructions.addElement(new Instruction());
    }
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    lsInstructions = new JList<Instruction>(instructions);
    lsInstructions.setCellRenderer(new InstructionRenderer());
    add(new JScrollPane(lsInstructions));
    setPreferredSize(new Dimension(300, 200));
    repaintLater();
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
    final int pendingDelay = getPendingDelay();

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
      if (isCurrentAddress) {
        updateDelayDisplay(instruction, pendingDelay,
                           instructionInfo.getDelay());
      }
    }
    lsInstructions.ensureIndexIsVisible(pc);
  }

  private void updateDelayDisplay(final Instruction instruction,
                                  final int pendingDelay, final int totalDelay)
  {
    final int completedDelay = totalDelay - pendingDelay;
    final float progress;
    final String progressText;
    if (totalDelay == 0) {
      progress = 0.0f;
      progressText = "";
    } else {
      progress = ((float)completedDelay) / totalDelay;
      progressText =
        String.format("%d of %d cycles", completedDelay, totalDelay);
    }
    final int progressValue = Math.round(progress * 1000.0f);
    this.progressText = progressText;
    this.progressValue = progressValue;
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
    checkedUpdateInstructions();
  }

  public void repaintLater()
  {
    SwingUtilities.invokeLater(() -> {
        pbDelay.setString(progressText);
        pbDelay.setValue(progressValue);
        final String toolTipText =
          String.format("code view for PIO%d, SM%d", pioNum, smNum);
        lsInstructions.setToolTipText(toolTipText);
        for (int address = 0; address < Constants.MEMORY_SIZE; address++) {
          final Instruction instruction = instructions.getElementAt(address);
          instructions.setElementAt(instruction, address); // trigger repaint
        }
      });
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
