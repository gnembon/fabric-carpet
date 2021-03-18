package carpet.script.annotation;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import carpet.CarpetSettings;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff.TriFunction;
import carpet.script.exception.InternalExpressionException;
import carpet.script.LazyValue;
import carpet.script.value.Value;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AnnotationParser {
	private static final List<Triple<String, Integer, TriFunction<Context, Integer, List<LazyValue>, LazyValue>>> functionList = new ArrayList<>();
	
	public static <T extends FunctionClass> void parseFunctionClass(Class<T> clazz) {
		if (Modifier.isAbstract(clazz.getModifiers())) {
			throw new IllegalArgumentException("Function class must be concrete! Class: " + clazz.getSimpleName()); //Provider not available yet
		}
		T instance;
		try {
			instance = clazz.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException("Couldn't create instance of given " + clazz.toString() + ". Make sure default constructor is available", e);
		}
		
		Method[] methodz = clazz.getDeclaredMethods();
		for (Method method : methodz) {
			if (!method.isAnnotationPresent(LazyFunction.class)) continue;
			//Checks
			if (Modifier.isAbstract(method.getModifiers())) { //TODO Probably remove since class is already checked to not be abstract
				throw new IllegalArgumentException("Annotated method '"+method.getName()+"', provided by '"+instance.getProvider()+"' must not be abstract");
			}
			if (Modifier.isStatic(method.getModifiers())) {
				throw new IllegalArgumentException("Annotated method '"+ method.getName() +"', provided by '"+instance.getProvider()+"' must not be static");
			}
			if (method.getExceptionTypes().length != 0) {
				throw new IllegalArgumentException("Annotated method '"+ method.getName() +"', provided by '"+instance.getProvider()+"' must not declare checked exceptions");
			}
			List<Pair<String, Class<?>>> params = new ArrayList<>(); //TODO (yet unused)
			
			Parameter[] paramz = method.getParameters();
			boolean passContext = false;
			if (paramz.length > 0 && paramz[0].getType() == Context.class)
				passContext = true;
			
			for (Parameter param : paramz) {
				if (!param.isVarArgs() && param.getType() != Context.class)
					params.add(Pair.of(param.getName(), param.getType()));
			}
			boolean unlimitedParams = false;
			if (method.isVarArgs()) {
				int maxParams = method.getAnnotation(LazyFunction.class).maxParams();
				if (maxParams == -2) 
					throw new IllegalArgumentException("No maximum number of params specified for " + method.getName() + ", use -1 for unlimited. "
							+ "Provided by " + instance.getProvider());
				else if (maxParams == -1)
					unlimitedParams = true;
				else if (maxParams < method.getParameterCount()) //TODO Locators and the like?
					throw new IllegalArgumentException("Provided maximum number of params for " + method.getName() + " is smaller than method's param count."
							+ "Provided by " + instance.getProvider());
				else {
					int pointer = params.size();
					while (maxParams - pointer > 0) {
						params.add(Pair.of("arg" + pointer + 1, Value.class));
						pointer++;
					}
				}
			}
			// TODO Replace this with the argument checker/converter
			int parameterCount = method.isVarArgs() ? -1 : passContext ? method.getParameterCount() -1 : method.getParameterCount();
			String functionName = method.getName();
			
			TriFunction<Context, Integer, List<LazyValue>, LazyValue> function = makeFunction(method, instance);
			CarpetSettings.LOG.info("Adding fname: "+functionName+". Expression paramcount: "+parameterCount+". Provided by '"+instance.getProvider()+"'");
			functionList.add(Triple.of(functionName, parameterCount, function));
			
		}
	}
	
	public static void apply(Expression expr) {
		for (Triple<String, Integer, TriFunction<Context, Integer, List<LazyValue>, LazyValue>> t : functionList)
			expr.addLazyFunction(t.getLeft(), t.getMiddle(), t.getRight());

	}
	
	private static <T> TriFunction<Context, Integer, List<LazyValue>, LazyValue> makeFunction(final Method method, final FunctionClass instance) {
		final boolean isVarArgs = method.isVarArgs();
		@SuppressWarnings("unchecked") // We are "defining" T in here
		final OutputConverter<T> outputConverter = OutputConverter.get((Class<T>) method.getReturnType());
		final List<ValueConverter<?>> valueConverters = new ObjectArrayList<>(); //Testing (at least slightly) more performant things
		int methodParamCount = method.getParameterCount(); // Not capturing since it's fast, just verbose
		Parameter param;
		for (int i = 0; i < methodParamCount; i++) {
			param = method.getParameters()[i];
			if (!isVarArgs || i != methodParamCount -1 )
				valueConverters.add(ValueConverter.fromAnnotatedType(param.getAnnotatedType()));
		}
		final ValueConverter<?> varArgsConverter = ValueConverter.fromAnnotatedType(method.getParameters()[methodParamCount - 1].getAnnotatedType());
		final Class<?> varArgsType = method.getParameters()[methodParamCount - 1].getType().getComponentType();
		//If using primitives, this is problematic when casting (cannot cast to Object[]). TODO Change if I find a better way.
		//TODO Option 2: Just not support unboxeds in varargs and ask to use boxed types, would be both simpler and faster, since this method requires creating new array and moving every item to cast Boxed[] -> primitive[]
		//TODO At least use one of them, since I got lazy and didn't even make this work (current idea: ArrayUtils.toPrimitive(boxedArray) )
		final Class<?> boxedVarArgsType = ClassUtils.primitiveToWrapper(varArgsType);
		final int minParams = 0; // TODO Populate this. Think about locators
		
		return (context, i, lv) -> {
			if (isVarArgs && lv.size() < minParams)
				throw new InternalExpressionException(method.getName() + " expects at least " + minParams + "arguments"); 
			Object[] params = getMethodParams(lv, valueConverters, varArgsConverter, varArgsType, isVarArgs, context, method.getParameterCount());
			// ^ TODO Decide whether it's a good idea to have this outside or just take it back here
			try {
				@SuppressWarnings("unchecked") // T is the return type of the method. Why doesn't Method have generics for the output?
				LazyValue ret = outputConverter.convert((T) method.invoke(instance, params)); 
				return ret;
			} catch (IllegalAccessException | IllegalArgumentException e) { // Some are Runtime, but are TODO s
				CarpetSettings.LOG.error("Reflection error during execution of method " + method.getName() + ", with params " + params + ". "
						+ "Provided by '" + instance.getProvider() + "'", e);
				throw new InternalExpressionException("Something bad happened, check logs and report to Carpet with log and program: " + e.getMessage());
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException)
					throw (RuntimeException) cause;
				throw (Error) cause; // Stackoverflow or something. Methods are guaranteed not to throw checked exceptions
			}
		};
	}
	
	private static Object[] getMethodParams(List<LazyValue> lv, List<ValueConverter<?>> valueConverters,
											ValueConverter<?> varArgsConverter, Class<?> varArgsType, boolean isVarArgs, 
											Context context, int methodParameterCount)
	{
		Object[] params;
		if (isVarArgs) {
			int regularRemaining = methodParameterCount - 1;
			int pointer = 0;
			Iterator<ValueConverter<?>> converterIterator = valueConverters.iterator(); 
			params = new Object[regularRemaining + 1];
			while (regularRemaining > 0) {
				params[pointer] = converterIterator.next().evalAndConvert(lv.remove(0), context);
				regularRemaining--; pointer++;
			}
			params[pointer] = lv.stream().map(i-> varArgsConverter.evalAndConvert(i, context)).toArray(size-> (Object[])Array.newInstance(varArgsType, size));
			//TODO (even) More efficient thing of the above ^
			//TODO The above, but for primitive varargs
		} else {
			params = new Object[lv.size()];
			for (int i = 0; i < lv.size(); i++)
				params[i] = valueConverters.get(i).evalAndConvert(lv.get(i), context);
		}
		return params;
	}
	
	private AnnotationParser() {}
}
