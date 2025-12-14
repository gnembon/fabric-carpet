package carpet.fakes;

import java.util.stream.Stream;

import carpet.script.EntityEventsGroup;
import net.minecraft.world.entity.Entity;

public interface EntityInterface
{
    float getMainYaw(float partialTicks);

    EntityEventsGroup getEventContainer();

    boolean isPermanentVehicle();

    void setPermanentVehicle(boolean permanent);

    int getPortalTimer();

    void setPortalTimer(int amount);

    int getPublicNetherPortalCooldown();
    void setPublicNetherPortalCooldown(int what);

    Stream<Entity> cm$getIndirectPassengersStream();
}
