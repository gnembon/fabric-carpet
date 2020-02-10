package carpet.fakes;

import carpet.script.EntityEventsGroup;

public interface EntityInterface
{
    float getMainYaw(float partialTicks);

    EntityEventsGroup getEventContainer();

    boolean isPermanentVehicle();

    void setPermanentVehicle(boolean permanent);
}
