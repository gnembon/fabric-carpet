package carpet.mixins;

import carpet.fakes.RaidManagerInterface;
import net.minecraft.entity.raid.Raid;
import net.minecraft.entity.raid.RaidManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(RaidManager.class)
public abstract class RaidManager_scarpetAPIMixin implements RaidManagerInterface
{
    @Shadow @Final private Map<Integer, Raid> raids;

    @Override
    public Map<Integer, Raid> getAllRaids()
    {
        return raids;
    }
}
