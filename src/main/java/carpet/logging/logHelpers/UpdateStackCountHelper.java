package carpet.logging.logHelpers;

import carpet.logging.LoggerRegistry;
import carpet.logging.TypeLogger;
import carpet.utils.Messenger;
import net.minecraft.network.chat.Component;

public class UpdateStackCountHelper {

    public static void onStackCount(int count) {
        if(!LoggerRegistry.__updateStackCount) return;
        @SuppressWarnings("unchecked")
        TypeLogger<Integer> stackCountLogger = (TypeLogger<Integer>) LoggerRegistry.getLogger("updateStackCount");
        stackCountLogger.log((TypeLogger.TypeMessageIgnorePlayer<Integer>) (option) -> {
            if (count < option) return null;
            return new Component[]{Messenger.c("w Stack finished with: " + count + " updates")};
        });
    }
}
