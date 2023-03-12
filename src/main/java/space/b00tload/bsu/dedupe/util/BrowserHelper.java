package space.b00tload.bsu.dedupe.util;

public class BrowserHelper {
    
    /**
     * Opens a <code>url</code> in the systems default browser
     * @param url
     */
    public static void openInBrowser(String url) {

        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder builder;

        if (os.indexOf("win") >= 0) {
            // Windows
            builder = new ProcessBuilder("rundll32.exe","url.dll,FileProtocolHandler", url);
        } else if (os.indexOf("mac") >= 0) {
            // Mac
            builder = new ProcessBuilder("open", url);
        } else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
            // Linux
            os = "linux";
            builder = new ProcessBuilder("xdg-open", url);
        } else {
            System.out.println("Please open the following link:\n"+url);
            builder = null;
        }
        
        try {
            if (builder != null) {
                Process exec = builder.start();
                if (os.equals("linux") && exec.exitValue() == 3) {
                    // on Linux in case of missing browser
                    System.out.println("Please open the following link:\n"+url);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
