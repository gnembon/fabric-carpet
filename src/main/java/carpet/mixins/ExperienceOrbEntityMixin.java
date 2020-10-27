package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.ExperienceOrbInterface;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin implements ExperienceOrbInterface
{
    @Shadow private int amount;

    public void setAmount(int amount) { this.amount = amount; }



    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    void removeDelay(PlayerEntity playerEntity_1, CallbackInfo ci)
    {
        if (CarpetSettings.xpNoCooldown)
            playerEntity_1.experiencePickUpDelay = 0;
    }

}
