package carpet.helpers;

import net.minecraft.util.math.BlockPos;

public class ThrowableSuppression extends RuntimeException{

    public final BlockPos pos;
    public ThrowableSuppression(String message, BlockPos pos) {
        super(message);
        this.pos = pos;
    }
}