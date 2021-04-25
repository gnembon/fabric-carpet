package carpet.script.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.util.Iterator;

import org.apache.commons.lang3.NotImplementedException;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.argument.Argument;
import carpet.script.argument.BlockArgument;
import carpet.script.argument.FunctionArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.bundled.Module;
import carpet.script.value.BlockValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Class that holds annotation for {@link Argument} locators to be used in Scarpet functions.</p>
 * 
 * <p>Note: Nothing in this class has been implemented at this point in time, since it requires Carpet changes. There is an implementation 
 * for Locator.Block working, but not implemented.</p>
 */
public interface Locator {
	/**
	 * <p>Represents that the annotated argument must be gotten by passing the arguments in there into a {@link BlockArgument} locator.</p>
	 * 
	 * <p>Must be used in either {@link BlockValue} or {@link BlockArgument} parameters</p>
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({PARAMETER, TYPE_USE})
	@Deprecated //Not implemented, although implementation available
	public @interface Block {
		/**
		 * <p>Whether or not should the locator accept a single {@link String} as the parameter and
		 * let parsing to {@link BlockValue}.</p>
		 */
		boolean acceptString() default false;
		
		/**
		 * <p>Whether or not should the {@link BlockValue} argument be optional.</p>
		 * <p>Requires the annotation to be present in a {@link BlockArgument} type, since it may return that the {@link BlockValue}
		 * is {@code null}, which would be considered as an incorrect type.</p>
		 */
		boolean optional() default false;
		
