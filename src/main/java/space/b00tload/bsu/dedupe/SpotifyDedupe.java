package space.b00tload.bsu.dedupe;

import space.b00tload.bsu.dedupe.util.TomlConfiguration;
import space.b00tload.bsu.dedupe.util.VersionChecker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SpotifyDedupe {

    public static final String LINE_SEPERATOR = System.getProperty("line.separator");
    public static final String USER_HOME = System.getProperty("user.home");
    public static final String CONFIG_BASE = Paths.get(USER_HOME, ".bsu", "dedupe").toString();
    public static final Path CREDENTIAL_LOCATION = Paths.get(CONFIG_BASE, "spotify.bsucred");

    private static List<String> requiredConfig = List.of("spotify.client", "spotify.secret");


    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(TomlConfiguration::close));

        VersionChecker.checkVerion();

        if(!Paths.get(CONFIG_BASE).toFile().exists()) Paths.get(CONFIG_BASE).toFile().mkdirs();

        TomlConfiguration.validate(requiredConfig);


    }

}
