package carpet.script.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class PostEffectDispatcher
{
    @Nullable
    private static Identifier activeShaderId = null;
    private static long expiryGameTime = -1;

    public static void set(@Nullable Identifier shaderId, long durationTicks)
    {
        if (shaderId == null)
        {
            activeShaderId = null;
            expiryGameTime = -1;
        }
        else
        {
            Minecraft client = Minecraft.getInstance();
            long now = client.level != null ? client.level.getGameTime() : 0;
            activeShaderId = shaderId;
            expiryGameTime = now + durationTicks;
        }
        reevaluate();
    }

    // Called from GameRenderer_scarpetPostEffectMixin while checkEntityPostEffect is already running -
    // must not call reevaluate() here, that would recurse back into checkEntityPostEffect.
    @Nullable
    public static Identifier getActiveEffect(long currentGameTime)
    {
        if (activeShaderId != null && currentGameTime >= expiryGameTime)
        {
            activeShaderId = null;
            expiryGameTime = -1;
        }
        return activeShaderId;
    }

    // Called once per frame (see LevelRenderer_scarpetRenderMixin) so a natural expiry is noticed even
    // when the camera entity hasn't changed and F5 hasn't been pressed - lets vanilla's own post-effect
    // logic (creeper/spider/enderman/clear) resume as soon as our effect times out.
    public static void purge(long currentGameTime)
    {
        if (activeShaderId != null && currentGameTime >= expiryGameTime)
        {
            activeShaderId = null;
            expiryGameTime = -1;
            reevaluate();
        }
    }

    private static void reevaluate()
    {
        Minecraft client = Minecraft.getInstance();
        client.gameRenderer.checkEntityPostEffect(client.getCameraEntity());
    }
}
