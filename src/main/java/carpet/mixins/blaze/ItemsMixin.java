package carpet.mixins.blaze;

import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Items.class)
public abstract class ItemsMixin
{
    @Shadow protected static Item register(Identifier id, Item item) { return null;};

    @Inject(method = "register(Ljava/lang/String;Lnet/minecraft/item/Item;)Lnet/minecraft/item/Item;", at = @At("HEAD"), cancellable = true)
    private static void registerBlazeEgg(String id, Item item, CallbackInfoReturnable<Item> cir)
    {
        if (id.equals("blaze_spawn_egg"))
        {
            cir.setReturnValue(register(
                    new Identifier(id),
                    (Item)(new SpawnEggItem(EntityType.BLAZE, 12369084,16382457, (new Item.Settings()).group(ItemGroup.MISC))
                    )
            ));
        }
    }
}
