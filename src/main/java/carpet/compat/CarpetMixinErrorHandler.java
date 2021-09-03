package carpet.compat;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import carpet.CarpetServer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * <p>Class Handles errors when applying mixins and just disables the rule instead of crashing the game</p>
 *
 */
public class CarpetMixinErrorHandler implements IMixinErrorHandler, PreLaunchEntrypoint {
	private static final Logger LOGGER = LogManager.getLogger("Carpet Mixin Error Handler");
	private static volatile boolean isGameReady = false;
	private static final Set<FeatureMixin> featuresToDisable = new HashSet<>();
	private static final MethodHandle getAnnotationNode;
	private static final MethodHandle getAnnotationValue;
	
	// Called on PreLaunch
	static {
		// Potentially accessing mixin internals here, so we use MethodHandles just in case and catch their exceptions. If they throw, we disable this
		MethodHandle getAnnotationNode0 = null, getAnnotationValue0 = null;
		try {
			Class<?> annotationsClass = Class.forName("org.spongepowered.asm.util.Annotations");
			getAnnotationNode0 = MethodHandles.lookup().findStatic(annotationsClass, "getInvisible",
					MethodType.methodType(AnnotationNode.class, ClassNode.class, Class.class));
			getAnnotationValue0 = MethodHandles.lookup().findStatic(annotationsClass, "getValue",
					MethodType.methodType(Object.class, AnnotationNode.class, String.class, Class.class));
		} catch (Exception e) {
			getAnnotationNode0 = null;
			LOGGER.info("Failed to setup Carpet's Mixin Error Handler. That compatibility layer will not be available", e);
		}
		getAnnotationNode = getAnnotationNode0;
		getAnnotationValue = getAnnotationValue0;
	}
	
	@Override
	public void onPreLaunch() {
		if (getAnnotationNode == null) return; // Don't register if something failed during initialization
		try {
			Mixins.registerErrorHandlerClass(this.getClass().getName());
		} catch (LinkageError err) { //I'm 99% sure this is public API, but just in case, this is here
			LOGGER.info("Failed to setup Carpet's Mixin Error Handler. That compatibility layer will not be available", err);
		}
	}
	
	@Override
	public ErrorAction onPrepareError(IMixinConfig config, Throwable th, IMixinInfo mixinInfo, ErrorAction action) {
		if (action != ErrorAction.ERROR) // Ignore warns or other things that are already handled
			return null;
		AnnotationNode node = getFeatureAnnotationNode(mixinInfo.getClassNode(ClassReader.EXPAND_FRAMES)); //TODO is the flag needed?
		if (!canSkipMixin(node)) {
			return null; // Crash! It may be a mandatory mixin of ours, or just some random mixin from someone else
		}
		FeatureMixin feature = new VirtualFeatureMixin(node);
		if (isGameReady) {
			disableRule(feature);
		} else {
			featuresToDisable.add(feature);
		}
		return ErrorAction.WARN;
	}
	
	@Override
	public ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action) {
		// Not catching apply, since the class can be in any state? (TODO: Check that's true)
		return null;
	}
	
	private static boolean canSkipMixin(AnnotationNode node) {
		return node != null; // Anything annotated with FeatureMixin can be skipped atm, the rule will just be disabled
	}
	
	private static void disableRule(FeatureMixin feature) {
		String rule = feature.value();
		if (!feature.settingsManager().equals("carpet")) {
			throw new NotImplementedException("Disabling rules from other extensions in Mixin errors is not implemented!");
		}
		CarpetServer.settingsManager.getRule(rule);
	}
	
	/**
	 * <p>Marks that the game is ready to be referenced and disables all rules scheduled to be disabled.</p>
	 */
	public static void setGameReady() {
		if (isGameReady) throw new IllegalStateException("Game has already been declared as ready!");
		isGameReady = true;
		for (FeatureMixin feature : featuresToDisable) {
			disableRule(feature);
		}
		featuresToDisable.clear();
	}
	
	private static AnnotationNode getFeatureAnnotationNode(ClassNode node) {
		try {
			return (AnnotationNode) getAnnotationNode.invokeExact(node, FeatureMixin.class);
		} catch (Throwable e) {
			if (e instanceof Error)
				throw (Error) e;
			throw (RuntimeException) e; // Doesn't throw checked
		}
	}
	
	private static <T> T getValue(AnnotationNode annotation, String key, Class<? extends Annotation> annotationType) {
		try {
			return (T) getAnnotationValue.invokeExact(annotation, key, Mixin.class);
		} catch (Throwable e) {
			if (e instanceof Error)
				throw (Error) e;
			throw (RuntimeException) e; // Doesn't throw checked
		}
	}

	/**
	 * <p>A fake FeatureMixin annotation, since we can't get the actual annotation because Mixin classes are not thought to
	 * be loaded by the JVM, and trying to get the annotation would load the class by referencing it.</p>
	 * 
	 * <p>Record so it implements {@link #equals(Object)} and {@link #hashCode()} easily</p>
	 */
	static record VirtualFeatureMixin(String value, String settingsManager) implements FeatureMixin {
		/**
		 * Constructs a new VirtualFeatureMixin from the data in the given {@link AnnotationNode}
		 * @param node An {@link AnnotationNode} corresponding to a FeatureMixin annotation
		 */
		public VirtualFeatureMixin(AnnotationNode node) {
			this(
				/* value = */ getValue(node, "value", FeatureMixin.class),
				/* settingsManager = */ getValue(node, "settingsManager", FeatureMixin.class)
			);
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return FeatureMixin.class;
		}
		
	}
}
