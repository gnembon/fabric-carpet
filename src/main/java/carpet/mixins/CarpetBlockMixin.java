package carpet.mixins;

import carpet.utils.WoolTool;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarpetBlock;
import net.minecraft.class_5815;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(class_5815.class) // WoolCarpetBlock
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
            WoolTool.carpetPlacedAction(((class_5815)(Object)this).method_33635(), context.getPlayer(), context.getBlockPos(), (ServerWorld) context.getWorld());
        }
        return state;
    }
}
