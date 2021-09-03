package carpet.compat;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.spongepowered.asm.mixin.Mixin;

import carpet.settings.SettingsManager;


/**
 * <p>Marks that this {@link Mixin} is completely related to a specific feature, and is not required for the rest of Carpet to work.</p> 
 *
 * <p>This allows errors regarding this {@link Mixin} to be converted into a warn in exchange of that feature not being available in the game.</p>
 * 
 * <p>If a Mixin is just not necessary even for that feature, consider marking it as {@code require = 0} instead, as this will disable the feature</p>
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface FeatureMixin {
	
	/**
	 * <p>The name of the rule this {@link Mixin} is for. It will be used to disable it if the Mixin fails.</p>
	 */
	public String value();
	
	/**
	 * <p>Defines the {@link SettingsManager} this Mixin's rules are related available into.</p>
	 * 
	 * <p>Defaults to Carpet's, but can be changed for extensions to use this system.</p>
	 * 
	 * <p><b>THIS FUNCTIONALITY IS NOT IMPLEMENTED YET</b></p>
	 */
	String settingsManager() default "carpet";
}
