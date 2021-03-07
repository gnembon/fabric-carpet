package carpet.script.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>Determines that this parameter allows passing a value directly instead of a list
 * of those values.</p>
 * 
 * <p>Can be used both in lists and in generic types of lists (eg {@code List<@AllowSingleton List<Entity>>}</p>
 *
 */
@Documented
@Retention(RUNTIME)
@Target({PARAMETER, TYPE_USE})
public @interface AllowSingleton {

}
