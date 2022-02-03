package carpet.utils;

import carpet.network.CarpetClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

public class FabricAPIHooks {

    public static final boolean WORLD_RENDER_EVENTS = hasMod("fabric-rendering-v1", "1.5.0");

    private FabricAPIHooks() {
    }

    public static void initialize() {
        if (WORLD_RENDER_EVENTS) {
            WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
                if (CarpetClient.shapes != null) {
                    CarpetClient.shapes.render(context.matrixStack(), context.camera(), context.tickDelta());
                }
            });
        }
    }

    private static boolean hasMod(String id, String minimumVersion) {
        return FabricLoader.getInstance().getModContainer(id).map(m -> {
            Version version = m.getMetadata().getVersion();

            try {
                return version.compareTo(SemanticVersion.parse(minimumVersion)) >= 0;
            } catch (VersionParsingException ignored) {
            }

            return false;
        }).orElse(false);
    }
}
