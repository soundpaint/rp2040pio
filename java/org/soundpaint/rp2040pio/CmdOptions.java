/*
 * @(#)CommadLineOptions.java 1.00 17/01/21
 *
 * Copyright (C) 2017, 2021 Jürgen Reuter
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
package org.soundpaint.rp2040pio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parsing and managing command line options.
 */
public class CmdOptions
{
  public abstract static class OptionDeclaration<T>
  {
    private final String typeName;
    private final boolean mandatory;
    private final Character shortName;
    private final String longName;
    private final String defaultValueAsString;
    private final String description;

    private OptionDeclaration()
    {
      throw new UnsupportedOperationException("unsupported empty constructor");
    }

    private OptionDeclaration(final String typeName,
                              final boolean mandatory,
                              final Character shortName,
                              final String longName,
                              final String defaultValueAsString,
                              final String description)
    {
      if ((shortName == null) && (longName == null)) {
        throw new NullPointerException("either shortName or longName " +
                                       "must be non-null");
      }
      this.typeName = typeName;
      this.mandatory = mandatory;
      this.shortName = shortName;
      this.longName = longName;
      this.description = description;
      this.defaultValueAsString = defaultValueAsString;
    }

    private String getTypeName()
    {
      return typeName;
    }

    private boolean isMandatory()
    {
      return mandatory;
    }

    private Character getShortName()
    {
      return shortName;
    }

    private String getShortNameAsString()
    {
      return
        shortName != null ? shortName.toString() : null;
    }

    private String getLongName()
    {
      return longName;
    }

    private String getDefaultValueAsString()
    {
      return defaultValueAsString;
    }

    private String getDescription()
    {
      return description;
    }

    abstract OptionDefinition<T> define() throws ParseException;

    private String getHelp()
    {
      final String ls = System.lineSeparator();
      final StringBuffer sb = new StringBuffer();
      sb.append("  ");
      sb.append(toString());
      if (description != null) {
        sb.append(ls);
        sb.append("            ");
        sb.append(description);
      }
      return sb.toString();
    }

    abstract String getDefaultTypeName();

    @Override
    public String toString()
    {
      final StringBuffer sb = new StringBuffer();
      if (shortName != null) {
        if (this instanceof BooleanOptionDeclaration) {
          sb.append("+");
          sb.append(shortName);
          sb.append(" / -");
          sb.append(shortName);
        } else {
          sb.append("-");
          sb.append(shortName);
        }
        if (longName != null) {
          sb.append(", ");
        }
      }
      if (longName != null) {
        sb.append("--");
        sb.append(longName);
      }
      if (!(this instanceof FlagOptionDeclaration) &&
          !(this instanceof BooleanOptionDeclaration)) {
        sb.append("=");
        if (typeName != null) {
          sb.append(typeName);
        } else {
          sb.append(getDefaultTypeName());
        }
      }
      if (defaultValueAsString != null) {
        sb.append(" (default: ");
        sb.append(defaultValueAsString != null ?
                  defaultValueAsString : "<empty>");
        sb.append(")");
      } else {
        sb.append(" (mandatory: ");
        sb.append(mandatory ? "yes" : "no");
        sb.append(")");
      }
      return sb.toString();
    }
  }

  public static class FlagOptionDeclaration extends OptionDeclaration<Flag>
  {
    public FlagOptionDeclaration(final boolean mandatory,
                                 final Character shortName,
                                 final String longName,
                                 final Flag defaultValue,
                                 final String description)
    {
      super(null, mandatory, shortName, longName,
            String.valueOf(defaultValue), description);
    }

    String getDefaultTypeName() { return "FLAG"; }

    OptionDefinition<Flag> define() throws ParseException
    {
      return new FlagOptionDefinition(this);
    }
  }

  public static FlagOptionDeclaration
    createFlagOption(final boolean mandatory,
                     final Character shortName,
                     final String longName,
                     final Flag defaultValue,
                     final String description)
  {
    return new FlagOptionDeclaration(mandatory, shortName, longName,
                                     defaultValue, description);
  }

