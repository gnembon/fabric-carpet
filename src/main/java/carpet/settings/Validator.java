package carpet.settings;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.BaseComponent;

public abstract class Validator<T>
{
    /**
     * Validate the new value of a rule
     * @return true if valid, false if new rule invalid.
     */
    public abstract T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string);

    /**
     * Overwrite and use {@link Validator#descriptionText} instead
     */
    @Deprecated
    public String description() {return null;}

    public BaseComponent descriptionText()
    {
        String desc = this.description();
        return desc == null ? null : Messenger.s(desc);
    }

    public void notifyFailure(CommandSourceStack source, ParsedRule<T> currentRule, String providedValue)
    {
        Messenger.m(source, "r", Messenger.tr("carpet.validator.base.notify_failure", currentRule.name, providedValue));
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
        public BaseComponent descriptionText() { return Messenger.tr("carpet.validator._command.desc");}
    }

    public static class _CLIENT<T> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            return newValue;
        }
        @Override
        public BaseComponent descriptionText() { return Messenger.tr("carpet.validator._client.desc");}
    }

    public static class _COMMAND_LEVEL_VALIDATOR extends Validator<String> {
        private static List<String> OPTIONS = List.of("true", "false", "ops", "0", "1", "2", "3", "4");
        @Override public String validate(CommandSourceStack source, ParsedRule<String> currentRule, String newValue, String userString) {
            if (!OPTIONS.contains(userString.toLowerCase(Locale.ROOT)))
            {
                Messenger.m(source, "r", Messenger.tr("carpet.validator._command_level.message.0"));
                Messenger.m(source, "r", Messenger.tr("carpet.validator._command_level.message.1"));
                Messenger.m(source, "r", Messenger.tr("carpet.validator._command_level.message.2"));
                return null;
            }
            return userString.toLowerCase(Locale.ROOT);
        }
        @Override
        public BaseComponent descriptionText() { return Messenger.tr("carpet.validator._command_level.desc");}
    }
    
    public static class _SCARPET<T> extends Validator<T> {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            return newValue;
        }
        @Override
        public BaseComponent descriptionText() { return Messenger.tr("carpet.validator._scarpet.desc");}
    }

    public static class WIP<T> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            Messenger.m(source, "r", Messenger.tr("carpet.validator.wip.message", currentRule.name));
            return newValue;
        }
        @Override
        public BaseComponent descriptionText() { return Messenger.tr("carpet.validator.wip.desc");}
    }
    public static class _STRICT<T> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            if (!currentRule.options.contains(string))
            {
                Messenger.m(source, "r", Messenger.tr("carpet.validator._strict.message", currentRule.options));
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
                Messenger.m(source, "r", Messenger.tr("carpet.validator._strict_ignorecase.message", currentRule.options));
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
        public BaseComponent descriptionText() { return Messenger.tr("carpet.validator.nonnegative_number.desc");}
    }

    public static class PROBABILITY <T extends Number> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string)
        {
            return (newValue.doubleValue() >= 0 && newValue.doubleValue() <= 1 )? newValue : null;
        }
        @Override
        public BaseComponent descriptionText() { return Messenger.tr("carpet.validator.probability.desc");}
    }
}
