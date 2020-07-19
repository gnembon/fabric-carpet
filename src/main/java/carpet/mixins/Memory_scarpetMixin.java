package carpet.mixins;

import carpet.fakes.MemoryInterface;
import net.minecraft.entity.ai.brain.Memory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Memory.class)
public class Memory_scarpetMixin implements MemoryInterface
{
    @Shadow private long expiry;

    @Override
    public long getScarpetExpiry()
    {
        return expiry;
    }
}
