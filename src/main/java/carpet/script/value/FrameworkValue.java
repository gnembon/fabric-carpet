package carpet.script.value;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;

public abstract class FrameworkValue extends Value
{
    @Override
    public String getString()
    {
        throw new UnsupportedOperationException("Scarpet language component cannot be used");
    }

    @Override
    public boolean getBoolean()
    {
        throw new UnsupportedOperationException("Scarpet language component cannot be used");
    }

    @Override
    public Value clone()
    {
        throw new UnsupportedOperationException("Scarpet language component cannot be used");
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException("Scarpet language component cannot be used as map key");
    }

    @Override
    public Tag toTag(boolean force, RegistryAccess regs)
    {
        throw new UnsupportedOperationException("Scarpet language component cannot be serialized to the tag");
    }
}
