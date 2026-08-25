package carpet.fakes;

import carpet.helpers.EntityPlayerActionPack;

public interface ServerPlayerInterface
{
    EntityPlayerActionPack getActionPack();
    void invalidateEntityObjectReference();
    boolean isInvalidEntityObject();

    interface ShadowPlayerInterface
    {
        default void carpet$shadowAfterDisconnect()
        {
            throw new AssertionError();
        }

        default boolean carpet$shouldShadow()
        {
            throw new AssertionError();
        }
    }
}