  public static class BooleanOptionDeclaration
    extends OptionDeclaration<Boolean>
  {
    public BooleanOptionDeclaration(final boolean mandatory,
                                    final Character shortName,
                                    final String longName,
                                    final Boolean defaultValue,
                                    final String description)
    {
      super(null, mandatory, shortName, longName,
            defaultValue != null ? String.valueOf(defaultValue) : null,
            description);
    }

    String getDefaultTypeName() { return "BOOLEAN"; }

    OptionDefinition<Boolean> define() throws ParseException
    {
      return new BooleanOptionDefinition(this);
    }
  }

  public static BooleanOptionDeclaration
    createBooleanOption(final boolean mandatory,
                        final Character shortName,
                        final String longName,
                        final Boolean defaultValue,
                        final String description)
  {
    return new BooleanOptionDeclaration(mandatory, shortName, longName,
                                        defaultValue, description);
  }

  public static class IntegerOptionDeclaration
    extends OptionDeclaration<Integer>
  {
    public IntegerOptionDeclaration(final String typeName,
                                    final boolean mandatory,
                                    final Character shortName,
                                    final String longName,
                                    final Integer defaultValue,
                                    final String description)
    {
      super(typeName, mandatory, shortName, longName,
            defaultValue != null ? String.valueOf(defaultValue) : null,
            description);
    }

    String getDefaultTypeName() { return "INTEGER"; }

    OptionDefinition<Integer> define() throws ParseException
    {
      return new IntegerOptionDefinition(this);
    }
  }

  public static IntegerOptionDeclaration
    createIntegerOption(final String typeName,
                        final boolean mandatory,
                        final Character shortName,
                        final String longName,
                        final Integer defaultValue,
                        final String description)
  {
    return new IntegerOptionDeclaration(typeName, mandatory, shortName,
                                        longName, defaultValue, description);
  }

  public static class StringOptionDeclaration
    extends OptionDeclaration<String>
  {
    public StringOptionDeclaration(final String typeName,
                                   final boolean mandatory,
                                   final Character shortName,
                                   final String longName,
                                   final String defaultValue,
                                   final String description)
    {
      super(typeName, mandatory, shortName, longName,
            defaultValue, description);
    }

    String getDefaultTypeName() { return "STRING"; }

    OptionDefinition<String> define() throws ParseException
    {
      return new StringOptionDefinition(this);
    }
  }

  public static StringOptionDeclaration
    createStringOption(final String typeName,
                       final boolean mandatory,
                       final Character shortName,
                       final String longName,
                       final String defaultValue,
                       final String description)
  {
    return new StringOptionDeclaration(typeName, mandatory, shortName,
                                       longName, defaultValue, description);
  }

  public static class ParseException extends Exception
  {
    private static final long serialVersionUID = 7201021940370903355L;

    private final OptionDeclaration<?> optionDeclaration;

    public ParseException(final String message)
    {
      this(message, (OptionDeclaration<?>)null);
    }

    public ParseException(final String message,
                          final OptionDeclaration<?> optionDeclaration)
    {
      super(message);
      this.optionDeclaration = optionDeclaration;
    }

    public ParseException(final String message, final Throwable cause)
    {
      this(message, cause, null);
    }

    public ParseException(final String message, final Throwable cause,
                          final OptionDeclaration<?> optionDeclaration)
    {
      super(message, cause);
      this.optionDeclaration = optionDeclaration;
    }

    public ParseException(final Throwable cause)
    {
      this(cause, null);
    }

    public ParseException(final Throwable cause,
                          final OptionDeclaration<?> optionDeclaration)
    {
      super(cause);
      this.optionDeclaration = optionDeclaration;
    }

    @Override
    public String getMessage()
    {
      return
        (optionDeclaration != null ?
         "option " + optionDeclaration + ": " : "") +
        super.getMessage();
    }
  }

