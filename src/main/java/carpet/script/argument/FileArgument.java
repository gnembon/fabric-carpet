package carpet.script.argument;

import carpet.CarpetServer;
import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.bundled.Module;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.PositionTracker;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagReaders;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.crash.CrashException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    private FileSystem zfs;
    private Path zipPath;

    public final static Object writeIOSync = new Object();

    public void close() {
        if (zfs != null && zfs.isOpen()) {
            try {
                zfs.close();
            } catch (IOException e) {
                throw new InternalExpressionException("Unable to close zip container: "+zipContainer);
            }
            zfs = null;
        }
    }

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

    public FileArgument(String resource, Type type, String zipContainer, boolean isFolder, boolean isShared, Reason reason)
    {
        this.resource = resource;
        this.type = type;
        this.zipContainer = zipContainer;
        this.isFolder = isFolder;
        this.isShared = isShared;
        this.reason = reason;
        this.zfs = null;
        this.zipPath = null;
    }

    @Override
    public String toString() {
        return "path: "+resource+" zip: "+zipContainer+" type: "+type.id+" folder: "+isFolder+" shared: "+isShared+" reason: "+reason.toString();
    }

    public static FileArgument from(List<LazyValue> lv, Context c, boolean isFolder, Reason reason)
    {
        if (lv.size() < 2) throw new InternalExpressionException("File functions require path and type as first two arguments");
        Pair<String, String> resource = recognizeResource(lv.get(0).evalValue(c), isFolder);
        String origtype = lv.get(1).evalValue(c).getString().toLowerCase(Locale.ROOT);
        boolean shared = origtype.startsWith("shared_");
        String typeString = shared ? origtype.substring(7) : origtype; //len(shared_)
        Type type = Type.of.get(typeString);
        if (type==null)
            throw new InternalExpressionException("Unsupported file type: "+origtype);
        if (type==Type.FOLDER && !isFolder)
            throw new InternalExpressionException("Folder types are no supported for this IO function");
        return  new FileArgument(resource.getLeft(), type,resource.getRight(), isFolder, shared, reason);

    }

    public static Pair<String,String> recognizeResource(Value value, boolean isFolder)
    {
        String origfile = value.getString();
        String[] pathElements = origfile.toLowerCase(Locale.ROOT).split("[/\\\\]+");
        List<String> path = new ArrayList<>();
        String zipPath = null;
        for (String token : pathElements)
        {
            boolean isZip = token.endsWith(".zip");
            if (zipPath != null && isZip) throw new InternalExpressionException(token+" indicates zip access in an already zipped location "+zipPath);
            if (isZip) token = token.substring(0, token.length()-4);
            token = token.replaceAll("[^A-Za-z0-9\\-+_]", "");
            if (token.isEmpty()) continue;
            if (isZip) token = token + ".zip";
            path.add(token);
            if (isZip)
            {
                zipPath = String.join("/", path);
                path.clear();
            }
        }
        if (path.isEmpty() && !isFolder)
            throw new InternalExpressionException(
                    "Cannot use "+origfile+" as resource name: indicated path is empty"+((zipPath==null)?"":" in zip container "+zipPath)
            );
        return Pair.of(String.join("/", path), zipPath);
    }

    private Path resolve(String suffix)
    {
        if (CarpetServer.minecraft_server == null)
            throw new InternalExpressionException("Accessing world files without server running");
        return CarpetServer.minecraft_server.getSavePath(WorldSavePath.ROOT).resolve("scripts/"+suffix);
    }

    private Path toPath(Module module)
    {
        if (!isShared && (module == null || module.getName() == null)) return null;
        if (zipContainer == null)
            return resolve(getDescriptor(module, resource)+(isFolder?"":type.extension));
        else
        {
            if (zfs == null)
            {
                Map<String, String> env = new HashMap<>();
                if (reason == Reason.CREATE) env.put("create", "true");
                zipPath = resolve(getDescriptor(module, zipContainer));
                try {
                    zfs = FileSystems.newFileSystem(URI.create("jar:"+ zipPath.toUri().toString()), env);
                }
                catch (FileSystemNotFoundException fsnfe)
                {
                    return null;
                }
                catch (IOException e)
                {
                    throw new ThrowStatement("Unable to open zip file: "+zipContainer, Throwables.IO_EXCEPTION);
                }
            }
            return zfs.getPath(resource+(isFolder?"/":type.extension));
        }
    }

    private Path moduleRootPath(Module module)
    {
        if (!isShared && (module == null || module.getName() == null)) return null;
        return resolve(isShared?"shared":module.getName()+".data");
    }

    public String getDisplayPath()
    {
        return (isShared?"shared/":"")+(zipContainer!=null?zipContainer+"/":"")+resource+type.extension;
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
        throw new InternalExpressionException("Invalid file descriptor: "+res);
    }

    public Stream<Path> listFiles(Module module)
    {
        Path dir = toPath(module);
        if (dir == null || !Files.exists(dir)) return null;
        String ext = type.extension;
        try
        {
            return Files.list(dir).filter(path -> (type==Type.FOLDER)?path.toString().endsWith("/"):path.toString().endsWith(ext));
        }
        catch (IOException ignored)
        {
            return null;
        }
    }

    public Stream<String> listFolder(Module module)
    {
        Stream<String> strings;
        try { synchronized (writeIOSync)
        {
            Stream<Path> result;
            result = listFiles(module);
            if (result == null) return null;
            Path rootPath = moduleRootPath(module);
            if (rootPath == null) return null;
            String zipComponent = (zipContainer != null) ? rootPath.relativize(zipPath).toString() : null;
            strings = result.map(p -> {
                if (zipContainer == null)
                    return rootPath.relativize(p).toString().replaceAll("[\\\\/]+", "/");
                return (zipComponent + '/'+ p.toString()).replaceAll("[\\\\/]+", "/");
            });
        } }
        finally
        {
            close();
        }
        if (type == Type.FOLDER)
            return strings.map(s -> s.endsWith("/")?s.substring(0, s.length()-1):s);
        return strings.map(FilenameUtils::removeExtension);
    }

    private void createPaths(Path file)
    {
        try {
            if ((zipContainer == null || file.getParent() != null) &&
                    !Files.exists(file.getParent()) &&
                    Files.createDirectories(file.getParent()) == null
            ) throw new IOException();
        } catch (IOException e) {
            throw new ThrowStatement("Unable to create paths for "+file.toString(), Throwables.IO_EXCEPTION);
        }

    }

    public boolean appendToTextFile(Module module, List<String> message)
    {
        try { synchronized (writeIOSync)
        {
            Path dataFile = toPath(module);//, resourceName, supportedTypes.get(type), isShared);
            if (dataFile == null) return false;
            createPaths(dataFile);
            OutputStream out = Files.newOutputStream(dataFile, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))
            {
                for (String line: message)
                {
                    writer.append(line);
                    if (type == Type.TEXT) writer.newLine();
                }
            }
        } }
        catch (IOException e)
        {
            throw new ThrowStatement("Error when writing to the file: "+e, Throwables.IO_EXCEPTION);
        }
        finally
        {
            close();
        }
        return true;
    }

    public Tag getNbtData(Module module) // aka getData
    {
        try { synchronized (writeIOSync) {
            Path dataFile = toPath(module);
            if (dataFile == null || !Files.exists(dataFile)) return null;
            return readTag(dataFile);
        } }
        finally {
            close();
        }
    }

    //copied private method from net.minecraft.nbt.NbtIo.read()
    // to read non-compound tags - these won't be compressed
    public static Tag readTag(Path path)
    {
        try
        {
            return NbtIo.readCompressed(Files.newInputStream(path));
        }
        catch (IOException e)
        {
            // Copy of NbtIo.read(File) because that's now client-side only
            if (!Files.exists(path))
            {
                return null;
            }
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path))))
            {
                return NbtIo.read(in);
            }
            catch (IOException ioException)
            {
                // not compressed compound tag neither uncompressed compound tag - trying any type of a tag
                try (DataInputStream dataInput_1 = new DataInputStream(new BufferedInputStream(Files.newInputStream(path))))
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
                    throw new ThrowStatement("Not a valid NBT tag in "+path.toString(), Throwables.NBT_ERROR);
                }
            }
        }
        catch (CrashException e)
        {
            throw new ThrowStatement("Error when reading NBT file "+path.toString(), Throwables.NBT_ERROR);
        }
    }

    public boolean saveNbtData(Module module, Tag tag) // aka saveData
    {
        try { synchronized (writeIOSync) {
            Path dataFile = toPath(module);
            if (dataFile == null) return false;
            createPaths(dataFile);
            return writeTagDisk(tag, dataFile, zipContainer != null);
        } }
        finally
        {
            close();
        }
    }

    //copied private method from net.minecraft.nbt.NbtIo.write() and client method safe_write
    public static boolean writeTagDisk(Tag tag_1, Path path, boolean zipped)
    {
        Path original = path;
        try
        {
            if (!zipped)
            {
                path = new File(path.toFile().getAbsolutePath() + "_tmp").toPath();
                Files.deleteIfExists(path);
            }

            if (tag_1 instanceof CompoundTag)
            {
                NbtIo.writeCompressed((CompoundTag) tag_1, Files.newOutputStream(path));
            }
            else
            {
                try (DataOutputStream dataOutputStream_1 = new DataOutputStream(Files.newOutputStream(path)))
                {
                    dataOutputStream_1.writeByte(tag_1.getType());
                    if (tag_1.getType() != 0)
                    {
                        dataOutputStream_1.writeUTF("");
                        tag_1.write(dataOutputStream_1);
                    }
                }
            }
            if (!zipped)
            {
                Files.deleteIfExists(original);
                Files.move(path, original);
            }
            return true;
        }
        catch (IOException e)
        {
            throw new ThrowStatement("Unable to write tag to "+original.toString(), Throwables.IO_EXCEPTION);
        }
    }

    public boolean dropExistingFile(Module module)
    {

        try { synchronized (writeIOSync)
        {
            Path dataFile = toPath(module);
            if (dataFile == null) return false;
            return Files.deleteIfExists(dataFile);
        } }
        catch (IOException e)
        {
            throw new ThrowStatement("Error while removing file: "+getDisplayPath(), Throwables.IO_EXCEPTION);
        }
        finally
        {
            close();
        }
    }

    public List<String> listFile(Module module)
    {
        try { synchronized (writeIOSync) {
            Path dataFile = toPath(module);
            if (dataFile == null) return null;
            if (!Files.exists(dataFile)) return null;
            return listFileContent(dataFile);
        } }
        finally {
            close();
        }
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
            throw new ThrowStatement("Failed to read text file "+filePath.toString(), Throwables.IO_EXCEPTION);
        }
    }

    public JsonElement readJsonFile(Module module) {
        try { synchronized (writeIOSync) {
            Path dataFile = toPath(module);
            if (dataFile == null || !Files.exists(dataFile)) return null;
            return readJsonContent(dataFile);
        } }
        finally {
            close();
        }
    }

    public static JsonElement readJsonContent(Path filePath)
    {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8))
        {
            return new JsonParser().parse(new JsonReader(reader));
        }
        catch (JsonParseException e)
        {
            Throwable exc = e;
            if(e.getCause() != null)
                exc = e.getCause();
            throw new ThrowStatement(MapValue.wrap(ImmutableMap.of(
                    StringValue.of("error"), StringValue.of(exc.getMessage()),
                    StringValue.of("path"), StringValue.of(filePath.toString())
            )), Throwables.JSON_ERROR);
        }
        catch (IOException e)
        {
            throw new ThrowStatement("Failed to read json file content "+filePath.toString(), Throwables.IO_EXCEPTION);
        }
    }

}
