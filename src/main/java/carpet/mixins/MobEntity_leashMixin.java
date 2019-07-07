package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntity_leashMixin
{
    @Shadow /*@Nullable*/ private Entity holdingEntity;

    @Shadow /*@Nullable*/ private CompoundTag leashTag;

    @Inject(method = "writeCustomDataToTag", at = @At("TAIL"))
    private void addLeashData(CompoundTag compoundTag_1, CallbackInfo ci)
    {
        if (holdingEntity == null && CarpetSettings.leadFix && leashTag != null)
        {
            compoundTag_1.put("Leash", leashTag);
        }
    }
}