  private abstract static class OptionDefinition<T>
  {
    private final OptionDeclaration<T> declaration;
    private T defaultValue;
    private T parsedValue;

    private OptionDefinition(final OptionDeclaration<T> declaration)
      throws ParseException
    {
      this.declaration = declaration;
      final String defaultValueAsString = declaration.getDefaultValueAsString();
      defaultValue =
        defaultValueAsString != null ? parse(defaultValueAsString) : null;
      parsedValue = null;
    }

    abstract T parse(final String strValue) throws ParseException;

    private T parseAndSet(final String strValue) throws ParseException
    {
      parsedValue = parse(strValue);
      return parsedValue;
    }

    protected OptionDeclaration<T> getDeclaration()
    {
      return declaration;
    }

    protected void setParsedValue(final T value)
    {
      if (parsedValue == null) {
        throw new NullPointerException("parsedValue");
      }
      this.parsedValue = parsedValue;
    }

    protected void clear()
    {
      parsedValue = null;
    }

    protected boolean isParsed()
    {
      return parsedValue != null;
    }

    protected boolean hasDefaultValue()
    {
      return defaultValue != null;
    }

    private boolean isDefined()
    {
      return isParsed() || hasDefaultValue();
    }

    private boolean isDefinedIfMandatory()
    {
      return isDefined() || !declaration.isMandatory();
    }

    protected T getValue()
    {
      return isParsed() ? parsedValue : defaultValue;
    }

    @Override
    public String toString()
    {
      return declaration.toString();
    }
  }

  public static enum Flag
  {
    OFF("off"), ON("on");

    private final String displayValue;

    private Flag(final String displayValue)
    {
      this.displayValue = displayValue;
    }

    @Override
    public String toString() { return displayValue; }
  }

  public static class FlagOptionDefinition extends OptionDefinition<Flag>
  {
    public FlagOptionDefinition(final FlagOptionDeclaration declaration)
      throws ParseException
    {
      super(declaration);
    }

    @Override
    Flag parse(final String strValue) throws ParseException
    {
      if (Flag.OFF.toString().equals(strValue)) {
        return Flag.OFF;
      } else if (Flag.ON.toString().equals(strValue)) {
        return Flag.ON;
      } else {
        throw new ParseException("'" + Flag.OFF + "' or " +
                                 "'" + Flag.ON + "' expected, " +
                                 "but found: " + strValue,
                                 getDeclaration());
      }
    }
  }

  public static class BooleanOptionDefinition extends OptionDefinition<Boolean>
  {
    public BooleanOptionDefinition(final BooleanOptionDeclaration declaration)
      throws ParseException
    {
      super(declaration);
    }

    @Override
    Boolean parse(final String strValue) throws ParseException
    {
      if ("false".equals(strValue)) {
        return false;
      } else if ("true".equals(strValue)) {
        return true;
      } else {
        throw new ParseException("'false' or 'true' expected, " +
                                 "but found: " + strValue,
                                 getDeclaration());
      }
    }
  }

  public static class IntegerOptionDefinition extends OptionDefinition<Integer>
  {
    public IntegerOptionDefinition(final IntegerOptionDeclaration declaration)
      throws ParseException
    {
      super(declaration);
    }

    @Override
    Integer parse(final String strValue) throws ParseException
    {
      try {
        if (strValue.startsWith("0x") || strValue.startsWith("0X")) {
          return Integer.parseUnsignedInt(strValue.substring(2), 16);
        } else {
          return Integer.parseInt(strValue);
        }
      } catch (final NumberFormatException e) {
        throw new ParseException("integer value expected: " + e.getMessage(),
                                 getDeclaration());
      }
    }
  }

  public static class StringOptionDefinition extends OptionDefinition<String>
  {
    public StringOptionDefinition(final StringOptionDeclaration declaration)
      throws ParseException
    {
      super(declaration);
    }

    @Override
    String parse(final String strValue) throws ParseException
    {
      return strValue;
    }
  }

