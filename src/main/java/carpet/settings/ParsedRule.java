package carpet.settings;

import carpet.utils.Translations;
import carpet.utils.Messenger;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import org.apache.commons.lang3.ClassUtils;

import static carpet.utils.Translations.tr;

/**
 * A parsed Carpet rule, with its field, name, value, and other useful stuff.
 * 
 * It is generated from the fields with the {@link Rule} annotation
 * when being parsed by {@link SettingsManager#parseSettingsClass(Class)}.
 *
 * @param <T> The field's type
 */
public final class ParsedRule<T> implements Comparable<ParsedRule> {
    public final Field field;
    public final String name;
    public final String description;
    public final String scarpetApp;
    public final List<String> extraInfo;
    public final List<String> categories;
    public final List<String> options;
    public boolean isStrict;
    public boolean isClient;
    public final Class<T> type;
    public final List<Validator<T>> validators;
    public final T defaultValue;
    public final String defaultAsString;
    public final SettingsManager settingsManager;

    ParsedRule(Field field, Rule rule, SettingsManager settingsManager)
    {
        this.field = field;
        this.name = rule.name().isEmpty() ? field.getName() : rule.name();
        this.type = (Class<T>) field.getType();
        this.description = rule.desc();
        this.isStrict = rule.strict();
        this.extraInfo = List.of(rule.extra());
        this.categories = List.of(rule.category());
        this.scarpetApp = rule.appSource();
        this.settingsManager = settingsManager;
        this.validators = new ArrayList<>();
        for (Class v : rule.validate())
            this.validators.add((Validator<T>) callConstructor(v));
        if (categories.contains(RuleCategory.COMMAND))
        {
            this.validators.add(callConstructor(Validator._COMMAND.class));
            if (this.type == String.class)
            {
                this.isStrict = false;
                this.validators.add((Validator<T>) callConstructor(Validator._COMMAND_LEVEL_VALIDATOR.class));
            }
        }
        if (!scarpetApp.isEmpty())
        {
            this.validators.add((Validator<T>) callConstructor(Validator._SCARPET.class));
        }
        this.isClient = categories.contains(RuleCategory.CLIENT);
        if (this.isClient)
        {
            this.validators.add(callConstructor(Validator._CLIENT.class));
        }
        this.defaultValue = get();
        this.defaultAsString = convertToString(this.defaultValue);
        if (rule.options().length > 0)
        {
            this.options = List.of(rule.options());
        }
        else if (this.type == boolean.class){
            this.options = List.of("true","false");
        }
        else if(this.type == String.class && categories.contains(RuleCategory.COMMAND))
        {
            this.options = List.of("true", "false", "ops");
        }
        else if (this.type.isEnum())
        {
            this.options = Arrays.stream(this.type.getEnumConstants()).map(e -> ((Enum) e).name().toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableList());
        }
        else
        {
            this.options = List.of();
        }
        if (isStrict && !this.options.isEmpty())
        {
            if (this.type == boolean.class || this.type == int.class || this.type == double.class || this.type == float.class)
            {
                this.validators.add(callConstructor(Validator._STRICT_IGNORECASE.class));
            }
            else
            {
                this.validators.add(callConstructor(Validator._STRICT.class));
            }
        }
    }

