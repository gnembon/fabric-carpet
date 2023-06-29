package carpet.mixins;

import carpet.fakes.BlockStateArgumentInterface;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockInput.class)
public class BlockInput_scarpetMixin implements BlockStateArgumentInterface
{
    @Shadow @Final private @Nullable CompoundTag tag;

    @Override
    public CompoundTag getCMTag()
    {
        return tag;
    }
}
