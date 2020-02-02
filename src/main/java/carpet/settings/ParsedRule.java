package carpet.settings;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import com.google.common.collect.ImmutableList;
import net.minecraft.server.command.ServerCommandSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ParsedRule<T> implements Comparable<ParsedRule> {
    public final Field field;
    public final String name;
    public final String description;
    public final ImmutableList<String> extraInfo;
    public final ImmutableList<String> categories;
    public final ImmutableList<String> options;
    public boolean isStrict;
    public final Class<T> type;
    public final List<Validator<T>> validators;
    public final T defaultValue;
    public final String defaultAsString;

    ParsedRule(Field field, Rule rule)
    {
        this.field = field;
        this.name = rule.name().isEmpty() ? field.getName() : rule.name();
        this.type = (Class<T>) field.getType();
        this.description = rule.desc();
        this.isStrict = rule.strict();
        this.extraInfo = ImmutableList.copyOf(rule.extra());
        this.categories = ImmutableList.copyOf(rule.category());
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
        this.defaultValue = get();
        this.defaultAsString = convertToString(this.defaultValue);
        if (rule.options().length > 0)
        {
            this.options = ImmutableList.copyOf(rule.options());
        }
        else if (this.type == boolean.class || (this.type == String.class && categories.contains(RuleCategory.COMMAND)))
        {
            this.options = ImmutableList.of("true", "false");
        }
        else if (this.type.isEnum())
        {
            this.options = Arrays.stream(this.type.getEnumConstants()).map(e -> ((Enum) e).name().toLowerCase(Locale.ROOT)).collect(ImmutableList.toImmutableList());
        }
        else
        {
            this.options = ImmutableList.of();
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

    public ParsedRule<T> set(ServerCommandSource source, String value)
    {
        if (CarpetServer.settingsManager != null && CarpetServer.settingsManager.locked)
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

    ParsedRule<T> set(ServerCommandSource source, T value, String stringValue)
    {
        try
        {
            for (Validator<T> validator : this.validators)
            {
                value = validator.validate(source, this, value, stringValue);
                if (value == null)
                {
                    Messenger.m(source, "r Wrong value for " + name + ": " + stringValue);
                    if (validator.description()!= null)
                        Messenger.m(source, "r " + validator.description());
                    return null;
                }
            }
            if (!value.equals(get()))
            {
                this.field.set(null, value);
                CarpetServer.settingsManager.notifyRuleChanged(source, this, stringValue);
            }
        }
        catch (IllegalAccessException e)
        {
            Messenger.m(source, "r Unable to access setting for  "+name);
            return null;
        }
        return this;
    }

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

    public String getAsString()
    {
        return convertToString(get());
    }

    public boolean getBoolValue()
    {
        if (type == boolean.class) return (Boolean) get();
        if (type.isAssignableFrom(Number.class)) return ((Number) get()).doubleValue() > 0;
        return false;
    }

    public boolean isDefault()
    {
        return defaultValue.equals(get());
    }

    public void resetToDefault(ServerCommandSource source)
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
}
