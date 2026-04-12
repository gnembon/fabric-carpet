package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import carpet.fakes.ItemEntityInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements ItemEntityInterface
{
    @Shadow private int age;
    @Shadow private int pickupDelay;

    public ItemEntityMixin(EntityType<?> entityType_1, Level world_1) {
        super(entityType_1, world_1);
    }

    @Override
    public void thunderHit(ServerLevel world, LightningBolt lightning) {
        if (CarpetSettings.lightningKillsDropsFix) {
            if (this.age > 8) { //Only kill item if it's older than 8 ticks
                super.thunderHit(world, lightning);
            }
        } else {
            super.thunderHit(world, lightning);
        }
    }

    @Override
    public int getPickupDelayCM() {
        return this.pickupDelay;
    }
}
