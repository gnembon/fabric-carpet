package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntity_scarpetEventsMixin extends Entity
{
    public FallingBlockEntity_scarpetEventsMixin(EntityType<?> type, Level world)
    {
        super(type, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickCall(CallbackInfo ci)
    {
        // calling extra on_tick because falling blocks do not fall back to super tick call
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_TICK);
    }
}
