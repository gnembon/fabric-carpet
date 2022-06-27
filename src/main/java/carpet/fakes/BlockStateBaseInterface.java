package carpet.fakes;

import carpet.helpers.PistonMoveBehaviorManager.PistonMoveBehavior;

public interface BlockStateBaseInterface {

    public boolean canChangePistonMoveBehaviorOverride();

    public PistonMoveBehavior getPistonMoveBehaviorOverride();

    public void setPistonMoveBehaviorOverride(PistonMoveBehavior behavior);

    public PistonMoveBehavior getDefaultPistonMoveBehaviorOverride();

    public void setDefaultPistonMoveBehaviorOverride(PistonMoveBehavior behavior);

}
