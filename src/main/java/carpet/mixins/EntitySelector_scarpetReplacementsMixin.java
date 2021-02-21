package carpet.mixins;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import carpet.script.utils.CarpetFakeReplacementEntity;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

@Mixin(EntitySelector.class)
public class EntitySelector_scarpetReplacementsMixin {
	private static final Predicate<Entity> notCarpetReplacement = (e) -> !(e instanceof CarpetFakeReplacementEntity);
	
    @Inject(method = "getPositionPredicate", at = @At("RETURN"), cancellable = true)
    public void ignoreCarpetReplacementsInSelectors(Vec3d vec3d, CallbackInfoReturnable<Predicate<Entity>> cir)
    {
        cir.setReturnValue(cir.getReturnValue().and(notCarpetReplacement));
    }
}
