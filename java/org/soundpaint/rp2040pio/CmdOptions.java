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

/**
 * Parsing and managing command line options.
 */
public class CmdOptions
{
  private static interface Definator
  {
    public OptionDefinition<?> define(final OptionDeclaration declaration)
      throws ParseException;
  }

  public static enum Type {
    FLAG(new Definator() {
        public OptionDefinition<Boolean>
          define(final OptionDeclaration declaration)
          throws ParseException
        {
          return new FlagOptionDefinition(declaration);
        }
      }),
    BOOLEAN(new Definator() {
        public OptionDefinition<Boolean>
          define(final OptionDeclaration declaration)
          throws ParseException
        {
          return new BooleanOptionDefinition(declaration);
        }
      }),
    INTEGER(new Definator() {
        public OptionDefinition<Integer>
          define(final OptionDeclaration declaration)
          throws ParseException
        {
          return new IntegerOptionDefinition(declaration);
        }
      }),
    STRING(new Definator() {
        public OptionDefinition<String>
          define(final OptionDeclaration declaration)
          throws ParseException
        {
          return new StringOptionDefinition(declaration);
        }
      });

    private final Definator definator;

    private Type(final Definator definator) {
      this.definator = definator;
    }

    public OptionDefinition<?>
      define(final OptionDeclaration declaration)
      throws ParseException
    {
      return definator.define(declaration);
    }

    public boolean isFlag()
    {
      return this == FLAG;
    }

    public boolean isBoolean()
    {
      return this == BOOLEAN;
    }
  };

  public static class OptionDeclaration
  {
    private final Type type;
    private final String typeName;
    private final boolean mandatory;
    private final Character shortName;
    private final String longName;
    private final String defaultValueAsString;
    private final String description;

    private OptionDeclaration() {
      throw new RuntimeException("unsupported constructor");
    }

    public OptionDeclaration(final Type type,
                             final String typeName,
                             final boolean mandatory,
                             final Character shortName,
                             final String longName,
                             final String defaultValueAsString,
                             final String description)
    {
      if (type == null) {
        throw new NullPointerException("type must be non-null");
      }
      if ((shortName == null) && (longName == null)) {
        throw new NullPointerException("either shortName or longName " +
                                       "must be non-null");
      }
      this.type = type;
      this.typeName = typeName;
      this.mandatory = mandatory;
      this.shortName = shortName;
      this.longName = longName;
      this.description = description;
      this.defaultValueAsString = defaultValueAsString;
    }

    public Type getType()
    {
      return type;
    }

    public String getTypeName()
    {
      return typeName;
    }

    public boolean isMandatory()
    {
      return mandatory;
    }

    public Character getShortName()
    {
      return shortName;
    }

    public String getShortNameAsString()
    {
      return
        shortName != null ? shortName.toString() : null;
    }

    public String getLongName()
    {
      return longName;
    }

    public String getDefaultValueAsString()
    {
      return defaultValueAsString;
    }

    public String getDescription()
    {
      return description;
    }

    private OptionDefinition<?> define() throws ParseException
    {
      return type.define(this);
    }

    public String getHelp()
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

