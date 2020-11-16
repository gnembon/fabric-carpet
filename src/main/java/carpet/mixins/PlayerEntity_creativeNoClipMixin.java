package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntity_creativeNoClipMixin extends LivingEntity
{
    protected PlayerEntity_creativeNoClipMixin(EntityType<? extends LivingEntity> type, World world)
    {
        super(type, world);
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z")
    )
    private boolean canClipTroughWorld(PlayerEntity playerEntity)
    {
        return playerEntity.isSpectator() || (CarpetSettings.creativeNoClip && playerEntity.isCreative() && playerEntity.getAbilities().flying);

    }

    @Redirect(method = "tickMovement", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z")
    )
    private boolean collidesWithEntities(PlayerEntity playerEntity)
    {
        return playerEntity.isSpectator() || (CarpetSettings.creativeNoClip && playerEntity.isCreative() && playerEntity.getAbilities().flying);
    }

    @Redirect(method = "updateSize", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z")
    )
    private boolean spectatorsDontPose(PlayerEntity playerEntity)
    {
        return playerEntity.isSpectator() || (CarpetSettings.creativeNoClip && playerEntity.isCreative() && playerEntity.getAbilities().flying);
    }
}
