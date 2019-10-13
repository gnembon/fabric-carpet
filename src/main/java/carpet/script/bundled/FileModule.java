package carpet.script.bundled;

import carpet.CarpetServer;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.PositionTracker;
import net.minecraft.nbt.Tag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

public class FileModule implements ModuleInterface
{
    private String name;
    private String code;
    public FileModule(File sourceFile)
    {
        try
        {
            name = sourceFile.getName().replaceFirst("\\.sc","").toLowerCase(Locale.ROOT);
            code = new String(Files.readAllBytes(sourceFile.toPath()));
        }
        catch ( IOException e)
        {
            name = null;
            code = null;
        }
    }
    public FileModule fromName(String name)
    {
        File sourceFile = CarpetServer.minecraft_server.getLevelStorage().resolveFile(
                CarpetServer.minecraft_server.getLevelName(), "scripts/"+name+".sc");
        return new FileModule(sourceFile);
    }
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getCode()
    {
        return code;
    }

    @Override
    public boolean isInternal()
    {
        return false;
    }

    //copied private method from net.minecraft.nbt.NbtIo.read()
    private static Tag read(File file)
    {
        try (DataInputStream dataInput_1 = new DataInputStream(new FileInputStream(file)))
        {
            byte byte_1 = dataInput_1.readByte();
            if (byte_1 == 0)
            {
                return new EndTag();
            }
            else
            {
                dataInput_1.readUTF();
                Tag tag_1 = Tag.createTag(byte_1);
                tag_1.read(dataInput_1, 0, PositionTracker.DEFAULT);
                return tag_1;
            }
        }
        catch (IOException ignored)
        {
        }
        return null;
    }

    //copied private method from net.minecraft.nbt.NbtIo.write() and client method safe_write
    private static void write(Tag tag_1, File file)
    {
        File file_2 = new File(file.getAbsolutePath() + "_tmp");
        if (file_2.exists()) file_2.delete();

        try(DataOutputStream dataOutputStream_1 = new DataOutputStream(new FileOutputStream(file_2)))
        {
            dataOutputStream_1.writeByte(tag_1.getType());
            if (tag_1.getType() != 0)
            {
                dataOutputStream_1.writeUTF("");
                tag_1.write(dataOutputStream_1);
            }
        }
        catch (IOException e)
        {
            return;
        }
        if (file.exists()) file.delete();
        if (!file.exists()) file_2.renameTo(file);
    }

    @Override
    public Tag getData(String file)
    {
        File dataFile =CarpetServer.minecraft_server.getLevelStorage().resolveFile(
                CarpetServer.minecraft_server.getLevelName(), "scripts/"+name+".data"+(file==null?"":"."+file)+".nbt");
        if (!(dataFile.isFile()))
            return null;
        return read(dataFile);
    }

    @Override
    public void saveData(String file, Tag globalState)
    {
        File dataFile =CarpetServer.minecraft_server.getLevelStorage().resolveFile(
                CarpetServer.minecraft_server.getLevelName(), "scripts/"+name+".data"+(file==null?"":"."+file)+".nbt");
        write(globalState, dataFile);

    }
}
