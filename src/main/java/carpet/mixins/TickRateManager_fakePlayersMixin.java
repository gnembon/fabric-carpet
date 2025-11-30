package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    private boolean handler(boolean alreadyFrozen, Entity entity)
    {
        if (alreadyFrozen) return true;
        if (runsNormally()) return false;

        return !isActualPlayer(entity) && // not carrying players
                ((EntityInterface) entity)
                    .cm$getIndirectPassengersStream()
                    .noneMatch(TickRateManager_fakePlayersMixin::isActualPlayer);
    }

    @Unique
    private static boolean isActualPlayer(Entity e)
    {
        return e instanceof Player && !(e instanceof EntityPlayerMPFake);
    }
}
