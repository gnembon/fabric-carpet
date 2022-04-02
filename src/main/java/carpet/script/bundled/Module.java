package carpet.script.bundled;

/**
 * @deprecated Use {@link carpet.script.Module} instead
 *
 */
@Deprecated(forRemoval = true)
public abstract class Module
{
    public abstract String getName();
    public abstract String getCode();
    public abstract boolean isLibrary();
    
    public carpet.script.Module toModule()
    {
    	return new carpet.script.Module(getName(), getCode(), isLibrary());
    }

    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }
}
