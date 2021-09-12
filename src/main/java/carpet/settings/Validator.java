package carpet.settings;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.utils.Messenger;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

/**
 * <p>A {@link Validator} is a class that is able to validate the values given to a {@link CarpetRule}, cancelling rule
 * modification if the value is not valid or even changing the value to a different one if needed.</p>
 * 
 * <p>Validators are used in the default implementation, {@link ParsedRule}, as the way of validating most input (other than validating
 * it can actually be introduced in the rule), but can (and may) also be used in other {@link CarpetRule} implementations, though those are not
 * required to.</p>
 * 
 * @see #validate(ServerCommandSource, CarpetRule, Object, String)
 * 
 * @param <T> The type of the rule's value
 */
public abstract class Validator<T>
{
    /**
     * <p>Validates whether the value passed to the given {@link CarpetRule} is valid as a new value for it.</p>
     * 
     * <p>Validators can also change the value that the rule is going to be set to </p>
     * @param source The {@link ServerCommandSource} that originated this change, and should be further notified
     *        about it. May be {@code null} during rule synchronization.
     * @param changingRule The {@link CarpetRule} that is being changed
     * @param newValue The new value that is being set to the rule
     * @param stringInput The value that is being given to the rule as a {@link String}
     * @return The new value to set the rule to instead, can return the {@code newValue} if the given value is correct.
     *         Returns {@code null} if the given value is not correct.
     */
    public T validate(ServerCommandSource source, CarpetRule<T> changingRule, T newValue, String stringInput) {
    	// Temporary compatibility code
        CarpetSettings.LOG.warn("Validator "+ getClass() +" implements the old validation method! "
                + "Tell the extension author(s) to use the new one using CarpetRule instead, the one they're using will be removed (and crash) soon!");
        CarpetSettings.LOG.warn("Normally it's as simple as changing the type 'ParsedRule' to 'CarpetRule' in the validate() method!");
        if (!(changingRule instanceof ParsedRule<T> parsedRule))
            throw new IllegalArgumentException("Passed a non-ParsedRule to a validator using the outdated method!");
        return validate(source, parsedRule, newValue, stringInput);
    }
    /**
     * @return A description of this {@link Validator}. It is used in the default {@link #notifyFailure(ServerCommandSource, CarpetRule, String)}
     *         implementation and to add extra information in {@link SettingsManager#printAllRulesToLog(String)}
     */
    public String description() {return null;}
    /**
     * <p>Called after failing validation of the {@link CarpetRule} in order to notify the causing {@link ServerCommandSource} about the
     * failure.</p>
     * 
     * @param source The {@link ServerCommandSource} that originated this change. It will never be {@code null}
     * @param currentRule The {@link CarpetRule} that failed verification
     * @param providedValue The {@link String} that was provided to the changing rule
     */
    public void notifyFailure(ServerCommandSource source, CarpetRule<T> currentRule, String providedValue)
    {
        Messenger.m(source, "r Wrong value for " + currentRule.name() + ": " + providedValue);
        if (description() != null)
            Messenger.m(source, "r " + description());
    }
    /**
     * @deprecated Implement {@link #validate(ServerCommandSource, CarpetRule, Object, String)} instead! It will get abstract soon!
     */
    @Deprecated(forRemoval = true)
    public T validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string) {
        throw new IllegalStateException("Called Validator that doesn't implement either the old nor the new validate method");
    }

    public static class _COMMAND<T> extends Validator<T>
    {
        @Override
        public T validate(ServerCommandSource source, CarpetRule<T> currentRule, T newValue, String string)
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
        public T validate(ServerCommandSource source, CarpetRule<T> currentRule, T newValue, String string)
        {
            return newValue;
        }
        @Override
        public String description() { return "Its a client command so can be issued and potentially be effective when connecting to non-carpet/vanilla servers. " +
                "In these situations (on vanilla servers) it will only affect the executing player, so each player needs to type it" +
                " separately for the desired effect";}
    }

    public static class _COMMAND_LEVEL_VALIDATOR extends Validator<String> {
        public static final List<String> OPTIONS = List.of("true", "false", "ops", "0", "1", "2", "3", "4");
        @Override
        public String validate(ServerCommandSource source, CarpetRule<String> currentRule, String newValue, String userString) {
            if (!OPTIONS.contains(newValue))
            {
                Messenger.m(source, "r Valid options for command type rules is 'true' or 'false'");
                Messenger.m(source, "r Optionally you can choose 'ops' to only allow operators");
                Messenger.m(source, "r or provide a custom required permission level");
                return null;
            }
            return newValue;
        }
        @Override public String description() { return "Can be limited to 'ops' only, or a custom permission level";}
    }
    
    public static class _SCARPET<T> extends Validator<T> { //TODO remove?
        @Override
        public T validate(ServerCommandSource source, CarpetRule<T> currentRule, T newValue, String string)
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
        public T validate(ServerCommandSource source, CarpetRule<T> currentRule, T newValue, String string)
        {
            Messenger.m(source, "r "+currentRule.name()+" is missing a few bits - we are still working on it.");
            return newValue;
        }
        @Override
        public String description() { return "A few bits still needs implementing - we are working on it";}
    }
    public static class _STRICT<T> extends Validator<T>
    {
        @Override
        public T validate(ServerCommandSource source, CarpetRule<T> currentRule, T newValue, String string)
        {
            if (!currentRule.suggestions().contains(string))
            {
                Messenger.m(source, "r Valid options: " + currentRule.suggestions().toString());
            }
            return newValue;
        }
    }

    public static class NONNEGATIVE_NUMBER <T extends Number> extends Validator<T>
    {
        @Override
        public T validate(ServerCommandSource source, CarpetRule<T> currentRule, T newValue, String string)
        {
            return newValue.doubleValue() >= 0 ? newValue : null;
        }
        @Override
        public String description() { return "Must be a positive number";}
    }

    public static class PROBABILITY <T extends Number> extends Validator<T>
    {
        @Override
        public T validate(ServerCommandSource source, CarpetRule<T> currentRule, T newValue, String string)
        {
            return (newValue.doubleValue() >= 0 && newValue.doubleValue() <= 1 )? newValue : null;
        }
        @Override
        public String description() { return "Must be between 0 and 1";}
    }
}
