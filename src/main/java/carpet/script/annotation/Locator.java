package carpet.script.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.google.common.collect.Lists;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.argument.Argument;
import carpet.script.argument.BlockArgument;
import carpet.script.argument.FunctionArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.Module;
import carpet.script.value.BlockValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;

import javax.annotation.Nullable;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Class that holds the annotations for {@link Argument} locators, in order for them to be used in Scarpet functions.</p>
 */
public interface Locator
{
    /**
     * <p>Represents that the annotated argument must be gotten by passing the arguments in there into a {@link BlockArgument} locator.</p>
     * 
     * <p>Must be used in either {@link BlockArgument}, {@link BlockValue}, {@link BlockPos} or {@link BlockState} parameters</p>
     */
    @Documented
    @Retention(RUNTIME)
    @Target({ PARAMETER, TYPE_USE })
    @interface Block
    {
        /**
         * <p>Whether or not should the locator accept a single {@link String} as the parameter and let parsing to {@link BlockValue}.</p>
         */
        boolean acceptString() default false;

        /**
         * <p>Whether or not should the {@link BlockValue} argument be optional.</p> <p>Requires the annotation to be present in a
         * {@link BlockArgument} type, since it may return that the {@link BlockValue} is {@code null}, which would be considered as an incorrect
         * type.</p>
         */
        boolean optional() default false;

        /**
         * <p>Whether or not should the {@link BlockArgument} locator accept any string as the argument.</p> <p>Requires the annotation to be present
         * in a {@link BlockArgument} type, since it may just return a {@link String}</p>
         */
        boolean anyString() default false;
    }

    /**
     * <p>Represents that the annotated argument must be gotten by passing the arguments in there into a {@link Vector3Argument} locator.</p>
     * 
     * <p>Must be used in either a {@link Vector3Argument} or a {@link net.minecraft.world.phys.Vec3 Vec3d} parameter.</p>
     */
    @Documented
    @Retention(RUNTIME)
    @Target({ PARAMETER, TYPE_USE })
    @interface Vec3d
    {
        /**
         * <p>Whether or not should the {@link Vector3Argument} locator accept an optional direction aside from the
         * {@link net.minecraft.world.phys.Vec3}</p> <p>This parameter can only be used in a {@link Vector3Argument} type, since else there is no way
         * to get the direction too.</p>
         */
        boolean optionalDirection() default false;

        /**
         * <p>Whether or not should the {@link Vector3Argument} locator accept an entity aside to get the {@link Vec3d} from and return that entity
         * too</p> <p>Note that you will only be able to get that entity if the annotation is present in a {@link Vector3Argument}</p>
         */
        boolean optionalEntity() default false;
    }

    /**
     * <p>Represents that the annotated argument must be gotten by passing the arguments in this annotation into a {@link FunctionArgument}
     * locator</p>
     * 
     * <p>Can be used in both {@link FunctionArgument} and {@link FunctionValue} types, but the last won't have access to arguments provided to the
     * function, even though they will still be consumed from the arguments the function was called with.</p>
     * 
     * <p><b>This will consume any remaining parameters passed to the function, therefore any other parameter after this will throw.</b></p>
     */
    @Documented
    @Retention(RUNTIME)
    @Target({ PARAMETER, TYPE_USE })
    @interface Function
    {
        /**
         * <p>Whether this Locator should allow no function to be passed.</p> <p>This is not compatible with {@link FunctionValue} type, since a
         * converter returning {@code null} will throw as if the passed argument was incorrect. You can still use it when targeting
         * {@link FunctionArgument}</p>
         */
        boolean allowNone() default false;

        /**
         * <p>Whether the locator should check that the number of arguments passed along with the function matches the number of arguments that the
         * located function requires. Note that FunctionLocators consume all remaining arguments even if this is set to {@code false}.</p>
         */
        boolean checkArgs();
    }

    /**
     * <p>Class that holds locators and methods to get them</p>
     * 
     * <p>Not part of the public API, just that interfaces must have all members public</p>
     */
    final class Locators
    {
        private Locators()
        {
            super();
        }

        static <R> ValueConverter<R> fromAnnotatedType(AnnotatedType annoType, Class<R> type)
        {
            if (annoType.isAnnotationPresent(Block.class))
            {
                return new BlockLocator<>(annoType.getAnnotation(Block.class), type);
            }
            if (annoType.isAnnotationPresent(Function.class))
            {
                return new FunctionLocator<>(annoType.getAnnotation(Function.class), type);
            }
            if (annoType.isAnnotationPresent(Vec3d.class))
            {
                return new Vec3dLocator<>(annoType.getAnnotation(Vec3d.class), type);
            }
            throw new IllegalStateException("Locator#fromAnnotatedType got called with an incompatible AnnotatedType");
        }