  private final String prgName;
  private final String prgSingleLineDescription;
  private final String prgNotes;
  private final List<OptionDeclaration<?>> declarations;
  private final OptionDefinition<?>[] definitions;

  public CmdOptions(final String prgName,
                    final String prgSingleLineDescription,
                    final String prgNotes,
                    final OptionDeclaration<?> ... declarations)
    throws ParseException
  {
    this(prgName, prgSingleLineDescription, prgNotes,
         Arrays.asList(declarations));
  }

  public CmdOptions(final String prgName,
                    final String prgSingleLineDescription,
                    final String prgNotes,
                    final List<OptionDeclaration<?>> declarations)
    throws ParseException
  {
    if (prgName == null) {
      throw new NullPointerException("prgName");
    }
    if (declarations == null) {
      throw new NullPointerException("declarations");
    }
    this.prgName = prgName;
    this.prgSingleLineDescription = prgSingleLineDescription;
    this.prgNotes = prgNotes;
    this.declarations = declarations;
    definitions = createDefinitions();
  }

  private String getPrgName()
  {
    return prgName;
  }

  private String getPrgSingleLineDescription()
  {
    return prgSingleLineDescription;
  }

  private String getPrgNotes()
  {
    return prgNotes;
  }

  public String getUsage()
  {
    return
      "Usage: " + prgName + " [OPTION]...";
  }

  public String getHelp()
  {
    final String ls = System.lineSeparator();
    final StringBuffer sb = new StringBuffer();
    if (declarations.size() > 0) {
      sb.append("Options:");
      sb.append(ls);
    }
    for (final OptionDeclaration<?> declaration : declarations) {
      sb.append(declaration.getHelp());
      sb.append(ls);
    }
    if (prgNotes != null) {
      sb.append(String.format("%nNotes:%n" + prgNotes + "%n"));
    }
    return sb.toString();
  }

  public String getFullInfo()
  {
    final String ls = System.lineSeparator();
    final String help = getHelp();
    return
      getUsage() + ls +
      (prgSingleLineDescription != null ? prgSingleLineDescription + ls : "") +
      (!help.isEmpty() ? ls + help : "");
  }

  private static final OptionDefinition<?>[] EMPTY_DEFINITIONS =
    new OptionDefinition<?>[0];

  private OptionDefinition<?>[] createDefinitions() throws ParseException
  {
    final ArrayList<OptionDefinition<?>> definitionList =
      new ArrayList<OptionDefinition<?>>();
    for (final OptionDeclaration<?> declaration : declarations) {
      final OptionDefinition<?> definition = declaration.define();
      definitionList.add(definition);
    }
    return definitionList.toArray(EMPTY_DEFINITIONS);
  }

  public void clear()
  {
    for (final OptionDefinition<?> definition : definitions) {
      definition.clear();
    }
  }

  private boolean
    updateDefinition(final OptionDefinition<?> definition, String strValue)
    throws ParseException
  {
    if (definition.isParsed()) {
      throw new ParseException("option redefined",
                               definition.getDeclaration());
    }
    if (definition instanceof FlagOptionDefinition) {
      if (strValue != null) {
        throw new ParseException("unexpected surplus argument: " + strValue,
                                 definition.getDeclaration());
      }
      definition.parseAndSet(Flag.ON.toString());
      return false;
    } else if (strValue != null) {
      definition.parseAndSet(strValue);
      return false;
    } else {
      return true;
    }
  }

  private OptionDefinition<?> parseLongOptionIdentifier(final String option)
    throws ParseException
  {
    if (option.isEmpty()) {
      throw new ParseException("long option must not be empty: --");
    }
    final String name;
    final String value;
    final int pos = option.indexOf('=');
    if (pos >= 0) {
      name = option.substring(0, pos);
      value = option.substring(pos + 1);
    } else {
      name = option;
      value = null;
    }
    if (name.isEmpty()) {
      throw new ParseException("long option name must not be empty: --" +
                               option);
    }
    OptionDefinition<?> definition = null;
    for (final OptionDeclaration<?> declaration : declarations) {
      final String longName = declaration.getLongName();
      if (name.equals(longName)) {
        definition = findDefinitionForDeclaration(declaration);
        break;
      }
    }
    if (definition != null) {
      return updateDefinition(definition, value) ? definition : null;
    } else {
      throw new ParseException("unknown long option name: " + name);
    }
  }

