package carpet.mixins;

import carpet.fakes.DummyEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Enchantments.class)
public abstract class __Enchantments_Mixin
{

    @Shadow private static Enchantment register(String string_1, Enchantment enchantment_1) {return null;}

    static
    {
        register("dummy1", new DummyEnchantments.Dummy1());
        register("dummy2", new DummyEnchantments.Dummy2());
        register("dummy3", new DummyEnchantments.Dummy3());
        register("dummy4", new DummyEnchantments.Dummy4());
        register("dummy5", new DummyEnchantments.Dummy5());
        register("dummy6", new DummyEnchantments.Dummy6());
        register("dummy7", new DummyEnchantments.Dummy7());
        register("dummy8", new DummyEnchantments.Dummy8());
        register("dummy9", new DummyEnchantments.Dummy9());
        register("dummy10", new DummyEnchantments.Dummy10());
        register("dummy11", new DummyEnchantments.Dummy11());
        register("dummy12", new DummyEnchantments.Dummy12());
        register("dummy13", new DummyEnchantments.Dummy13());
        register("dummy14", new DummyEnchantments.Dummy14());
        register("dummy15", new DummyEnchantments.Dummy15());
        register("dummy16", new DummyEnchantments.Dummy16());
        register("dummy17", new DummyEnchantments.Dummy17());
        register("dummy18", new DummyEnchantments.Dummy18());
        register("dummy19", new DummyEnchantments.Dummy19());
        register("dummy20", new DummyEnchantments.Dummy20());
        register("dummy21", new DummyEnchantments.Dummy21());
        register("dummy22", new DummyEnchantments.Dummy22());
        register("dummy23", new DummyEnchantments.Dummy23());
        register("dummy24", new DummyEnchantments.Dummy24());
        register("dummy25", new DummyEnchantments.Dummy25());
        register("dummy26", new DummyEnchantments.Dummy26());
        register("dummy27", new DummyEnchantments.Dummy27());
        register("dummy28", new DummyEnchantments.Dummy28());
        register("dummy29", new DummyEnchantments.Dummy29());
        register("dummy30", new DummyEnchantments.Dummy30());
        register("dummy31", new DummyEnchantments.Dummy31());
        register("dummy32", new DummyEnchantments.Dummy32());
    }
}
