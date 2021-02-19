package carpet.script.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import carpet.CarpetSettings;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff.TriFunction;
import carpet.script.exception.InternalExpressionException;
import carpet.script.LazyValue;
import carpet.script.value.Value;

public class AnnotationParser {
	private static final List<Triple<String, Integer, TriFunction<Context, Integer, List<LazyValue>, LazyValue>>> functionList = new ArrayList<>();
	
	public static <T> void parseFunctionClass(Class<T> clazz) {
		T instance;
		try {
			instance = clazz.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new IllegalStateException("Couldn't create instance of given " + clazz.toString() + ". Make sure default constructor is available", e);
		}
		
		Method[] methodz = clazz.getDeclaredMethods();
		for (Method method : methodz) {
			if (!method.isAnnotationPresent(LazyFunction.class) || Modifier.isStatic(method.getModifiers())) continue;
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
					throw new IllegalArgumentException("No maximum number of params specified for "+ method.getName() +", use -1 for unlimited");
				else if (maxParams == -1)
					unlimitedParams = true;
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
			CarpetSettings.LOG.info("Adding fname: "+functionName+". Expression paramcount: "+parameterCount);
			functionList.add(Triple.of(functionName, parameterCount, function));
			
		}
	}
	
	public static void apply(Expression expr) {
		for (Triple<String, Integer, TriFunction<Context, Integer, List<LazyValue>, LazyValue>> t : functionList)
			expr.addLazyFunction(t.getLeft(), t.getMiddle(), t.getRight());

	}
	
	private static TriFunction<Context, Integer, List<LazyValue>, LazyValue> makeFunction(Method method, Object instance) {
		final boolean isVarArgs = method.isVarArgs();
		final int minParams = 0; //TODO Populate this
		return (context, i, lv) -> {
			if (isVarArgs && lv.size() < minParams)
				i = i;
			List<Value> evaledLv = lv.stream().map(val -> val.evalValue(context)).collect(Collectors.toList());
			Object[] params;
			if (isVarArgs) {
				int regularRemaining = method.getParameterCount() - 1;
				int pointer = 0;
				params = new Object[regularRemaining+1];
				while (regularRemaining > 0) {
					params[pointer] = evaledLv.remove(0);
					regularRemaining--; pointer++;
				}
				params[pointer] = evaledLv.toArray(new Value[0]);
			} else {
				params = evaledLv.toArray();
			}
			try {
				return (LazyValue) method.invoke(instance, params);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) { // Some are Runtime, but are TODO s
				CarpetSettings.LOG.error("Reflection error during execution of method " + method.getName() + ", with params " + params, e);
				throw new InternalExpressionException("Something bad happened, report to Carpet with log and program: "+e.getMessage());
			}
		};
	}
	
	private AnnotationParser() {}
}
