package carpet.script.bundled;

import carpet.CarpetServer;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public abstract class Module
{
    public abstract String getName();
    public abstract String getCode();
    public abstract boolean isLibrary();
    public final static Object writeIOSync = new Object();

    private static String getDescriptor(Module module, String file, boolean isShared)
    {
        if (isShared)
        {
            return "shared/"+file;
        }
        else if (module != null && module.getName() != null) // appdata
        {
            return module.getName()+".data"+(file==null?"":"/"+file);
        }
        else
        {
            throw  new RuntimeException("Invalid file descriptor: "+file);
        }
    }

    public static Tag getData(Module module, String file, boolean isShared)
    {
        File dataFile = resolveResource(module, file, "nbt", isShared);
        if (dataFile == null) return null;
        if (!Files.exists(dataFile.toPath()) || !(dataFile.isFile())) return null;
        synchronized (writeIOSync) { return FileModule.read(dataFile); }
    }

    public static boolean saveData(Module module, String file, Tag globalState, boolean isShared)
    {
        File dataFile = resolveResource(module, file, "nbt", isShared);
        if (dataFile == null) return false;
        if (!Files.exists(dataFile.toPath().getParent()) && !dataFile.getParentFile().mkdirs()) return false;
        synchronized (writeIOSync) { return FileModule.write(globalState, dataFile); }
    }

    public static boolean appendToTextFile(Module module, String resourceName, String type, boolean isShared, List<String> message)
    {
        File dataFile = resolveResource(module, resourceName, "txt", isShared);
        if (dataFile == null) return false;
        if (!Files.exists(dataFile.toPath().getParent()) && !dataFile.getParentFile().mkdirs()) return false;
        synchronized (writeIOSync) { return FileModule.appendText(dataFile, type.equals("text"), message); }
    }

    public static boolean dropExistingFile(Module module, String resourceName, String ext, boolean isShared)
    {
        File dataFile = resolveResource(module, resourceName, ext, isShared);
        if (dataFile == null) return false;
        synchronized (writeIOSync) { return dataFile.delete(); }
    }

    public static List<String> listFile(Module module, String resourceName, String ext, boolean isShared)
    {
        File dataFile = resolveResource(module, resourceName, ext, isShared);
        if (dataFile == null) return null;
        if (!dataFile.exists()) return null;
        synchronized (writeIOSync) { return FileModule.listFileContent(dataFile); }
    }

    private static File resolveResource(Module module, String resourceName, String ext, boolean isShared)
    {
        if (!isShared && (module == null || module.getName() == null)) return null;
        return CarpetServer.minecraft_server.getLevelStorage().resolveFile(
                CarpetServer.minecraft_server.getLevelName(), "scripts/"+getDescriptor(module, resourceName, isShared)+"."+ext);
    }

    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }
}
