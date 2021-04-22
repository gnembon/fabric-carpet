package carpet.helpers;

import carpet.utils.Messenger;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.WorldSavePath;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class ScriptDownloader {

    public static int downloadScript(CommandContext<ServerCommandSource> cc, String path){
        String code = getScriptCode(path);
        return saveScriptToFile(path, code, cc.getSource().getMinecraftServer(),true);
    }

    public static String getScriptCode(String path){
        try {
            String link = "https://raw.githubusercontent.com/gnembon/scarpet/master/programs/"+ path;
            URL appURL = new URL(link);
            HttpURLConnection http = (HttpURLConnection) appURL.openConnection();
            return getStringFromStream((InputStream) http.getContent());
        } catch (FileNotFoundException e){
            throw new CommandException(new LiteralText("'"+ path + "' is not a valid path to a scarpet app"));
        } catch (IOException e) {
            throw new CommandException(new LiteralText("Error while getting code: "+ e));//todo figure out what else can trigger this
        }
    }

    public static int saveScriptToFile(String name, String code, MinecraftServer server, boolean globalSavePath){

        Path scriptPath;
        String location;

        if(globalSavePath){
            scriptPath = FabricLoader.getInstance().getConfigDir().resolve("carpet/scripts");
            location = "global script config folder";
        } else {
            scriptPath = server.getSavePath(WorldSavePath.ROOT).resolve("scripts");
            location = "world script folder";
        }

        Messenger.m(server.getCommandSource(),"gi Path to place file: '"+scriptPath + "/" + name + ".sc'");

        FileWriter fileWriter;
        try {
            if((new File(scriptPath + "/" + name + ".sc")).exists())
                throw new CommandException(new LiteralText(String.format("%s.sc already exists in %s, will not overwrite", name, location)));
            fileWriter = new FileWriter(scriptPath + "/" + name + ".sc");
            fileWriter.write(code);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        Messenger.m(server.getCommandSource(), "gi Successfuly created "+ name+ ".sc in " + location);
        return 1;
    }

    // converting stream to string
    private static String getStringFromStream(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Writer stringWriter = new StringWriter();

            char[] charBuffer = new char[2048];
            try{
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                int counter;
                while ((counter = reader.read(charBuffer)) != -1) {
                    stringWriter.write(charBuffer, 0, counter);
                }
            } finally {
                inputStream.close();
            }
            return stringWriter.toString();
        } else {
            return "";
        }
    }
}