  private OptionDefinition<?> parseShortOptionIdentifier(final String name)
    throws ParseException
  {
    if (name.length() != 1) {
      throw new ParseException("short option name: " +
                               "expected single character, but found: " +
                               name);
    }
    OptionDefinition<?> definition = null;
    for (final OptionDeclaration<?> declaration : declarations) {
      final String shortName = declaration.getShortNameAsString();
      if (name.equals(shortName)) {
        definition = findDefinitionForDeclaration(declaration);
        break;
      }
    }
    if (definition != null) {
      return updateDefinition(definition, null) ? definition : null;
    } else {
      throw new ParseException("unknown short option name: " + name);
    }
  }

  private OptionDefinition<?> parseOptionIdentifier(final String arg)
    throws ParseException
  {
    if (arg.startsWith("--")) {
      return parseLongOptionIdentifier(arg.substring(2));
    } else if (arg.startsWith("-")) {
      return parseShortOptionIdentifier(arg.substring(1));
    } else {
      throw new ParseException("option identifier expected, but found: " + arg);
    }
  }

  public void parse(final String argv[]) throws ParseException
  {
    clear();
    OptionDefinition<?> currentOption = null;
    for (final String arg : argv) {
      if (arg.isEmpty()) continue;
      if (arg == null) {
        throw new NullPointerException("arg");
      }
      if (currentOption != null) {
        if (currentOption.isParsed()) {
          throw new ParseException("option redefined",
                                   currentOption.getDeclaration());
        }
        currentOption.parseAndSet(arg);
        currentOption = null;
      } else {
        currentOption = parseOptionIdentifier(arg);
      }
    }
    if (currentOption != null) {
      throw new ParseException("missing argument",
                               currentOption.getDeclaration());
    }
    for (final OptionDefinition<?> definition : definitions) {
      if (!definition.isDefinedIfMandatory()) {
        throw new ParseException("mandatory option not specified",
                                 definition.getDeclaration());
      }
    }
  }

  private OptionDefinition<?>
    findDefinitionForDeclaration(final OptionDeclaration<?> declaration)
  {
    // TODO: Performance: Pre-build a hash map, rather than
    // iterating each time thorugh all definitions.
    for (final OptionDefinition<?> definition : definitions) {
      if (definition.getDeclaration() == declaration) {
        return definition;
      }
    }
    return null;
  }

  public Flag getValue(final FlagOptionDeclaration declaration)
  {
    final FlagOptionDefinition definition =
      (FlagOptionDefinition)findDefinitionForDeclaration(declaration);
    return definition != null ? definition.getValue() : null;
  }

  public Boolean getValue(final BooleanOptionDeclaration declaration)
  {
    final BooleanOptionDefinition definition =
      (BooleanOptionDefinition)findDefinitionForDeclaration(declaration);
    return definition != null ? definition.getValue() : null;
  }

  public Integer getValue(final IntegerOptionDeclaration declaration)
  {
    final IntegerOptionDefinition definition =
      (IntegerOptionDefinition)findDefinitionForDeclaration(declaration);
    return definition != null ? definition.getValue() : null;
  }

  public String getValue(final StringOptionDeclaration declaration)
  {
    final StringOptionDefinition definition =
      (StringOptionDefinition)findDefinitionForDeclaration(declaration);
    return definition != null ? definition.getValue() : null;
  }

  public boolean isDefined(final OptionDeclaration<?> declaration)
  {
    final OptionDefinition<?> definition =
      findDefinitionForDeclaration(declaration);
    if (definition == null) {
      throw new InternalError("no such declaration in this set of options: " +
                              declaration);
    }
    return definition.isDefined();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
