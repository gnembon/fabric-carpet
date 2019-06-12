package carpet.mixins;

import carpet.settings.CarpetSettings;
import carpet.fakes.PortalForcerInterface;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(PortalForcer.class)
public class PortalForcer_portalCachingMixin implements PortalForcerInterface
{
    @Shadow @Final private ServerWorld world;
    private Map<ColumnPos, Object> storedTicketInfos = new HashMap<>();

    @Override
    public void invalidateCache()
    {
        storedTicketInfos.clear();
    }

    @Inject(method = "createPortal", at = @At("HEAD"))
    private void portalSpawnedByTheGame(Entity entity_1, CallbackInfoReturnable<Boolean> cir)
    {
        invalidateCache();
    }

    @Redirect(method = "getPortal", at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
    ))
    private Object /* private TicketInfo */ getTicketInfo(Map map, Object /* ColumnPos */ key)
    {
        //Do the dance with ticketinfos. If vanilla has one, we gonna borrow, if vanilla is missing, we supply
        if (!CarpetSettings.portalCaching)
            return map.get(key);
        Object ticketInfo = map.get(key);
        if (ticketInfo != null)
        {
            storedTicketInfos.put((ColumnPos) key, ticketInfo);
            return ticketInfo;
        }
        ticketInfo = storedTicketInfos.get(key);
        if (ticketInfo != null)
        {
            // vanilla got removed, but we stored it meaning we not only need to add it but also let it chunkload
            map.put(key, ticketInfo);
            ColumnPos cpos = (ColumnPos)key;
            BlockPos fakePos = new BlockPos(((ColumnPos) key).x, 0, ((ColumnPos) key).z);
            world.method_14178().addTicket(ChunkTicketType.PORTAL, new ChunkPos(fakePos), 3, (ColumnPos) key);
        }
        return ticketInfo;
    }

    @Redirect(method = "getPortal", at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    ))
    private Object /* TicketInfo */ putNewTicket(Map map, Object /*ColumnPos */ key, Object /*TicketInfo */ value)
    {
        if (CarpetSettings.portalCaching)
            storedTicketInfos.put((ColumnPos) key, value);
        return map.put(key, value);
    }
}
