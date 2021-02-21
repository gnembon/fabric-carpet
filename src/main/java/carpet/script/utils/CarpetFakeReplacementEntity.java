package carpet.script.utils;

import java.util.List;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Defines an entity that will be used by Carpet to replace a 
 * Carpet client shape with something compatible with vanilla clients.
 * <p>Entities implementing this interface will be prevented from being synced
 * to clients not defined in {@link #getPlayersToSendList()}, and will
 * not be available in entity selectors.
 *
 */
public interface CarpetFakeReplacementEntity {
    /**
     * @return The list of players that this entity should be sent to
     */
    public List<ServerPlayerEntity> getPlayersToSendList();
}
