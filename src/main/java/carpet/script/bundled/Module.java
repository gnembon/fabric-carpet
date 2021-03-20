package carpet.script.bundled;

import carpet.CarpetServer;
import carpet.script.argument.FileArgument;
import net.minecraft.nbt.Tag;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Module
{
    public abstract String getName();
    public abstract String getCode();
    public abstract boolean isLibrary();

    public static Tag getData(Module module)
    {
        Path dataFile = resolveResource(module);
        if (dataFile == null) return null;
        if (!Files.exists(dataFile) || !(dataFile.toFile().isFile())) return null;
        synchronized (FileArgument.writeIOSync) { return FileArgument.readTag(dataFile); }
    }

    public static void saveData(Module module, Tag globalState)
    {
        Path dataFile = resolveResource(module);
        if (dataFile == null) return;
        if (!Files.exists(dataFile.getParent()) && !dataFile.toFile().getParentFile().mkdirs()) return;
        synchronized (FileArgument.writeIOSync) { FileArgument.writeTagDisk(globalState, dataFile, false); }
    }

    private static Path resolveResource(Module module)
    {
        if (module == null || module.getName() == null) return null; // commandline app
        return CarpetServer.minecraft_server.getSavePath(WorldSavePath.ROOT).resolve("scripts/"+module.getName()+".data.nbt");
    }

    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }
}
