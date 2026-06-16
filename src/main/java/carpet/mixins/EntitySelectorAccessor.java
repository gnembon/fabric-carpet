package carpet.mixins;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.commands.arguments.selector.EntitySelector;

@Mixin(EntitySelector.class)
public interface EntitySelectorAccessor {
    @Accessor("playerName")
    @Nullable String cm$getPlayerName();
}
