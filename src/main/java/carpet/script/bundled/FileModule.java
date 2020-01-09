package carpet.script.bundled;

import carpet.CarpetServer;
import net.minecraft.nbt.PositionTracker;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagReaders;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

public class FileModule extends Module
{
    private String name;
    private String code;
    private boolean library;
    public FileModule(File sourceFile)
    {
        library = sourceFile.getName().endsWith(".scl");
        try
        {
            name = sourceFile.getName().replaceFirst("\\.scl?","").toLowerCase(Locale.ROOT);
            code = new String(Files.readAllBytes(sourceFile.toPath()));
        }
        catch ( IOException e)
        {
            name = null;
            code = null;
        }
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
    public boolean isLibrary()
    {
        return library;
    }

    //copied private method from net.minecraft.nbt.NbtIo.read()
    public static Tag read(File file)
    {
        try (DataInputStream dataInput_1 = new DataInputStream(new FileInputStream(file)))
        {
            byte byte_1 = dataInput_1.readByte();
            if (byte_1 == 0)
            {
                return null;
            }
            else
            {
                dataInput_1.readUTF();
                return TagReaders.of(byte_1).read(dataInput_1, 0, PositionTracker.DEFAULT);
            }
        }
        catch (IOException ignored)
        {
        }
        return null;
    }

    //copied private method from net.minecraft.nbt.NbtIo.write() and client method safe_write
    public static void write(Tag tag_1, File file)
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
}
