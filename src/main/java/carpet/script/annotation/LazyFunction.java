package carpet.script.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyFunction {
	/**
	 * <p>If the function can accept a variable number of parameters,
	 * this must define the maximum number of params this function can take.
	 * 
	 * <p>The parser will throw in case a function is defined with varargs
	 * but no maxParams value has been specified in this annotation.
	 * <br>The value, however, will be ignored if the function has fixed args
	 * 
	 * <p>Use -1 for unlimited number of params.
	 * @return The maximum of parameters this function can accept
	 */
    int maxParams() default -2;
}
