# SpotifyDedupe
This command-line-tool removes all duplicate tracks (also from different releases, matching by Trackname and Artists) from a spotify playlist.

---

## Prerequisites
 - A Spotify application
 - Java 19 (as this tool is not shipped with a JRE you may need to install a JDK)
 - Usage on Linux requires having xdg-open installed

## Setup
### Spotify
1. Visit [the Spotify Developer Dashboard](https://developer.spotify.com/dashboard/)
2. Create an app
3. In the "Users and Access" menu, add your Email-Address and Name
4. In the "Edit Settings" add "http://localhost:9876/callback/spotify/" as a Redirect URI
5. Note your Client ID and Client Secret
6. 

## Usage
In your command line run ```java -jar bsu-dedupe-%version%.jar [arguments]```.
