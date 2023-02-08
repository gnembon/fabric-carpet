package carpet.script.value;

public class LContainerValue extends FrameworkValue
{
    private final ContainerValueInterface container;
    private final Value address;
    public static final LContainerValue NULL_CONTAINER = new LContainerValue(null, null);

    public LContainerValue(ContainerValueInterface c, Value v)
    {
        container = c;
        address = v;
    }

    public ContainerValueInterface container()
    {
        return container;
    }

    public Value address()
    {
        return address;
    }
}
