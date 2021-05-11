package carpet.script.value;

public class LContainerValue extends FrameworkValue
{
    private ContainerValueInterface container;
    private Value address;
    public static final LContainerValue NULL_CONTAINER = new LContainerValue(null, null);

    public LContainerValue(ContainerValueInterface c, Value v)
    {
        container = c;
        address = v;
    }

    public ContainerValueInterface getContainer() {return container; }
    public Value getAddress() {return address; }
}
