package carpet.settings;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;

public abstract class Validator<T>
{
    /**
     * Validate the new value of a rule
     * @return true if valid, false if new rule invalid.
     */
    public abstract T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string);
    public String description() {return null;}
    public void notifyFailure(CommandSourceStack source, ParsedRule<T> currentRule, String providedValue)
    {
        Messenger.m(source, "r Wrong value for " + currentRule.name + ": " + providedValue);
    }

    public static class _COMMAND<T> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            if (CarpetServer.settingsManager != null && source != null)
                CarpetServer.settingsManager.notifyPlayersCommandsChanged();
            return newValue;
        }
        @Override
        public String description() { return "It has an accompanying command";}
    }

    public static class _CLIENT<T> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            return newValue;
        }
        @Override
        public String description() { return "Its a client command so can be issued and potentially be effective when connecting to non-carpet/vanilla servers. " +
                "In these situations (on vanilla servers) it will only affect the executing player, so each player needs to type it" +
                " separately for the desired effect";}
    }

    public static class _COMMAND_LEVEL_VALIDATOR extends Validator<String> {
        private static List<String> OPTIONS = List.of("true", "false", "ops", "0", "1", "2", "3", "4");
        @Override public String validate(CommandSourceStack source, ParsedRule<String> currentRule, String newValue, String userString) {
            if (!OPTIONS.contains(userString.toLowerCase(Locale.ROOT)))
            {
                Messenger.m(source, "r Valid options for command type rules is 'true' or 'false'");
                Messenger.m(source, "r Optionally you can choose 'ops' to only allow operators");
                Messenger.m(source, "r or provide a custom required permission level");
                return null;
            }
            return userString.toLowerCase(Locale.ROOT);
        }
        @Override public String description() { return "Can be limited to 'ops' only, or a custom permission level";}
    }
    
    public static class _SCARPET<T> extends Validator<T> {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            return newValue;
        }
        @Override public String description() {
            return "It controls an accompanying Scarpet App";
        }
    }

    public static class WIP<T> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
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
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            if (!currentRule.options.contains(string))
            {
                Messenger.m(source, "r Valid options: " + currentRule.options.toString());
                return null;
            }
            return newValue;
        }
    }

    public static class _STRICT_IGNORECASE<T> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            if (!currentRule.options.stream().map(s->s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet())
                    .contains(string.toLowerCase(Locale.ROOT)))
            {
                Messenger.m(source, "r Valid options (case insensitive): " + currentRule.options.toString());
                return null;
            }
            return newValue;
        }
    }

    public static class NONNEGATIVE_NUMBER <T extends Number> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            return newValue.doubleValue() >= 0 ? newValue : null;
        }
        @Override
        public String description() { return "Must be a positive number";}
    }

    public static class PROBABILITY <T extends Number> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            return (newValue.doubleValue() >= 0 && newValue.doubleValue() <= 1 )? newValue : null;
        }
        @Override
        public String description() { return "Must be between 0 and 1";}
    }
}
