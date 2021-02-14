package carpet.script.bundled;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.PositionTracker;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagReaders;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class FileModule extends Module
{
    private String name;
    private String code;
    private boolean library;
    public FileModule(Path sourcePath)
    {
        library = sourcePath.getFileName().toString().endsWith(".scl");
        try
        {
            name = sourcePath.getFileName().toString().replaceFirst("\\.scl?","").toLowerCase(Locale.ROOT);
            code = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
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
    // to read non-compound tags - these won't be compressed
    public static Tag read(File file)
    {
        try
        {
            return NbtIo.readCompressed(file);
        }
        catch (IOException e)
        {
            // Copy of NbtIo.read(File) because that's now client-side only
            if (!file.exists())
            {
                return null;
            }
            try (DataInputStream in = new DataInputStream(new FileInputStream(file)))
            {
                return NbtIo.read(in);
            }
            catch (IOException ioException)
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
        }
    }

    //copied private method from net.minecraft.nbt.NbtIo.write() and client method safe_write
    public static boolean write(Tag tag_1, File file)
    {
        File file_2 = new File(file.getAbsolutePath() + "_tmp");
        if (file_2.exists()) file_2.delete();

        if (tag_1 instanceof CompoundTag)
        {
            try
            {
                NbtIo.writeCompressed((CompoundTag) tag_1, file_2);
            }
            catch (IOException e)
            {
                return false;
            }
        }
        else
        {
            try (DataOutputStream dataOutputStream_1 = new DataOutputStream(new FileOutputStream(file_2)))
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
                return false;
            }
        }
        if (file.exists()) file.delete();
        if (!file.exists()) file_2.renameTo(file);
        return true;
    }

    public static boolean appendText(Path filePath, boolean addNewLines, List<String> data)
    {
        try
        {
            OutputStream out = Files.newOutputStream(filePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))
            {
                for (String line: data)
                {
                    writer.append(line);
                    if (addNewLines) writer.newLine();
                }
            }
        }
        catch (IOException e)
        {
            return false;
        }
        return true;
    }


    public static List<String> listFileContent(Path filePath)
    {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            List<String> result = new ArrayList<>();
            for (;;) {
                String line = reader.readLine();
                if (line == null)
                    break;
                result.add(line.replaceAll("[\n\r]+",""));
            }
            return result;
        }
        catch (IOException e)
        {
            return null;
        }
    }
    
    public static JsonElement readJsonContent(Path filePath)
    {
    	try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8))
        {
            return new JsonParser().parse(new JsonReader(reader));
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public static Stream<Path> listFiles(Path dir, String ext)
    {
        boolean folderListing = ext.equals("folder");
        try
        {
            return Files.list(dir).
                    filter(path -> folderListing?Files.isDirectory(path):path.toString().endsWith(ext));
        }
        catch (IOException ignored)
        {
            return null;
        }
    }
}
