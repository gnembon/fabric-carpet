package carpet.script.utils;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validator;
import carpet.script.CarpetScriptHost;
import carpet.script.CarpetScriptServer;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.utils.Messenger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.storage.LevelResource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * A class used to save scarpet app store scripts to disk
 */
public class AppStoreManager
{
    /** A local copy of the scarpet repo's file structure, to avoid multiple queries to github.com while typing out the
     * {@code /script download} command and getting the suggestions.
     */
    private static StoreNode APP_STORE_ROOT = StoreNode.folder(null, "");

    /** This is the base link to the scarpet app repo from the github api.
     */
    private static String scarpetRepoLink = "https://api.github.com/repos/gnembon/scarpet/contents/programs/";

    private static record AppInfo(String name, String url, StoreNode source) {}

    public static class ScarpetAppStoreValidator extends Validator<String>
    {
        @Override public String validate(CommandSourceStack source, CarpetRule<String> currentRule, String newValue, String stringInput)
        {
            APP_STORE_ROOT = StoreNode.folder(null, "");
            if (newValue.equalsIgnoreCase("none"))
            {
                scarpetRepoLink = null;
                return newValue;
            }
            if (newValue.endsWith("/")) newValue = newValue.replaceAll("/$", "");
            scarpetRepoLink = "https://api.github.com/repos/"+newValue+"/";
            return newValue;
        }
        @Override
        public String description() { return "Appstore link should point to a valid github repository";}
    }

    public static class StoreNode
    {
        public String name;
        public StoreNode parent;
        public Map<String, StoreNode> children;
        public boolean sealed;
        public String value;
        private CountDownLatch childrenProgress;
        public static StoreNode folder(StoreNode parent, String name)
        {
            StoreNode node = new StoreNode(parent, name);
            node.children = new HashMap<>();
            node.value = null;
            node.sealed = false;
            return node;
        }

        public static StoreNode scriptFile(StoreNode parent, String name, String value)
        {
            StoreNode node = new StoreNode(parent, name);
            node.children = null;
            node.value = value;
            node.sealed = true;
            return node;
        }

        public boolean isLeaf()
        {
            return value != null;
        }
        public String pathElement()
        {
            return name+(isLeaf()?"":"/");
        }
        public String getPath()
        {
            return createPrePath().toString();
        }
        private StringBuilder createPrePath()
        {
            return this == APP_STORE_ROOT ? new StringBuilder() : parent.createPrePath().append(pathElement());
        }
        private StoreNode(StoreNode parent, String name)
        {
            this.parent = parent;
            this.name = name;
            this.sealed = false;
        }

        public void fillChildren() throws IOException
        {
            if (sealed) return;
            if (scarpetRepoLink == null) throw new IOException("Accessing scarpet app repo is disabled");
            try
            {
                if (childrenProgress != null)
                {
                    childrenProgress.await();
                    if (sealed) return;
                    else throw new IOException("Problems fetching suggestions. Check more details in previous exceptions.");
                }
            }
            catch (InterruptedException e)
            {
                throw new IOException("Suggestion provider thread was interrupted, unexpected!", e);
            }
            childrenProgress = new CountDownLatch(1);

            String queryPath = scarpetRepoLink + getPath();
            String response;
            try
            {
                response = AppStoreManager.getStringFromStream(queryPath);
            }
            catch (IOException e)
            {
                childrenProgress.countDown();
                childrenProgress = null; // Reset to allow retrying
                throw new IOException("Problems fetching " + queryPath, e);
            }
            JsonArray files = new JsonParser().parse(response).getAsJsonArray();
            for(JsonElement je : files)
            {
                JsonObject jo = je.getAsJsonObject();
                String name = jo.get("name").getAsString();
                if (jo.get("type").getAsString().equals("dir"))
                {
                    children.put(name, folder(this, name));
                }
                else// if (name.matches("(\\w+\\.scl?)"))
                {
                    String value = jo.get("download_url").getAsString();
                    children.put(name, scriptFile(this, name, value));
                }
            }
            sealed = true;
            childrenProgress.countDown();
        }

