package carpet.script.bundled;

import carpet.CarpetServer;
import net.minecraft.nbt.Tag;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

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
            return file.isEmpty()?"shared":"shared/"+file;
        }
        if (module != null && module.getName() != null) // appdata
        {
            return module.getName()+".data"+(file==null || file.isEmpty()?"":"/"+file);
        }
        throw  new RuntimeException("Invalid file descriptor: "+file);
    }

    public static Tag getData(Module module, String file, boolean isShared)
    {
        Path dataFile = resolveResource(module, file, "nbt", isShared);
        if (dataFile == null) return null;
        if (!Files.exists(dataFile) || !(dataFile.toFile().isFile())) return null;
        synchronized (writeIOSync) { return FileModule.read(dataFile.toFile()); }
    }

    public static boolean saveData(Module module, String file, Tag globalState, boolean isShared)
    {
        Path dataFile = resolveResource(module, file, "nbt", isShared);
        if (dataFile == null) return false;
        if (!Files.exists(dataFile.getParent()) && !dataFile.toFile().getParentFile().mkdirs()) return false;
        synchronized (writeIOSync) { return FileModule.write(globalState, dataFile.toFile()); }
    }

    public static boolean appendToTextFile(Module module, String resourceName, String type, boolean isShared, List<String> message)
    {
        Path dataFile = resolveResource(module, resourceName, "txt", isShared);
        if (dataFile == null) return false;
        if (!Files.exists(dataFile.getParent()) && !dataFile.toFile().getParentFile().mkdirs()) return false;
        synchronized (writeIOSync) { return FileModule.appendText(dataFile.toFile(), type.equals("text"), message); }
    }

    public static boolean dropExistingFile(Module module, String resourceName, String ext, boolean isShared)
    {
        Path dataFile = resolveResource(module, resourceName, ext, isShared);
        if (dataFile == null) return false;
        synchronized (writeIOSync) { return dataFile.toFile().delete(); }
    }

    public static List<String> listFile(Module module, String resourceName, String ext, boolean isShared)
    {
        Path dataFile = resolveResource(module, resourceName, ext, isShared);
        if (dataFile == null) return null;
        if (!dataFile.toFile().exists()) return null;
        synchronized (writeIOSync) { return FileModule.listFileContent(dataFile.toFile()); }
    }

    public static Stream<String> listFolder(Module module, String resourceName, String ext, boolean isShared)
    {
        Path dir = resolveResource(module, resourceName, null, isShared);
        if (dir == null) return null;
        if (!Files.exists(dir)) return null;
        Stream<Path> result;
        synchronized (writeIOSync) { result = FileModule.listFiles(dir, ext); }
        if (result == null) return null;
        Path rootPath = resolveResource(module, "", null, isShared);
        Stream<String> strings = result.map(p -> rootPath.relativize(p).toString().replaceAll("[\\\\/]","/"));
        if (!ext.equals("folder"))
            return strings.map(FilenameUtils::removeExtension);
        return strings;
    }

    private static Path resolveResource(Module module, String resourceName, String ext, boolean isShared)
    {
        if (!isShared && (module == null || module.getName() == null)) return null;
        return CarpetServer.minecraft_server.getSavePath(WorldSavePath.ROOT).resolve("scripts/"+getDescriptor(module, resourceName, isShared)+(ext==null?"":"."+ext));
    }

    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }
}
