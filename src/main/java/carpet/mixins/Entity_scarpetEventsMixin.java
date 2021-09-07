package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.script.EntityEventsGroup;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class Entity_scarpetEventsMixin implements EntityInterface
{
    //@Shadow public boolean removed;

    @Shadow protected int netherPortalTime;
    @Shadow private int netherPortalCooldown;

    @Shadow public abstract boolean isRemoved();

    @Shadow private Vec3d pos, velocity;

    private boolean permanentVehicle;

    private final EntityEventsGroup events = new EntityEventsGroup((Entity) (Object)this);

    private Vec3d pos1, motion;

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
        return netherPortalCooldown;
    }

    @Override
    public void setPublicNetherPortalCooldown(int what)
    {
        netherPortalCooldown = what;
    }

    @Override
    public int getPortalTimer()
    {
        return netherPortalTime;
    }

    @Override
    public void setPortalTimer(int amount)
    {
        netherPortalTime = amount;
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


    @Inject(method = "setPos", at = @At("HEAD"))
    private void firstPos(CallbackInfo ci)
    {
        pos1 = this.pos;
        motion = this.velocity;
    }

    @Inject(method = "setPos", at = @At("TAIL"))
    private void secondPos(CallbackInfo ci)
    {
        events.onEvent(EntityEventsGroup.Event.ON_MOVE,
        motion.x, motion.y, motion.z,
        pos1.x, pos1.y, pos1.z,
        this.pos.x, this.pos.y, this.pos.z);
    }
}
