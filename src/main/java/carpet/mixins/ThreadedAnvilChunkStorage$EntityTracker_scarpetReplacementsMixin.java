package carpet.mixins;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.CarpetSettings;
import carpet.script.utils.CarpetFakeReplacementEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;

@Mixin(targets = "net.minecraft.server.world.ThreadedAnvilChunkStorage$EntityTracker")
public class ThreadedAnvilChunkStorage$EntityTracker_scarpetReplacementsMixin {
    //private boolean isCarpetReplacementEntity;
    @Shadow @Final Entity entity;
    @Shadow @Final @Mutable Set<ServerPlayerEntity> playersTracking;
    
    @Inject(method = "<init>", at = @At("RETURN"))
    public void checkType(ThreadedAnvilChunkStorage tacs, Entity entity, int maxDistance, int tickInterval, boolean bl, CallbackInfo ci)
    {
        if (entity instanceof CarpetFakeReplacementEntity)
        {
            //isCarpetReplacementEntity = true;
            playersTracking = new CarpetFilteredHashSet<>(playersTracking, ((CarpetFakeReplacementEntity)entity).getPlayersToSend());
        }
    }
    
    public static class CarpetFilteredHashSet<T> extends HashSet<T>
    {
        private final Set<T> filter;
        public CarpetFilteredHashSet(Set<T> current, Set<T> filter) {
            super(current);
            this.filter = filter;
        }
        @Override
        public boolean add(T e) {
            return filter.contains(e) && super.add(e);
        }
    }
}
