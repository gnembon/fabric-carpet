package carpet.patches;

import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.ArrayList;
import java.util.List;

public class FakePlayerManager {
    public static List<ServerGamePacketListenerImpl> connections = new ArrayList<>();

    public static void reset() {
        connections.clear();
    }
}
