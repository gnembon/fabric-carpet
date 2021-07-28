package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BuddingAmethystBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BuddingAmethystBlock.class)
public class BuddingAmethystBlock_movableAmethystMixin extends Block {
    public BuddingAmethystBlock_movableAmethystMixin(Settings settings) {
        super(settings);
    }

    @Inject(at = @At("HEAD"), method = "getPistonBehavior(Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/piston/PistonBehavior;", cancellable = true)
    void getPistonBehavior(BlockState state, CallbackInfoReturnable<PistonBehavior> cir) {
        if (CarpetSettings.movableAmethyst) cir.setReturnValue(PistonBehavior.NORMAL);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        super.afterBreak(world, player, pos, state, blockEntity, stack);
        // doing it here rather than though loottables since loottables are loaded on reload
        // drawback - not controlled via loottables, but hey
        if (CarpetSettings.movableAmethyst &&
                stack.getItem() instanceof PickaxeItem &&
                EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, stack) > 0
        )
            dropStack(world, pos, Items.BUDDING_AMETHYST.getDefaultStack());
    }
}
