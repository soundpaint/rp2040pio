/*
 * @(#)RegistersDocs.java 1.00 21/04/15
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.soundpaint.rp2040pio.Constants;

/**
 * Documentation interface for automatic creation of registers
 * documentation from annotations in the code.
 */
public interface RegistersDocs<T>
{
  public enum BitsType
  {
    RESERVED("―", "n/a"),
    UNUSED("―", "unused"),
    SC("SC", "???"),
    WC("WC", "write 1 to clear"),
    RW("RW", "read/write"),
    RO("RO", "read-only"),
    WO("WO", "write-only"),
    RF("RF", "read to trigger function"),
    WF("WF", "wrtite to trigger function");

    private final String id;
    private final String description;

    private BitsType(final String id, final String description)
    {
      this.id = id;
      this.description = description;
    }

    public boolean isRelevant()
    {
      return (this != RESERVED) && (this != UNUSED);
    }

    public String getId() { return id; }

    public String getDescription() { return description; }
  }

  public static class BitsRange
  {
    private final int msb;
    private final int lsb;

    private BitsRange()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    public BitsRange(final int msb, final int lsb)
    {
      if (msb < 0) {
        throw new IllegalArgumentException("msb < 0: " + msb);
      }
      if (msb > 31) {
        throw new IllegalArgumentException("msb > 31: " + msb);
      }
      if (lsb < 0) {
        throw new IllegalArgumentException("lsb < 0: " + lsb);
      }
      if (lsb > 31) {
        throw new IllegalArgumentException("lsb > 31: " + lsb);
      }
      if (lsb > msb) {
        throw new IllegalArgumentException("lsb > msb: " + lsb + " > " + msb);
      }
      this.msb = msb;
      this.lsb = lsb;
    }

    public int getMsb() { return msb; }

    public int getLsb() { return lsb; }

    public String toShortString()
    {
      return
        msb == lsb ?
        String.format("%d", msb) :
        String.format("%d:%d", msb, lsb);
    }

    @Override
    public String toString()
    {
      return
        msb == lsb ?
        String.format("bit %d", msb) :
        String.format("bits [%d:%d]", msb, lsb);
    }
  }

  public static class BitsInfo
  {
    private final String name;
    private final BitsRange bitsRange;
    private final String description;
    private final BitsType type;
    private final Integer resetValue;

    private BitsInfo()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    public BitsInfo(final String name,
                    final int msb,
                    final int lsb,
                    final String description,
                    final BitsType type,
                    final Integer resetValue)
    {
      this(name, new BitsRange(msb, lsb), description, type, resetValue);
    }

    public BitsInfo(final String name,
                    final BitsRange bitsRange,
                    final String description,
                    final BitsType type,
                    final Integer resetValue)
    {
      if (bitsRange == null) {
        throw new NullPointerException("bitsRange");
      }
      if (type == null) {
        throw new NullPointerException("type");
      }
      if (resetValue != null) {
        final int msb = bitsRange.msb;
        final int lsb = bitsRange.lsb;
        final long maxResetValue = ((long)0x1 << (msb - lsb + 1)) - 1;
        final long resetValueAsLong = 0x00000000FFFFFFFFL & (long)resetValue;
        if (resetValueAsLong > maxResetValue) {
          final String message =
            String.format("%s [%d:%d]: " +
                          "resetValueAsLong > maxResetValue: %d > %d",
                          name, msb, lsb, resetValueAsLong, maxResetValue);
          throw new IllegalArgumentException(message);
        }
      }
      this.name = name;
      this.bitsRange = bitsRange;
      this.description = description;
      this.type = type;
      this.resetValue = resetValue;
    }

    public int getMsb() { return bitsRange.msb; }
    public int getLsb() { return bitsRange.lsb; }
    public String getName() { return name; }
    public BitsRange getBitsRange() { return bitsRange; }
    public String getDescription() { return description; }
    public BitsType getType() { return type; }
    public Integer getResetValue() { return resetValue; }

    private static String renderName(final String name)
    {
      return name != null ? name + ": " : "";
    }

    private static String renderName(final String name,
                                     final String defaultName)
    {
      return renderName(name != null ? name : defaultName);
    }

    private String renderBitsRange()
    {
      return bitsRange.toString();
    }

    private String renderDescription()
    {
      return description != null ? " " + description : "";
    }

    private String renderType()
    {
      return String.format("Type: %s", type);
    }

    private String renderResetValue()
    {
      if ((type == BitsType.UNUSED) || (type == BitsType.RESERVED))
        return "";
      return String.format(", Reset Value: %s",
                           resetValue != null ? resetValue : "―");
    }

    public String toString(final String defaultName)
    {
      return String.format("%s %s%s, %s %s",
                           renderName(name, defaultName),
                           renderBitsRange(),
                           renderDescription(),
                           renderType(),
                           renderResetValue());
    }

    @Override
    public String toString()
    {
      return toString(null);
    }
  }

  public static class RegisterDetails
  {
    public static final int SM_UNDEFINED = -1;

    private String info;
    private int smNum;
    private List<BitsInfo> bitsInfos;

    private static void checkSmNum(final int smNum)
    {
      if (smNum != SM_UNDEFINED) {
        Constants.checkSmNum(smNum);
      }
    }

    private RegisterDetails()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    public RegisterDetails(final String info, final BitsInfo[] bitsInfos)
    {
      this(info, Arrays.asList(bitsInfos));
    }

    public RegisterDetails(final String info, final int smNum,
                           final BitsInfo[] bitsInfos)
    {
      this(info, smNum, Arrays.asList(bitsInfos));
    }

    public RegisterDetails(final String info, final List<BitsInfo> bitsInfos)
    {
      this(info, SM_UNDEFINED, bitsInfos);
    }

    public RegisterDetails(final String info, final int smNum,
                           final List<BitsInfo> bitsInfos)
    {
      Objects.requireNonNull(info);
      Objects.requireNonNull(bitsInfos);
      checkSmNum(smNum);
      this.info = info;
      this.smNum = smNum;
      this.bitsInfos = new ArrayList<BitsInfo>();
      this.bitsInfos.addAll(bitsInfos);
    }

    public String getInfo() { return info; }

    public int getSmNum() { return smNum; }

    public Iterable<BitsInfo> getBitsInfos()
    {
      final List<BitsInfo> bitsInfosCopy = new ArrayList<BitsInfo>();
      bitsInfosCopy.addAll(bitsInfos);
      return bitsInfosCopy;
    }

    public RegisterDetails createCopyForDifferentSm(final int smNum)
    {
      return new RegisterDetails(info, smNum, bitsInfos);
    }
  }

  String getInfo();
  RegisterDetails getRegisterDetails();
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
