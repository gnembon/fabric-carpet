package carpet.mixins;

import carpet.fakes.InventoryBearerInterface;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PillagerEntity.class)
public class PillagerEntity_scarpetMixin implements InventoryBearerInterface
{
    @Shadow @Final private SimpleInventory inventory;

    @Override
    public Inventory getCMInventory()
    {
        return inventory;
    }
}
