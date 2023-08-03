package carpet.fakes;

import carpet.helpers.EntityPlayerActionPack;

public interface ServerPlayerInterface
{
    default EntityPlayerActionPack carpet$getActionPack() { throw new UnsupportedOperationException(); }

    default void carpet$invalidateEntityObjectReference() { throw new UnsupportedOperationException(); }

    default boolean carpet$isInvalidEntityObject() { throw new UnsupportedOperationException(); }

    default String carpet$getLanguage() { throw new UnsupportedOperationException(); }
}
