package carpet.mixins;

import carpet.utils.Translations;
import net.minecraft.network.chat.TranslatableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

@Mixin(TranslatableComponent.class)
public abstract class TranslatableComponent_translationMixin
{
    @Shadow
    @Final
    private String key;

    @ModifyVariable(
            method = "decompose",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/ImmutableList;builder()Lcom/google/common/collect/ImmutableList$Builder;"
            )
    )
    private String applyCarpetTranslation(String vanillaTranslationString)
    {
        if (vanillaTranslationString.equals(this.key))  // vanilla failed to translate the key
        {
            Optional<String> optional = Translations.key2Translation(Translations.getServerLanguage(), this.key);
            if (optional.isEmpty())
            {
                optional = Translations.key2Translation(Translations.DEFAULT_LANGUAGE, this.key);
            }
            if (optional.isPresent())
            {
                return optional.get();
            }
        }
        return vanillaTranslationString;
    }
}
