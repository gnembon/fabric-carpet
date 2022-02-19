package carpet.api.settings;

import java.util.List;

import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;

/**
 * <p>A collection of standard {@link Validator validators} you can use in your rules.</p>
 * 
 * @see Rule
 * @see Rule#validators()
 *
 */
public final class StandardValidators {
    private StandardValidators() {};
    
    /**
     * <p>A {@link Validator} that checks whether the given {@link String} value was a valid command level as Carpet allows them,
     * so either a number from 0 to 4, or one of the keywords {@code true}, {@code false} or {@code ops} </p>
     * 
     * <p>While there is no public API method for checking whether a source can execute a command,
     * {@link CommandHelper#canUseCommand(CommandSourceStack, Object)} is not expected to change anytime soon.</p>
     *
     */
    public static class CommandLevelValidator extends Validator<String> {
        @Deprecated(forRemoval = true) // internal use only, will be made pckg private when phasing out old api
        public static final List<String> OPTIONS = List.of("true", "false", "ops", "0", "1", "2", "3", "4");
        @Override
        public String validate(CommandSourceStack source, CarpetRule<String> currentRule, String newValue, String userString) {
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
    
    /**
     * <p>A {@link Validator} that checks whether the entered number is equal or greater than {@code 0}.</p>
     */
    public static class NonNegativeNumberValidator<T extends Number> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, CarpetRule<T> currentRule, T newValue, String string)
        {
            return newValue.doubleValue() >= 0 ? newValue : null;
        }
        @Override
        public String description() { return "Must be a positive number or 0";}
    }
    
    /**
     * <p>A {@link Validator} that checks whether the entered number is between 0 and 1, inclusive.</p>
     */
    public static class ProbablityValidator<T extends Number> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, CarpetRule<T> currentRule, T newValue, String string)
        {
            return (newValue.doubleValue() >= 0 && newValue.doubleValue() <= 1 )? newValue : null;
        }
        @Override
        public String description() { return "Must be between 0 and 1";}
    }
}
