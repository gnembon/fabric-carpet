package carpet.script.argument;

import carpet.CarpetServer;
import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.bundled.Module;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.Value;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.PositionTracker;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagReaders;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.io.FilenameUtils;

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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileArgument
{
    public String resource;
    public Type type;
    public String zipContainer;
    public boolean isFolder;
    public boolean isShared;
    public Reason reason;

    public final static Object writeIOSync = new Object();

    public enum Type
    {
        RAW("raw", ".txt"),
        TEXT("text", ".txt"),
        NBT("nbt", ".nbt"),
        JSON("json", ".json"),
        FOLDER("folder", "");

        private final String id;
        private final String extension;

        public static Map<String, Type> of = Arrays.stream(values()).collect(Collectors.toMap(t -> t.id, t -> t));

        Type(String id, String extension)
        {
             this.id = id;
             this.extension = extension;
        }
    }

    public enum Reason
    {
        READ, CREATE, DELETE
    }

    private FileArgument(String resource, Type type, String zipContainer, boolean isFolder, boolean isShared, Reason reason)
    {
        this.resource = resource;
        this.type = type;
        this.zipContainer = zipContainer;
        this.isFolder = isFolder;
        this.isShared = isShared;
        this.reason = reason;
    }

    public static FileArgument from(List<LazyValue> lv, Context c, boolean isFolder, Reason reason)
    {
        if (lv.size() < 2) throw new InternalExpressionException("File functions require path and type as first two arguments");
        String resource = recognizeResource(lv.get(0).evalValue(c), isFolder);
        String origtype = lv.get(1).evalValue(c).getString().toLowerCase(Locale.ROOT);
        boolean shared = origtype.startsWith("shared_");
        String typeString = shared ? origtype.substring(7) : origtype; //len(shared_)
        Type type = Type.of.get(typeString);
        if (!Type.of.containsKey(typeString))
            throw new InternalExpressionException("Unsupported file type: "+origtype);

        if (type==Type.FOLDER && !isFolder)
            throw new InternalExpressionException("Folder types are no supported for this IO function");
        return  new FileArgument(resource, type,null, isFolder, shared, reason);

    }

    public static String recognizeResource(Value value, boolean isFolder)
    {
        String origfile = value.getString();
        String file = origfile.toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9\\-+_/]", "");
        file = Arrays.stream(file.split("/+")).filter(s -> !s.isEmpty()).collect(Collectors.joining("/"));
        if (file.isEmpty() && !isFolder)
        {
            throw new InternalExpressionException("Cannot use "+origfile+" as resource name - must have some letters and numbers");
        }
        return file;
    }

    private Path toPath(Module module)
    {
        if (!isShared && (module == null || module.getName() == null)) return null;
        return CarpetServer.minecraft_server.getSavePath(WorldSavePath.ROOT).resolve("scripts/"+getDescriptor(module, resource)+type.extension);
    }

    private Path moduleRootPath(Module module)
    {
        if (!isShared && (module == null || module.getName() == null)) return null;
        return CarpetServer.minecraft_server.getSavePath(WorldSavePath.ROOT).resolve("scripts/"+getDescriptor(module, ""));
    }

    private String getDescriptor(Module module, String res)
    {
        if (isShared)
        {
            return res.isEmpty()?"shared":"shared/"+res;
        }
        if (module != null && module.getName() != null) // appdata
        {
            return module.getName()+".data"+(res==null || res.isEmpty()?"":"/"+res);
        }
        throw  new RuntimeException("Invalid file descriptor: "+res);
    }

    public Stream<Path> listFiles(Module module)
    {
        Path dir = toPath(module);
        if (dir == null || !Files.exists(dir)) return null;
        String ext = type.extension;
        try
        {
            return Files.list(dir).
                    filter(path -> isFolder?Files.isDirectory(path):path.toString().endsWith(ext));
        }
        catch (IOException ignored)
        {
            return null;
        }
    }

    public Stream<String> listFolder(Module module)
    {
        Stream<Path> result;
        synchronized (writeIOSync) { result = listFiles(module); }
        if (result == null) return null;
        Path rootPath = moduleRootPath(module);
        Stream<String> strings = result.map(p -> rootPath.relativize(p).toString().replaceAll("[\\\\/]","/"));
        if (type != Type.FOLDER)
            return strings.map(FilenameUtils::removeExtension);
        return strings;
    }

    public boolean appendToTextFile(Module module, List<String> message)
    {
        Path dataFile = toPath(module);//, resourceName, supportedTypes.get(type), isShared);
        if (dataFile == null) return false;
        if (!Files.exists(dataFile.getParent()) && !dataFile.toFile().getParentFile().mkdirs()) return false;
        synchronized (writeIOSync)
        {
            try
            {
                OutputStream out = Files.newOutputStream(dataFile, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))
                {
                    for (String line: message)
                    {
                        writer.append(line);
                        if (type == Type.TEXT) writer.newLine();
                    }
                }
            }
            catch (IOException e)
            {
                return false;
            }
            return true;
        }
    }

    public Tag getNbtData(Module module) // aka getData
    {
        Path dataFile = toPath(module);
        if (dataFile == null) return null;
        if (!Files.exists(dataFile) || !(dataFile.toFile().isFile())) return null;
        synchronized (writeIOSync) { return readTag(dataFile.toFile()); }
    }

    //copied private method from net.minecraft.nbt.NbtIo.read()
    // to read non-compound tags - these won't be compressed
    public static Tag readTag(File file)
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

    public boolean saveNbtData(Module module, Tag tag) // aka saveData
    {
        Path dataFile = toPath(module);
        if (dataFile == null) return false;
        if (!Files.exists(dataFile.getParent()) && !dataFile.toFile().getParentFile().mkdirs()) return false;
        synchronized (writeIOSync) { return writeTag(tag, dataFile.toFile()); }
    }

    //copied private method from net.minecraft.nbt.NbtIo.write() and client method safe_write
    public static boolean writeTag(Tag tag_1, File file)
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

    public boolean dropExistingFile(Module module)
    {
        Path dataFile = toPath(module);
        if (dataFile == null) return false;
        synchronized (writeIOSync) { return dataFile.toFile().delete(); }
    }

    public List<String> listFile(Module module)
    {
        Path dataFile = toPath(module);
        if (dataFile == null) return null;
        if (!dataFile.toFile().exists()) return null;
        synchronized (writeIOSync) { return listFileContent(dataFile); }
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

    public JsonElement readJsonFile(Module module) {
        Path dataFile = toPath(module);
        if (dataFile == null) return null;
        if (!dataFile.toFile().exists()) return null;
        synchronized (writeIOSync) { return readJsonContent(dataFile); }
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

}
