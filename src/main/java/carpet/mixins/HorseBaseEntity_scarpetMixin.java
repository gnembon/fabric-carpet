package carpet.mixins;

import carpet.fakes.InventoryBearerInterface;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HorseBaseEntity.class)
public class HorseBaseEntity_scarpetMixin implements InventoryBearerInterface
{

    @Shadow protected SimpleInventory items;

    @Override
    public Inventory getCMInventory()
    {
        return items;
    }
}
