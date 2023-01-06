package carpet.settings;

import carpet.CarpetSettings;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validators;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;

/**
 * @deprecated Use {@link carpet.api.settings.Validator} instead
 */
@Deprecated(forRemoval = true)
public abstract class Validator<T> extends carpet.api.settings.Validator<T>
{
	{
		// Print deprecation warning once while instantiating the class
	    CarpetSettings.LOG.warn("""
                Validator '%s' is implementing the old Validator class! This class is deprecated and will be removed \
                and crash in later Carpet versions!""".formatted(getClass().getName()));
	}
    /**
     * Validate the new value of a rule
     * @return a value if the given one was valid or could be cleanly adapted, null if new value is invalid.
     */
    @Override
    public final T validate(CommandSourceStack source, CarpetRule<T> changingRule, T newValue, String stringInput) {
        // Compatibility code
        if (!(changingRule instanceof ParsedRule<T> parsedRule))
            // Throwing here is not an issue because Carpet's current implementation only calls validators with ParsedRule.
            // This would be thrown if a different implementation tries to use it, and then it's their issue in multiple ways
            throw new IllegalArgumentException("Passed a non-ParsedRule to a validator using the outdated method!");
        return validate(source, parsedRule, newValue, stringInput);
    }
    /**
     * @deprecated Implement {@link #validate(CommandSourceStack, CarpetRule, Object, String)} instead! It will get abstract soon!
     */
    @Deprecated(forRemoval = true)
    public abstract T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string);

    /**
     * @deprecated Use {@link Validators.CommandLevel} instead
     */
    @Deprecated(forRemoval = true) // to remove
    public static class _COMMAND_LEVEL_VALIDATOR extends Validators.CommandLevel {}

    /**
     * @deprecated Use {@link Validators.NonNegativeNumber} instead
     */
    @Deprecated(forRemoval = true) // to remove
    public static class NONNEGATIVE_NUMBER<T extends Number> extends Validators.NonNegativeNumber<T> {}

    /**
     * @deprecated Use {@link Validators.Probablity} instead
     */
    @Deprecated(forRemoval = true) // to remove
    public static class PROBABILITY <T extends Number> extends Validators.Probablity<T> {}

    // The ones below are part of the implementation of ParsedRule or printRulesToLog, so they need to be close to it to stay hidden
    // They will need to be moved when moving ParsedRule
    
    static class _COMMAND<T> extends carpet.api.settings.Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, CarpetRule<T> currentRule, T newValue, String string)
        {
            if (source != null)
                CommandHelper.notifyPlayersCommandsChanged(source.getServer());
            return newValue;
        }
        @Override
        public String description() { return "It has an accompanying command";}
    }

    // maybe remove this one and make printRulesToLog check for canBeToggledClientSide instead
    static class _CLIENT<T> extends carpet.api.settings.Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, CarpetRule<T> currentRule, T newValue, String string)
        {
            return newValue;
        }
        @Override
        public String description() { return "Its a client command so can be issued and potentially be effective when connecting to non-carpet/vanilla servers. " +
                "In these situations (on vanilla servers) it will only affect the executing player, so each player needs to type it" +
                " separately for the desired effect";}
    }
    
    static class ScarpetValidator<T> extends carpet.api.settings.Validator<T> { //TODO remove? The additional info isn't that useful tbh
        @Override
        public T validate(CommandSourceStack source, CarpetRule<T> currentRule, T newValue, String string)
        {
            return newValue;
        }
        @Override public String description() {
            return "It controls an accompanying Scarpet App";
        }
    }

    static class StrictValidator<T> extends carpet.api.settings.Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, CarpetRule<T> currentRule, T newValue, String string)
        {
            if (!currentRule.suggestions().contains(string))
            {
                Messenger.m(source, "r Valid options: " + currentRule.suggestions().toString());
                return null;
            }
            return newValue;
        }
    }
}
