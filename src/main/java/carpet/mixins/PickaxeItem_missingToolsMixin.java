package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.tag.Tag;
import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PickaxeItem.class)
public class PickaxeItem_missingToolsMixin extends MiningToolItem
{

    protected PickaxeItem_missingToolsMixin(float attackDamage, float attackSpeed, ToolMaterial material, Tag<Block> tag, Settings settings) {
        super(attackDamage, attackSpeed, material, tag, settings);
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        Material material = state.getMaterial();
        if (CarpetSettings.missingTools && material == Material.GLASS)
             return miningSpeed;
        return super.getMiningSpeedMultiplier(stack, state);
    }
}
