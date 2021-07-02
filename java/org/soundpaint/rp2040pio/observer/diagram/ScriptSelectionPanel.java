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
package org.soundpaint.rp2040pio.observer.diagram;

import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.soundpaint.rp2040pio.IOUtils;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.monitor.Monitor;

public class ScriptSelectionPanel extends Box
{
  private static final long serialVersionUID = 3945227462166267605L;
  private static final Dimension PREFERRED_SHOW_EXAMPLE_SIZE =
    new Dimension(480, 320);

  private final PrintStream console;
  private final JFileChooser scriptFileChooser;
  private final JTabbedPane tabbedPane;
  private final ExampleSelectionPanel exampleSelectionPanel;
  private final FileSelectionPanel fileSelectionPanel;

  public ScriptSelectionPanel(final PrintStream console)
  {
    super(BoxLayout.PAGE_AXIS);
    Objects.requireNonNull(console);
    this.console = console;
    scriptFileChooser = createScriptFileChooser();
    tabbedPane = new JTabbedPane();
    add(tabbedPane);
    exampleSelectionPanel = new ExampleSelectionPanel();
    tabbedPane.addTab("Example Scripts", null, exampleSelectionPanel,
                      "Load & configure emulator by running an example script");
    tabbedPane.setMnemonicAt(0, KeyEvent.VK_X);
    fileSelectionPanel = new FileSelectionPanel();
    tabbedPane.addTab("User Scripts", null, fileSelectionPanel,
                      "Load & configure emulator by running a user script");
    tabbedPane.setMnemonicAt(1, KeyEvent.VK_U);
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
    taScript.setEditable(false);
    try {
      taScript.read(reader, scriptName);
    } catch (final IOException e) {
      final String message =
        String.format("failed reading script %s: %s",
                      scriptName, e.getMessage());
      JOptionPane.showMessageDialog(this, message, title,
                                    JOptionPane.ERROR_MESSAGE);
    }
    final JScrollPane spScript = new JScrollPane(taScript);
    spScript.setPreferredSize(PREFERRED_SHOW_EXAMPLE_SIZE);
    JOptionPane.showMessageDialog(this, spScript, title,
                                  JOptionPane.INFORMATION_MESSAGE);
  }

  private void executeScript(final String scriptId,
                             final InputStream in,
                             final PrintStream console)
  {
    console.printf("%s: executing…%n", scriptId);
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

  public void execute()
  {
    final Component selectedComponent = tabbedPane.getSelectedComponent();
    if (selectedComponent == exampleSelectionPanel) {
      exampleSelectionPanel.execute();
    } else if (selectedComponent == fileSelectionPanel) {
      fileSelectionPanel.execute();
    } else {
      console.printf("warning: unexpected tab: %s%n", selectedComponent);
    }
  }

  private class ExampleSelectionPanel extends JPanel
  {
    private static final long serialVersionUID = -7249218754281104584L;

    private final JComboBox<String> cbExamples;

    private void execute()
    {
      executeExampleScript((String)cbExamples.getSelectedItem());
    }

    private ExampleSelectionPanel()
    {
      setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
      final Border loweredEtched =
        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
      final TitledBorder titled =
        BorderFactory.createTitledBorder(loweredEtched,
                                         "Select built-in example");
      titled.setTitleJustification(TitledBorder.CENTER);
      final Box selectionLine = new Box(BoxLayout.LINE_AXIS);
      setBorder(titled);
      add(selectionLine);
      final JLabel lbExampleScript = new JLabel("Examples:");
      lbExampleScript.setDisplayedMnemonic(KeyEvent.VK_X);
      selectionLine.add(lbExampleScript);
      selectionLine.add(Box.createHorizontalStrut(5));
      cbExamples = new JComboBox<String>(getExampleScripts());
      cbExamples.setMaximumSize(cbExamples.getPreferredSize());
      lbExampleScript.setLabelFor(cbExamples);
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
      SwingUtils.setPreferredHeightAsMaximum(selectionLine);
      add(Box.createVerticalGlue());
    }
  }

  private class FileSelectionPanel extends JPanel
  {
    private static final long serialVersionUID = -6631139141176608724L;

    private final JTextField tfFileName;

    private void execute()
    {
      executeFileScript(tfFileName.getText());
    }

    private FileSelectionPanel()
    {
      setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
      final Border loweredEtched =
        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
      final TitledBorder titled =
        BorderFactory.createTitledBorder(loweredEtched, "Choose file");
      titled.setTitleJustification(TitledBorder.CENTER);
      setBorder(titled);
      final Box selectionLine = new Box(BoxLayout.LINE_AXIS);
      add(selectionLine);
      final JLabel lbScriptFilePath = new JLabel("File path:");
      lbScriptFilePath.setDisplayedMnemonic(KeyEvent.VK_F);
      selectionLine.add(lbScriptFilePath);
      selectionLine.add(Box.createHorizontalStrut(5));
      tfFileName = new JTextField();
      tfFileName.setColumns(15);
      tfFileName.setMaximumSize(tfFileName.getPreferredSize());
      lbScriptFilePath.setLabelFor(tfFileName);
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
      SwingUtils.setPreferredHeightAsMaximum(selectionLine);
      add(Box.createVerticalGlue());
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
