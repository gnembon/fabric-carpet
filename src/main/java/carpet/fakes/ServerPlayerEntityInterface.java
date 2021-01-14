package carpet.fakes;

import carpet.helpers.EntityPlayerActionPack;

public interface ServerPlayerEntityInterface
{
    EntityPlayerActionPack getActionPack();
    void invalidateEntityObjectReference();
    boolean isInvalidEntityObject();

}
