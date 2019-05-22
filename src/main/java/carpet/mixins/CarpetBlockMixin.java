package carpet.mixins;

import carpet.utils.WoolTool;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarpetBlock;
import net.minecraft.item.ItemPlacementContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CarpetBlock.class)
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
        {
            WoolTool.carpetPlacedAction(((CarpetBlock)(Object)this).getColor(), context.getPlayer(), context.getBlockPos(), context.getWorld());
        }
        return state;
    }
}
