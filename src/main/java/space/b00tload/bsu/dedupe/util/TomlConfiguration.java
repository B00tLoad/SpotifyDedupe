package space.b00tload.bsu.dedupe.util;

import com.electronwill.nightconfig.core.file.FileConfig;
import space.b00tload.bsu.dedupe.exceptions.ConfigException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static space.b00tload.bsu.dedupe.SpotifyDedupe.CONFIG_BASE;

public class TomlConfiguration {

    static {
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static FileConfig config;

    private static void init() throws IOException {
        config = FileConfig.builder(Paths.get(CONFIG_BASE, "config.toml")).defaultResource("/config/default.toml").autosave().autoreload().build();
        config.load();
        checkVersion();
    }

    public static void validate(List<String> required){
        if(isDefault()) throw new ConfigException("You have not modified your config at " + config.getNioPath().toString() + " yet.");
        for(String s : required){
            if(!config.contains(s)){
                throw new ConfigException("Missing entry "+ s);
            }
        }
    }

    public static void close() {
        config.save();
        config.close();
    }

    public static void checkVersion(){
        String currentVersion = TomlConfiguration.class.getPackage().getImplementationVersion();
        String configVersion = getString("file.version");
        if (currentVersion == null) {
            System.out.println("Error: Failed to retrieve current version. Assuming 0.0.1");
            currentVersion = "0.0.1";
        }
        if (configVersion == null) {
            throw new ConfigException("Invalid config. \"file.version\" is not set.");
        } else if (currentVersion.compareTo(configVersion) < 0) {
            System.out.println("Software updated. Please check wiki for migration help.");
            setString("file.version", currentVersion);
        }
    }

    public static boolean isDefault(){
        return config.getOrElse("default", false);
    }

    public static String getString(String key){
        return config.get(key);
    }

    public static void setString(String key, String value){
        config.set(key, value);
    }

    public static boolean getBoolean(String key){
        return config.get(key);
    }

    public static void setBoolean(String key, boolean value){
        config.set(key, value);
    }




}
