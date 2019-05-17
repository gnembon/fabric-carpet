package carpet.fakes;

import java.util.function.BooleanSupplier;

public interface MinecraftServerInterface
{
    void forceTick(BooleanSupplier sup);
}
