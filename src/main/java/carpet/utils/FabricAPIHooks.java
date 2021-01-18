package carpet.utils;

import carpet.network.CarpetClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;

public class FabricAPIHooks {

    private FabricAPIHooks() {
    }

    public static void initialize() {
        if (FabricLoader.getInstance().isModLoaded("fabric")) {
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                initializeClient();
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static void initializeClient() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(FabricAPIHooks::beforeDebugRender);
    }

    @Environment(EnvType.CLIENT)
    private static void beforeDebugRender(WorldRenderContext context) {
        if (CarpetClient.shapes != null) {
            RenderSystem.pushMatrix();
            CarpetClient.shapes.render(context.camera(), context.tickDelta());
            RenderSystem.popMatrix();
        }
    }
}
