package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PickaxeItem.class)
public class PickaxeItem_missingToolsMixin extends DiggerItem
{

    protected PickaxeItem_missingToolsMixin(Tier material, TagKey<Block> tag, Properties settings) {
        super(material, tag, settings);
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
