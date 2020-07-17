package carpet.mixins;

import carpet.CarpetSettings;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.gen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PhantomSpawner.class)
public class PhantomEntityMixin {
    @Inject(method = "spawn", at = @At("HEAD"), cancellable = true)
    public void checkMobCap(ServerWorld serverWorld, boolean spawnMonsters, boolean spawnAnimals, CallbackInfoReturnable<Integer> cir) {
        SpawnHelper.Info spawnInfo = serverWorld.getChunkManager().getSpawnInfo();
        Object2IntMap<SpawnGroup> count = spawnInfo.getGroupToCount();


        if(CarpetSettings.phantomsRespectMobCap && count.getOrDefault(SpawnGroup.MONSTER, -1) < SpawnGroup.MONSTER.getCapacity()) {
            cir.setReturnValue(0);
            cir.cancel();
        }
    }
}
