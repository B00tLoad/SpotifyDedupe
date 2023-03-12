package space.b00tload.bsu.dedupe.util;


import static space.b00tload.bsu.dedupe.SpotifyDedupe.*;

public class TokenHelper {

    /**
     * Manages saving a <code>space.b00tload.bsu.dedupe.util.SpotifyCredentials</code> into "~/.lfm2s/spotify.lfm2scred" using <code>space.b00tload.bsu.dedupe.util.CryptoHelper.serializeEncrypted(...)</code>
     * @param cred The <code>space.b00tload.bsu.dedupe.util.SpotifyCredentials</code> to be saved
     */
    public static void saveTokens(SpotifyCredentials cred) {
        CryptoHelper.serializeEncrypted(cred, CREDENTIAL_LOCATION, CryptoHelper.createKeyFromPassword(TomlConfiguration.getString("spotify.caching.password")));
    }

    /**
     * Manages retrieving a <code>space.b00tload.bsu.dedupe.util.SpotifyCredentials</code> from "~/.bsu/dedupe/spotify.bsucred" using <code>space.b00tload.bsu.dedupe.util.CryptoHelper.deserializeEncrypted(...)</code>
     * @return The retrieved <code>space.b00tload.bsu.dedupe.util.SpotifyCredentials</code>
     */
    public static SpotifyCredentials fetchTokens() {
        return (SpotifyCredentials) CryptoHelper.deserializeEncrypted(CREDENTIAL_LOCATION, CryptoHelper.createKeyFromPassword(TomlConfiguration.getString("spotify.caching.password")));
    }

    /**
     * Checks whether the saved SpotifyCredentials at "~/.bsu/dedupe/spotify.bsucred" exist
     * @return true if file exists, false if not
     */
    public static boolean existsTokens(){
        return CREDENTIAL_LOCATION.toFile().exists();
    }
}
