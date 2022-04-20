package carpet.logging.logHelpers;

import carpet.logging.LoggerRegistry;
import carpet.logging.TypeLogger;
import carpet.utils.Messenger;
import net.minecraft.network.chat.BaseComponent;

public class UpdateStackCountHelper {


    @SuppressWarnings("unchecked")
    public static void onStackCount(int count) {
        if(LoggerRegistry.__updateStackCount) {
            ((TypeLogger<Integer>) LoggerRegistry.getLogger("updateStackCount")).log(
                    (TypeLogger.TypeMessageIgnorePlayer<Integer>) (option) -> {
                if (count >= option) {
                    return new BaseComponent[]{Messenger.c(
                            "w Stack finished with: " + count + " updates"
                    )};
                }
                return null;
            });
        }
    }
}
