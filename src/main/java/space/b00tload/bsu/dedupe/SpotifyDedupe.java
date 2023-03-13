package space.b00tload.bsu.dedupe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.ModelObjectType;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.*;
import space.b00tload.bsu.dedupe.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpotifyDedupe {

    public static final String LINE_SEPERATOR = System.getProperty("line.separator");
    public static final String USER_HOME = System.getProperty("user.home");
    public static String SOFTWARE_VERSION = SpotifyDedupe.class.getPackage().getImplementationVersion();
    public static String USER_AGENT = "SpotifyDedupe/%VERSION% (" + System.getProperty("os.name") + "; " + System.getProperty("os.arch") + ") Java/" + System.getProperty("java.version");
    public static final String CONFIG_BASE = Paths.get(USER_HOME, ".bsu", "dedupe").toString();
    public static final Path CREDENTIAL_LOCATION = Paths.get(CONFIG_BASE, "spotify.bsucred");

    private static final List<String> requiredConfig = List.of("spotify.client", "spotify.secret");


    public static void main(String[] args) throws IOException, ParseException, SpotifyWebApiException {
        if(Objects.isNull(SOFTWARE_VERSION)){
            SOFTWARE_VERSION = "0.1.1-indev";
        }
        USER_AGENT = USER_AGENT.replace("%VERSION%", SOFTWARE_VERSION);
        Runtime.getRuntime().addShutdownHook(new Thread(TomlConfiguration::close));

        VersionChecker.checkVerion();

        if(!Paths.get(CONFIG_BASE).toFile().exists()) Paths.get(CONFIG_BASE).toFile().mkdirs();

        TomlConfiguration.validate(requiredConfig);

        SpotifyApi.Builder builder = SpotifyApi.builder();
        builder.setClientId(TomlConfiguration.getString("spotify.client"));
        builder.setClientSecret(TomlConfiguration.getString("spotify.secret"));
        builder.setRedirectUri(URI.create("http://localhost:9876/callback/spotify/"));
        SpotifyApi api = builder.build();

        AtomicBoolean waiting = new AtomicBoolean(true);
        if (TomlConfiguration.getBoolean("spotify.caching.enabled") && TokenHelper.existsTokens()) {
            System.out.println("Cached credentials have been found.");
            System.out.println("Fetching credentials from cache.");
            SpotifyCredentials cred = TokenHelper.fetchTokens();
            api.setRefreshToken(cred.getRefreshToken());

            if(!cred.isValid()){
                System.out.println("Cached credentials are invalid due to age. Refreshing and saving to cache");
                cred.refreshCredentials(api.authorizationCodeRefresh().build().execute());
                TokenHelper.saveTokens(cred);
            }
            api.setAccessToken(cred.getAccessToken());
        } else {
            try (Javalin webserver = Javalin.create().start(9876)) {
                if (TomlConfiguration.getBoolean("spotify.caching.enabled")) System.out.println("No cached credentials have been found.");
                System.out.println("Starting webserver to initiate web based authentication.");
                Runtime.getRuntime().addShutdownHook(new Thread(webserver::stop));
                    webserver.exception(Exception.class, (exception, ctx) -> {
                        ctx.result(exception.getMessage());
                    });
                webserver.get("/callback/spotify", ctx -> {
                    if(ctx.queryParamMap().containsKey("code")) {
                        System.out.println("Received spotify authentication code. Requesting credentials.");
                        SpotifyCredentials cred = new SpotifyCredentials(api.authorizationCode(ctx.queryParam("code")).setHeader("User-Agent", USER_AGENT).build().execute());
                        api.setAccessToken(cred.getAccessToken());
                        if(TomlConfiguration.getBoolean("spotify.caching.enabled")) {
                            System.out.println("Saving credentials to cache.");
                            TokenHelper.saveTokens(cred);
                        }
                        ctx.result("success. <script>let win = window.open(null, '_self');win.close();</script>").contentType(ContentType.TEXT_HTML).status(HttpStatus.OK);
                        waiting.set(false);
                    } else {
                        System.out.println("Error: Spotify authorization failed."+LINE_SEPERATOR+ctx.queryParam("error"));
                        System.exit(500);
                    }
                });
                System.out.println("Waiting for Spotify authorization.");


                String authPage = "https://accounts.spotify.com/authorize?client_id="
                        + api.getClientId()
                        + "&response_type=code&state=state" +
                        "&redirect_uri=" + URLEncoder.encode(api.getRedirectURI().toString(), StandardCharsets.UTF_8)
                        + "&scope=user-read-private%20playlist-modify-private%20playlist-modify-public%20playlist-read-private%20playlist-read-collaborative";
                BrowserHelper.openInBrowser(authPage);

                while (waiting.get());
            }
        }
        String id = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Enter playlist link: ");
            String s = br.readLine();
            id = s.split("://")[1].replace("open.spotify.com/playlist/", "").split("\\?")[0];
        }
        if(Objects.isNull(id)){
            System.out.println("Invalid link");
            return;
        }
        Paging<PlaylistTrack> listPage = api.getPlaylistsItems(id).setHeader("User-Agent", USER_AGENT).build().execute();
        List<PlaylistTrack> playlist = new LinkedList<>(List.of(listPage.getItems()));
        for(int offset = 100; offset < listPage.getTotal(); offset += 100){
            Paging<PlaylistTrack> nextListPage = api.getPlaylistsItems(id).offset(offset).setHeader("User-Agent", USER_AGENT).build().execute();
            playlist.addAll(List.of(nextListPage.getItems()));
        }

        List<Track> uniqueTracks = new LinkedList<>();
        List<String> uniqueConcat = new LinkedList<>();
        List<DuplicateTrack> duplicateTracks = new LinkedList<>();

        int i = 0;
        for (PlaylistTrack t : playlist){
            if(t.getTrack().getType() == ModelObjectType.TRACK) {
                Track track = (Track) t.getTrack();
                System.out.println("Examining \"" + getArtistNames(track.getArtists()) + ": " + track.getName() + "\" at position " + i + ".");
                if(uniqueTracks.contains(track) || uniqueConcat.contains(getConcat(track))){
                        duplicateTracks.add(new DuplicateTrack(track, i));
                } else {
                    uniqueTracks.add(track);
                    uniqueConcat.add(getConcat(track));
                }
            } else {
                System.out.println("Track at position " + i + " is not a song. Skipping.");
            }
            i++;
        }
        System.out.println("Original size: " + playlist.size());
        System.out.println("Unique tracks: " + uniqueTracks.size());
        System.out.println("Duplicate tracks: " + duplicateTracks.size());
        JsonArray tracks = new JsonArray();
        int pagination = 0;
        Collections.reverse(duplicateTracks);
        for(DuplicateTrack dupe : duplicateTracks){
            pagination++;
            JsonObject track = new JsonObject();
            track.addProperty("uri", dupe.track().getUri());
            JsonArray loc = new JsonArray();
            loc.add(dupe.location());
            track.add("positions", loc);
            tracks.add(track);
            if(pagination==100){
                System.out.println("Removing page of 100.");
                api.removeItemsFromPlaylist(id, tracks).build().execute();
                tracks = new JsonArray();
                pagination=0;
            }
        }
        System.out.println("Removing remaining " + tracks.asList().size() + " elements.");
        api.removeItemsFromPlaylist(id, tracks).build().execute();
    }

    public static String getArtistNames(ArtistSimplified[] artists){
        StringBuilder ret = new StringBuilder();

        for(ArtistSimplified a : artists){
            ret.append(a.getName()).append(", ");
        }

        return ret.substring(0, ret.toString().length()-2);
    }

    public static String getConcat(Track t){
        StringBuilder ret = new StringBuilder();
        StringBuilder artists = new StringBuilder();

        for(ArtistSimplified a : t.getArtists()){
            artists.append(a.getName()).append(",");
        }

        ret.append(artists.substring(0, artists.toString().length()-1)).append(":").append(t.getName());
        return ret.toString();
    }
}
