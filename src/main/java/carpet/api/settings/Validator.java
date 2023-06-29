package carpet.api.settings;

import org.jetbrains.annotations.Nullable;

import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;

/**
 * <p>A {@link Validator} is a class that is able to validate the values given to a {@link CarpetRule}, cancelling rule
 * modification if the value is not valid or even changing the value to a different one if needed.</p>
 * 
 * <p>Validators are used in the default implementation, {@link ParsedRule}, as the way of validating most input (other than validating
 * it can actually be introduced in the rule), but can (and may) also be used in other {@link CarpetRule} implementations, though those are not
 * required to.</p>
 * 
 * @see #validate(CommandSourceStack, CarpetRule, Object, String)
 * 
 * @param <T> The type of the rule's value
 */
public abstract class Validator<T>
{
    /**
     * <p>Validates whether the value passed to the given {@link CarpetRule} is valid as a new value for it.</p>
     * 
     * <p>Validators can also change the value that the rule is going to be set to by returning a value different to the
     * one that has been passed to them</p>
     * 
     * <p>This method must not throw any exceptions.</p>
     * 
     * @param source The {@link CommandSourceStack} that originated this change, and should be further notified
     *        about it. May be {@code null} during rule synchronization.
     * @param changingRule The {@link CarpetRule} that is being changed
     * @param newValue The new value that is being set to the rule
     * @param userInput The value that is being given to this rule by the user as a {@link String}, or a best-effort representation
     *                  of the value as a {@link String}. This value may not correspond to the result of {@link RuleHelper#toRuleString(Object)}
     *                  if other validators have modified the value, it's just a representation of the user's input.
     * @return The new value to set the rule to instead, can return the {@code newValue} if the given value is correct.
     *         Returns {@code null} if the given value is not correct.
     */
    public abstract T validate(@Nullable CommandSourceStack source, CarpetRule<T> changingRule, T newValue, String userInput);

    /**
     * @return A description of this {@link Validator}. It is used in the default {@link #notifyFailure(CommandSourceStack, CarpetRule, String)}
     *         implementation and to add extra information in {@link SettingsManager#printAllRulesToLog(String)}
     */
    public String description() {return null;}
    /**
     * <p>Called after failing validation of the {@link CarpetRule} in order to notify the causing {@link CommandSourceStack} about the
     * failure.</p>
     * 
     * @param source The {@link CommandSourceStack} that originated this change. It will never be {@code null}
     * @param currentRule The {@link CarpetRule} that failed verification
     * @param providedValue The {@link String} that was provided to the changing rule
     */
    public void notifyFailure(CommandSourceStack source, CarpetRule<T> currentRule, String providedValue)
    {
        Messenger.m(source, "r Wrong value for " + currentRule.name() + ": " + providedValue);
        if (description() != null)
            Messenger.m(source, "r " + description());
    }
}
