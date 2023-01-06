package carpet.script.argument;

import carpet.CarpetServer;
import carpet.script.CarpetScriptServer;
import carpet.script.Module;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.ReportedException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagTypes;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import java.util.function.Consumer;
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
        FOLDER("folder", ""),
        ANY("any", "");

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

    public static FileArgument from(List<Value> lv, boolean isFolder, Reason reason)
    {
        if (lv.size() < 2) throw new InternalExpressionException("File functions require path and type as first two arguments");
        String origtype = lv.get(1).getString().toLowerCase(Locale.ROOT);
        boolean shared = origtype.startsWith("shared_");
        String typeString = shared ? origtype.substring(7) : origtype; //len(shared_)
        Type type = Type.of.get(typeString);
        Pair<String, String> resource = recognizeResource(lv.get(0).getString(), isFolder, type);
        if (type==null)
            throw new InternalExpressionException("Unsupported file type: "+origtype);
        if (type==Type.FOLDER && !isFolder)
            throw new InternalExpressionException("Folder types are no supported for this IO function");
        return  new FileArgument(resource.getLeft(), type,resource.getRight(), isFolder, shared, reason);

    }

    public static FileArgument resourceFromPath(String path, Reason reason, boolean shared)
    {
        Pair<String, String> resource = recognizeResource(path, false, Type.ANY);
        return  new FileArgument(resource.getLeft(), Type.ANY,resource.getRight(), false, shared, reason);
    }

    public static Pair<String,String> recognizeResource(String origfile, boolean isFolder, Type type)
    {
        String[] pathElements = origfile.split("[/\\\\]+");
        List<String> path = new ArrayList<>();
        String zipPath = null;
        for (int i =0; i < pathElements.length; i++ )
        {
            String token = pathElements[i];
            boolean isZip = token.endsWith(".zip") && (isFolder || (i < pathElements.length-1));
            if (zipPath != null && isZip) throw new InternalExpressionException(token+" indicates zip access in an already zipped location "+zipPath);
            if (isZip) token = token.substring(0, token.length()-4);
            token = (type==Type.ANY && i == pathElements.length-1)? // sloppy really, but should work
                    token.replaceAll("[^A-Za-z0-9\\-+_.]", ""):
                    token.replaceAll("[^A-Za-z0-9\\-+_]", "");
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
        return CarpetServer.minecraft_server.getWorldPath(LevelResource.ROOT).resolve("scripts/"+suffix);
    }

    private Path toPath(Module module)
    {
        if (!isShared && module == null) return null;
        if (zipContainer == null)
            return resolve(getDescriptor(module, resource)+(isFolder?"":type.extension));
        else
        {
            if (zfs == null)
            {
                Map<String, String> env = new HashMap<>();
                if (reason == Reason.CREATE) env.put("create", "true");
                zipPath = resolve(getDescriptor(module, zipContainer));
                if (!Files.exists(zipPath) && reason != Reason.CREATE) return null; // no zip file
                try {
                    if (!Files.exists(zipPath.getParent())) Files.createDirectories(zipPath.getParent());
                    zfs = FileSystems.newFileSystem(URI.create("jar:"+ zipPath.toUri().toString()), env);
                }
                catch (FileSystemNotFoundException | IOException e)
                {
                    CarpetScriptServer.LOG.warn("Exception when opening zip file", e);
                    throw new ThrowStatement("Unable to open zip file: "+zipContainer, Throwables.IO_EXCEPTION);
                }
            }
            return zfs.getPath(resource+(isFolder?"/":type.extension));
        }
    }

    private Path moduleRootPath(Module module)
    {
        if (!isShared && module == null) return null;
        return resolve(isShared?"shared":module.name()+".data");
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
        if (module != null) // appdata
        {
            return module.name()+".data"+(res==null || res.isEmpty()?"":"/"+res);
        }
        throw new InternalExpressionException("Invalid file descriptor: "+res);
    }


    public boolean findPathAndApply(Module module, Consumer<Path> action)
    {
        try { synchronized (writeIOSync)
        {
            Path dataFile = toPath(module);//, resourceName, supportedTypes.get(type), isShared);
            if (dataFile == null) return false;
            createPaths(dataFile);
            action.accept(dataFile);
        } }
        finally
        {
            close();
        }
        return true;
    }

    public Stream<Path> listFiles(Module module)
    {
        Path dir = toPath(module);
        if (dir == null || !Files.exists(dir)) return null;
        String ext = type.extension;
        try
        {
            return Files.list(dir).filter(path -> (type==Type.FOLDER)
                    ?Files.isDirectory(path)
                    :(Files.isRegularFile(path) &&  path.toString().endsWith(ext))
            );
        }
        catch (IOException ignored)
        {
            return null;
        }
    }

    public Stream<String> listFolder(Module module)
    {
        Stream<String> strings;
        try (Stream<Path> result = listFiles(module)) { synchronized (writeIOSync)
        {
            if (result == null) return null;
            Path rootPath = moduleRootPath(module);
            if (rootPath == null) return null;
            String zipComponent = (zipContainer != null) ? rootPath.relativize(zipPath).toString() : null;
            // need to evaluate the stream before exiting try-with-resources else there'll be no data to stream
            strings = (zipContainer == null)
                    ? result.map(p -> rootPath.relativize(p).toString().replaceAll("[\\\\/]+", "/")).toList().stream()
                    : result.map(p -> (zipComponent + '/'+ p.toString()).replaceAll("[\\\\/]+", "/")).toList().stream();
        } }
        finally
        {
            close();
        }
        if (type == Type.FOLDER)
            // java 8 paths are inconsistent. in java 16 they all should not have trailing slashes
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
            CarpetScriptServer.LOG.warn("IOException when creating paths", e);
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
            CarpetScriptServer.LOG.warn("IOException when appending to text file", e);
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
                        return TagTypes.getType(byte_1).load(dataInput_1, 0, NbtAccounter.UNLIMITED);
                    }
                }
                catch (IOException secondIO)
                {
                    CarpetScriptServer.LOG.warn("IOException when trying to read nbt file, something may have gone wrong with the fs", e);
                    CarpetScriptServer.LOG.warn("", ioException);
                    CarpetScriptServer.LOG.warn("", secondIO);
                    throw new ThrowStatement("Not a valid NBT tag in "+path.toString(), Throwables.NBT_ERROR);
                }
            }
        }
        catch (ReportedException e)
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
                path = path.getParent().resolve(path.getFileName() + "_tmp");
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
                    dataOutputStream_1.writeByte(tag_1.getId());
                    if (tag_1.getId() != 0)
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
            CarpetScriptServer.LOG.warn("IO Exception when writing nbt file", e);
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
            CarpetScriptServer.LOG.warn("IOException when removing file", e);
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
            CarpetScriptServer.LOG.warn("IOException when reading text file", e);
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
            return JsonParser.parseReader(reader);
        }
        catch (JsonParseException e)
        {
            Throwable exc = e;
            if(e.getCause() != null)
                exc = e.getCause();
            throw new ThrowStatement(MapValue.wrap(Map.of(
                    StringValue.of("error"), StringValue.of(exc.getMessage()),
                    StringValue.of("path"), StringValue.of(filePath.toString())
            )), Throwables.JSON_ERROR);
        }
        catch (IOException e)
        {
            CarpetScriptServer.LOG.warn("IOException when reading JSON file", e);
            throw new ThrowStatement("Failed to read json file content "+filePath.toString(), Throwables.IO_EXCEPTION);
        }
    }

}
