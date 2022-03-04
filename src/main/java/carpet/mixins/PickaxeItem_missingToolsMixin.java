package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PickaxeItem.class)
public class PickaxeItem_missingToolsMixin extends DiggerItem
{

    protected PickaxeItem_missingToolsMixin(float attackDamage, float attackSpeed, Tier material, TagKey<Block> tag, Properties settings) {
        super(attackDamage, attackSpeed, material, tag, settings);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        Material material = state.getMaterial();
        if (CarpetSettings.missingTools && material == Material.GLASS)
             return speed;
        return super.getDestroySpeed(stack, state);
    }
}
