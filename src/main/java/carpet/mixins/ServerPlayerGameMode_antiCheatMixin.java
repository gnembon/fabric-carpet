package carpet.mixins;

import carpet.CarpetSettings;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayer.class)
public abstract class ServerPlayerGameMode_antiCheatMixin extends Player
{
    public ServerPlayerGameMode_antiCheatMixin(final Level level, final BlockPos blockPos, final float f, final GameProfile gameProfile)
    {
        super(level, blockPos, f, gameProfile);
    }

    @Inject(method = "canInteractWithBlock", at = @At("HEAD"), cancellable = true)
    private void canInteractLongRangeBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir)
    {
        double maxRange = blockInteractionRange() + 1.0;
        maxRange = maxRange * maxRange;
        if (CarpetSettings.antiCheatDisabled && maxRange < 1024 && getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) < 1024) cir.setReturnValue(true);
    }

    @Inject(method = "canInteractWithEntity", at = @At("HEAD"), cancellable = true)
    private void canInteractLongRangeEntity(AABB aabb, CallbackInfoReturnable<Boolean> cir)
    {
        double maxRange = entityInteractionRange() + 1.0;
        maxRange = maxRange * maxRange;
        if (CarpetSettings.antiCheatDisabled && maxRange < 1024 && aabb.distanceToSqr(getEyePosition()) < 1024) cir.setReturnValue(true);
    }
}
