package carpet.utils;

import carpet.network.CarpetClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.util.function.Supplier;

public class FabricAPIHooks {

    private static final boolean CLIENT = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    public static final boolean WORLD_RENDER_EVENTS = CLIENT && exists(() -> WorldRenderEvents.class);

    private FabricAPIHooks() {
    }

    public static void initialize() {
        if (WORLD_RENDER_EVENTS) {
            WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
                if (CarpetClient.shapes != null) {
                    RenderSystem.pushMatrix();
                    CarpetClient.shapes.render(context.camera(), context.tickDelta());
                    RenderSystem.popMatrix();
                }
            });
        }
    }

    private static boolean exists(Supplier<Class<?>> supplier) {
        try {
            return supplier.get() != null;
        } catch (NoClassDefFoundError error) {
            return false;
        }
    }
}
