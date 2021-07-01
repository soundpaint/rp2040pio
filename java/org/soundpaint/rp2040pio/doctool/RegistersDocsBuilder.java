/*
 * @(#)RegistersDocsBuilder.java 1.00 21/04/15
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
package org.soundpaint.rp2040pio.doctool;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PIOEmuRegisters;

/**
 * Automatically create registers documentation from annotations in
 * the code.
 */
public class RegistersDocsBuilder<T extends Enum<T> & RegistersDocs<T>>
{
  private static String formatBitsRange(final RegistersDocs.BitsInfo bitsInfo)
  {
    final int msb = bitsInfo.getMsb();
    final int lsb = bitsInfo.getLsb();
    if (msb == lsb)
      return String.format("%d", msb);
    return String.format("%d:%d", msb, lsb);
  }

  private static String formatName(final RegistersDocs.BitsInfo bitsInfo)
  {
    final String name = bitsInfo.getName();
    if (bitsInfo.getType() == RegistersDocs.BitsType.RESERVED)
      return "Reserved.";
    if (bitsInfo.getType() == RegistersDocs.BitsType.UNUSED)
      return "Unused.";
    if (name == null)
      return "―";
    return String.format(name);
  }

  private static String
    formatTableDescription(final String defaultDescription,
                           final Iterable<RegistersDocs.BitsInfo> bitsInfos)
  {
    boolean hasBitsDescription = false;
    for (final RegistersDocs.BitsInfo bitsInfo : bitsInfos) {
      if (bitsInfo.getDescription() != null) {
        hasBitsDescription = true;
        break;
      }
    }
    if (hasBitsDescription) {
      if (defaultDescription != null) {
        return String.format(defaultDescription);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private static String formatDescription(final RegistersDocs.BitsInfo bitsInfo,
                                          final String defaultDescription)
  {
    final RegistersDocs.BitsType type = bitsInfo.getType();
    final String bitsInfoDescription = bitsInfo.getDescription();
    final String description;
    if (bitsInfoDescription != null) {
      description = bitsInfoDescription;
    } else if (type == RegistersDocs.BitsType.RESERVED) {
      description = "―";
    } else if (type == RegistersDocs.BitsType.UNUSED) {
      description = "―";
    } else if (defaultDescription != null) {
      description = defaultDescription;
    } else {
      description = "―";
    }
    return DocsBuilder.csvEncode(description.replace("%n", " "));
  }

  private static String formatType(final RegistersDocs.BitsInfo bitsInfo)
  {
    final RegistersDocs.BitsType type = bitsInfo.getType();
    return String.format("%s",
                         (type == RegistersDocs.BitsType.RESERVED) ||
                         (type == RegistersDocs.BitsType.UNUSED) ?
                         "―" : type);
  }

  private static String formatResetValue(final RegistersDocs.BitsInfo bitsInfo)
  {
    final Integer resetValue = bitsInfo.getResetValue();
    if (resetValue == null)
      return "―";
    return String.format("%d", resetValue);
  }

  private String createDetailTableLabels(final List<T> regsList)
  {
    final StringBuilder labels = new StringBuilder();
    for (final T reg : regsList) {
      labels.append(String.format(".. _%s-details-label:%n", reg.toString()));
    }
    labels.append(String.format("%n"));
    return labels.toString();
  }

  private String createDetailTableIndices(final List<T> regsList)
  {
    final StringBuilder labels = new StringBuilder();
    for (final T reg : regsList) {
      labels.append(String.format(".. index::%n"));
      labels.append(String.format("   single: register details; %s%n",
                                  reg.toString()));
      labels.append(String.format("   single: %s%n%n", reg.toString()));
    }
    return labels.toString();
  }

  private String formatRegNames(final List<T> regsList)
  {
    final StringBuilder regNames = new StringBuilder();
    boolean multiple = false;
    for (final T reg : regsList) {
      if (regNames.length() > 0) {
        regNames.append(", ");
        multiple = true;
      }
      regNames.append(reg.toString());
    }
    regNames.append(multiple ? " Registers" : " Register");
    return String.format("%s", regNames);
  }

  private String formatOffsets(final List<T> regsList)
  {
    final StringBuilder offsets = new StringBuilder();
    boolean haveMultipleRegs = false;
    for (final T reg : regsList) {
      if (offsets.length() > 0) {
        offsets.append(", ");
        haveMultipleRegs = true;
      }
      offsets.append(String.format("0x%03x", reg.ordinal() << 2));
    }
    return String.format("**%s:** %s",
                         haveMultipleRegs ? "Offsets" : "Offset", offsets);
  }

  private String createDetailTable(final String registersSetLabel,
                                   final RegistersDocs.RegisterDetails
                                   registerDetails,
                                   final List<T> regsList)
  {
    final StringBuilder s = new StringBuilder();
    s.append(createDetailTableLabels(regsList));
    s.append(createDetailTableIndices(regsList));
    final String regNames = formatRegNames(regsList);
    final String registersSetId =
      DocsBuilder.createIdFromLabel(registersSetLabel);
    final String headLine =
      String.format(":ref:`%s <section-top_%s>`: %s",
                    registersSetLabel, registersSetId, regNames);
    s.append(String.format("%s%n", headLine));
    s.append(String.format("%s%n", DocsBuilder.fill('-', headLine.length())));
    s.append(String.format("%n"));
    final String offsets = formatOffsets(regsList);
    s.append(String.format("%s%n", offsets));
    s.append(String.format("%n"));
    final String defaultDescription = registerDetails.getInfo();
    final String tableDescription =
      formatTableDescription(defaultDescription,
                             registerDetails.getBitsInfos());
    if (tableDescription != null) {
      s.append(String.format("**Description**%n%n%s%n", tableDescription));
      s.append(String.format("%n"));
    }
    s.append(String.format(".. csv-table::%n"));
    s.append(String.format("   :header: Bits, Name, Description, Type, Reset%n"));
    s.append(String.format("   :widths: 8, 20, 40, 8, 20%n"));
    s.append(String.format("%n"));
    for (final T.BitsInfo bitsInfo : registerDetails.getBitsInfos()) {
      final String bitsRange = formatBitsRange(bitsInfo);
      final String name = formatName(bitsInfo);
      final String description =
        formatDescription(bitsInfo, defaultDescription);
      final String type = formatType(bitsInfo);
      final String reset = formatResetValue(bitsInfo);
      s.append(String.format("   %s, %s, %s, %s, %s%n",
                             bitsRange, name, description, type, reset));
    }
    s.append(String.format("%n"));
    return s.toString();
  }

  private String createDetailTableRef(final T reg)
  {
    final String regName = reg.toString();
    return String.format(":ref:`%s <%s-details-label>`", regName, regName);
  }

  private String createOverviewTable(final String registersSetLabel,
                                     final String registersSetDescription,
                                     final EnumSet<T> regs,
                                     final Map<T.RegisterDetails, List<T>>
                                     registerDetails2regs)
  {
    final StringBuilder s = new StringBuilder();
    final String registersSetId =
      DocsBuilder.createIdFromLabel(registersSetLabel);
    s.append(String.format(".. _section-top_%s:%n", registersSetId));
    s.append(String.format("%n"));
    s.append(String.format(".. index::%n"));
    s.append(String.format("   single: %s%n", registersSetLabel));
    s.append(String.format("   single: registers set; %s%n", registersSetLabel));
    s.append(String.format("%n"));
    final String sectionHeader = String.format("%s", registersSetLabel);
    s.append(String.format("%s%n", sectionHeader));
    s.append(String.format("%s%n",
                           DocsBuilder.fill('=', sectionHeader.length())));
    s.append(String.format("%n"));
    final String overviewTableHeader = "List of Registers";
    s.append(String.format("%s%n", overviewTableHeader));
    s.append(String.format("%s%n",
                           DocsBuilder.fill('-', overviewTableHeader.length())));
    s.append(String.format("%n"));
    if (registersSetDescription != null) {
      s.append(String.format(registersSetDescription));
      s.append(String.format("%n"));
      s.append(String.format("%n"));
    }
    s.append(String.format(".. csv-table::%n"));
    s.append(String.format("   :header: Offset, Name, Info%n"));
    s.append(String.format("   :widths: 8, 20, 40%n"));
    s.append(String.format("%n"));
    int address = 0x000;
    for (final T reg : regs) {
      final T.RegisterDetails registerDetails = reg.getRegisterDetails();
      final List<T> regsList;
      if (registerDetails2regs.containsKey(registerDetails)) {
        regsList = registerDetails2regs.get(registerDetails);
      } else {
        regsList = new ArrayList<T>();
        registerDetails2regs.put(registerDetails, regsList);
      }
      regsList.add(reg);
      s.append(String.format("   0x%03x, %s, %s%n",
                             address, createDetailTableRef(reg),
                             DocsBuilder.csvEncode(reg.getInfo().
                                                   replace("%n", " "))));
      address += 0x004;
    }
    s.append(String.format("%n"));
    return s.toString();
  }

  private String createDocs(final String registersSetLabel,
                            final String registersSetDescription,
                            final EnumSet<T> regs)
  {
    final Map<T.RegisterDetails, List<T>> registerDetails2regs
      = new LinkedHashMap<T.RegisterDetails, List<T>>();
    final StringBuilder s = new StringBuilder();
    s.append(String.format(DocsBuilder.leadinComment, Instant.now()));
    s.append(createOverviewTable(registersSetLabel, registersSetDescription,
                                 regs, registerDetails2regs));
    for (final T.RegisterDetails registerDetails :
           registerDetails2regs.keySet()) {
      final List<T> regsList = registerDetails2regs.get(registerDetails);
      s.append(createDetailTable(registersSetLabel, registerDetails, regsList));
    }
    return s.toString();
  }

  private RegistersDocsBuilder()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public RegistersDocsBuilder(final Class<T> regsClass,
                              final String registerSetLabel,
                              final String registerSetDescription,
                              final String rstFilePath)
    throws IOException
  {
    final String docs =
      createDocs(registerSetLabel, registerSetDescription,
                 EnumSet.allOf(regsClass));
    DocsBuilder.writeToFile(rstFilePath, docs);
  }

  public static void main(final String argv[])
  {
    try {
      new RegistersDocsBuilder<PicoEmuRegisters.Regs>
        (PicoEmuRegisters.Regs.class,
         PicoEmuRegisters.Regs.getRegisterSetLabel(),
         PicoEmuRegisters.Regs.getRegisterSetDescription(),
         "pico-emu-registers.rst");
      new RegistersDocsBuilder<PIOEmuRegisters.Regs>
        (PIOEmuRegisters.Regs.class,
         PIOEmuRegisters.Regs.getRegisterSetLabel(),
         PIOEmuRegisters.Regs.getRegisterSetDescription(),
         "pio-emu-registers.rst");
    } catch (final IOException e) {
      final String message =
        String.format("failed creating registers documentation: %s%n",
                      e.getMessage());
      System.err.printf(message);
      System.exit(-1);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
