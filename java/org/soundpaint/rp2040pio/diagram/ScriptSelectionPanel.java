/*
 * @(#)ScriptSelectionPanel.java 1.00 21/04/09
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

import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.soundpaint.rp2040pio.IOUtils;
import org.soundpaint.rp2040pio.monitor.Monitor;

public class ScriptSelectionPanel extends Box
{
  private static final long serialVersionUID = 3945227462166267605L;

  private final PrintStream console;
  private final JFileChooser scriptFileChooser;

  public ScriptSelectionPanel(final PrintStream console)
  {
    super(BoxLayout.Y_AXIS);
    Objects.requireNonNull(console);
    this.console = console;
    scriptFileChooser = createScriptFileChooser();
    createExampleSelectionPanel();
    createFileSelectionPanel();
    add(Box.createVerticalGlue());
  }

  private JFileChooser createScriptFileChooser()
  {
    final JFileChooser scriptFileChooser = new JFileChooser();
    scriptFileChooser.setDialogTitle("Select Monitor Script");
    scriptFileChooser.setAcceptAllFileFilterUsed(true);
    final FileNameExtensionFilter filter =
      new FileNameExtensionFilter("Monitor script file (*.mon)", "MON", "mon");
    scriptFileChooser.setFileFilter(filter);
    return scriptFileChooser;
  }

  private String[] getExampleScripts()
  {
    final String suffix = ".mon";
    final List<String> examples;
    try {
      examples =
        IOUtils.list("examples").stream().
        filter(s -> s.endsWith(suffix)).
        map(s -> { return s.substring(0, s.length() - suffix.length()); }).
        collect(Collectors.toList());
    } catch (final IOException e) {
      console.println(e.getMessage());
      return new String[0];
    }
    return examples.stream().toArray(String[]::new);
  }

  private InputStream getExampleScriptStream(final String scriptName)
  {
    final InputStream in;
    try {
      in = IOUtils.getStreamForResourcePath("/examples/" + scriptName + ".mon");
    } catch (final IOException e) {
      final String message =
        String.format("Built-in script \"%s\" not found: %s",
                      scriptName, e.getMessage());
      JOptionPane.showMessageDialog(this, message,
                                    "Internal Error",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
    return in;
  }

  private void showExampleScript(final String scriptName)
  {
    final InputStream in = getExampleScriptStream(scriptName);
    if (in == null) return;
    final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    final String title = String.format("Script %s", scriptName);
    final JTextArea taScript = new JTextArea(10, 10);
    try {
      taScript.read(reader, scriptName);
    } catch (final IOException e) {
      final String message =
        String.format("failed reading script %s: %s",
                      scriptName, e.getMessage());
      JOptionPane.showMessageDialog(this, message, title,
                                    JOptionPane.ERROR_MESSAGE);
    }
    JOptionPane.showMessageDialog(this, taScript, title,
                                  JOptionPane.INFORMATION_MESSAGE);
  }

  private void executeScript(final String scriptId,
                             final InputStream in,
                             final PrintStream console)
  {
    console.printf("%s: executing...%n", scriptId);
    final int exitCode = Monitor.main(new String[0], in, console, true);
    final String message;
    final String title;
    final int messageType;
    if (exitCode == 0) {
      message =
        String.format("%s: execution successfully completed%n", scriptId);
      title = "Script Completed";
      messageType = JOptionPane.INFORMATION_MESSAGE;
    } else {
      message =
        String.format("%s: execution failed with exit code %d%n",
                      scriptId, exitCode);
      title = "Script Failed";
      messageType = JOptionPane.ERROR_MESSAGE;
    }
    console.printf(message);
    JOptionPane.showMessageDialog(this, message, title, messageType);
  }

  private void executeExampleScript(final String scriptName)
  {
    final InputStream in = getExampleScriptStream(scriptName);
    if (in == null) return;
    executeScript(scriptName, in, console);
  }

  private void executeFileScript(final String scriptFilePath)
  {
    final InputStream in;
    try {
      in = IOUtils.getStreamForResourcePath(scriptFilePath);
    } catch (final IOException e) {
      final String message =
        String.format("Script file \"%s\" not found: %s",
                      scriptFilePath, e.getMessage());
      JOptionPane.showMessageDialog(this, message,
                                    "I/O Error",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    executeScript(scriptFilePath, in, console);
  }

  private void createExampleSelectionPanel()
  {
    final Box selectionLine = new Box(BoxLayout.X_AXIS);
    add(selectionLine);
    final Border loweredEtched =
      BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    final TitledBorder titled =
      BorderFactory.createTitledBorder(loweredEtched,
                                       "Select built-in example load script");
    titled.setTitleJustification(TitledBorder.CENTER);
    selectionLine.setBorder(titled);
    final JLabel lbExampleScript = new JLabel("Example script");
    selectionLine.add(lbExampleScript);
    selectionLine.add(Box.createHorizontalStrut(5));
    final JComboBox<String> cbExamples =
      new JComboBox<String>(getExampleScripts());
    cbExamples.setMaximumSize(cbExamples.getPreferredSize());
    selectionLine.add(cbExamples);
    selectionLine.add(Box.createHorizontalStrut(5));

    final JButton btShow = new JButton("Show");
    btShow.setMnemonic(KeyEvent.VK_S);
    btShow.addActionListener((event) -> {
        showExampleScript((String)cbExamples.getSelectedItem());
      });
    selectionLine.add(btShow);
    selectionLine.add(Box.createHorizontalStrut(5));

    selectionLine.add(Box.createHorizontalGlue());
    final JButton btExecute = new JButton("Execute");
    btExecute.setMnemonic(KeyEvent.VK_E);
    btExecute.addActionListener((event) -> {
        executeExampleScript((String)cbExamples.getSelectedItem());
      });
    selectionLine.add(btExecute);
    selectionLine.add(Box.createHorizontalStrut(5));
  }

  private void createFileSelectionPanel()
  {
    final Box selectionLine = new Box(BoxLayout.X_AXIS);
    add(selectionLine);
    final Border loweredEtched =
      BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    final TitledBorder titled =
      BorderFactory.createTitledBorder(loweredEtched,
                                       "Select load script from file");
    titled.setTitleJustification(TitledBorder.CENTER);
    selectionLine.setBorder(titled);
    final JLabel lbScriptFilePath = new JLabel("Script file path");
    selectionLine.add(lbScriptFilePath);
    selectionLine.add(Box.createHorizontalStrut(5));
    final JTextField tfFileName = new JTextField();
    tfFileName.setColumns(15);
    tfFileName.setMaximumSize(tfFileName.getPreferredSize());
    selectionLine.add(tfFileName);
    selectionLine.add(Box.createHorizontalStrut(5));
    final JButton btOpen = new JButton("Browse…");
    btOpen.setMnemonic(KeyEvent.VK_B);
    btOpen.addActionListener((event) -> {
        final int result = scriptFileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
          final File file = scriptFileChooser.getSelectedFile();
          tfFileName.setText(file.getPath());
        }
      });
    selectionLine.add(btOpen);
    selectionLine.add(Box.createHorizontalStrut(5));
    selectionLine.add(Box.createHorizontalGlue());
    final JButton btExecute = new JButton("Execute");
    btExecute.setMnemonic(KeyEvent.VK_E);
    btExecute.addActionListener((event) -> {
        executeFileScript(tfFileName.getText());
      });
    selectionLine.add(btExecute);
    selectionLine.add(Box.createHorizontalStrut(5));
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
