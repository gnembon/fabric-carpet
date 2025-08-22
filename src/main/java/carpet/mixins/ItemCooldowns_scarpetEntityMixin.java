package carpet.mixins;

import carpet.fakes.ItemCooldownsInterface;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(ItemCooldowns.class)
public class ItemCooldowns_scarpetEntityMixin implements ItemCooldownsInterface {
    @Shadow @Final private Map<Item, ItemCooldowns.CooldownInstance> cooldowns;

    @Shadow private int tickCount;

    @Override
    public int getCooldownTicks(Item item) {
        if(!cooldowns.containsKey(item)) return 0;

        var cooldown = ((CooldownInstanceAccessor) cooldowns.get(item));
        return cooldown.getEndTime()-tickCount;
    }
}
