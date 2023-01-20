package carpet.settings;

import carpet.CarpetSettings;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.InvalidRuleValueException;
import carpet.api.settings.RuleHelper;
import carpet.api.settings.SettingsManager;
import carpet.api.settings.Validators;
import carpet.utils.Messenger;
import carpet.utils.TranslationKeys;
import carpet.utils.Translations;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ClassUtils;

/**
 * A Carpet rule parsed from a field, with its name, value, and other useful stuff.
 * 
 * It is used for the fields with the {@link Rule} annotation
 * when being parsed by {@link SettingsManager#parseSettingsClass(Class)}.
 *
 * @param <T> The field's (and rule's) type
 * @deprecated Use the type {@link CarpetRule} instead
 */
@Deprecated(forRemoval = true) // to move to api.settings package and visibility to package private
public final class ParsedRule<T> implements CarpetRule<T>, Comparable<ParsedRule<?>> {
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
    @Deprecated(forRemoval = true) // to private
    public final Field field;
    /**
     * @deprecated Use {@link CarpetRule#name()} instead
     */
    @Deprecated(forRemoval = true) // to private
    public final String name;
    /**
     * @deprecated Use {@link RuleHelper#translatedDescription(CarpetRule)}, or get it from the translation system
     */
    @Deprecated(forRemoval = true) // to remove
    public final String description;
    /**
     * @deprecated Use {@link CarpetRule#extraInfo()} instead
     */
    @Deprecated(forRemoval = true) // to remove
    public final List<String> extraInfo;
    /**
     * @deprecated Use {@link CarpetRule#categories()} instead
     */
    @Deprecated(forRemoval = true) // to private
    public final List<String> categories;
    /**
     * @deprecated Use {@link CarpetRule#suggestions()} instead
     */
    @Deprecated(forRemoval = true) // to private (and rename?)
    public final List<String> options;
    /**
     * @deprecated Use {@link CarpetRule#strict()} instead
     */
    @Deprecated(forRemoval = true) // to remove or fix
    public boolean isStrict;
    /**
     * @deprecated Use {@link CarpetRule#canBeToggledClientSide()} instead
     */
    @Deprecated(forRemoval = true) // to private (and maybe rename?)
    public boolean isClient;
    /**
     * @deprecated Use {@link CarpetRule#type()} instead
     */
    @Deprecated(forRemoval = true) // to private (or remove and delegate to typedfield?)
    public final Class<T> type;
    /**
     * @deprecated Use {@link CarpetRule#defaultValue()} instead
     */
    @Deprecated(forRemoval = true) // to private
    public final T defaultValue;
    /**
     * @deprecated Use {@link CarpetRule#settingsManager()} instead.
     *             This field may be {@code null} if the settings manager isn't an instance of the old type
     */
    @Deprecated(forRemoval = true) // to remove in favour of realSettingsManager
    public final carpet.settings.SettingsManager settingsManager;
    /**
     * @deprecated No replacement for this. A Carpet rule may not use {@link Validator}
     */
    @Deprecated(forRemoval = true) // to remove (in favour of realValidators)
    public final List<Validator<T>> validators;
    /**
     * @deprecated Use {@link CarpetRule#defaultValue()} and pass it to {@link RuleHelper#toRuleString(Object)} instead
     */
    @Deprecated(forRemoval = true) // to remove
    public final String defaultAsString;
    /**
     * @deprecated No replacement for this, Scarpet Rules should be managed by the rule implementation 
     */
    @Deprecated(forRemoval = true) // to private/subclass
    public final String scarpetApp;
    private final FromStringConverter<T> converter;
    private final SettingsManager realSettingsManager; // to rename to settingsManager
    /**
     * If you reference this field I'll steal your kneecaps
     */
    @Deprecated(forRemoval = true)
    public final List<carpet.api.settings.Validator<T>> realValidators; // to rename to validators and to package private for printRulesToLog
    private final boolean isLegacy; // to remove, only used for fallbacks
    
    @FunctionalInterface
    interface FromStringConverter<T> {
        T convert(String value) throws InvalidRuleValueException;
    }
    
    record RuleAnnotation(boolean isLegacy, String name, String desc, String[] extra, String[] category, String[] options, boolean strict, String appSource, Class<? extends carpet.api.settings.Validator>[] validators) {
    }

