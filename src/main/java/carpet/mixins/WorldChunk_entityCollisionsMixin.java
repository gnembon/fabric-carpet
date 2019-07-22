package carpet.mixins;

import carpet.fakes.TypeFilterableListInterface;
import carpet.fakes.WorldChunkInterface;
import net.minecraft.entity.Entity;
import net.minecraft.util.TypeFilterableList;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldChunk.class)
public abstract class WorldChunk_entityCollisionsMixin implements WorldChunkInterface
{
    @Shadow @Final private TypeFilterableList<Entity>[] entitySections;
    boolean modified = false;

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void dirtyAdd(Entity entity_1, CallbackInfo ci) { modified = true; }

    @Inject(method = "remove(Lnet/minecraft/entity/Entity;I)V", at = @At("HEAD"))
    private void dirtyRemove(Entity entity_1, int int_1, CallbackInfo ci) { modified = true; }

    int[] entityStarts = new int[17];


    @Override
    public int getEntityCount(int from, int to)
    {
        boolean needsUpdate = checkModified();
        if (needsUpdate)
        {
            int prevSize = 0;
            for(int i = 0; i < 16; i++)
            {
                entityStarts[i] = prevSize;
                prevSize += entitySections[i].size();
            }
            entityStarts[16] = prevSize;
        }
        return entityStarts[to+1]-entityStarts[from];
    }

    @Override
    public Entity getEntityAtIndex(int index, int from, int to)
    { // assumes no entity got added/removed between last call to  getEntityCount and this
        index += entityStarts[from];
        for (int chunkId = from; chunkId <= to; chunkId++)
        {
            if (index < entityStarts[chunkId+1])
            {
                return ((TypeFilterableListInterface<Entity>)entitySections[chunkId]).getAtIndex(index - entityStarts[chunkId]);
            }
        }
        //should neve happen
        return null;
    }

    public boolean checkModified()
    {
        boolean res = modified;
        modified = false;
        return res;
    };
}
