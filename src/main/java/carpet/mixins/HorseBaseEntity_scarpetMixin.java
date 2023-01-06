package carpet.mixins;

import carpet.fakes.InventoryBearerInterface;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractHorse.class)
public class HorseBaseEntity_scarpetMixin implements InventoryBearerInterface
{

    @Shadow protected SimpleContainer inventory;

    @Override
    public Container getCMInventory()
    {
        return inventory;
    }
}
