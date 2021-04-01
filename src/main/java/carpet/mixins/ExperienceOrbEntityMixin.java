package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.ExperienceOrbInterface;
import carpet.helpers.XPcombine;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin implements ExperienceOrbInterface
{
    @Shadow private int amount;

    @Shadow private int pickingCount;

    @Shadow protected abstract int getMendingRepairCost(int repairAmount);

    @Shadow protected abstract int getMendingRepairAmount(int experienceAmount);

    public void setAmount(int amount) { this.amount = amount; }

    public int combineDelay = 50;

    public int getCombineDelay() { return combineDelay; }
    public void setCombineDelay(int what) { combineDelay = what; }

    @Inject(method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ExperienceOrbEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V",
                    shift = At.Shift.AFTER
            ))
    void checkCombineAtTick(CallbackInfo ci)
    {
        if (CarpetSettings.combineXPOrbs)
        {
            if (getCombineDelay() > 0)
            {
                setCombineDelay(getCombineDelay()-1);
            }

            if (getCombineDelay() == 0)
            {
                XPcombine.searchForOtherXPNearby((ExperienceOrbEntity) (Object) this);
            }
        }
    }

    @Override
    public int getCount() {
        return pickingCount;
    }

    @Override
    public void setCount(int i) {
        pickingCount = i;
    }

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    void removeDelay(PlayerEntity playerEntity_1, CallbackInfo ci)
    {
        if (CarpetSettings.xpNoCooldown)
            playerEntity_1.experiencePickUpDelay = 0;
    }

    @Redirect(method = "onPlayerCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addExperience(I)V"))
    void addXP(PlayerEntity playerEntity, int experience)
    {
        playerEntity.addExperience(experience);
        if (CarpetSettings.xpNoCooldown) while(pickingCount > 1)
        {
            pickingCount --;
            playerEntity.addExperience(experience);
        }
    }
}
