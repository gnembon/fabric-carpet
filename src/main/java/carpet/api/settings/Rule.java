package carpet.api.settings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any field in this class annotated with this class is interpreted as a carpet rule.
 * The field must be static and have a type of one of:
 * - boolean
 * - int
 * - double
 * - String
 * - long
 * - float
 * - a subclass of Enum
 * The default value of the rule will be the initial value of the field.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Rule
{
    /**
     * <p>An array of categories the rule is in, as {@link String strings}.</p>
     * 
     * <p>Those must have a corresponding translation key. Categories provided in Carpet's {@link RuleCategory} already
     * have translation key.</p>
     */
    String[] categories();

    /**
     * Options to select in menu.
     * Inferred for booleans and enums. Otherwise, must be present.
     */
    String[] options() default {};

    /**
     * if a rule is not strict - can take any value, otherwise it needs to match
     * any of the options
     * For enums, its always strict, same for booleans - no need to set that for them.
     */
    boolean strict() default true;
    
    /**
     * If specified, the rule will automatically enable or disable 
     * a builtin Scarpet Rule App with this name.
     */
    String appSource() default "";

    /**
     * The class of the validator checked right before the rule is changed, using the returned value as the new value to set, or cancel the change if null is returned.
     */
    @SuppressWarnings("rawtypes")
    Class<? extends Validator>[] validators() default {};

    /**
     * <p>A class or list of classes implementing {@link Condition} that will have their {@link Condition#shouldRegister()} method
     * executed while the rule is being parsed in {@link SettingsManager#parseSettingsClass(Class)}, that will cause the rule to be skipped
     * if it evaluates to false</p>
     * 
     * @see Condition
     */
    Class<? extends Condition>[] conditions() default {};
    
    /**
     * <p>Represents a condition that must be met for a rule to be registered in a {@link SettingsManager} via
     * {@link SettingsManager#parseSettingsClass(Class)}</p>
     * 
     * @see Rule#conditions()
     * @see #shouldRegister()
     */
    interface Condition {
        /**
         * <p>Returns whether the rule that has had this {@link Condition} added should register.</p>
         * @return {@code true} to register the rule, {@code false} otherwise
         */
        boolean shouldRegister();
    }
}
