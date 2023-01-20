package carpet.helpers;

import carpet.CarpetSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class QuasiConnectivity {

    public static boolean hasQuasiSignal(Level level, BlockPos pos) {
        for (int i = 1; i <= CarpetSettings.quasiConnectivity; i++) {
            BlockPos above = pos.above(i);

            if (level.isOutsideBuildHeight(above)) {
                break;
            }
            if (level.hasNeighborSignal(above)) {
                return true;
            }
        }

        return false;
    }
}
