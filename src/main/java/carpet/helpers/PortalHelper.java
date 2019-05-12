package carpet.helpers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;

public class PortalHelper
{
    public static boolean player_holds_obsidian(PlayerEntity playerIn)
    {
        if ( (!playerIn.getMainHandStack().isEmpty()
            && playerIn.getMainHandStack().getItem() instanceof BlockItem &&
            ((BlockItem)(playerIn.getMainHandStack().getItem())).getBlock() == Blocks.OBSIDIAN   ))
        {
            return true;
        }
        return false;
    }
}
