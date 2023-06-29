package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Display.class)
public abstract class Display_scarpetEventMixin extends Entity
{
    public Display_scarpetEventMixin(final EntityType<?> entityType, final Level level)
    {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickCall(CallbackInfo ci)
    {
        // calling extra on_tick because displays don't tick
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_TICK);
    }
}
