package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PickaxeItem.class)
public class PickaxeItem_missingToolsMixin extends DiggerItem
{


    protected PickaxeItem_missingToolsMixin(final ToolMaterial toolMaterial, final TagKey<Block> tagKey, final float f, final float g, final Properties properties)
    {
        super(toolMaterial, tagKey, f, g, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (CarpetSettings.missingTools && state.getSoundType() == SoundType.GLASS)
        {
            final Tool tool = stack.get(DataComponents.TOOL);
            return tool != null ? tool.getMiningSpeed(Blocks.STONE.defaultBlockState()) : super.getDestroySpeed(stack, state);
        }
        return super.getDestroySpeed(stack, state);
    }
}
