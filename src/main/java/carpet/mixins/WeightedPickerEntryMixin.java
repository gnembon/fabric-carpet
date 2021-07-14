package carpet.mixins;

import net.minecraft.util.collection.WeightedPicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WeightedPicker.Entry.class)
public interface WeightedPickerEntryMixin
{/* /// not needed in 1.17
    @Accessor("weight")
    int getWeight();*/
}
