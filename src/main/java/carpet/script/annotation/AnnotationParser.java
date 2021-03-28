package carpet.script.annotation;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Triple;

import carpet.CarpetSettings;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff.TriFunction;
import carpet.script.exception.InternalExpressionException;
import carpet.script.LazyValue;
import carpet.script.value.Value;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * <p>This class parses methods annotated with the {@link LazyFunction} annotation in a given {@link Class}, generating
 * fully-featured, automatically parsed and converted functions to be used in the Scarpet language.</p>
 * 
 * <p>This class and the rest in this package will try to ensure that the annotated method receives the proper parameters
 * directly, without all the always-repeated code of evaluating {@link Value}s, checking and casting them to their respective
 * types, and converting them to the final needed object.</p>
 * 
 * <p>To do that, functions will save a list of {@link ValueConverter}s to convert all their parameters. {@link ValueConverter}s
 * are able to convert from any compatible {@link Value} (or {@link LazyValue}) instance into the requested parameter type,
 * as long as they are registered using their respective {@code register} functions.</p>
 * 
 * <p>Built-in {@link ValueConverter}s include converters to convert {@link List}s to actual Java lists while also converting every item
 * inside of the {@link List} to the specified generic parameter ({@code <>}), with the same applying for maps</p>
 * 
 * <p>Parameters can be given the annotations (present in {@link Locator} and {@link Param} interfaces) in order to restrict them or 
 * make them more permissive to accept types, such as {@link Param.AllowSingleton} for lists.</p>
 * 
 * <p>You can also declare optional parameters by using Java's {@link Optional} as the type of one of your parameters, though it must be
 * at the end of the function, just before varargs if present and/or any other {@link Optional} parameters.</p>
 * 
 * <p>Output of the annotated methods will also be converted to a compatible {@link LazyValue} using the registered {@link OutputConverter}s,
 * allowing to remove the need of explicitly converting to a {@link Value} and then to a {@link LazyValue} just to end the method.</p>
 * 
 * <p>For a variable argument count, the Java varargs notation can be used in the last parameter, converting the function into a variable argument
 * function that will pass all the rest of parameters to that last varargs parameter, also converted into the specified type.</p>
 * 
 * <p>To begin, use the {@link #parseFunctionClass(Class)} method in a class with methods annotated with the {@link LazyFunction} annotation.</p>
 *
 * @see LazyFunction
 * @see Locator.Block
 * @see Locator.Vec3d
 * @see Param.Strict
 * @see Param.AllowSingleton
 * @see Param.KeyValuePairs
 * @see Optional
 * @see OutputConverter#register(Class, java.util.function.Function)
 * @see ValueCaster#register(Class, String)
 * @see SimpleTypeConverter#registerType(Class, Class, java.util.function.Function)
 * @see Param.Params#registerStrictConverter(Class, boolean, ValueConverter)
 */
public class AnnotationParser {
	private static final List<Triple<String, Integer, TriFunction<Context, Integer, List<LazyValue>, LazyValue>>> functionList = new ArrayList<>();
	
	/**
	 * <p>Parses a given {@link Class} and registers its annotated methods, the ones with the {@link LazyFunction} annotation, to be used
	 * in the Scarpet language.</p>
	 * 
	 * <p>There is a set of requirements for the class in order to get the best experience (and to work, at all):</p>
	 * <ul>
	 * <li>Class must be concrete. That is, no interfaces or abstract classes</li>
	 * <li>Class must have the default constructor (or an equivalent) available. That is done in order to not need the {@code static} modifier
	 * in every method, making them faster to code and simpler to look at.</li>
	 * <li>Annotated methods must not be {@code static}. See claim above</li>
	 * <li>Annotated methods must not throw checked exceptions. They can throw regular {@link RuntimeException}s (including but not limited to
	 * {@link InternalExpressionException}). Basically, it's fine as long as you don't add a {@code throws} declaration to it.</li>
	 * <li>Varargs (or effectively varargs) annotated methods must explicitly declare a maximum number of parameters to ingest in the {@link LazyFunction}
	 * annotation. They can still declare an unlimited amount by setting that maximum to {@code -1}. "Effectively varargs" means a function that has
	 * a parameter using/requiring a {@link ValueConverter} that has declared {@link ValueConverter#consumesVariableArgs()}.</li>
	 * <li>Annotated methods must not have a parameter with generics as the varargs parameter. This is just because it was painful for me (altrisi) and
	 * didn't want to support it. Those will crash with a {@code ClassCastException}</li>
	 * </ul>
	 * 
	 * @see LazyFunction
	 * @param <T> The generic type of the class to parse.
	 * @param clazz The class to parse
	 */
	public static <T> void parseFunctionClass(Class<T> clazz) {
		if (Modifier.isAbstract(clazz.getModifiers())) {
			throw new IllegalArgumentException("Function class must be concrete! Class: " + clazz.getSimpleName());
		}
		T instance;
		try {
			instance = clazz.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException("Couldn't create instance of given " + clazz + ". Make sure default constructor is available", e);
		}
		
		Method[] methodz = clazz.getDeclaredMethods();
		for (Method method : methodz) {
			if (!method.isAnnotationPresent(LazyFunction.class)) continue;
			//Checks
			if (Modifier.isStatic(method.getModifiers())) {
				throw new IllegalArgumentException("Annotated method '"+ method.getName() +"', provided in '" + clazz + "' must not be static");
			}
			if (method.getExceptionTypes().length != 0) {
				throw new IllegalArgumentException("Annotated method '"+ method.getName() +"', provided in '"+ clazz +"' must not declare checked exceptions");
			}
			
			String functionName = method.getName();
			List<ValueConverter<?>> valueConverters = new ObjectArrayList<>();
			Parameter param;
			for (int i = 0; i < method.getParameterCount(); i++) {
				param = method.getParameters()[i];
				if (!method.isVarArgs() || i != method.getParameterCount() -1 ) // Varargs converter is separate, inside #makeFunction
					valueConverters.add(ValueConverter.fromAnnotatedType(param.getAnnotatedType()));
			}
			boolean isEffectivelyVarArgs = method.isVarArgs() ? true : valueConverters.stream().anyMatch(ValueConverter::consumesVariableArgs);
			int actualMinParams = valueConverters.stream().mapToInt(ValueConverter::howManyValuesDoesThisEat).sum(); //Note: In !varargs, this is params
			
			int maxParams = actualMinParams; // Unlimited == Integer.MAX_VALUE
			if (isEffectivelyVarArgs) {
				maxParams = method.getAnnotation(LazyFunction.class).maxParams();
				if (maxParams == -2)
					throw new IllegalArgumentException("No maximum number of params specified for " + method.getName() + ", use -1 for unlimited. "
							+ "Provided in " + clazz);
				if (maxParams < actualMinParams)
					throw new IllegalArgumentException("Provided maximum number of params for " + method.getName() + " is smaller than method's param count."
							+ "Provided in " + clazz);
				if (maxParams == -1)
					maxParams = Integer.MAX_VALUE;
			}
			
			TriFunction<Context, Integer, List<LazyValue>, LazyValue> function = makeFunction(method, instance, valueConverters, actualMinParams, maxParams);
			
			int parameterCount = isEffectivelyVarArgs ? -1 : actualMinParams;
			CarpetSettings.LOG.info("Adding fname: "+functionName+". Expression paramcount: "+parameterCount+". Provided in '" + clazz.getName() + "'");
			functionList.add(Triple.of(functionName, parameterCount, function));
			
		}
	}
	
	public static void apply(Expression expr) {
		for (Triple<String, Integer, TriFunction<Context, Integer, List<LazyValue>, LazyValue>> t : functionList)
			expr.addLazyFunction(t.getLeft(), t.getMiddle(), t.getRight());

	}
	
	private static <T> TriFunction<Context, Integer, List<LazyValue>, LazyValue> makeFunction(final Method method, final Object instance, 
														final List<ValueConverter<?>> valueConverters, final int minParams, final int maxParams)
	{
		final boolean isVarArgs = method.isVarArgs();
		@SuppressWarnings("unchecked") // We are "defining" T in here.
		final OutputConverter<T> outputConverter = OutputConverter.get((Class<T>)method.getReturnType());
		int methodParamCount = method.getParameterCount();
		
		final ValueConverter<?> varArgsConverter = ValueConverter.fromAnnotatedType(method.getParameters()[methodParamCount - 1].getAnnotatedType());
		final Class<?> varArgsType = method.getParameters()[methodParamCount - 1].getType().getComponentType();
		//If using primitives, this is problematic when casting (cannot cast to Object[]). TODO Change if I find a better way.
		//TODO Option 2: Just not support unboxeds in varargs and ask to use boxed types, would be both simpler and faster, since this 
		//              method requires creating new array and moving every item to cast Boxed[] -> primitive[]
		//TODO At least use one of them, since I got lazy and didn't even make this work (current idea: ArrayUtils.toPrimitive(boxedArray) )
		//final Class<?> boxedVarArgsType = ClassUtils.primitiveToWrapper(varArgsType);
		
		
		// MethodHandles are blazing fast (in some situations they can even compile to a single invokeVirtual), but slightly complex to work with.
		// Their "polymorphic signature" makes them (by default) require the exact signature of the method and return type, in order to call the
		// functions directly, not even accepting Objects. Therefore we change them to spreaders (accept array instead), return type of Object,
		// and we also bind them to our instance. That makes them ever-so-slightly slower, since they have to cast the params, but it's not
		// noticeable, and comparing the overhead that reflection's #invoke had over this, the difference is substantial
		// (checking access at invoke vs create).
		// Note: there is also MethodHandle#invoke and #invokeWithArguments, but those run #asType at every invocation, which is quite slow, so we
		//       are basically running it here.
		final MethodHandle handle;
		try {
			MethodHandle tempHandle = MethodHandles.publicLookup().unreflect(method).asFixedArity().asSpreader(Object[].class, methodParamCount);
			handle = tempHandle.asType(tempHandle.type().changeReturnType(Object.class)).bindTo(instance);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		}
		return (context, i, lv) -> {
			if (isVarArgs) {
				if (lv.size() < minParams) //TODO More descriptive messages using ValueConverter#getTypeName (or even param.getName()?)
					throw new InternalExpressionException(method.getName() + " expects at least " + minParams + "arguments");
				if (lv.size() > maxParams)
					throw new InternalExpressionException(method.getName() + " expects up to " + maxParams + " arguments");
			}
			Object[] params = getMethodParams(lv, valueConverters, varArgsConverter, varArgsType, isVarArgs, context, method.getParameterCount());
			try {
				return outputConverter.convert((T) handle.invokeExact(params));
			} catch (Throwable e) {
				//Throwable cause = e.getCause();
				if (e instanceof RuntimeException)
					throw (RuntimeException) e;
				throw (Error) e; // Stack overflow or something. Methods are guaranteed not to throw checked exceptions
			}
		};
	}
	
	// Hot code: Must be optimized
	private static Object[] getMethodParams(List<LazyValue> lv, List<ValueConverter<?>> valueConverters,
											ValueConverter<?> varArgsConverter, Class<?> varArgsType, boolean isMethodVarArgs, 
											Context context, int methodParameterCount)
	{
		Object[] params = new Object[methodParameterCount];
		ListIterator<LazyValue> lvIterator = lv.listIterator();
		
		int regularArgs = isMethodVarArgs ? methodParameterCount -1 : methodParameterCount;
		for (int i = 0; i < regularArgs; i++) {
			params[i] = valueConverters.get(i).evalAndConvert(lvIterator, context);
		}
		if (isMethodVarArgs) {
			int remaining = lv.size() - lvIterator.nextIndex();
			Object[] varArgs;
			if (varArgsConverter.consumesVariableArgs()) {
				List<Object> varArgsList = new ObjectArrayList<>();
				while (lvIterator.hasNext())
					varArgsList.add(varArgsConverter.evalAndConvert(lvIterator, context));
				varArgs = varArgsList.toArray();
			} else {
				varArgs = (Object[])Array.newInstance(varArgsType, remaining/varArgsConverter.howManyValuesDoesThisEat());
				int pointer = 0;
				while (lvIterator.hasNext()) {
					varArgs[pointer] = varArgsConverter.evalAndConvert(lvIterator, context);
					pointer++;
				}
			}
			
			params[methodParameterCount - 1] = varArgs;
			//TODO The above, but for primitive varargs
		}
		return params;
	}
	
	private AnnotationParser() {}
}
