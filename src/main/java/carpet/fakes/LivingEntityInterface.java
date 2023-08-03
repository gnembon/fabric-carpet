package carpet.fakes;

public interface LivingEntityInterface
{
    default void carpet$doJump() { throw new UnsupportedOperationException(); }

    default boolean carpet$isJumping() { throw new UnsupportedOperationException(); }
}
