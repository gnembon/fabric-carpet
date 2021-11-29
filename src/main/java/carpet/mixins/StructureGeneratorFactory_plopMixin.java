package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.structure.StructureGeneratorFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(StructureGeneratorFactory.class)
public interface StructureGeneratorFactory_plopMixin
{
    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "method_39845(Ljava/util/function/Predicate;Ljava/util/Optional;Lnet/minecraft/structure/StructureGeneratorFactory$Context;)Ljava/util/Optional;", at = @At(
            value = "INVOKE",
            target = "java/util/function/Predicate.test(Ljava/lang/Object;)Z"
    ))
    private static boolean checkMate(Predicate<Object> predicate, Object o)
    {
        return CarpetSettings.skipGenerationChecks.get() || predicate.test(o);
    }
}
