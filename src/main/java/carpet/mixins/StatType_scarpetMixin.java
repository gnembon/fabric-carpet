package carpet.mixins;

import carpet.fakes.StatTypeInterface;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(StatType.class)
public class StatType_scarpetMixin<T> implements StatTypeInterface<T>
{
    @Shadow @Final private Map<T, Stat<T>> stats;

    @Override
    public boolean hasStatCreated(T key)
    {
        return this.stats.containsKey(key);
    }
}