    public String toString()
    {
      final StringBuffer sb = new StringBuffer();
      if (shortName != null) {
        if (type.isBoolean()) {
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
      if ((type != Type.FLAG) && (type != Type.BOOLEAN)) {
        sb.append("=");
        if (typeName != null) {
          sb.append(typeName);
        } else {
          sb.append(type.toString());
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

  public static class ParseException extends Exception
  {
    private static final long serialVersionUID = 7201021940370903355L;

    public ParseException(final String message)
    {
      super(message);
    }

    public ParseException(final String message, final Throwable cause)
    {
      super(message, cause);
    }

    public ParseException(final Throwable cause)
    {
      super(cause);
    }
  }

  private abstract static class OptionDefinition<T>
  {
    private final OptionDeclaration declaration;
    private T defaultValue;
    private T parsedValue;

    private OptionDefinition(final OptionDeclaration declaration)
      throws ParseException
    {
      if (getType() != declaration.getType()) {
        final String msg =
          "option " + declaration + ": " +
          "definition type does not match delcaration type: " +
          getType() + " != " + declaration.getType();
        throw new ParseException(msg);
      }
      this.declaration = declaration;
      final String defaultValueAsString = declaration.getDefaultValueAsString();
      defaultValue =
        defaultValueAsString != null ? parse(defaultValueAsString) : null;
      parsedValue = null;
    }

    abstract T parse(final String strValue) throws ParseException;

    public T parseAndSet(final String strValue) throws ParseException
    {
      parsedValue = parse(strValue);
      return parsedValue;
    }

    public OptionDeclaration getDeclaration()
    {
      return declaration;
    }

    abstract Type getType();

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

    public boolean isDefined()
    {
      return isParsed() || hasDefaultValue();
    }

    public boolean isValid()
    {
      return isDefined() || !declaration.isMandatory();
    }

    public T getDefinition()
    {
      return isParsed() ? parsedValue : defaultValue;
    }

    public String toString()
    {
      return declaration.toString();
    }
  }

  public static class FlagOptionDefinition extends OptionDefinition<Boolean>
  {
    public static String OFF = "off";
    public static String ON = "on";

    public Type getType() { return Type.FLAG; }

    public FlagOptionDefinition(final OptionDeclaration declaration)
      throws ParseException
    {
      super(declaration);
    }

    public Boolean parse(final String strValue) throws ParseException
    {
      if (OFF.equals(strValue)) {
        return false;
      } else if (ON.equals(strValue)) {
        return true;
      } else {
        throw new ParseException(this +
                                 ": '" + OFF + "' or '" + ON + "' expected");
      }
    }

    public boolean isTrue()
    {
      return getDefinition();
    }
  }

  public static class BooleanOptionDefinition extends OptionDefinition<Boolean>
  {
    public Type getType() { return Type.BOOLEAN; }

    public BooleanOptionDefinition(final OptionDeclaration declaration)
      throws ParseException
    {
      super(declaration);
    }

    public Boolean parse(final String strValue) throws ParseException
    {
      if ("false".equals(strValue)) {
        return false;
      } else if ("true".equals(strValue)) {
        return true;
      } else {
        throw new ParseException(this + ": 'false' or 'true' expected");
      }
    }

    public boolean isTrue()
    {
      return getDefinition();
    }
  }

  public static class IntegerOptionDefinition extends OptionDefinition<Integer>
  {
    public Type getType() { return Type.INTEGER; }

    public IntegerOptionDefinition(final OptionDeclaration declaration)
      throws ParseException
    {
      super(declaration);
    }

    public Integer parse(final String strValue) throws ParseException
    {
      if (strValue.startsWith("0x") || strValue.startsWith("0X")) {
        return Integer.parseUnsignedInt(strValue.substring(2), 16);
      } else {
        return Integer.parseInt(strValue);
      }
    }

    public Integer getValue()
    {
      return getDefinition();
    }
  }

  public static class StringOptionDefinition extends OptionDefinition<String>
  {
    public Type getType() { return Type.STRING; }

    public StringOptionDefinition(final OptionDeclaration declaration)
      throws ParseException
    {
      super(declaration);
    }

    public String parse(final String strValue) throws ParseException
    {
      return strValue;
    }

    public String getValue()
    {
      return getDefinition();
    }
  }

  private final String prgName;
  private final String prgDescription;
  private final OptionDeclaration[] declarations;
  private final OptionDefinition<?>[] definitions;

  public CmdOptions(final String prgName,
                    final String prgDescription,
                    final OptionDeclaration[] declarations)
    throws ParseException
  {
    if (prgName == null) {
      throw new NullPointerException("prgName");
    }
    if (declarations == null) {
      throw new NullPointerException("declarations");
    }
    this.prgName = prgName;
    this.prgDescription = prgDescription;
    this.declarations = declarations;
    definitions = createDefinitions();
  }

  public String getPrgName()
  {
    return prgName;
  }

  public String getPrgDescription()
  {
    return prgDescription;
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
    for (final OptionDeclaration declaration : declarations) {
      sb.append(declaration.getHelp());
      sb.append(ls);
    }
    return sb.toString();
  }

  public String getFullInfo()
  {
    final String ls = System.lineSeparator();
    return
      getUsage() + ls +
      (prgDescription != null ? prgDescription + ls : "") +
      getHelp();
  }

  private static final OptionDefinition<?>[] EMPTY_DEFINITIONS =
    new OptionDefinition<?>[0];

  private OptionDefinition<?>[] createDefinitions() throws ParseException
  {
    final ArrayList<OptionDefinition<?>> definitionList =
      new ArrayList<OptionDefinition<?>>();
    for (final OptionDeclaration declaration : declarations) {
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
      throw new ParseException("option redefined: " +
                               definition.getDeclaration());
    }
    if (definition instanceof FlagOptionDefinition) {
      if (strValue != null) {
        throw new ParseException("unexpected arg for option: " + definition);
      }
      definition.parseAndSet(FlagOptionDefinition.ON);
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
    if (option.length() < 1) {
      throw new ParseException("long option must not be empty: --" + option);
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
    if (name.length() < 1) {
      throw new ParseException("long option name must not be empty: --" +
                               option);
    }
    OptionDefinition<?> definition = null;
    for (final OptionDeclaration declaration : declarations) {
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
      throw new ParseException("short option name must be a single character: " +
                               name);
    }
    OptionDefinition<?> definition = null;
    for (final OptionDeclaration declaration : declarations) {
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
      throw new ParseException("option identifier expected: " + arg);
    }
  }

  public void parse(final String argv[]) throws ParseException
  {
    OptionDefinition<?> currentOption = null;
    for (final String arg : argv) {
      if (arg == null) {
        throw new NullPointerException("arg");
      }
      if (currentOption != null) {
        if (currentOption.isParsed()) {
          throw new ParseException("option redefined: " +
                                   currentOption.getDeclaration());
        }
        currentOption.parseAndSet(arg);
        currentOption = null;
      } else {
        currentOption = parseOptionIdentifier(arg);
      }
    }
    if (currentOption != null) {
      throw new ParseException("missing argument for option: " +
                               currentOption.getDeclaration());
    }
    for (final OptionDefinition<?> definition : definitions) {
      if (!definition.isValid()) {
        throw new ParseException("missing option: " +
                                 definition.getDeclaration());
      }
    }
  }

  public <T> T getDefinition(final OptionDefinition<T> definition)
  {
    return definition.getDefinition();
  }

  public OptionDefinition<?>
    findDefinitionForDeclaration(final OptionDeclaration declaration)
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
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
