package carpet.fakes;

import carpet.helpers.EntityPlayerActionPack;

public interface ServerPlayerInterface
{
    EntityPlayerActionPack getActionPack();
    void invalidateEntityObjectReference();
    boolean isInvalidEntityObject();

    interface ShadowPlayerInterface
    {
        default void fabric_carpet$shadowBeforeDisconnect()
        {
            throw new AssertionError();
        }

        default boolean fabric_carpet$shouldShadow()
        {
            throw new AssertionError();
        }
    }
}
