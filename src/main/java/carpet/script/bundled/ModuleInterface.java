package carpet.script.bundled;

import net.minecraft.nbt.Tag;

public interface ModuleInterface
{
    String getName();
    String getCode();
    default boolean isInternal() {return true; }
    default Tag getData(String file) {return null; }
    default void saveData(String file, Tag globalState) {}
}
