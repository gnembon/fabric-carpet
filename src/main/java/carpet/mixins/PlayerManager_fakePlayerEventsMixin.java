package carpet.mixins;

import carpet.patches.EntityPlayerMPFake;
import carpet.patches.NetHandlerPlayServerFake;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerManager_fakePlayerEventsMixin
{
    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void onPlaceNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci)
    {
        // Fire Fabric API JOIN event for fake players
        if (player instanceof EntityPlayerMPFake && player.connection instanceof NetHandlerPlayServerFake)
        {
            try
            {
                Class<?> eventClass = Class.forName("net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents");
                Object joinEvent = eventClass.getField("JOIN").get(null);
                Object invoker = joinEvent.getClass().getMethod("invoker").invoke(joinEvent);
                
                java.lang.reflect.Method method = null;
                for (java.lang.reflect.Method m : invoker.getClass().getMethods()) {
                    if (m.getName().equals("onPlayReady")) {
                        method = m;
                        break;
                    }
                }
                
                if (method != null) {
                    method.setAccessible(true);
                    method.invoke(invoker, player.connection, null, ((PlayerList)(Object)this).getServer());
                }
            }
            catch (Exception ignored)
            {
                // Fabric API not available or event failed
            }
        }
    }
    
    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemovePlayer(ServerPlayer player, CallbackInfo ci)
    {
        // Fire Fabric API DISCONNECT event for fake players
        if (player instanceof EntityPlayerMPFake && player.connection instanceof NetHandlerPlayServerFake)
        {
            try
            {
                Class<?> eventClass = Class.forName("net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents");
                Object disconnectEvent = eventClass.getField("DISCONNECT").get(null);
                Object invoker = disconnectEvent.getClass().getMethod("invoker").invoke(disconnectEvent);
                
                java.lang.reflect.Method method = null;
                for (java.lang.reflect.Method m : invoker.getClass().getMethods()) {
                    if (m.getName().equals("onPlayDisconnect")) {
                        method = m;
                        break;
                    }
                }
                
                if (method != null) {
                    method.setAccessible(true);
                    method.invoke(invoker, player.connection, ((PlayerList)(Object)this).getServer());
                }
            }
            catch (Exception ignored)
            {
                // Fabric API not available or event failed
            }
        }
    }
}