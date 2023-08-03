package carpet.fakes;

public interface MobCategoryInterface
{
    default int carpet$getInitialSpawnCap() { throw new UnsupportedOperationException(); }
}
