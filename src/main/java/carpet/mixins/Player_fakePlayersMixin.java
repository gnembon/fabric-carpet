package carpet.mixins;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class Player_fakePlayersMixin
{
    /**
     * To make sure player attacks are able to knockback fake players
     */
    @Redirect(
            method = "attack",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Entity;hurtMarked:Z",
                    ordinal = 0
            )
    )
    private boolean velocityModifiedAndNotCarpetFakePlayer(Entity target)
    {
        return target.hurtMarked && !(target instanceof EntityPlayerMPFake);
    }
}
