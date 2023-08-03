package carpet.fakes;

public interface ItemEntityInterface
{
    default int carpet$getPickupDelay() { throw new UnsupportedOperationException(); }
}
