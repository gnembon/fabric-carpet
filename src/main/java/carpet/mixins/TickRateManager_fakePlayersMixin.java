package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import carpet.fakes.EntityInterface;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@Mixin(TickRateManager.class)
abstract class TickRateManager_fakePlayersMixin {
    @Shadow
    abstract boolean runsNormally();

    @ModifyReturnValue(method = "isEntityFrozen", at = @At("TAIL"))
    private boolean handler(boolean alreadyFrozen, Entity entity) {
        if (alreadyFrozen) return true;
        return !runsNormally() && (entity instanceof EntityPlayerMPFake)
                && !((EntityInterface) entity).cm$getIndirectPassengersStream().anyMatch(e -> e instanceof Player && !(e instanceof EntityPlayerMPFake));
    }
}