    /**
     * If you call this method I'll steal your kneecaps
     */
    @Deprecated(forRemoval = true)
    public static <T> ParsedRule<T> of(Field field, SettingsManager settingsManager) {
        RuleAnnotation rule;
        if (settingsManager instanceof carpet.settings.SettingsManager && field.isAnnotationPresent(Rule.class)) { // Legacy path
            Rule a = field.getAnnotation(Rule.class);
            rule = new RuleAnnotation(true, a.name(), a.desc(), a.extra(), a.category(), a.options(), a.strict(), a.appSource(), a.validate());
        } else if (field.isAnnotationPresent(carpet.api.settings.Rule.class)) {
            carpet.api.settings.Rule a = field.getAnnotation(carpet.api.settings.Rule.class);
            rule = new RuleAnnotation(false, null, null, null, a.categories(), a.options(), a.strict(), a.appSource(), a.validators());
        } else {
            // Don't allow to use old rule types in custom AND migrated settings manager
            throw new IllegalArgumentException("Old rule annotation is only supported in legacy SettngsManager!");
        }
        return new ParsedRule<>(field, rule, settingsManager);
    }

    private ParsedRule(Field field, RuleAnnotation rule, SettingsManager settingsManager)
    {
        this.isLegacy = rule.isLegacy();
        this.name = !isLegacy || rule.name().isEmpty() ? field.getName() : rule.name();
        this.field = field;
        @SuppressWarnings("unchecked") // We are "defining" T here
        Class<T> type = (Class<T>)ClassUtils.primitiveToWrapper(field.getType());
        this.type = type;
        this.isStrict = rule.strict();
        this.categories = List.of(rule.category());
        this.scarpetApp = rule.appSource();
        this.realSettingsManager = settingsManager;
        if (!(settingsManager instanceof carpet.settings.SettingsManager)) {
            // this is awkward... but people using a custom, new (extends only new api) manager should not be using this anyway but the interface method
            this.settingsManager = null;
        } else {
            this.settingsManager = (carpet.settings.SettingsManager) settingsManager;
        }
        this.realValidators = Stream.of(rule.validators()).map(this::instantiateValidator).collect(Collectors.toList());
        this.defaultValue = value();
        FromStringConverter<T> converter0 = null;
        
        if (categories.contains(RuleCategory.COMMAND))
        {
            this.realValidators.add(new Validator._COMMAND<T>());
            if (this.type == String.class)
            {
                this.realValidators.add(instantiateValidator(Validators.CommandLevel.class));
            }
        }
        
        this.isClient = categories.contains(RuleCategory.CLIENT);
        if (this.isClient)
        {
            this.realValidators.add(new Validator._CLIENT<>());
        }
        
        if (!scarpetApp.isEmpty())
        {
            this.realValidators.add(new Validator.ScarpetValidator<>());
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
            this.options = Validators.CommandLevel.OPTIONS;
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
                    throw new InvalidRuleValueException("Valid values for this rule are: " + this.options);
                }
            };
        }
        else
        {
            this.options = List.of();
        }
        if (isStrict && !this.options.isEmpty())
        {
            this.realValidators.add(0, new Validator.StrictValidator<>()); // at 0 prevents validators with side effects from running when invalid
        }
        if (converter0 == null) {
            @SuppressWarnings("unchecked")
            FromStringConverter<T> converterFromMap = (FromStringConverter<T>)CONVERTER_MAP.get(type);
            if (converterFromMap == null) throw new UnsupportedOperationException("Unsupported type for ParsedRule" + type);
            converter0 = converterFromMap;
        }
        this.converter = converter0;
        
        // Language "constants"
        String nameKey = TranslationKeys.RULE_NAME_PATTERN.formatted(settingsManager().identifier(), name());
        String descKey = TranslationKeys.RULE_DESC_PATTERN.formatted(settingsManager().identifier(), name());
        String extraPrefix = TranslationKeys.RULE_EXTRA_PREFIX_PATTERN.formatted(settingsManager().identifier(), name());
        
        // to remove
        this.description = isLegacy ? rule.desc() : Objects.requireNonNull(Translations.trOrNull(descKey), "No language key provided for " + descKey);
        this.extraInfo = isLegacy ? List.of(rule.extra()) : getTranslationArray(extraPrefix);
        this.defaultAsString = RuleHelper.toRuleString(this.defaultValue);
        this.validators = realValidators.stream().filter(Validator.class::isInstance).map(v -> (Validator<T>) v).toList();
        if (!isLegacy && !validators.isEmpty()) throw new IllegalArgumentException("Can't use legacy validators with new rules!");

        // Language fallbacks - Also asserts the strings will be available in non-english languages, given current system has no fallback
        if (isLegacy && !rule.name().isEmpty()) Translations.registerFallbackTranslation(nameKey, name);
        Translations.registerFallbackTranslation(descKey, description);
        Iterator<String> infoIterator = extraInfo.iterator();
        for (int i = 0; infoIterator.hasNext(); i++) {
            Translations.registerFallbackTranslation(extraPrefix + i, infoIterator.next());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // Needed because of the annotation
    private carpet.api.settings.Validator<T> instantiateValidator(Class<? extends carpet.api.settings.Validator> cls)
    {
        try
        {
            Constructor<? extends carpet.api.settings.Validator> constr = cls.getDeclaredConstructor();
            constr.setAccessible(true);
            return constr.newInstance();
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(CommandSourceStack source, String value) throws InvalidRuleValueException
    {
        set(source, converter.convert(value), value);
    }

    private void set(CommandSourceStack source, T value, String userInput) throws InvalidRuleValueException
    {
        for (carpet.api.settings.Validator<T> validator : this.realValidators)
        {
            value = validator.validate(source, this, value, userInput); // should this recalculate the string? Another validator may have changed value
            if (value == null) {
                if (source != null) validator.notifyFailure(source, this, userInput);
                throw new InvalidRuleValueException();
            }
        }
        if (!value.equals(value()) || source == null)
        {
            try {
                this.field.set(null, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Couldn't access field for rule: " + name, e);
            }
            if (source != null) settingsManager().notifyRuleChanged(source, this, userInput);
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
    public List<Component> extraInfo() {
        return getTranslationArray(TranslationKeys.RULE_EXTRA_PREFIX_PATTERN.formatted(settingsManager().identifier(), name()))
                .stream()
                .map(str -> Messenger.c("g " + str))
                .toList();
    }
    
    private List<String> getTranslationArray(String prefix) {
        List<String> ret = new ArrayList<>();
        for (int i = 0; Translations.hasTranslation(prefix + i); i++) {
            ret.add(Translations.tr(prefix + i));
        }
        return ret;
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
        return realSettingsManager;
    }

    @Override
    @SuppressWarnings("unchecked") // T comes from the field
    public T value() {
        try {
            return (T) field.get(null);
        } catch (IllegalAccessException e) {
            // Can't happen at regular runtime because we'd have thrown it on construction 
            throw new IllegalArgumentException("Couldn't access field for rule: " + name, e);
        }
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
    public void set(CommandSourceStack source, T value) throws InvalidRuleValueException {
        set(source, value, RuleHelper.toRuleString(value));
    }

    @Override
    public boolean strict() {
        return !realValidators.isEmpty() && realValidators.get(0) instanceof Validator.StrictValidator;
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
     * @deprecated Use {@link RuleHelper#resetToDefault(CarpetRule, CommandSourceStack)}
     */
    @Deprecated(forRemoval = true)
    public void resetToDefault(CommandSourceStack source)
    {
        RuleHelper.resetToDefault(this, source);
    }

    /**
     * @deprecated Forcing {@link Comparable} isn't a thing on {@link CarpetRule}s. Instead, pass a comparator by name to your
     *             sort methods, you can get one by calling {@code Comparator.comparing(CarpetRule::name)}
     */
    @Override
    @Deprecated(forRemoval = true)
    public int compareTo(ParsedRule<?> o)
    {
        if (!warnedComparable) {
            warnedComparable = true;
            CarpetSettings.LOG.warn("""
                    Extension is relying on carpet rules to be comparable! This is not true for all carpet rules anymore, \
                    and will crash the game in future versions or if an extension adds non-comparable rules!
                    Fixing it is as simple as passing Comparator.comparing(CarpetRule::name) to the sorting method!""",
                    new Throwable("Location:").fillInStackTrace());
        }
        return this.name.compareTo(o.name);
    }
    private static boolean warnedComparable = false;

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
     * @deprecated Use {@link CarpetRule#extraInfo()} instead
     */
    @Deprecated(forRemoval = true)
    public List<String> translatedExtras()
    {
        return extraInfo().stream().map(Component::getString).toList();
    }
}
