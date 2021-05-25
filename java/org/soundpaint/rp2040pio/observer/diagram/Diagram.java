/*
 * @(#)Diagram.java 1.00 21/01/31
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

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.observer.GUIObserver;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Draws a timing diagram of a selected set of PIO signals and
 * generated from a specific PIO program.  By now, most of the
 * configuration is hard-wired in order to make it running out of the
 * box.
 *
 * TODO: Ellipsis, see e.g. Fig. 55.
 *
 * TODO: Labelled external data via GPIO or DMA (e.g. data bits "D0",
 * "D1", "D2", …).
 *
 * Syntax:
 * CLK=SIGNAL
 * DMA.SIGNAL_NAME=(SIGNAL|BIT)
 * SMx.SIGNAL_NAME=(SIGNAL|BIT)
 * GPIOx=(SIGNAL|BIT)
 */
public class Diagram extends GUIObserver
{
  private static final long serialVersionUID = -2547071637413332775L;

  private static final String APP_TITLE = "Diagram Creator";
  private static final String APP_FULL_NAME =
    "Timing Diagram Creator Version 0.1";

  private final DiagramConfig diagramConfig;
  private final TimingDiagram diagramData;
  private final DiagramPanel diagramPanel;
  private final ScriptDialog scriptDialog;

  private Diagram(final PrintStream console, final String[] argv)
    throws IOException
  {
    super(APP_TITLE, APP_FULL_NAME, console, argv);
    diagramConfig = new DiagramConfig();
    diagramPanel = new DiagramPanel(diagramConfig);
    diagramData =
      new TimingDiagram(console, getSDK(), diagramConfig, diagramPanel);
    configureDiagramData();
    add(diagramPanel);
    diagramPanel.updatePreferredHeight();
    scriptDialog = new ScriptDialog(this, console);
    pack();
    setVisible(true);
    startUpdating();
  }

  @Override
  protected ActionPanel createActionPanel(final PrintStream console)
  {
    return new ActionPanel(this);
  }

  @Override
  protected MenuBar createMenuBar(final PrintStream console)
  {
    return new MenuBar(this, console);
  }

  @Override
  protected void updateView()
  {
    diagramData.checkForUpdate();
    SwingUtilities.invokeLater(() -> diagramPanel.repaint());
  }

  public void showScriptDialog()
  {
    scriptDialog.setVisible(true);
  }

  private static Supplier<Boolean> createDelayFilter(final SDK sdk,
                                                     final int pioNum,
                                                     final int smNum)
  {
    final Supplier<Boolean> displayFilter = () -> {
      final int smDelayCycleAddress =
      PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                   PIOEmuRegisters.Regs.SM0_DELAY_CYCLE);
      try {
        final boolean isDelayCycle =
          sdk.readAddress(smDelayCycleAddress) != 0x0;
        return !isDelayCycle;
      } catch (final IOException e) {
        // TODO: Maybe log warning that we failed to evaluate delay?
        return false;
      }
    };
    return displayFilter;
  }

  private void configureDiagramData() throws IOException
  {
    diagramData.addSignal(DiagramConfig.createClockSignal("clock")).
      setVisible(true);

    diagramData.addSignal(PIOEmuRegisters.
                          getAddress(0, PIOEmuRegisters.Regs.SM0_CLK_ENABLE), 0).
      setVisible(true);
    final GPIOIOBank0Registers.Regs regGpio0Status =
      GPIOIOBank0Registers.Regs.GPIO0_STATUS;
    for (int gpioNum = 0; gpioNum < 32; gpioNum++) {
      final String label = "GPIO " + gpioNum;
      final int address =
        GPIOIOBank0Registers.getGPIOAddress(gpioNum, regGpio0Status);
      final DiagramConfig.Signal signal =
        diagramData.addSignal(label, address, 8, 8);
      if (gpioNum == 0) signal.setVisible(true);
    }
    final SDK sdk = getSDK();
    final int addrSm0Pc =
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_PC);
    diagramData.addSignal("SM0_PC", addrSm0Pc);
    diagramData.addSignal("SM0_PC (hidden delay)",
                          addrSm0Pc, createDelayFilter(sdk, 0, 0)).
      setVisible(true);
    final int instrAddr =
      PIORegisters.getAddress(0, PIORegisters.Regs.SM0_INSTR);
    final DiagramConfig.Signal instr1 =
      DiagramConfig.createInstructionSignal(sdk, sdk.getPIO0SDK(), instrAddr,
                                            0, "SM0_INSTR",
                                            true, null);
    diagramData.addSignal(instr1);
    final DiagramConfig.Signal instr2 =
      DiagramConfig.createInstructionSignal(sdk, sdk.getPIO0SDK(), instrAddr,
                                            0, "SM0_INSTR (hidden delay)",
                                            true, createDelayFilter(sdk, 0, 0));
    instr2.setVisible(true);
    diagramData.addSignal(instr2);
  }

  public void clear()
  {
    diagramData.clear();
    SwingUtilities.invokeLater(() -> diagramPanel.repaint());
  }

  public void executeCycles(final int cycles) throws IOException
  {
    diagramData.executeCycles(cycles);
    SwingUtilities.invokeLater(() -> diagramPanel.repaint());
  }

  public static void main(final String argv[])
  {
    final PrintStream console = System.out;
    try {
      new Diagram(System.out, argv);
    } catch (final IOException e) {
      console.println(e.getMessage());
    }
  }

  public void fillInCurrentSignals(final List<DiagramConfig.Signal> signals)
  {
    diagramData.fillInCurrentSignals(signals);
    SwingUtilities.invokeLater(() -> diagramPanel.repaint());
  }

  public void updateListOfSignals(final List<DiagramConfig.Signal> signals)
  {
    diagramData.updateListOfSignals(signals);
    SwingUtilities.invokeLater(() -> diagramPanel.repaint());
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
