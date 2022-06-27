package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BuddingAmethystBlock.class)
public class BuddingAmethystBlock_harvestableAmethystMixin extends Block {
    public BuddingAmethystBlock_harvestableAmethystMixin(Properties settings) {
        super(settings);
    }

    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        super.playerDestroy(world, player, pos, state, blockEntity, stack);
        // doing it here rather than though loottables since loottables are loaded on reload
        // drawback - not controlled via loottables, but hey
        if (CarpetSettings.harvestableAmethyst &&
                stack.getItem() instanceof PickaxeItem &&
                EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0
        )
            popResource(world, pos, Items.BUDDING_AMETHYST.getDefaultInstance());
    }
}
