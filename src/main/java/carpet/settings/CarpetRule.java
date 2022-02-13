package carpet.settings;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;

import carpet.network.ServerNetworkHandler;
import net.minecraft.commands.CommandSourceStack;

/**
 * <p>A Carpet rule, that can return its required properties and stores a value.</p>
 * 
 * @param <T> The value's type
 */
public interface CarpetRule<T> extends Comparable<CarpetRule<?>> {
	/**
	 * <p>Returns this rule's name</p>
	 */
	String name();
	
	/**
	 * <p>Returns this rule's description</p>
	 */
	String description();
	
	/**
	 * <p>Returns an immutable {@link List} of {@link String strings} with extra information about this rule, that is,
	 * the lines after the rule's description.</p>
	 */
	List<String> extraInfo();
	
	/**
	 * Returns an immutable {@link Collection} of categories this rule is on.
	 */
	Collection<String> categories();
	
	/**
	 * <p>Returns an immutable {@link Collection} of suggestions for values that this rule will be able
	 * to accept as Strings. The rule must be able to accept all of the suggestions in the returned {@link Collection} as a value,
	 * though it may have requirements for those to be applicable.</p>
	 */
	Collection<String> suggestions();
	
	/**
	 * <p>Returns the {@link SettingsManager} this rule is in.</p>
	 * 
	 * <p>This method may be removed or changed in the future, and is not part of the contract, but it's needed to sync
	 * the rule with clients.</p>
	 */
	SettingsManager settingsManager();
	
	/**
	 * <p>Returns this rule's value</p>
	 */
	T value();
	
	/**
	 * <p>Returns whether this rule can be toggled in the client-side when not connected to a
	 * Carpet server.</p>
	 * 
	 * <p>In the default implementation, this is the case when {@link #categories()} contains {@link RuleCategory#CLIENT} </p>
	 */
	boolean canBeToggledClientSide();
	
	/**
	 * <p>Returns the type of this rule's value.</p>
	 * 
	 * <p>If this rule's type is primitive, it returns a wrapped version of it (such as the result of running
	 * {@link ClassUtils#primitiveToWrapper(Class)} on it) </p>
	 */
	Class<T> type();
	
	/**
	 * <p>Returns this rule's default value.</p>
	 * 
	 * <p>This value will never be {@code null}, and will always be a valid value for {@link #set(CommandSourceStack, Object)}.</p>
	 */
	T defaultValue();
	
	/**
	 * <p>Sets this rule's value to the provided {@link String}, after first converting the {@link String} into a suitable type.</p>
	 * 
	 * <p>This methods run any required validation on the value first, and throws {@link InvalidRuleValueException} if the value is not suitable
	 * for this rule, regardless of whether it was impossible to convert the value to the required type, the rule doesn't accept the
	 * value, or the rule is immutable.</p>
	 * 
	 * <p>Implementations of this method must notify their {@link SettingsManager} by calling
	 * {@link SettingsManager#notifyRuleChanged(CommandSourceStack, CarpetRule)} , and are responsible for
	 * notifying the {@link ServerNetworkHandler} (if the rule isn't restricted from being synchronized with clients)
	 * and the {@link CarpetEventServer.Event#CARPET_RULE_CHANGES#onCarpetRuleChanges(CarpetRule, CommandSourceStack)} Scarpet event
	 * in case the value of the rule was changed because of the invocation.</p>
	 * 
	 * @param source The {@link CommandSourceStack} to notify about the result of this rule change or {@code null} in order to not notify
	 * @param value The new value for this rule as a {@link String}
	 * @throws InvalidRuleValueException if the value passed to the method was not valid as a value to this rule, either because of incompatible type,
	 *                                   because the rule can't accept that value or because there was some requirement missing for that value to be allowed
	 */
	void set(CommandSourceStack source, String value) throws InvalidRuleValueException;
	
	/**
	 * <p>This method follows the same contract as {@link #set(CommandSourceStack, String)}, but accepts a value already parsed (though not verified).</p>
	 * @see #set(CommandSourceStack, String)
	 */
	void set(CommandSourceStack source, T value) throws InvalidRuleValueException; //TODO doesn't work with T = String smh
	
	/**
	 * <p>Compares this {@link CarpetRule} against another one, without taking the current value into account.</p>
	 * 
	 * <p>In the default {@link SettingsManager} implementation, this is used to sort the rules before displaying them.</p>
	 * 
	 * <p>The default comparison method depends only on the rule's name.</p>
	 * 
	 * <h1>int compareTo(CarpetRule<?> o)</h1>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	default int compareTo(CarpetRule<?> o) {
		return this.name().compareTo(o.name());
	}
	
	@Override
	boolean equals(Object o);
	
	@Override
	int hashCode();
}
