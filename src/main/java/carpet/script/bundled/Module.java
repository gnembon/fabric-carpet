package carpet.script.bundled;

import carpet.CarpetServer;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static carpet.script.bundled.FileModule.read;
import static carpet.script.bundled.FileModule.write;

public abstract class Module
{
    public abstract String getName();
    public abstract String getCode();
    public abstract boolean isLibrary();

    public Tag getData(String file)
    {
        File dataFile = CarpetServer.minecraft_server.getLevelStorage().resolveFile(
                CarpetServer.minecraft_server.getLevelName(), "scripts/"+getName()+".data"+(file==null?"":"."+file)+".nbt");
        if (!Files.exists(dataFile.toPath()) || !(dataFile.isFile())) return null;
        return read(dataFile);
    }

    public void saveData(String file, Tag globalState)
    {
        File dataFile =CarpetServer.minecraft_server.getLevelStorage().resolveFile(
                CarpetServer.minecraft_server.getLevelName(), "scripts/"+getName()+".data"+(file==null?"":"."+file)+".nbt");
        if (!Files.exists(dataFile.toPath().getParent()))
        {
            try
            {
                Files.createDirectory(dataFile.toPath().getParent());
            }
            catch (IOException ignored)
            {
            }
        }
        write(globalState, dataFile);

    }

    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }
}