        /**
         * Returns true if doing down the directory structire cannot continue since the matching element is either a leaf or
         * a string not matching of any node.
         * @param pathElement
         * @return
         */
        public boolean cannotContinueFor(String pathElement) throws IOException
        {
            if (isLeaf()) return true;
            fillChildren();
            return !children.containsKey(pathElement);
        }

        public List<String> createPathSuggestions() throws IOException
        {
            if (isLeaf())
            {
                if (name.endsWith(".sc"))
                    return Collections.singletonList(getPath());
                return Collections.emptyList();
            }
            fillChildren();
            String prefix = getPath();
            return children.values().stream().
                    filter(n -> (!n.isLeaf() || n.name.endsWith(".sc"))).
                    map(s -> prefix+s.pathElement().replaceAll("/$", "")).
                    collect(Collectors.toList());
        }

        public StoreNode drillDown(String pathElement) throws IOException
        {
            if (isLeaf()) throw new IOException(pathElement+" is not a folder");
            fillChildren();
            if (!children.containsKey(pathElement)) throw new IOException("Folder "+pathElement+" is not present");
            return children.get(pathElement);
        }

        public String getValue(String file) throws IOException
        {
            StoreNode leaf = drillDown(file);
            if (!leaf.isLeaf()) throw new IOException(file+" is not a file");
            return leaf.value;
        }
    }

    /** This method searches for valid file names from the user-inputted string, e.g if the user has thus far typed
     * {@code survival/a} then it will return all the files in the {@code survival} directory of the scarpet repo (and
     * will automatically highlight those starting with a), and the string {@code survival/} as the current most valid path.
     *
     * @param currentPath The path down which we want to search for files
     * @return A pair of the current valid path, as well as the set of all the file/directory names at the end of that path
     */
    public static List<String> suggestionsFromPath(String currentPath) throws IOException
    {
        String[] path = currentPath.split("/");
        StoreNode appKiosk = APP_STORE_ROOT;
        for(String pathElement : path)
        {
            if (appKiosk.cannotContinueFor(pathElement)) break;
            appKiosk = appKiosk.children.get(pathElement);
        }
        List<String> filteredSuggestions = appKiosk.createPathSuggestions().stream().filter(s -> s.startsWith(currentPath)).collect(Collectors.toList());
        if (filteredSuggestions.size() == 1) {
            if (!appKiosk.isLeaf())
                return suggestionsFromPath(filteredSuggestions.get(0)); // Start suggesting directory contents
        }
        return filteredSuggestions;
    }



    /** Downloads script and saves it to appropriate place.
     *
     * @param path The user-inputted path to the script
     * @return {@code 1} if we succesfully saved the script, {@code 0} otherwise
     */

    public static int downloadScript(CommandSourceStack source, String path)
    {
        AppInfo nodeInfo = getFileNode(path);
        return downloadScript(source, path, nodeInfo, false);
    }

    private static int downloadScript(CommandSourceStack source, String path, AppInfo nodeInfo, boolean useTrash)
    {
        String code;
        try
        {
            code = getStringFromStream(nodeInfo.url());
        }
        catch (IOException e)
        {
            throw new CommandRuntimeException(Messenger.c("rb Failed to obtain app file content: "+e.getMessage()));
        }
        if (!saveScriptToFile(source, path, nodeInfo.name(), code, false, useTrash)) return 0;
        boolean success = CarpetServer.scriptServer.addScriptHost(source, nodeInfo.name().replaceFirst("\\.sc$", ""), null, true, false, false, nodeInfo.source());
        return success?1:0;
    }

