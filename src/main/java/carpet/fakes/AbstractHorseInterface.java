package carpet.fakes;

import net.minecraft.world.Container;

public interface AbstractHorseInterface
{
    default Container carpet$getInventory() { throw new UnsupportedOperationException(); }
}
