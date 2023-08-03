package carpet.fakes;

import net.minecraft.nbt.CompoundTag;

public interface BlockInputInterface
{
    default CompoundTag carpet$getTag() { throw new UnsupportedOperationException(); }
}
