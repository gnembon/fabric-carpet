package carpet.mixins;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBox;
import net.minecraft.commands.ParseContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandBox.class)
public interface CommandBoxAccessor<C extends ParseContext>
{
    @Accessor("dispatcher")
    CommandDispatcher<C> carpet$getDispatcher();

    @Mutable
    @Accessor("dispatcher")
    void carpet$setDispatcher(CommandDispatcher<C> dispatcher);
}
