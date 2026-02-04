package carpet.fakes;

import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemorySlot;

import java.util.Map;

public interface BrainInterface {
    Map<MemoryModuleType<?>, MemorySlot<?>> getMemoriesCM();
}
