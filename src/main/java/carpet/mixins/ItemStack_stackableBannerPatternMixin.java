package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ItemStack.class)
public class ItemStack_stackableBannerPatternMixin {
    // Create a static set of all banner pattern items for efficient lookup
    @Unique
    private static final Set<net.minecraft.world.item.Item> BANNER_PATTERN_ITEMS = Set.of(
            Items.CREEPER_BANNER_PATTERN,
            Items.SKULL_BANNER_PATTERN,
            Items.FLOWER_BANNER_PATTERN,
            Items.MOJANG_BANNER_PATTERN,
            Items.GLOBE_BANNER_PATTERN,
            Items.PIGLIN_BANNER_PATTERN,
            Items.FLOW_BANNER_PATTERN,
            Items.GUSTER_BANNER_PATTERN,
            Items.FIELD_MASONED_BANNER_PATTERN,
            Items.BORDURE_INDENTED_BANNER_PATTERN
    );

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void getCMMAxStackSize(CallbackInfoReturnable<Integer> cir)
    {
        if (CarpetSettings.bannerPatternStackSize > 1) {
            ItemStack thisStack = (ItemStack) (Object) this;
            if (BANNER_PATTERN_ITEMS.contains(thisStack.getItem())) {
                cir.setReturnValue(CarpetSettings.bannerPatternStackSize);
            }
        }
    }
}