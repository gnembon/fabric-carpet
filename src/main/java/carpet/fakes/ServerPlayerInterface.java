package carpet.fakes;

import carpet.helpers.EntityPlayerActionPack;

public interface ServerPlayerInterface
{
    EntityPlayerActionPack getActionPack();
    void invalidateEntityObjectReference();
    boolean isInvalidEntityObject();
    String getLanguage();
}
