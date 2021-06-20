/*
 * @(#)ScriptInfo.java 1.00 21/06/20
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
package org.soundpaint.rp2040pio.monitor;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.soundpaint.rp2040pio.IOUtils;

public class ScriptInfo
{
  public static class ParseException extends IOException
  {
    private static final long serialVersionUID = 8116265717249238138L;

    private static String createMessage(final String innerMessage,
                                        final String scriptId)
    {
      return String.format("script info parse exception: script %s: %s",
                           scriptId, innerMessage);
    }

    private ParseException()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    public ParseException(final String innerMessage, final String scriptId)
    {
      super(createMessage(innerMessage, scriptId));
    }
  }

  public static final String DEFAULT_GROUP_NAME = "Other";

  private final String scriptId;
  private final String scriptName;
  private final String groupName;
  private final String description;

  public ScriptInfo(final String scriptId,
                    final String scriptName,
                    final String groupName,
                    final String description)
  {
    if (scriptId == null) {
      throw new NullPointerException("scriptId");
    }
    if (scriptName == null) {
      throw new NullPointerException("scriptName");
    }
    this.scriptId = scriptId;
    this.scriptName = scriptName;
    this.groupName = groupName;
    this.description = description;
  }

  public String getScriptId() { return scriptId; }

  public String getScriptName() { return scriptName; }

  public String getGroupName() { return groupName; }

  public String getDescription() { return description; }

  private static void checkHeaderAlreadyDefined(final String header,
                                                final String headerId,
                                                final String scriptId)
    throws IOException
  {
    if (header != null) {
      final String message = String.format("duplicate '%s' header", headerId);
      throw new ScriptInfo.ParseException(message, scriptId);
    }
  }

  private static void checkHeaderDefined(final String header,
                                         final String headerId,
                                         final String scriptId)
    throws IOException
  {
    if (header == null) {
      final String message = String.format("missing '%s' header", headerId);
      throw new ScriptInfo.ParseException(message, scriptId);
    }
  }

  private static final String SCRIPT_HEADER_ID = "Script:";
  private static final String GROUP_HEADER_ID = "Group:";

  private static ScriptInfo createScriptInfo(final String scriptId)
    throws IOException
  {
    final String resourcePath = String.format("/examples/%s.mon", scriptId);
    final LineNumberReader reader =
      IOUtils.getReaderForResourcePath(resourcePath);

    // parse headers
    String scriptName = null;
    String groupName = null;
    while (true) {
      final String line = reader.readLine();
      if (line == null) break;
      if (!(line.startsWith("#"))) break;
      final String comment = line.substring(1).trim();
      if (comment.isEmpty()) break;
      if (comment.startsWith(SCRIPT_HEADER_ID)) {
        checkHeaderAlreadyDefined(scriptName, SCRIPT_HEADER_ID, scriptId);
        scriptName = comment.substring(SCRIPT_HEADER_ID.length()).trim();
      } else if (comment.startsWith(GROUP_HEADER_ID)) {
        checkHeaderAlreadyDefined(groupName, GROUP_HEADER_ID, scriptId);
        groupName = comment.substring(GROUP_HEADER_ID.length()).trim();
      } else {
        final String message =
          String.format("invalid start of header line: %s", comment);
        throw new ScriptInfo.ParseException(message, scriptId);
      }
    }
    checkHeaderDefined(scriptName, SCRIPT_HEADER_ID, scriptId);
    if (groupName == null) groupName = ScriptInfo.DEFAULT_GROUP_NAME;

    // parse description
    final StringBuffer s = new StringBuffer();
    while (true) {
      final String line = reader.readLine();
      if (line == null) break;
      if (!(line.startsWith("#"))) break;
      s.append(String.format("%s%n", line.substring(1).trim()));
    }
    final String description = s.toString();

    return new ScriptInfo(scriptId, scriptName, groupName, description);
  }

  public static Map<String, Map<String, ScriptInfo>> createScriptsInfo()
    throws IOException
  {
    final String suffix = ".mon";
    final List<String> scriptIds =
      IOUtils.list("examples").stream().
      filter(t -> t.endsWith(suffix)).
      map(t -> { return t.substring(0, t.length() - suffix.length()); }).
      collect(Collectors.toList());
    final Map<String, Map<String, ScriptInfo>> scriptsInfo =
      new TreeMap<String, Map<String, ScriptInfo>>();
    for (final String scriptId : scriptIds) {
      final ScriptInfo scriptInfo = createScriptInfo(scriptId);
      final String groupName = scriptInfo.getGroupName();
      final Map<String, ScriptInfo> scriptsGroupInfo;
      if (scriptsInfo.containsKey(groupName)) {
        scriptsGroupInfo = scriptsInfo.get(groupName);
      } else {
        scriptsGroupInfo = new TreeMap<String, ScriptInfo>();
        scriptsInfo.put(groupName, scriptsGroupInfo);
      }
      scriptsGroupInfo.put(scriptId, scriptInfo);
    }
    return scriptsInfo;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
