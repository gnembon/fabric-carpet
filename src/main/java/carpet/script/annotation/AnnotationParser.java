package carpet.script.annotation;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
	private static final List<ParsedFunction> functionList = new ArrayList<>();
	
	/**
	 * <p>Parses a given {@link Class} and registers its annotated methods, the ones with the {@link LazyFunction} annotation, to be used
	 * in the Scarpet language.</p>
	 * 
	 * <p>There is a set of requirements for the class in order to be accepted:</p>
	 * <ul>
	 * <li>Class must be concrete. That is, no interfaces or abstract classes should be passed</li>
	 * <li>Class must have the default constructor (or an equivalent) available. That is done in order to not need the {@code static} modifier
	 * in every method, making them faster to code and simpler to look at.</li>
	 * <li>Annotated methods must not be {@code static}. See claim above</li>
	 * <li>Annotated methods must not throw checked exceptions. They can throw regular {@link RuntimeException}s (including but not limited to
	 * {@link InternalExpressionException}). Basically, it's fine as long as you don't add a {@code throws} declaration to your methods.</li>
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
			
			ParsedFunction function = new ParsedFunction(method, instance);
			
			CarpetSettings.LOG.info("Adding fname: "+function.name()+". Expr paramcount: "+function.scarpetParamCount()+". Provided in " + clazz.getName());
			functionList.add(function);
		}
	}
	
	/**
	 * <p>Adds all parsed functions to the given {@link Expression}.</p>
	 * <p>This is handled automatically by Carpet</p>
	 * @param expr The expression to add every function to
	 */
	public static void apply(Expression expr) {
		for (ParsedFunction function : functionList)
			expr.addLazyFunction(function.name(), function.scarpetParamCount(), function);
	}
	
	private static class ParsedFunction implements TriFunction<Context, Integer, List<LazyValue>, LazyValue> {
		private final String name;
		private final boolean isVarArgs;
		private final int methodParamCount;
		private final List<ValueConverter<?>> valueConverters;
		private final Class<?> varArgsType;
		private final ValueConverter<?> varArgsConverter;
		private final OutputConverter<Object> outputConverter;
		private final int minParams;
		private final int maxParams;
		private final MethodHandle handle;
		private final int scarpetParamCount;
		
		private ParsedFunction(final Method method, final Object instance) {
			this.name = method.getName();
			this.isVarArgs = method.isVarArgs();
			this.methodParamCount = method.getParameterCount();
			
			this.valueConverters = new ObjectArrayList<>();
			for (int i = 0; i < this.methodParamCount; i++) {
				Parameter param = method.getParameters()[i];
				if (!isVarArgs || i != this.methodParamCount -1 ) // Varargs converter is separate, inside #makeFunction
					this.valueConverters.add(ValueConverter.fromAnnotatedType(param.getAnnotatedType()));
			}
			this.varArgsType = method.getParameters()[methodParamCount - 1].getType().getComponentType();
			this.varArgsConverter = isVarArgs ? ValueConverter.fromAnnotatedType(method.getParameters()[methodParamCount - 1].getAnnotatedType()) : null;
			@SuppressWarnings("unchecked") // Yes. Making a T is not worth
			OutputConverter<Object> converter = OutputConverter.get((Class<Object>)method.getReturnType());
			this.outputConverter = converter;
			
			//If using primitives, this is problematic when casting (cannot cast to Object[]). TODO Change if I find a better way.
			//TODO Option 2: Just not support unboxeds in varargs and ask to use boxed types, would be both simpler and faster, since this 
			//              method requires creating new array and moving every item to cast Boxed[] -> primitive[]
			//TODO At least use one of them, since I got lazy and didn't even make this work (current idea: ArrayUtils.toPrimitive(boxedArray) )
			//final Class<?> boxedVarArgsType = ClassUtils.primitiveToWrapper(varArgsType);
			//TODO Find a better place for this TODO (this talks about varArgsConverter)
			
			boolean isEffectivelyVarArgs = isVarArgs ? true : valueConverters.stream().anyMatch(ValueConverter::consumesVariableArgs);
			this.minParams = valueConverters.stream().mapToInt(ValueConverter::valueConsumption).sum(); //Note: In !varargs, this is params
			int maxParams = this.minParams; // Unlimited == Integer.MAX_VALUE
			if (isEffectivelyVarArgs) {
				maxParams = method.getAnnotation(LazyFunction.class).maxParams();
				if (maxParams == -2)
					throw new IllegalArgumentException("No maximum number of params specified for " + name + ", use -1 for unlimited. "
							+ "Provided in " + instance.getClass());
				if (maxParams < this.minParams)
					throw new IllegalArgumentException("Provided maximum number of params for " + name + " is smaller than method's param count."
							+ "Provided in " + instance.getClass());
				if (maxParams == -1)
					maxParams = Integer.MAX_VALUE;
			}
			this.maxParams = maxParams;
			
			// Why MethodHandles?
			// MethodHandles are blazing fast (in some situations they can even compile to a single invokeVirtual), but slightly complex to work with.
			// Their "polymorphic signature" makes them (by default) require the exact signature of the method and return type, in order to call the
			// functions directly, not even accepting Objects. Therefore we change them to spreaders (accept array instead), return type of Object,
			// and we also bind them to our instance. That makes them ever-so-slightly slower, since they have to cast the params, but it's not
			// noticeable, and comparing the overhead that reflection's #invoke had over this, the difference is substantial
			// (checking access at invoke vs create).
			// Note: there is also MethodHandle#invoke and #invokeWithArguments, but those run #asType at every invocation, which is quite slow, so we
			//       are basically running it here.
			try {
				MethodHandle tempHandle = MethodHandles.publicLookup().unreflect(method).asFixedArity().asSpreader(Object[].class, this.methodParamCount);
				this.handle = tempHandle.asType(tempHandle.type().changeReturnType(Object.class)).bindTo(instance);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
			
			this.scarpetParamCount = isEffectivelyVarArgs ? -1 : this.minParams;
			
		}
		
		@Override
		public LazyValue apply(Context context, Integer t, List<LazyValue> lv) {
			if (isVarArgs) {
				if (lv.size() < minParams) //TODO More descriptive messages using ValueConverter#getTypeName (or even param.getName()?)
					throw new InternalExpressionException(name + " expects at least " + minParams + " arguments. " + getUsage());
				if (lv.size() > maxParams)
					throw new InternalExpressionException(name + " expects up to " + maxParams + " arguments. " + getUsage());
			}
			Object[] params = getMethodParams(lv, context);
			try {
				return outputConverter.convert(handle.invokeExact(params));
			} catch (Throwable e) {
				if (e instanceof RuntimeException)
					throw (RuntimeException) e;
				throw (Error) e; // Stack overflow or something. Methods are guaranteed not to throw checked exceptions
			}
		}

		// Hot code: Must be optimized
		private Object[] getMethodParams(final List<LazyValue> lv, final Context context) {
			Object[] params = new Object[methodParamCount];
			ListIterator<LazyValue> lvIterator = lv.listIterator();
			
			int regularArgs = isVarArgs ? methodParamCount -1 : methodParamCount;
			for (int i = 0; i < regularArgs; i++) {
				params[i] = valueConverters.get(i).evalAndConvert(lvIterator, context);
				if (params[i] == null)
					throw new InternalExpressionException("Incorrect argument passsed to "+name+" function.\n" + getUsage());
			}
			if (isVarArgs) {
				int remaining = lv.size() - lvIterator.nextIndex();
				Object[] varArgs;
				if (varArgsConverter.consumesVariableArgs()) {
					List<Object> varArgsList = new ObjectArrayList<>();
					while (lvIterator.hasNext()) {
						Object obj = varArgsConverter.evalAndConvert(lvIterator, context);
						if (obj == null)
							throw new InternalExpressionException("Incorrect argument passsed to "+name+" function.\n" + getUsage());
						varArgsList.add(obj);
					}
					varArgs = varArgsList.toArray();
				} else {
					varArgs = (Object[])Array.newInstance(varArgsType, remaining/varArgsConverter.valueConsumption());
					for (int i = 0; lvIterator.hasNext(); i++) {
						varArgs[i] = varArgsConverter.evalAndConvert(lvIterator, context);
						if (varArgs[i] == null)
							throw new InternalExpressionException("Incorrect argument passsed to "+name+" function.\n" + getUsage());
					}
				}
				params[methodParamCount - 1] = varArgs;
				//TODO The above, but for primitive varargs
			}
			return params;
		}
		
		private String getUsage() {
			StringBuilder builder = new StringBuilder("Usage: ");
			builder.append(name);
			builder.append('(');
			builder.append(valueConverters.stream().map(ValueConverter::getTypeName).filter(Objects::nonNull).collect(Collectors.joining(", ")));
			if (varArgsConverter != null) {
				builder.append(", ");
				builder.append(varArgsConverter.getTypeName());
				builder.append("s...)");
			}
			return builder.toString();
		}
		
		private int scarpetParamCount() {
			return scarpetParamCount;
		}
		private String name() {
			return name;
		}
	}
	
	private AnnotationParser() {}
}
