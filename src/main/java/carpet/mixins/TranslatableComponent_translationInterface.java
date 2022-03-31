package carpet.mixins;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TranslatableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

@Mixin(TranslatableComponent.class)
public interface TranslatableComponent_translationInterface
{
    @Invoker
    void invokeDecomposeTemplate(String string, Consumer<FormattedText> consumer);
}
