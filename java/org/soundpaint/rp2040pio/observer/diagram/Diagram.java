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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import org.soundpaint.rp2040pio.Constants;
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

  public DiagramModel getModel()
  {
    return model;
  }

  @Override
  protected ActionPanel createActionPanel()
  {
    return new ActionPanel(this);
  }

  @Override
  protected MenuBar createMenuBar()
  {
    return new MenuBar(this, getSDK());
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

  /**
   * Add pseudo signals that are not directly displayed, but provided
   * for shared use for instruction rendering.
   */
  private void createInternalSignals() throws IOException
  {
    for (int pioNum = 0; pioNum < Constants.PIO_NUM; pioNum++) {
      for (int smNum = 0; smNum < Constants.SM_COUNT; smNum++) {
        final String labelPrefix = String.format("_PIO%d_SM%d_", pioNum, smNum);
        final int addressPinCtrl =
          PIORegisters.getSMAddress(pioNum, smNum,
                                    PIORegisters.Regs.SM0_PINCTRL);
        final String labelPinCtrl = labelPrefix + "PINCTRL";
        model.addInternalSignal(this, labelPinCtrl, addressPinCtrl);
        final int addressExecCtrl =
          PIORegisters.getSMAddress(pioNum, smNum,
                                    PIORegisters.Regs.SM0_EXECCTRL);
        final String labelExecCtrl = labelPrefix + "EXECCTRL";
        model.addInternalSignal(this, labelExecCtrl, addressExecCtrl);
        final int addressDelayCycle =
          PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                       PIOEmuRegisters.Regs.SM0_DELAY_CYCLE);
        final String labelDelayCycle = labelPrefix + "DELAY_CYCLE";
        model.addInternalSignal(this, labelDelayCycle, addressDelayCycle);
        final int addressDelay =
          PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                       PIOEmuRegisters.Regs.SM0_DELAY);
        final String labelDelay = labelPrefix + "DELAY";
        model.addInternalSignal(this, labelDelay, addressDelay);
        final int addressInstrOrigin =
          PIOEmuRegisters.getSMAddress(pioNum, smNum,
                                       PIOEmuRegisters.Regs.SM0_INSTR_ORIGIN);
        final String labelInstrOrigin = labelPrefix + "INSTR_ORIGIN";
        model.addInternalSignal(this, labelInstrOrigin, addressInstrOrigin);
      }
    }
  }

  private void configureModel() throws IOException
  {
    createInternalSignals();
    model.addSignal(SignalFactory.createRuler("cycle#")).setVisible(true);
    model.addSignal(SignalFactory.createClockSignal("clock")).setVisible(true);
    model.addSignal(this, "SM0_CLK_ENABLE", PIOEmuRegisters.
                    getAddress(0, PIOEmuRegisters.Regs.SM0_CLK_ENABLE), 0).
      setVisible(true);
    final GPIOIOBank0Registers.Regs regGpio0Status =
      GPIOIOBank0Registers.Regs.GPIO0_STATUS;
    for (int gpioNum = 0; gpioNum < 30; gpioNum++) {
      final String label = "GPIO" + gpioNum + " (out from peri)";
      final int address =
        GPIOIOBank0Registers.getGPIOAddress(gpioNum, regGpio0Status);
      model.addSignal(this, label + " Value", address, 8, 8).
        setVisible(gpioNum < 2);
      model.addSignal(this, label + " Level", address, 8, null, -1, -1);
    }
    final List<SignalFilter> noDelayFilter =
      ValueFilterPanel.createFilters(true, false);
    final int addrSm0Pc =
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_PC);
    model.addSignal(this, "SM0_PC", addrSm0Pc);
    model.addSignal(this, "SM0_PC (hidden delay)", addrSm0Pc,
                    noDelayFilter, 0, 0);
    final int instrAddr =
      PIORegisters.getAddress(0, PIORegisters.Regs.SM0_INSTR);
    final List<SignalFilter> displayFilters =
      ValueFilterPanel.createFilters(true, true);
    model.addSignal(SignalFactory.
                    createFromRegister(this, getSDK(), "PIO0_SM0_INSTR",
                                       instrAddr, 15, 0,
                                       SignalRendering.Mnemonic,
                                       displayFilters, 0, 0)).
      setVisible(true);
    final int addrSm0RegX =
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_REGX);
    model.addSignal(this, "SM0_REGX", addrSm0RegX).setVisible(true);
    final int addrSm0RegY =
      PIOEmuRegisters.getAddress(0, PIOEmuRegisters.Regs.SM0_REGY);
    model.addSignal(this, "SM0_REGY", addrSm0RegY);
  }

  public void clear()
  {
    model.resetSignals();
    modelChanged();
  }

  private void applyCycles(final int count) throws IOException
  {
    model.applyCycles(count);
    modelChanged();
    diagramPanel.ensureCycleIsVisible(model.getSignalSize() - 1);
    diagramPanel.rebuildToolTips();
  }

  public void applyCycles()
  {
    final int cycles = ((ActionPanel)getActionPanel()).getCycles();
    try {
      applyCycles(cycles);
    } catch (final IOException e) {
      final String title = "Emulation Failed";
      final String message = "I/O error: " + e.getMessage();
      JOptionPane.showMessageDialog(this, message, title,
                                    JOptionPane.WARNING_MESSAGE);
      clear();
    }
  }

  public void setZoom(final int zoom)
  {
    diagramPanel.setZoom(zoom);
  }

  public RegisterIntSignal getInternalSignalByAddress(final int address)
  {
    return model.getInternalSignalByAddress(address);
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
