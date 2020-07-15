package carpet.fakes;

import net.minecraft.entity.ai.brain.Memory;
import net.minecraft.entity.ai.brain.MemoryModuleType;

import java.util.Map;
import java.util.Optional;

public interface BrainInterface
{
    Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> getMobMemories();
}
