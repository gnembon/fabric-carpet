package carpet.fakes;

import carpet.script.EntityEventsGroup;

public interface EntityInterface
{
    default float carpet$getMainYaw(float partialTicks) { throw new UnsupportedOperationException(); }

    default EntityEventsGroup carpet$getEventContainer() { throw new UnsupportedOperationException(); }

    default boolean carpet$isPermanentVehicle() { throw new UnsupportedOperationException(); }

    default void carpet$setPermanentVehicle(boolean permanent) { throw new UnsupportedOperationException(); }

    default int carpet$getPortalTimer() { throw new UnsupportedOperationException(); }

    default void carpet$setPortalTimer(int amount) { throw new UnsupportedOperationException(); }

    default int carpet$getPublicNetherPortalCooldown() { throw new UnsupportedOperationException(); }

    default void carpet$setPublicNetherPortalCooldown(int what) { throw new UnsupportedOperationException(); }
}
