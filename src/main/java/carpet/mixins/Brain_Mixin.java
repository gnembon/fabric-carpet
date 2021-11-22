package carpet.mixins;

import carpet.fakes.BrainInterface;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.Memory;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Optional;

@Mixin(Brain.class)
public class Brain_Mixin implements BrainInterface
{

    @Shadow @Final private Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> memories;

    @Override
    public Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> getMobMemories()
    {
        return memories;
    }
}
