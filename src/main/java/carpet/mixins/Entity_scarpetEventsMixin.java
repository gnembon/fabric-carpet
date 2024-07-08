package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.fakes.PortalProcessorInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class Entity_scarpetEventsMixin implements EntityInterface
{
    //@Shadow public boolean removed;

    @Shadow private int portalCooldown;

    @Shadow public abstract boolean isRemoved();

    @Shadow private Vec3 position, deltaMovement;

    @Shadow @Nullable public PortalProcessor portalProcess;
    private boolean permanentVehicle;

    private final EntityEventsGroup events = new EntityEventsGroup((Entity) (Object)this);

    private Vec3 pos1, motion;

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

    @Override
    public int getPublicNetherPortalCooldown()
    {
        return portalCooldown;
    }

    @Override
    public void setPublicNetherPortalCooldown(int what)
    {
        portalCooldown = what;
    }

    @Override
    public int getPortalTimer()
    {
        return portalProcess.getPortalTime();
    }

    @Override
    public void setPortalTimer(int amount)
    {
        ((PortalProcessorInterface)portalProcess).setPortalTime(amount);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickCall(CallbackInfo ci)
    {
        events.onEvent(EntityEventsGroup.Event.ON_TICK);
    }


    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(CallbackInfo ci)
    {
        if (!isRemoved()) events.onEvent(EntityEventsGroup.Event.ON_REMOVED);  // ! isRemoved()
    }


    @Inject(method = "setPosRaw", at = @At("HEAD"))
    private void firstPos(CallbackInfo ci)
    {
        pos1 = this.position;
        motion = this.deltaMovement;
    }

    @Inject(method = "setPosRaw", at = @At("TAIL"))
    private void secondPos(CallbackInfo ci)
    {
        if(pos1!=this.position)
            events.onEvent(EntityEventsGroup.Event.ON_MOVE, motion, pos1, this.position);
    }
}
