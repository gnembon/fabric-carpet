package carpet.utils;

import carpet.mixins.ChunkTicketManagerMixin;
import carpet.mixins.ServerChunkManager_ticketManagerMixin;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import net.minecraft.server.world.*;
import net.minecraft.text.BaseText;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.List;

public class ChunkInfo
{
    public static List<BaseText> chunkInfo(ColumnPos columnPos, ServerWorld world)
    {
        List<BaseText> lst = new ArrayList<>();

        ChunkPos chunkPos = new ChunkPos(columnPos.x >> 4, columnPos.z >> 4);

        lst.add(Messenger.s(""));
        lst.add(Messenger.s("==============================="));
        lst.add(Messenger.s(String.format("Input x=%d z=%d ==> chunk x=%d z=%d",
                    columnPos.x, columnPos.z, chunkPos.x, chunkPos.z)));
        lst.add(Messenger.s(String.format("Chunk info for chunk x=%d z=%d:", chunkPos.x, chunkPos.z)));

        // Get chunk from the manager. Last parameter seems to be "throw exception if chunk is unloaded".
        ServerChunkManager chunkManager = (ServerChunkManager) world.getChunkManager();
        Chunk chunk = chunkManager.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);

        if (chunk == null)
        {
            lst.add(Messenger.s("- Chunk is not loaded"));
            return lst;
        }

        lst.add(Messenger.s("- Chunk is loaded"));

        ChunkTicketManager ticketManager = ((ServerChunkManager_ticketManagerMixin) chunkManager).getTicketManager();
        ChunkHolder chunkHolder = ((ChunkTicketManagerMixin) ticketManager).invokeGetChunkHolder(chunkPos.toLong());

        if (chunkHolder == null)
        {
            lst.add(Messenger.s("- Err: unexpected missing chunk holder"));
        }
        else
        {
            lst.add(Messenger.s(
                    String.format("- Chunk holder: level: %d, type: %s",
                            chunkHolder.getLevel(), chunkHolder.getLevelType())));
        }

        ObjectSortedSet<ChunkTicket<?>> ticketSet =
                ((ChunkTicketManagerMixin) ticketManager).invokeGetTicketSet(chunkPos.toLong());

        lst.add(Messenger.s("- Chunk tickets:"));
        for (ChunkTicket<?> ticket : ticketSet)
        {
            // "Distance" is speculative - this is only used for teleport/random tickets anyway, so omit it if not set
            long distance = ticket.getType().method_20629();
            String distanceDescription = distance == 0L ? "" : String.format(", distance: '%d'", distance);

            lst.add(Messenger.s(
                    String.format("  * level: %d, type: '%s'%s",
                            ticket.getLevel(),
                            ticket.getType(),
                            distanceDescription)));
        }

        return lst;
    }
}
