package carpet.mixins;

import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Style.class)
public interface Style_translationMixin
{
    @Accessor("hoverEvent")
    HoverEvent getHoverEventField$CM();
}
