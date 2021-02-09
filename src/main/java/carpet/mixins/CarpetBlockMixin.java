package carpet.mixins;

import carpet.utils.WoolTool;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DyedCarpetBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DyedCarpetBlock.class) // WoolCarpetBlock
public abstract class CarpetBlockMixin extends Block
{

    public CarpetBlockMixin(Settings block$Settings_1)
    {
        super(block$Settings_1);
    }

    public BlockState getPlacementState(ItemPlacementContext context)
    {
        BlockState state = super.getPlacementState(context);
        if (context.getPlayer() != null && !context.getWorld().isClient)
        { // getColor()
            WoolTool.carpetPlacedAction(((DyedCarpetBlock)(Object)this).getDyeColor(), context.getPlayer(), context.getBlockPos(), (ServerWorld) context.getWorld());
        }
        return state;
    }
}
