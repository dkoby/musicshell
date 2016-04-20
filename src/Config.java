/*
 *
 */
package mshell;
/* */
import java.awt.Insets;
import java.awt.Color;
import java.awt.Font;
import java.io.File;

public class Config {
    public static boolean fullScreen     = false;
    public static int windowWidth  = 1440;
    public static int windowHeight = 900;
//    public static int windowWidth  = 1920;
//    public static int windowHeight = 1080;
    /* */
    public static Color baseColor           = new Color(0x44, 0x44, 0x66);
    public static Font listFont             = new Font(Font.MONOSPACED, Font.BOLD, 14);
    public static Font statusFont           = new Font(Font.MONOSPACED, Font.BOLD, 16);
    public static Font mpdConnectStatusFont = new Font(Font.MONOSPACED, Font.PLAIN, 10);
    /* */
    public static final boolean useSpectrumView = false;
    /* */
    public static final boolean separateAlbumByColor = true;
    /* */
    public static int volumeStep = 5; /* in percents */
    /* */
    public static final double sidePanelWidth = 0.2; /* 0.0 to 1.0, XXX weight, not actual proprtion */
    public static final int defaultPadding = 8;
    /* Playlist columns config */
    public static PlaylistColumnsConfig[] playlistColumnConfig;
    /* MPD settings */
    public static final String mpdHost = "127.0.0.1";
    public static final int    mpdPort = 6600;
    /* For proper cover finding in local directories */
    public static final String musicDirectory = new String("/home/music");
    public static String coverCacheDirectory; /* NOTE maked at startup if necessary, can be overriden here */
//    public static final String coverCacheDirectory = new String("/home/mshell/.mshell/cover");
    public static final String coverCacheFormat = new String("png");
//    public static final String coverCacheFormat = new String("jpg");
    /*
     * Cover source search priority. Represent glob-style patterns, except
     * of CACHE ant LASTFM. 
     */
    public static String[] coverPriority = {
        "CACHE",
        "LASTFM",
        "cover.jpg",
        "cover.png",
        "*.jpg",
        "*.png"
    };
//    /* 
//     * Copy cover file from remote source (CACHE or LASTFM) to local directory (if not exists)
//     */
//    public static final boolean coverCopyToLocal = true;
//    public static final String tempCoverPath = "/dev/shm/cover.jpg";
    /* */
    public long lastFMDelay = (10 * 1000); /* ms */
    /* */
    public Config() {
        playlistColumnConfig = new PlaylistColumnsConfig[] {
            new PlaylistColumnsConfig("track" , 4),
            new PlaylistColumnsConfig("title" , 41),
            new PlaylistColumnsConfig("time"  , 5),
            new PlaylistColumnsConfig("artist", 20),
            new PlaylistColumnsConfig("album" , 26),
            new PlaylistColumnsConfig("date"  , 4),
        };
    }
    /*
     * Playlist columns 
     */
    public class PlaylistColumnsConfig {
        public String name;
        public int width;
        /**
         *
         */
        public PlaylistColumnsConfig(String name, int width) {
            this.name = name;
            this.width = width;
        }
    }
}

