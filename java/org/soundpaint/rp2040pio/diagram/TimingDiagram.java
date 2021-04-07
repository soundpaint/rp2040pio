/*
 * @(#)TimingDiagram.java 1.00 21/02/12
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
package org.soundpaint.rp2040pio.diagram;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.sdk.Program;
import org.soundpaint.rp2040pio.sdk.ProgramParser;
import org.soundpaint.rp2040pio.sdk.PIOSDK;
import org.soundpaint.rp2040pio.sdk.SDK;

/**
 * Framework for displaying a timing diagram resulting from an
 * emulation run.
 *
 * TODO: Ellipsis, see e.g. Fig. 55.
 *
 * TODO: Labelled external data via GPIO or DMA (e.g. data bits "D0",
 * "D1", "D2", ...).
 *
 * Syntax:
 * CLK=SIGNAL
 * DMA.SIGNAL_NAME=(SIGNAL|BIT)
 * SMx.SIGNAL_NAME=(SIGNAL|BIT)
 * GPIOx=(SIGNAL|BIT)
 */
public class TimingDiagram extends JFrame implements Constants
{
  private static final long serialVersionUID = -8853990994193814003L;

  private final PrintStream console;
  private final SDK sdk;
  private final PIOSDK pioSdk;
  private final DiagramConfig diagramConfig;
  private final DiagramPanel diagramPanel;
  private Program program;

  private TimingDiagram()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public TimingDiagram(final PrintStream console, final SDK sdk)
    throws IOException
  {
    super("Timing Diagram");
    if (console == null) {
      throw new NullPointerException("console");
    }
    if (sdk == null) {
      throw new NullPointerException("sdk");
    }
    this.console = console;
    this.sdk = sdk;
    pioSdk = sdk.getPIO0SDK();
    diagramConfig = new DiagramConfig();
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    getContentPane().add(diagramPanel = new DiagramPanel(diagramConfig));
    getContentPane().add(new ActionPanel(this), BorderLayout.SOUTH);
    program = null;
  }

  public DiagramConfig.Signal addSignal(final DiagramConfig.Signal signal)
  {
    if (signal == null) {
      throw new NullPointerException("signal");
    }
    diagramConfig.addSignal(signal);
    return signal;
  }

  public DiagramConfig.Signal addSignal(final String label, final int address,
                                        final int msb, final int lsb,
                                        final Supplier<Boolean> displayFilter)
    throws IOException
  {
    final DiagramConfig.ValuedSignal<Integer> signal =
      DiagramConfig.createFromRegister(sdk, label, address, msb, lsb,
                                       displayFilter);
    return addSignal(signal);
  }

  public DiagramConfig.Signal addSignal(final String label, final int address,
                                        final int msb, final int lsb)
    throws IOException
  {
    return addSignal(label, address, msb, lsb, null);
  }

  public DiagramConfig.Signal addSignal(final String label, final int address,
                                        final int bit)
    throws IOException
  {
    final DiagramConfig.BitSignal signal =
      DiagramConfig.createFromRegister(sdk, label, address, bit);
    return addSignal(signal);
  }

  public DiagramConfig.Signal addSignal(final int address, final int bit)
    throws IOException
  {
    return addSignal(null, address, bit);
  }

  public DiagramConfig.Signal addSignal(final String label, final int address)
    throws IOException
  {
    return addSignal(label, address, 31, 0);
  }

  public DiagramConfig.Signal addSignal(final String label, final int address,
                                        final Supplier<Boolean> displayFilter)
    throws IOException
  {
    return addSignal(label, address, 31, 0, displayFilter);
  }

  public DiagramConfig.Signal addSignal(final int address) throws IOException
  {
    return addSignal(null, address);
  }

  public DiagramConfig.Signal addSignal(final int address,
                                        final Supplier<Boolean> displayFilter)
    throws IOException
  {
    return addSignal(null, address, displayFilter);
  }

  public void create() throws IOException
  {
    diagramPanel.updatePreferredHeight();
    pack();
    setVisible(true);
  }

  public void clear()
  {
    for (final DiagramConfig.Signal signal : diagramConfig) {
      signal.reset();
    }
    SwingUtilities.invokeLater(() -> diagramPanel.repaint());
  }

  public void createSnapShot(final int stopCycle) throws IOException
  {
    sdk.reset();
    if (program != null) {
      pioSdk.addProgram(program);
    }
    pioSdk.setSmMaskEnabled((1 << SM_COUNT) - 1, false);
    pioSdk.restartSmMask(SM_COUNT - 1);
    // TODO: Enabling SM should be part of configuration and
    // replayed, whenever the simulation is restarted.
    pioSdk.setSmMaskEnabled(1, true);
    for (int pin = 0; pin < Constants.GPIO_NUM; pin++) {
      pioSdk.gpioInit(pin);
    }
    for (final DiagramConfig.Signal signal : diagramConfig) {
      signal.reset();
    }
    for (int cycle = 0; cycle < stopCycle; cycle++) {
      sdk.triggerCyclePhase0(true);
      for (final DiagramConfig.Signal signal : diagramConfig) {
        signal.record();
      }
      sdk.triggerCyclePhase1(true);
    }
    SwingUtilities.invokeLater(() -> diagramPanel.repaint());
  }

  public void setProgram(final String programResourcePath)
    throws IOException
  {
    program = ProgramParser.parse(programResourcePath);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
