package carpet.script.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates this integer is The Lazy `t`, whatever that actually is.
 * 
 * <p>This has no parameters, since The Lazy `t` is The Lazy `t`, without
 * further discussion
 *
 * <p>{@code expression.addLazyFunction("name", -1, (context, t <-- HERE, lv)}
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface TheLazyT {

}
