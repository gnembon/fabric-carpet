package carpet.settings;

import carpet.utils.TypedField;
import net.minecraft.server.command.ServerCommandSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Carpet rule parsed from a field, with its name, value, and other useful stuff.
 * 
 * It is used for the fields with the {@link Rule} annotation
 * when being parsed by {@link SettingsManager#parseSettingsClass(Class)}.
 *
 * @param <T> The field's (and rule's) type
 * @deprecated Use the type {@link CarpetRule} instead, since it's not implementation specific
 */
@Deprecated(forRemoval = true) // to package private
public final class ParsedRule<T> implements CarpetRule<T> {
    private static final Map<Class<?>, FromStringConverter<?>> CONVERTER_MAP = Map.ofEntries(
            Map.entry(String.class, str -> str),
            Map.entry(Boolean.class, str -> {
                return switch (str) {
                    case "true" -> true;
                    case "false" -> false;
                    default -> throw new InvalidRuleValueException("Invalid boolean value");
                };
            }),
            numericalConverter(Integer.class, Integer::parseInt),
            numericalConverter(Double.class, Double::parseDouble),
            numericalConverter(Long.class, Long::parseLong),
            numericalConverter(Float.class, Float::parseFloat)
        );
    /**
     * @deprecated No replacement for this, since a {@link CarpetRule} may not always use a {@link Field}.
     *             Use {@link #value()} to access the rule's value
     */
    @Deprecated(forRemoval = true) // to remove
    public final Field field;
    /**
     * @deprecated Use {@link CarpetRule#name()}
     */
    @Deprecated(forRemoval = true) // to private
    public final String name;
    /**
     * @deprecated Use {@link CarpetRule#description}
     */
    @Deprecated(forRemoval = true) // to private
    public final String description;
    /**
     * @deprecated Use {@link CarpetRule#extraInfo()}
     */
    @Deprecated(forRemoval = true) // to private
    public final List<String> extraInfo;
    /**
     * @deprecated Use {@link CarpetRule#categories()}
     */
    @Deprecated(forRemoval = true) // to private
    public final List<String> categories;
    /**
     * @deprecated Use {@link CarpetRule#suggestions()} instead
     */
    @Deprecated(forRemoval = true) // to private (and rename?)
    public final List<String> options;
    /**
     * @deprecated No replacement for this
     */
    @Deprecated(forRemoval = true) // to pckg private (for printRulesToLog, or get a different way)
    public boolean isStrict;
    /**
     * @deprecated Use {@link CarpetRule#canBeToggledClientSide()}
     */
    @Deprecated(forRemoval = true) // to private (and maybe rename?)
    public boolean isClient;
    /**
     * @deprecated Use {@link CarpetRule#type()}
     */
    @Deprecated(forRemoval = true) // to private (or remove and delegate to typedfield?)
    public final Class<T> type;
    /**
     * @deprecated Use {@link CarpetRule#defaultValue()}
     */
    @Deprecated(forRemoval = true) // to private
    public final T defaultValue;
    /**
     * @deprecated Use {@link CarpetRule#settingsManager()}
     */
    @Deprecated(forRemoval = true) // to private
    public final SettingsManager settingsManager;
    /**
     * @deprecated No replacement for this. A Carpet rule may not use {@link Validator}
     */
    @Deprecated(forRemoval = true) // to pckg private (for printRulesToLog)
    public final List<Validator<T>> validators;
    /**
     * @deprecated Use {@link CarpetRule#defaultValue()} and pass it to {@link RuleHelper#toRuleString(Object)}
     */
    @Deprecated(forRemoval = true) // to remove
    public final String defaultAsString;
    /**
     * @deprecated No replacement for this, Scarpet Rules should be managed by the rule implementation 
     */
    @Deprecated(forRemoval = true) // to private/subclass
    public final String scarpetApp;
    private final FromStringConverter<T> converter;
    private final TypedField<T> typedField; // to rename to field
    
    @FunctionalInterface
    interface FromStringConverter<T> {
        T convert(String value) throws InvalidRuleValueException;
    }

