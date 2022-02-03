package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(Player.class)
public abstract class Player_creativeNoClipMixin extends LivingEntity
{
    protected Player_creativeNoClipMixin()
    {
        super(null, null);
    }
    
    @Shadow public abstract boolean isCreative();
    @Shadow @Final private Abilities abilities;

    @ModifyExpressionValue(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z")
    )
    private boolean canClipTroughWorld(boolean original)
    {
        return original || (CarpetSettings.creativeNoClip && isCreative() && abilities.flying);

    }

    @ModifyExpressionValue(method = "aiStep", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z")
    )
    private boolean collidesWithEntities(boolean original)
    {
        return original || (CarpetSettings.creativeNoClip && isCreative() && abilities.flying);
    }

    @ModifyExpressionValue(method = "updatePlayerPose", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z")
    )
    private boolean spectatorsDontPose(boolean original)
    {
        return original || (CarpetSettings.creativeNoClip && isCreative() && abilities.flying);
    }
}
