package carpet.script.utils;

import java.util.List;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class CarpetArmorStandLabel extends ArmorStandEntity implements CarpetFakeReplacementEntity {
    private final List<ServerPlayerEntity> playersToSendList;

    public CarpetArmorStandLabel(World world, List<ServerPlayerEntity> playersToSend) {
        super(EntityType.ARMOR_STAND, world);
        setCustomNameVisible(true);
        setInvulnerable(true);
        setNoGravity(true);
        this.playersToSendList = playersToSend;
        
        // Cheap setMarker(true) since it's private
        this.dataTracker.set(ARMOR_STAND_FLAGS, (byte)((Byte)this.dataTracker.get(ARMOR_STAND_FLAGS) | 16));
    }

    // Don't save. Ever
    @Override public CompoundTag toTag(CompoundTag tag)
    {
        return tag;
    }
    @Override public boolean saveSelfToTag(CompoundTag tag)
    {
        return false;
    }
    
    // We shouldn't need this
    @Override
    public void tick() {}
    
    // Seems to fix MC-135809
    @Override
    public boolean canAvoidTraps()
    {
        return true;
    }
    
    @Override
    public List<ServerPlayerEntity> getPlayersToSendList()
    {
        return playersToSendList;
    }
}