    // More flexible than a constructor, since it can return subclasses (doesn't do that yet) and null
    static <T> ParsedRule<T> of(Field field, Rule rule, SettingsManager settingsManager) {
        return new ParsedRule<>(field, rule, settingsManager);
    }

    private ParsedRule(Field field, Rule rule, SettingsManager settingsManager)
    {
        this.name = rule.name().isEmpty() ? field.getName() : rule.name();
        try {
            this.typedField = new TypedField<>(field);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Couldn't access given field", e);
        }
        this.type = typedField.type();
        this.description = rule.desc();
        this.isStrict = rule.strict();
        this.extraInfo = List.of(rule.extra());
        this.categories = List.of(rule.category());
        this.scarpetApp = rule.appSource();
        this.settingsManager = settingsManager;
        this.validators = Stream.of(rule.validate()).map(this::instantiateValidator).collect(Collectors.toList());
        this.defaultValue = value();
        FromStringConverter<T> converter0 = null;
        
        if (categories.contains(RuleCategory.COMMAND))
        {
            this.validators.add(new Validator._COMMAND<T>());
            if (this.type == String.class)
            {
                this.validators.add(instantiateValidator(Validator._COMMAND_LEVEL_VALIDATOR.class));
            }
        }
        
        this.isClient = categories.contains(RuleCategory.CLIENT);
        if (this.isClient)
        {
            this.validators.add(new Validator._CLIENT<>());
        }
        
        if (!scarpetApp.isEmpty())
        {
            this.validators.add(new Validator._SCARPET<>());
        }
        
        if (rule.options().length > 0)
        {
            this.options = List.of(rule.options());
        }
        else if (this.type == Boolean.class) {
            this.options = List.of("true", "false");
        }
        else if (this.type == String.class && categories.contains(RuleCategory.COMMAND))
        {
            this.options = Validator._COMMAND_LEVEL_VALIDATOR.OPTIONS;
        }
        else if (this.type.isEnum())
        {
            this.options = Arrays.stream(this.type.getEnumConstants()).map(e -> ((Enum<?>) e).name().toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableList());
            converter0 = str -> {
                try {
                    @SuppressWarnings({"unchecked", "rawtypes"}) // Raw necessary because of signature. Unchecked because compiler doesn't know T extends Enum
                    T ret = (T)Enum.valueOf((Class<? extends Enum>) type, str.toUpperCase(Locale.ROOT));
                    return ret;
                } catch (IllegalArgumentException e) {
                    throw new InvalidRuleValueException("Invalid value for rule. Valid ones are: " + this.options);
                }
            };
        }
        else
        {
            this.options = List.of();
        }
        if (isStrict && !this.options.isEmpty())
        {
            this.validators.add(new Validator._STRICT<>());
        }
        if (converter0 == null) {
            @SuppressWarnings("unchecked")
            FromStringConverter<T> converterFromMap = (FromStringConverter<T>)CONVERTER_MAP.get(type);
            if (converterFromMap == null) throw new UnsupportedOperationException("Unsupported type for ParsedRule" + type);
            converter0 = converterFromMap;
        }
        this.converter = converter0;
        
        // to remove
        this.defaultAsString = RuleHelper.toRuleString(this.defaultValue);
        this.field = field;
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // Needed because of the annotation
    private Validator<T> instantiateValidator(Class<? extends Validator> cls)
    {
        try
        {
            Constructor<? extends Validator> constr = cls.getDeclaredConstructor();
            constr.setAccessible(true);
            return constr.newInstance();
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    public void set(ServerCommandSource source, String value) throws InvalidRuleValueException
    {
        set(source, converter.convert(value), value);
    }

    private void set(ServerCommandSource source, T value, String stringValue) throws InvalidRuleValueException
    {
        for (Validator<T> validator : this.validators)
        {
            stringValue = RuleHelper.toRuleString(value);
            value = validator.validate(source, (CarpetRule<T>)this, value, stringValue);
            if (value == null) {
                if (source != null) validator.notifyFailure(source, this, stringValue);
                throw new InvalidRuleValueException();
            }
        }
        if (!value.equals(value()) || source == null)
        {
            this.typedField.setStatic(value);
            if (source != null) settingsManager.notifyRuleChanged(source, this);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof ParsedRule && ((ParsedRule<?>) obj).name.equals(this.name);
    }

    @Override
    public int hashCode()
    {
        return this.name.hashCode();
    }

    @Override
    public String toString()
    {
        return this.name + ": " + RuleHelper.toRuleString(value());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public List<String> extraInfo() {
        return extraInfo;
    }

    @Override
    public Collection<String> categories() {
        return categories;
    }

    @Override
    public Collection<String> suggestions() {
        return options;
    }
    
    @Override
    public SettingsManager settingsManager() {
        return settingsManager;
    }

    @Override
    public T value() {
        return typedField.getStatic();
    }

    @Override
    public boolean canBeToggledClientSide() {
        return isClient;
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public T defaultValue() {
        return defaultValue;
    }

    @Override
    public void set(ServerCommandSource source, T value) throws InvalidRuleValueException {
        set(source, value, RuleHelper.toRuleString(value));
    }

    private static <T> Map.Entry<Class<T>, FromStringConverter<T>> numericalConverter(Class<T> outputClass, Function<String, T> converter) {
        return Map.entry(outputClass, str -> {
            try {
                return converter.apply(str);
            } catch (NumberFormatException e) {
                throw new InvalidRuleValueException("Invalid number for rule");
            }
        });
    }
    
    //TO REMOVE
    
    /**
     * @deprecated Use {@link CarpetRule#value()} instead
     */
    @Deprecated(forRemoval = true)
    public T get()
    {
        return value();
    }

    /**
     * @deprecated Use {@link RuleHelper#toRuleString(Object) RuleHelper.convertToRuleString(rule.value())}
     */
    @Deprecated(forRemoval = true)
    public String getAsString()
    {
        return RuleHelper.toRuleString(value());
    }

    /**
     * @return The value of this {@link ParsedRule}, converted to a {@link boolean}.
     *         It will only return {@link true} if it's a true {@link boolean} or
     *         a number greater than zero.
     * @deprecated Use {@link RuleHelper#getBooleanValue(CarpetRule)}
     */
    @Deprecated(forRemoval = true)
    public boolean getBoolValue()
    {
        return RuleHelper.getBooleanValue(this);
    }

    /**
     * @deprecated Use {@link RuleHelper#isInDefaultValue(CarpetRule)}
     */
    @Deprecated(forRemoval = true)
    public boolean isDefault()
    {
        return RuleHelper.isInDefaultValue(this);
    }

    /**
     * @deprecated Use {@link RuleHelper#resetToDefault(CarpetRule, ServerCommandSource)}
     */
    @Deprecated(forRemoval = true)
    public void resetToDefault(ServerCommandSource source)
    {
        RuleHelper.resetToDefault(this, source);
    }

    /**
     * @return A {@link String} being the translated {@link ParsedRule#name} of this rule,
     *                          in Carpet's configured language.
     * @deprecated Use {@link RuleHelper#translatedName(CarpetRule)} instead
     */
    @Deprecated(forRemoval = true)
    public String translatedName() {
        return RuleHelper.translatedName(this);
    }

    /**
     * @return A {@link String} being the translated {@link ParsedRule#description description} of this rule,
     *                          in Carpet's configured language.
     * @deprecated Use {@link RuleHelper#translatedDescription(CarpetRule)} instead
     */
    @Deprecated(forRemoval = true)
    public String translatedDescription()
    {
        return RuleHelper.translatedDescription(this);
    }

    /**
     * @return A {@link String} being the translated {@link ParsedRule#extraInfo extraInfo} of this 
     *                             {@link ParsedRule}, in Carpet's configured language.
     * @deprecated Use {@link RuleHelper#translatedExtras(CarpetRule)} instead
     */
    @Deprecated(forRemoval = true)
    public List<String> translatedExtras()
    {
        return RuleHelper.translatedExtras(this);
    }
}
