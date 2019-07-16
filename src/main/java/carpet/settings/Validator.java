package carpet.settings;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Locale;

public abstract class Validator<T>
{
    /**
     * Validate the new value of a rule
     * @return true if valid, false if new rule invalid.
     */
    public abstract T validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string);
    public String description() {return null;}

    public static class POSITIVE_NUMBER<T extends Number> extends Validator<T>
    {
        @Override
        public T validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string)
        {
            return newValue.doubleValue() > 0 ? newValue : null;
        }
        @Override
        public String description() { return "Must be a positive number"; }
    }

    public static class _COMMAND<T> extends Validator<T>
    {
        @Override
        public T validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string)
        {
            if (CarpetServer.settingsManager != null)
                CarpetServer.settingsManager.notifyPlayersCommandsChanged();
            return newValue;
        }
        @Override
        public String description() { return "It has an accompanying command";}
    }

    public static class WIP<T> extends Validator<T>
    {
        @Override
        public T validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string)
        {
            Messenger.m(source, "r "+currentRule.name+" is missing a few bits - we are still working on it.");
            return newValue;
        }
        @Override
        public String description() { return "A few bits still needs implementing - we are working on it";}
    }
    public static class _STRICT<T> extends Validator<T>
    {
        @Override
        public T validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string)
        {
            if (!currentRule.options.contains(string.toLowerCase(Locale.ROOT)))
            {
                Messenger.m(source, "r Valid options: " + currentRule.options.toString());
                return null;
            }
            return newValue;
        }
    }

    public static class NONNEGATIVE_NUMBER <T extends Number> extends Validator<T>
    {
        @Override
        public T validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string)
        {
            return newValue.doubleValue() >= 0 ? newValue : null;
        }
        @Override
        public String description() { return "Must be a positive number";}
    }
}
