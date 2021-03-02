package carpet.script.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.swing.text.html.parser.Entity;

import carpet.script.argument.Argument;
import carpet.script.argument.BlockArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.value.BlockValue;
import carpet.script.value.Value;

/**
 * Class that holds annotation for {@link Argument} locators to be used in Scarpet functions
 */
public final class Locator {
	/**
	 * <p>Represents that the annotated argument must be gotten by passing the arguments in there into a {@link BlockArgument} locator.
	 * 
	 * <p>Must be used in {@link BlockValue} parameters
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface Block {
		/**
		 * <p>Whether or not should the locator accept a single {@link String} as the parameter and
		 * let parsing to {@link BlockValue}.
		 */
		boolean acceptString() default false;
		
		/**
		 * <p>Whether or not should the {@link BlockValue} argument be optional. In case it is optional,
		 * {@code null} will be passed to the function.</p>
		 */
		boolean optional() default false;
		
		/**
		 * <p>Whether or not should the {@link BlockArgument} locator accept any string as the argument.</p>
		 * @deprecated This is NOT currently supported, since it would mean returning a {@link String} instead of
		 * a {@link BlockValue}, which I don't know how to do, if possible at all without breaking the contract of
		 * returning the {@link Value} directly
		 */
		@Deprecated
		boolean anyString() default false;
	}

	/**
	 * <p>Represents that the annotated argument must be gotten by passing the arguments in there into a {@link Vector3Argument} locator.</p>
	 * 
	 * <p>Must be used in {@link net.minecraft.util.math.Vec3d} parameters</p>
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface Vec3d {
		/**
		 * <p>Whether or not should the {@link Vector3Argument} locator accept an optional direction aside from the {@link net.minecraft.util.math.Vec3d}</p>
		 * @deprecated This is NOT currently supported, since it would mean returning a {@code pitch} and a {@code yaw} aside from the 
		 *             {@link net.minecraft.util.math.Vec3d}, which is impossible in a single argument  
		 */
		@Deprecated
		boolean optionalDirection() default false;
		
		/**
		 * Whether or not should the {@link Vector3Argument} locator accept an entity aside to get the {@link Vec3d} from and return that entity too</p>
		 * @deprecated This is NOT currently supported, since it would mean returning an {@link Entity} aside from the {@link net.minecraft.util.math.Vec3d},
		 *             which is impossible in a single argument. In a future, this may be used to allow passing an entity, but the 
		 *             {@link net.minecraft.entity.Entity} is probably not coming
		 */
		@Deprecated
		boolean optionalEntity() default false;
	}
	private Locator() {}
}
