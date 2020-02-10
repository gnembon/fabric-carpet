package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class Entity_scarpetEventsMixin implements EntityInterface
{
    @Shadow public boolean removed;

    private boolean permanentVehicle;

    private final EntityEventsGroup events = new EntityEventsGroup();

    @Override
    public EntityEventsGroup getEventContainer()
    {
        return events;
    }

    @Override
    public boolean isPermanentVehicle()
    {
        return permanentVehicle;
    }

    @Override
    public void setPermanentVehicle(boolean permanent)
    {
        permanentVehicle = permanent;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickCall(CallbackInfo ci)
    {
        events.onEvent(EntityEventsGroup.EntityEventType.ON_TICK, (Entity) (Object) this);
    }


    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(CallbackInfo ci)
    {
        if (!removed) events.onEvent(EntityEventsGroup.EntityEventType.ON_REMOVED, (Entity) (Object) this);
    }


}
