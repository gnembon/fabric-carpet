package carpet.mixins;

import net.minecraft.MinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MinecraftVersion.class)
public class __MinecraftVersion_honeyMixin
{
    /**
     * @author gnembon
     * @reason because
     */
    @Overwrite
    public String getName() {
        return "19w41b";
    }
}
