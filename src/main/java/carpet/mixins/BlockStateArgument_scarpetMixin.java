package carpet.mixins;

import carpet.fakes.BlockStateArgumentInterface;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockStateArgument.class)
public class BlockStateArgument_scarpetMixin implements BlockStateArgumentInterface
{
    @Shadow @Final private @Nullable CompoundTag data;

    @Override
    public CompoundTag getCMTag()
    {
        return data;
    }
}
