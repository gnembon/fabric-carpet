package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class)
public abstract class ServerPlayerGameMode_antiCheatMixin extends LivingEntity
{
    @Shadow public abstract double entityInteractionRange();

    @Shadow public abstract double blockInteractionRange();

    protected ServerPlayerGameMode_antiCheatMixin(final EntityType<? extends LivingEntity> entityType, final Level level)
    {
        super(entityType, level);
    }

    @Inject(method = "canInteractWithBlock", at = @At("HEAD"), cancellable = true)
    private void canInteractLongRangeBlock(BlockPos pos, double d, CallbackInfoReturnable<Boolean> cir)
    {
        double maxRange = blockInteractionRange() + d;
        maxRange = maxRange * maxRange;
        if (CarpetSettings.antiCheatDisabled && maxRange < 1024 && getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) < 1024) cir.setReturnValue(true);
    }

    @Inject(method = "canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z", at = @At("HEAD"), cancellable = true)
    private void canInteractLongRangeEntity(AABB aabb, double d, CallbackInfoReturnable<Boolean> cir)
    {
        double maxRange = entityInteractionRange() + d;
        maxRange = maxRange * maxRange;
        if (CarpetSettings.antiCheatDisabled && maxRange < 1024 && aabb.distanceToSqr(getEyePosition()) < 1024) cir.setReturnValue(true);
    }
}
