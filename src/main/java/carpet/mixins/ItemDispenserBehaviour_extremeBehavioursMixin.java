package carpet.mixins;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemDispenserBehavior.class)
public class ItemDispenserBehaviour_extremeBehavioursMixin
{
    @Redirect(method = "spawnItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ItemEntity;setVelocity(DDD)V"
    ))
    private static void setExtremeVelocity(ItemEntity itemEntity, double x, double y, double z,
                                           World world, ItemStack stack, int offset, Direction side, Position pos)
    {
        if (CarpetSettings.extremeBehaviours)
        {
            double g = CarpetServer.rand.nextDouble() * 0.1D + 0.2D;
            itemEntity.setVelocity(
                    (16*world.random.nextDouble()-8) * 0.007499999832361937D * (double) offset + (double) side.getOffsetX() * g,
                    (16*world.random.nextDouble()-8) * 0.007499999832361937D * (double) offset + 0.20000000298023224D,
                    (16*world.random.nextDouble()-8) * 0.007499999832361937D * (double) offset + (double) side.getOffsetZ() * g
            );
        }
        else
        {
            itemEntity.setVelocity(x, y, z);
        }
    }

}
