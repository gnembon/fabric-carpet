package carpet.mixins;

import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCooldowns.CooldownInstance.class)
public interface CooldownInstance_scarpetEntityMixin {
    @Accessor("startTime")
    int getStartTime();

    @Accessor("endTime")
    int getEndTime();
}
