package carpet.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import carpet.utils.Translations;
import net.minecraft.server.command.ServerCommandSource;

/**
 * <p>A helper class for operating with {@link CarpetRule} instances and values.</p>
 * 
 * <p>If a method is visible and has javadocs it's probably API</p>
 */
public final class RuleHelper {
	private RuleHelper() {}
	
	/**
	 * <p>Gets a {@code boolean} value for a given {@link CarpetRule}.</p>
	 * 
	 * <p>The current implementation is as follows:</p>
	 * <ul>
	 *   <li>If the rule's type is a {@code boolean}, it will return the value directly.</li>
	 *   <li>If the rule's type is a {@link Number}, it will return {@code true} if the value is greater than zero.</li>
	 *   <li>In any other case, it will return {@code false}.</li>
	 * </ul>
	 * @param rule The rule to get the {@code boolean} value from
	 * @return A {@code boolean} representation of this rule's value
	 */
	public static boolean getBooleanValue(CarpetRule<?> rule) {
		if (rule.type() == Boolean.class) return (boolean) rule.value();
        if (Number.class.isAssignableFrom(rule.type())) return ((Number) rule.value()).doubleValue() > 0;
        return false;
	}
	
	/**
	 * <p>Converts a rule's value into its similar {@link String} representation.</p>
	 * 
	 * <p>If the value is an {@link Enum}, this method returns the name of the enum constant lowercased, else
	 * it returns the result of running {@link #toString()} on the value.</p>
	 * @param value A rule's value
	 * @return A {@link String} representation of the given value
	 */
	public static String toRuleString(Object value) {
		if (value instanceof Enum) return ((Enum<?>) value).name().toLowerCase(Locale.ROOT);
        return value.toString();
	}
	
	/**
	 * <p>Checks whether the given {@code CarpetRule rule} is in its default value</p>
	 * 
	 * @param rule The rule to check
	 * @return {@code true} if the rule's default value equals its current value
	 */
	public static boolean isInDefaultValue(CarpetRule<?> rule) {
		return rule.defaultValue().equals(rule.value());
	}
	
	/**
	 * <p>Resets the given {@link CarpetRule rule} to its default value, and notifies the given source, if provided.</p>
	 * @param rule The {@link CarpetRule} to reset to its default value
	 * @param source A {@link ServerCommandSource} to notify about this change, or {@code null}
	 * 
	 * @param <T> The type of the {@link CarpetRule}
	 */
	public static <T> void resetToDefault(CarpetRule<T> rule, ServerCommandSource source) {
		try {
			rule.set(source, rule.defaultValue());
		} catch (InvalidRuleValueException e) {
			throw new IllegalStateException("Rule couldn't be set to default value!", e);
		}
	}
	
	// Translations
	// TODO decide if worth keeping public
	
	/**
	 * @param rule The {@link CarpetRule} to get the translated name of
     * @return A {@link String} being the translated {@link CarpetRule#name()} of this rule,
     *         in Carpet's configured language.
     */
    public static String translatedName(CarpetRule<?> rule) {
        String key = String.format("rule.%s.name", rule.name());
        return Translations.hasTranslation(key) ? Translations.tr(key) + String.format(" (%s)", rule.name()): rule.name();
    }
    
    /**
     * @param rule The {@link CarpetRule} to get the translated description of
     * @return A {@link String} being the translated {@link CarpetRule#description() description} of this rule,
     *         in Carpet's configured language.
     */
    public static String translatedDescription(CarpetRule<?> rule)
    {
        return Translations.tr(String.format("rule.%s.desc", rule.name()), rule.description());
    }
    
    /**
     * @param rule The {@link CarpetRule} to get the translated extraInfo of
     * @return An {@link List} of {@link String} being the translated {@link CarpetRule#extraInfo() extraInfo} of this 
     * 	       {@link CarpetRule}, in Carpet's configured language.
     */
    public static List<String> translatedExtras(CarpetRule<?> rule)
    {
        if (!Translations.hasTranslations()) return rule.extraInfo();
        String keyBase = String.format("rule.%s.extra.", rule.name());
        List<String> extras = new ArrayList<>();
        int i = 0;
        while (Translations.hasTranslation(keyBase + i))
        {
            extras.add(Translations.tr(keyBase+i));
            i++;
        }
        return (extras.isEmpty()) ? rule.extraInfo() : extras;
    }
    
}