    /** Gets the code once the user inputs the command.
     *
     * @param appPath The user inputted path to the scarpet script
     * @return Pair of app file name and content
     */
    public static AppInfo getFileNode(String appPath)
    {
        return getFileNodeFrom(APP_STORE_ROOT, appPath);
    }

    public static AppInfo getFileNodeFrom(StoreNode start, String appPath)
    {
        String[] path = appPath.split("/");
        StoreNode appKiosk = start;
        try
        {
            for(String pathElement : Arrays.copyOfRange(path, 0, path.length-1))
                appKiosk = appKiosk.drillDown(pathElement);
            String appName = path[path.length-1];
            appKiosk.getValue(appName);
            return new AppInfo(appName, appKiosk.getValue(appName), appKiosk);
        }
        catch (IOException e)
        {
            throw new CommandRuntimeException(Messenger.c("rb '"+ appPath + "' is not a valid path to a scarpet app: "+e.getMessage()));
        }
    }


    public static boolean saveScriptToFile(CommandSourceStack source, String path, String appFileName, String code, boolean globalSavePath, boolean useTrash)
    {
        Path scriptLocation;
        if (globalSavePath && !source.getServer().isDedicatedServer()) // never happens, this is always called with globalSavePath being false
        { //cos config folder only is in clients
            scriptLocation = FabricLoader.getInstance().getConfigDir().resolve("carpet/scripts/appstore").toAbsolutePath().resolve(path);
        }
        else
        {
            scriptLocation = source.getServer().getWorldPath(LevelResource.ROOT).resolve("scripts").toAbsolutePath().resolve(appFileName);
        }
        try
        {
            Files.createDirectories(scriptLocation.getParent());
            if (Files.exists(scriptLocation))
            {
                if (useTrash)
                {
                    Files.createDirectories(scriptLocation.getParent().resolve("trash"));
                    Path trashPath = scriptLocation.getParent().resolve("trash").resolve(path);
                    int i = 0;
                    while (Files.exists(trashPath))
                    {
                        String[] nameAndExtension = appFileName.split("\\.");
                        String newFileName = String.format(nameAndExtension[0] + "%02d." + nameAndExtension[1],i);
                        trashPath = trashPath.getParent().resolve(newFileName);
                        i++;
                    }
                    Files.move(scriptLocation, trashPath);
                }
                Messenger.m(source, String.format("gi Note: replaced existing app '%s'"+(useTrash ? " (old moved to /trash folder)" : ""), appFileName));
            }
            BufferedWriter writer = Files.newBufferedWriter(scriptLocation);
            writer.write(code);
            writer.close();
        }
        catch (IOException e)
        {
            Messenger.m(source, "r Error while downloading app: " + e.toString());
            CarpetScriptServer.LOG.warn("Error while downloading app", e);
            return false;
        }
        return true;
    }

    /** Returns the string from the inputstream gotten from the html request.
     * Thanks to App Shah in <a href="https://crunchify.com/in-java-how-to-read-github-file-contents-using-httpurlconnection-convert-stream-to-string-utility/">this post</a>
     * for this code.
     *
     * @return the string input from the InputStream
     * @throws IOException if an I/O error occurs
     */
    public static String getStringFromStream(String url) throws IOException
    {
        InputStream inputStream = new URL(url).openStream();
        if (inputStream == null)
            throw new IOException("No app to be found on the appstore");

        Writer stringWriter = new StringWriter();
        char[] charBuffer = new char[2048];
        try
        {
            Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            int counter;
            while ((counter = reader.read(charBuffer)) != -1)
                stringWriter.write(charBuffer, 0, counter);

        }
        finally
        {
            inputStream.close();
        }
        return stringWriter.toString();
    }

