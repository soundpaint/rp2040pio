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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ScrollPaneConstants;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

  private final DiagramModel model;
  private final DiagramViewPanel diagramPanel;
  private final TelemetryPanel telemetryPanel;
  private final ScriptDialog scriptDialog;

  private Diagram(final PrintStream console, final String[] argv)
    throws IOException
  {
    super(APP_TITLE, APP_FULL_NAME, console, argv);
    model = new DiagramModel(console, getSDK());
    diagramPanel = new DiagramViewPanel(model);
    telemetryPanel =
      new TelemetryPanel(model, () -> diagramPanel.getLeftMostVisibleCycle());
    configureModel();
    modelChanged();
    add(createView());
    scriptDialog = new ScriptDialog(this, console);
    pack();
    setVisible(true);
    startUpdating();
  }

  private JPanel createView()
  {
    final JPanel view = new JPanel();
    view.setLayout(new BoxLayout(view, BoxLayout.PAGE_AXIS));
    final JScrollPane scrollPane =
      new JScrollPane(diagramPanel,
                      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    view.add(scrollPane);
    view.add(telemetryPanel);
    return view;
  }

  @Override
  protected ActionPanel createActionPanel(final PrintStream console)
  {
    return new ActionPanel(this);
  }

  @Override
  protected MenuBar createMenuBar(final PrintStream console)
  {
    return new MenuBar(this, getSDK(), console);
  }

  private void modelChanged()
  {
    diagramPanel.modelChanged();
    telemetryPanel.modelChanged();
  }

  @Override
  protected void updateView()
  {
    modelChanged();
  }

  public void showScriptDialog()
  {
    scriptDialog.setVisible(true);
  }

  private void configureModel() throws IOException
  {
    model.addSignal(SignalFactory.createRuler("cycle#")).setVisible(true);
    model.addSignal(SignalFactory.createClockSignal("clock")).setVisible(true);
    model.addSignal(PIOEmuRegisters.
                    getAddress(0, PIOEmuRegisters.Regs.SM0_CLK_ENABLE), 0);
    final GPIOIOBank0Registers.Regs regGpio0Status =
      GPIOIOBank0Registers.Regs.GPIO0_STATUS;
    for (int gpioNum = 0; gpioNum < 32; gpioNum++) {
      final String label = "GPIO" + gpioNum + " (out from peri)";
      final int address =
        GPIOIOBank0Registers.getGPIOAddress(gpioNum, regGpio0Status);
      model.addSignal(label + " Value", address, 8, 8).setVisible(gpioNum < 2);
      model.addSignal(label + " Level", address, 8);
    }
    final SDK sdk = getSDK();
    final Supplier<Boolean> noDelayFilter =
      ValueFilterPanel.createNoDelayFilter(sdk, 0, 0);
    final int addrSm0Pc =
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_PC);
    model.addSignal("SM0_PC", addrSm0Pc);
    model.addSignal("SM0_PC (hidden delay)", addrSm0Pc, noDelayFilter);
    final int instrAddr =
      PIORegisters.getAddress(0, PIORegisters.Regs.SM0_INSTR);
    final Signal instr1 =
      SignalFactory.createInstructionSignal(sdk, sdk.getPIO0SDK(), instrAddr,
                                            0, "SM0_INSTR",
                                            true, null);
    model.addSignal(instr1);
    final Signal instr2 =
      SignalFactory.createInstructionSignal(sdk, sdk.getPIO0SDK(), instrAddr,
                                            0, "SM0_INSTR (hidden delay)",
                                            true, noDelayFilter);
    model.addSignal(instr2).setVisible(true);
    final int addrSm0RegX =
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_REGX);
    model.addSignal("SM0_REGX", addrSm0RegX).setVisible(true);
    final int addrSm0RegY =
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_REGY);
    model.addSignal("SM0_REGY", addrSm0RegY);
  }

  public void clear()
  {
    model.resetSignals();
    modelChanged();
  }

  public void applyCycles(final int count) throws IOException
  {
    model.applyCycles(count);
    modelChanged();
    diagramPanel.ensureCycleIsVisible(model.getSignalSize() - 1);
  }

  public void setZoom(final int zoom)
  {
    diagramPanel.setZoom(zoom);
  }

  public void pullSignals(final List<Signal> signals)
  {
    model.pullSignals(signals);
    modelChanged();
  }

  public void pushSignals(final List<Signal> signals)
  {
    model.pushSignals(signals);
    modelChanged();
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
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
