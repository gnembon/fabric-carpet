package carpet.mixins;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.pathfinder.Node;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragon.class)
public interface EnderDragon_scarpetMixin {
    @Accessor
    Node[] getNodes();

    @Accessor
    int[] getNodeAdjacency();
}
