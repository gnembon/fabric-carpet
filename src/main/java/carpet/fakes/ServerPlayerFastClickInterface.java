package carpet.fakes;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public interface ServerPlayerFastClickInterface
{
    void saveOldPosRot(Vec3 pos, Vec2 rot);

    void swapOldPosRot(boolean lastTickValues);

}
