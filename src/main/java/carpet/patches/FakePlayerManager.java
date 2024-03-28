package carpet.patches;

import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.ArrayList;
import java.util.List;

public class FakePlayerManager {
    private static List<ServerGamePacketListenerImpl> connections = new ArrayList<>();

    public static void reset() {
        connections.clear();
    }

    public static void add(ServerGamePacketListenerImpl connection) {
        connections.add(connection);
    }

    public static void tick() {
        for (ServerGamePacketListenerImpl connection : FakePlayerManager.connections) {
            // from ServerGamePacketListenerImpl#tick

            connection.player.xo = connection.player.getX();
            connection.player.yo = connection.player.getY();
            connection.player.zo = connection.player.getZ();

            connection.player.doTick();
            //  connection.player.absMoveTo(connection.firstGoodX, connection.firstGoodY, connection.firstGoodZ, connection.player.getYRot(), connection.player.getXRot());

            // todo: vehicle?
        }
        connections.removeIf(connection -> connection.player.hasDisconnected());
    }
}
