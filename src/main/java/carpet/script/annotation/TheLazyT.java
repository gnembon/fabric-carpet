package carpet.script.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates this integer is The Lazy `t`, whatever that actually is.
 * 
 * <p>This has no parameters, since The Lazy `t` is The Lazy `t`, without
 * further discussion
 *
 * <p><code>expression.addLazyFunction("name", -1, (context, t <-- HERE, lv)</code>
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface TheLazyT {

}
