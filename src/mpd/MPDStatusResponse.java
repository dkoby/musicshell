/**
 *
 */
package mshell.mpd;
/* */
import mshell.util.DPrint;

/*
 *
 */
public class MPDStatusResponse {
    public Integer volume;
    public Boolean repeat;
    public Boolean single;
    public Boolean random;
    public Integer id;
    public Integer pos;
    public State   state;
    public Boolean updating_db;
    public Integer time[] = new Integer[2];
    public CurrentSong currentSong;
    /**
     * State encoding
     */
    public enum State {
        STOP,
        PAUSE,
        PLAY,
    };
    /**
     * Current song info
     */
    public class CurrentSong {
        public String file;
        public Integer id;
        public Integer pos;
        public String artist;
        public String title;
        public String album;
        public String date;
        public Integer time;
        public Integer track[] = new Integer[2];
    }
}

