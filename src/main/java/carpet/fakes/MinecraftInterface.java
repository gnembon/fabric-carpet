package carpet.fakes;

import carpet.helpers.TickRateManager;

import java.util.Optional;

public interface MinecraftInterface
{
    default Optional<TickRateManager> carpet$getTickRateManager() { throw new UnsupportedOperationException(); }

    default float carpet$getPausePartialTick() { throw new UnsupportedOperationException(); }
}
