package carpet.mixins;

import carpet.fakes.BrainInterface;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemorySlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(Brain.class)
public class Brain_scarpetMixin implements BrainInterface {
    @Shadow @Final
    private Map<MemoryModuleType<?>, MemorySlot<?>> memories;

    @Override
    public Map<MemoryModuleType<?>, MemorySlot<?>> getMemoriesCM() {
        return memories;
    }
}