		/**
		 * <p>Whether or not should the {@link BlockArgument} locator accept any string as the argument.</p>
		 * <p>Requires the annotation to be present in a {@link BlockArgument} type, since it may just return a {@link String}</p>
		 */
		boolean anyString() default false;
	}

	/**
	 * <p>Represents that the annotated argument must be gotten by passing the arguments in there into a {@link Vector3Argument} locator.</p>
	 * 
	 * <p>Must be used in either a {@link Vector3Argument} or a {@link Vec3d} parameter, though the latest may not get
	 * all the data.</p>
	 * 
	 * <p>Note: This locator has not been implemented yet (TODO)</p>
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({PARAMETER, TYPE_USE})
	@Deprecated // Not implemented
	public @interface Vec3d {
		/**
		 * <p>Whether or not should the {@link Vector3Argument} locator accept an optional direction aside from the {@link net.minecraft.util.math.Vec3d}</p>
		 * <p>This parameter can only be used in a {@link Vector3Argument} type, since else there is no way to get the direction too.</p>  
		 */
		boolean optionalDirection() default false;
		
		/**
		 * <p>Whether or not should the {@link Vector3Argument} locator accept an entity aside to get the {@link Vec3d} from and return that entity too</p>
		 * <p>Note that you will only be able to get that entity if the annotation is present in a {@link Vector3Argument}</p>
		 */
		boolean optionalEntity() default false;
	}
	
	/**
	 * <p>Represents that the annotated argument must be gotten by passing the arguments in this annotation into a {@link FunctionArgument} locator</p>
	 * 
	 * <p>Can be used in both {@link FunctionArgument FunctionArgument<LazyValue>} and {@link FunctionValue} types, but the last won't have access to
	 * arguments provided to the function, even though they will still be consumed from the arguments the function was called with.</p>
	 * 
	 * <p><b>This will consume any remaining parameters passed to the function, therefore any other parameters after this will throw.</b></p>
	 * 
	 * <p>Note: This locator has not been implemented yet (TODO)</p>
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({PARAMETER, TYPE_USE})
	@Deprecated // Not implemented
	public @interface Function {
		/**
		 * <p>Whether this Locator should allow no function to be passed.</p>
		 * <p>This is not compatible with {@link FunctionValue} type, since a converter returning {@code null} will throw as if
		 * the passed argument was incorrect. You can still use it when targeting {@link FunctionArgument}</p>
		 */
		boolean allowNone() default false;
		
		/**
		 * <p>Whether the locator should check that the number of arguments passed along with the function matches the number of
		 * arguments that the located function requires. Note that FunctionLocators consume all remaining arguments even if this
		 * is set to {@code false}.</p>
		 */
		boolean checkArgs();
	}
	
	/**
	 * <p>Class that holds locators and methods to get them</p>
	 * 
	 * <p>Not part of the public API, just that interfaces must have all members public</p>
	 */
	static class Locators {
		private Locators() {}
		static <R> ValueConverter<R> fromAnnotatedType(AnnotatedType annoType, Class<R> type) {
			if (annoType.isAnnotationPresent(Block.class))
				return new BlockLocator<R>(annoType.getAnnotation(Block.class), type);
			if (annoType.isAnnotationPresent(Function.class))
				return new FunctionLocator<R>(annoType.getAnnotation(Function.class), type);
			if (annoType.isAnnotationPresent(Vec3d.class))
				return new Vec3dLocator<R>(annoType.getAnnotation(Vec3d.class), type);
			throw new IllegalStateException("Locator#fromAnnotatedType got called with an incompatible AnnotatedType");
		}
		
		private static class BlockLocator<R> extends AbstractLocator<R> {
			private final boolean returnBlockValue;
			private final boolean acceptString;
			private final boolean anyString;
			private final boolean optional;
			
			public BlockLocator(Block annotation, Class<R> type) {
				this.acceptString = annotation.acceptString();
				this.anyString = annotation.anyString();
				this.optional = annotation.optional();
				this.returnBlockValue = type == BlockValue.class;
				if (returnBlockValue && (anyString || optional)) {
					throw new IllegalArgumentException("Can only use anyString or optional parameters of Locator.Block if targeting a BlockArgument");
				}
				if (!returnBlockValue && type != BlockArgument.class) {
					throw new IllegalArgumentException("Locator.Block can only be used against BlockArgument or BlockValue types!");
				}
			}

			@Override
			public String getTypeName() {
				return "block";
			}

			@Override
			public R evalAndConvert(Iterator<LazyValue> lazyValueIterator, Context context, Integer theLazyT) {
				BlockArgument locator = null;
				//return (R) (returnBlockValue ? locator.block : locator);
				throw new NotImplementedException("Locator.Block still requires adapting BlockArgument to accept iterators (which is actually simple)");
			}
		}
		
		private static class Vec3dLocator<R> extends AbstractLocator<R> {
			private final boolean optionalDirection;
			private final boolean optionalEntity;
			private final boolean returnVec3d;
			
			public Vec3dLocator(Vec3d annotation, Class<R> type) {
				this.optionalDirection = annotation.optionalDirection();
				this.optionalEntity = annotation.optionalEntity();
				this.returnVec3d = type == net.minecraft.util.math.Vec3d.class; // Because of the locator
				if (returnVec3d && optionalDirection)
					throw new IllegalArgumentException("optionalDirection Locator.Vec3d cannot be used for Vec3d type, use Vector3Argument instead");
				if (!returnVec3d && type != Vector3Argument.class)
					throw new IllegalArgumentException("Locator.Vec3d can only be used in Vector3Argument or Vec3d types");
			}

			@Override
			public String getTypeName() {
				return "position";
			}

			@Override
			public R evalAndConvert(Iterator<LazyValue> lazyValueIterator, Context context, Integer theLazyT) {
				Vector3Argument locator = null;
				// TODO Make the locator
				//return (R) (returnVec3d ? locator.vec : locator);
				throw new NotImplementedException("Locator.Vec3d still require adapting Vector3Argument to accept iterators!");
			}
		}
		
		private static class FunctionLocator<R> extends AbstractLocator<R> {
			private final boolean returnFunctionValue;
			private final boolean allowNone;
			private final boolean checkArgs;
			
			FunctionLocator(Function annotation, Class<R> type) {
				this.returnFunctionValue = type == FunctionValue.class;
				if (!returnFunctionValue && type != FunctionArgument.class)
					throw new IllegalArgumentException("Params annotated with Locator.Function must be of either FunctionArgument or FunctionValue type");
				this.allowNone = annotation.allowNone();
				this.checkArgs = annotation.checkArgs();
				if (returnFunctionValue && allowNone)
					throw new IllegalArgumentException("Cannot use allowNone of Locator.Function in FunctionValue types, use FunctionArgument");
			}
			
			@Override
			public R evalAndConvert(Iterator<LazyValue> lazyValueIterator, Context context, Integer theLazyT) {
				Module module = context.host.main;
				FunctionArgument locator = null;
				// TODO Make the locator
				//return (R) (returnFunctionValue ? locator.function : locator);
				throw new NotImplementedException("Locator.FunctionValue still requires adapting the annotation system to somehow get lv size"
						+ "or FunctionArgument to not depend on it!");
			}

			@Override
			public String getTypeName() {
				return "function";
			}
		}
		
		private static abstract class AbstractLocator<R> implements ValueConverter<R> {
			@Override
			public R convert(Value value) {
				throw new UnsupportedOperationException("Cannot call a locator in a parameter that doesn't contain a context!");
			}
			@Override
			public boolean consumesVariableArgs() {
				return true;
			}
			@Override
			public int valueConsumption() {
				return 1;
			}
			@Override
			public abstract R evalAndConvert(Iterator<LazyValue> lazyValueIterator, Context context, Integer theLazyT);
		}
		
	}
}