    public static void writeUrlToFile(String url, Path destination) throws IOException
    {
        try (final InputStream in = new URL(url).openStream())
        {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String getFullContentUrl(String original, StoreNode storeSource)
    {
        if (original.matches("^https?://.*$")) // We've got a full url here: Just use it
            return original;
        if (original.charAt(0) == '/') // We've got an absolute path: Use app store root
            return getFileNode(original.substring(1)).url();
        return getFileNodeFrom(storeSource, original).url(); // Relative path: Use download location
    }

    public static void addResource(CarpetScriptHost carpetScriptHost, StoreNode storeSource, Value resource)
    {
        if (!(resource instanceof MapValue))
            throw new InternalExpressionException("This is not a valid resource map: "+resource.getString());
        Map<String, Value> resourceMap = ((MapValue) resource).getMap().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getString(), Map.Entry::getValue));
        if (!resourceMap.containsKey("source")) throw new InternalExpressionException("Missing 'source' field in resource descriptor: "+resource.getString());
        String source = resourceMap.get("source").getString();
        String contentUrl = getFullContentUrl(source, storeSource);
        String target = resourceMap.computeIfAbsent("target", k -> new StringValue(contentUrl.substring(contentUrl.lastIndexOf('/') + 1))).getString();
        boolean shared = resourceMap.getOrDefault("shared", Value.FALSE).getBoolean();

        if (!carpetScriptHost.applyActionForResource(target, shared, p -> {
            try {
                writeUrlToFile(contentUrl, p);
            } catch (IOException e) {
                throw new InternalExpressionException("Unable to write resource "+target+": "+e.toString());
            }
        }))
        {
            throw new InternalExpressionException("Unable to write resource "+target);
        }
        CarpetScriptServer.LOG.info("Downloaded resource "+target+" from "+contentUrl);
    }

    /**
     * Gets a new StoreNode for an app's dependency with proper relativeness. Will be null if it comes from an external URL
     * @param originalSource The StoreNode from the container's app
     * @param sourceString The string the app specified as source
     * @param contentUrl The full content URL, from {@link #getFullContentUrl(String, StoreNode)}
     * @return A {@link StoreNode} that can be used in an app that came from the provided source
     */
    private static StoreNode getNewStoreNode(StoreNode originalSource, String sourceString, String contentUrl)
    {
        StoreNode next = originalSource;
        if (sourceString == contentUrl) // External URL (check getFullUrlContent)
            return null;
        if (sourceString.charAt(0) == '/') // Absolute URL
        {
            next = APP_STORE_ROOT;
            sourceString = sourceString.substring(1);
        }
        String[] dirs = sourceString.split("/");
        try
        {
            for (int i = 0; i < dirs.length - 1; i++)
                next = next.drillDown(dirs[i]);
        }
        catch (IOException e)
        {
            return null; // Should never happen, but let's not give a potentially incorrect node just in case
        }
        return next;
    }

    public static void addLibrary(CarpetScriptHost carpetScriptHost, StoreNode storeSource, Value library) {
        if (!(library instanceof MapValue))
            throw new InternalExpressionException("This is not a valid library map: "+library.getString());
        Map<String, String> libraryMap = ((MapValue) library).getMap().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getString(), e -> e.getValue().getString()));
        String source = libraryMap.get("source");
        String contentUrl = getFullContentUrl(source, storeSource);
        String target = libraryMap.computeIfAbsent("target", k -> contentUrl.substring(contentUrl.lastIndexOf('/') + 1));
        if (!(contentUrl.endsWith(".sc") || contentUrl.endsWith(".scl")))
            throw new InternalExpressionException("App resource type must download a scarpet app or library.");
        if (target.indexOf('/') != -1)
            throw new InternalExpressionException("App resource tried to leave script reserved space");
        try
        {
            downloadScript(carpetScriptHost.responsibleSource, target, new AppInfo(target, contentUrl, getNewStoreNode(storeSource, source, contentUrl)), true);
        }
        catch (CommandRuntimeException e)
        {
            throw new InternalExpressionException("Error when installing app dependencies: " + e.toString());
        }
        CarpetScriptServer.LOG.info("Downloaded app "+target+" from "+contentUrl);
    }
}