        private static class BlockLocator<R> extends AbstractLocator<R>
        {
            private final java.util.function.Function<BlockArgument, R> returnFunction;
            private final boolean acceptString;
            private final boolean anyString;
            private final boolean optional;

            public BlockLocator(Block annotation, Class<R> type)
            {
                super();
                this.acceptString = annotation.acceptString();
                this.anyString = annotation.anyString();
                this.optional = annotation.optional();
                if (type != BlockArgument.class && (anyString || optional))
                {
                    throw new IllegalArgumentException("Can only use anyString or optional parameters of Locator.Block if targeting a BlockArgument");
                }
                this.returnFunction = getReturnFunction(type);
                if (returnFunction == null)
                {
                    throw new IllegalArgumentException("Locator.Block can only be used against BlockArgument, BlockValue, BlockPos or BlockState types!");
                }
            }

            @Nullable
            @SuppressWarnings("unchecked")
            private static <R> java.util.function.Function<BlockArgument, R> getReturnFunction(Class<R> type)
            {
                if (type == BlockArgument.class)
                {
                    return r -> (R) r;
                }
                if (type == BlockValue.class)
                {
                    return r -> (R) r.block;
                }
                if (type == BlockPos.class)
                {
                    return r -> (R) r.block.getPos();
                }
                if (type == BlockState.class)
                {
                    return r -> (R) r.block.getBlockState();
                }
                return null;
            }

            @Override
            public String getTypeName()
            {
                return "block";
            }

            @Override
            public R checkAndConvert(Iterator<Value> valueIterator, Context context, Context.Type theLazyT)
            {
                BlockArgument locator = BlockArgument.findIn((CarpetContext) context, valueIterator, 0, acceptString, optional, anyString);
                return returnFunction.apply(locator);
            }
        }

        private static class Vec3dLocator<R> extends AbstractLocator<R>
        {
            private final boolean optionalDirection;
            private final boolean optionalEntity;
            private final boolean returnVec3d;

            public Vec3dLocator(Vec3d annotation, Class<R> type)
            {
                this.optionalDirection = annotation.optionalDirection();
                this.optionalEntity = annotation.optionalEntity();
                this.returnVec3d = type == net.minecraft.world.phys.Vec3.class; // Because of the locator
                if (returnVec3d && optionalDirection)
                {
                    throw new IllegalArgumentException("optionalDirection Locator.Vec3d cannot be used for Vec3d type, use Vector3Argument instead");
                }
                if (!returnVec3d && type != Vector3Argument.class)
                {
                    throw new IllegalArgumentException("Locator.Vec3d can only be used in Vector3Argument or Vec3d types");
                }
            }

            @Override
            public String getTypeName()
            {
                return "position";
            }

            @Override
            public R checkAndConvert(Iterator<Value> valueIterator, Context context, Context.Type theLazyT)
            {
                Vector3Argument locator = Vector3Argument.findIn(valueIterator, 0, optionalDirection, optionalEntity);
                @SuppressWarnings("unchecked") R ret = (R) (returnVec3d ? locator.vec : locator);
                return ret;
            }
        }

        private static class FunctionLocator<R> extends AbstractLocator<R>
        {
            private final boolean returnFunctionValue;
            private final boolean allowNone;
            private final boolean checkArgs;

            FunctionLocator(Function annotation, Class<R> type)
            {
                super();
                this.returnFunctionValue = type == FunctionValue.class;
                if (!returnFunctionValue && type != FunctionArgument.class)
                {
                    throw new IllegalArgumentException("Params annotated with Locator.Function must be of either FunctionArgument or FunctionValue type");
                }
                this.allowNone = annotation.allowNone();
                this.checkArgs = annotation.checkArgs();
                if (returnFunctionValue && allowNone)
                {
                    throw new IllegalArgumentException("Cannot use allowNone of Locator.Function in FunctionValue types, use FunctionArgument");
                }
            }

            @Override
            public R checkAndConvert(Iterator<Value> valueIterator, Context context, Context.Type theLazyT)
            {
                Module module = context.host.main;
                FunctionArgument locator = FunctionArgument.findIn(context, module, Lists.newArrayList(valueIterator), 0, allowNone, checkArgs);
                @SuppressWarnings("unchecked") R ret = (R) (returnFunctionValue ? locator.function : locator);
                return ret;
            }

            @Override
            public String getTypeName()
            {
                return "function";
            }
        }

        private abstract static class AbstractLocator<R> implements ValueConverter<R>, Locator
        {
            @Override
            public R convert(Value value, @Nullable Context context)
            {
                throw new UnsupportedOperationException("Cannot call a locator in a parameter that doesn't contain a context!");
            }

            @Override
            public boolean consumesVariableArgs()
            {
                return true;
            }

            @Override
            public int valueConsumption()
            {
                return 1;
            }

            @Override
            public abstract R checkAndConvert(Iterator<Value> valueIterator, Context context, Context.Type theLazyT);
        }

    }
}