    private <T> T callConstructor(Class<T> cls)
    {
        try
        {
            Constructor<T> constr = cls.getDeclaredConstructor();
            constr.setAccessible(true);
            return constr.newInstance();
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    public ParsedRule<T> set(CommandSourceStack source, String value)
    {
        if (settingsManager != null && settingsManager.locked)
            return null;
        if (type == String.class)
        {
            return set(source, (T) value, value);
        }
        else if (type == boolean.class)
        {
            return set(source, (T) (Object) Boolean.parseBoolean(value), value);
        }
        else if (type == int.class)
        {
            return set(source, (T) (Object) Integer.parseInt(value), value);
        }
        else if (type == double.class)
        {
            return set(source, (T) (Object) Double.parseDouble(value), value);
        }
        else if (type.isEnum())
        {
            String ucValue = value.toUpperCase(Locale.ROOT);
            return set(source, (T) (Object) Enum.valueOf((Class<? extends Enum>) type, ucValue), value);
        }
        else
        {
            Messenger.m(source, "r Unknown type " + type.getSimpleName());
            return null;
        }
    }

    ParsedRule<T> set(CommandSourceStack source, T value, String stringValue)
    {
        try
        {
            for (Validator<T> validator : this.validators)
            {
                value = validator.validate(source, this, value, stringValue);
                if (value == null)
                {
                    if (source != null)
                    {
                        validator.notifyFailure(source, this, stringValue);
                        if (validator.description() != null)
                            Messenger.m(source, "r " + validator.description());
                    }
                    return null;
                }
            }
            if (!value.equals(get()) || source == null)
            {
                this.field.set(null, value);
                if (source != null) settingsManager.notifyRuleChanged(source, this, stringValue);
            }
        }
        catch (IllegalAccessException e)
        {
            Messenger.m(source, "r Unable to access setting for  "+name);
            return null;
        }
        return this;
    }

    /**
     * @return The value of this {@link ParsedRule}, in its type
     */
    public T get()
    {
        try
        {
            return (T) this.field.get(null);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return The value of this {@link ParsedRule}, as a {@link String}
     */
    public String getAsString()
    {
        return convertToString(get());
    }

    /**
     * @return The value of this {@link ParsedRule}, converted to a {@link boolean}.
     *         It will only return {@link true} if it's a true {@link boolean} or
     *         a number greater than zero.
     */
    public boolean getBoolValue()
    {
        if (type == boolean.class) return (Boolean) get();
        if (ClassUtils.primitiveToWrapper(type).isAssignableFrom(Number.class)) return ((Number) get()).doubleValue() > 0;
        return false;
    }

    /**
     * @return Wether or not this {@link ParsedRule} is in its default value
     */
    public boolean isDefault()
    {
        return defaultValue.equals(get());
    }

    /**
     * Resets this rule to its default value
     */
    public void resetToDefault(CommandSourceStack source)
    {
        set(source, defaultValue, defaultAsString);
    }


    private static String convertToString(Object value)
    {
        if (value instanceof Enum) return ((Enum) value).name().toLowerCase(Locale.ROOT);
        return value.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj.getClass() == ParsedRule.class && ((ParsedRule) obj).name.equals(this.name);
    }

    @Override
    public int hashCode()
    {
        return this.name.hashCode();
    }

    @Override
    public int compareTo(ParsedRule o)
    {
        return this.name.compareTo(o.name);
    }

    @Override
    public String toString()
    {
        return this.name + ": " + getAsString();
    }

    private String translationKey()
    {
        return String.format("rule.%s.name", name);
    }

    /**
     * @return A {@link String} being the translated {@link ParsedRule#name} of this rule,
     *                          in Carpet's configured language.
     */
    public String translatedName(){
        String key = translationKey();
        return Translations.hasTranslation(key) ? tr(key) + String.format(" (%s)", name): name;
    }

    /**
     * @return A {@link String} being the translated {@link ParsedRule#description} of this rule,
     *                          in Carpet's configured language.
     */
    public String translatedDescription()
    {
        return tr(String.format("rule.%s.desc", (name)), description);
    }

    /**
     * @return A {@link String} being the translated {@link ParsedRule#extraInfo} of this 
     * 	                        {@link ParsedRule}, in Carpet's configured language.
     */
    public List<String> translatedExtras()
    {
        if (!Translations.hasTranslations()) return extraInfo;
        String keyBase = String.format("rule.%s.extra.", name);
        List<String> extras = new ArrayList<>();
        int i = 0;
        while (Translations.hasTranslation(keyBase+i))
        {
            extras.add(Translations.tr(keyBase+i));
            i++;
        }
        return (extras.isEmpty()) ? extraInfo : extras;
    }
}
