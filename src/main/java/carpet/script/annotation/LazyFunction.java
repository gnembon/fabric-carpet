package carpet.script.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import carpet.script.LazyValue;
import carpet.script.value.Value;

/**
 * <p>Defines a method that can be used as a lazy function in order to be processed to be used
 * in the Scarpet language.</p>
 * 
 * <p>Methods annotated with this annotation are not required to accept and return the typical
 * {@code Context context, Integer t, List<LazyValue> lv}, but instead can specify whatever parameters
 * they actually need that will be automatically converted from their respective {@link LazyValue}s and
 * passed to the method as the expected type. Functions will automatically fail if given parameters are
 * not compatible with the specified ones, or if the number of provided arguments is either too large or too small.</p>
 * 
 * <p>Types to be used in those functions are already registered in their respective {@link ValueConverter} implementations.<br>
 * In order to register a new type to convert to, you can do so in {@link SimpleTypeConverter#registerType(Class, Class, java.util.function.Function)},
 * and in order to register a new variant of {@link Value}, use {@link ValueCaster#register(Class, String)}.<br>
 * In order to convert the output of your method to a {@link LazyValue} you will also need to register its conversion in {@link OutputConverter}</p>
 * 
 * <p>In order for Carpet to find methods annotated with this annotation, you must add your function class(es) to Carpet
 * by running {@link AnnotationParser#parseFunctionClass(Class)}. The provided {@link Class} must be concrete and provide
 * the default constructor, and implement the {@link FunctionClass} interface.</p>
 * //TODO Decide whether to actually require implementing FunctionClass
 * <p>Methods annotated with this annotation must not be static and must not be declared to throw any checked exceptions.</p>
 * 
 * <p>If the first parameter of a method annotated with this annotation is {@link carpet.script.Context} or a subclass of it,
 * Carpet will pass the Context of the expression to the method. In order to get <em>The lazy t</em>, annotate an {@link Integer}
 * with {@link TheLazyT}. See that class for more details.</p>
 * 
 * @see AllowSingleton
 * @see Locator.Block
 * @see Locator.Vec3d
 * @see TheLazyT
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface LazyFunction {
	/**
	 * <p>If the function can accept a variable number of parameters (varargs),
	 * this must define the maximum number of params this function can take.</p>
	 * 
	 * <p>The parser will throw in case a function is defined with varargs
	 * but no maxParams value has been specified in this annotation.
	 * <br>The value, however, will be ignored if the function has fixed args</p>
	 * 
	 * <p>Use -1 for unlimited number of params.</p>
	 * @return The maximum of parameters this function can accept
	 */
    int maxParams() default -2;
}
