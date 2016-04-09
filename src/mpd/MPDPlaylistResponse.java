/**
 *
 */
package mshell.mpd;
/* */
import java.util.ArrayList;

/**
 *
 */
public class MPDPlaylistResponse {
    public ArrayList<TrackInfo> tracks;
    /**
     *
     */
    public MPDPlaylistResponse() {
        tracks = new ArrayList<>();
    }
    /**
     *
     */
    public void addTrackInfo(TrackInfo info) {
        tracks.add(info);
    }
    /**
     *
     */
    public class TrackInfo {
        public String file;
        public String artist;
        public String title;
        public String album;
        public Integer track[];
        public String date;
        public Integer time;
        public Integer pos;
        public Integer id;
        /**
         *
         */
        public TrackInfo() {
            track = new Integer[2];
        }
    }
}

/*
 * file: mp3/Dark Lunacy/2003 - Forget Me Not/11-Dark Lunacy--Forget Me Not.mp3
 * Last-Modified: 2015-05-06T16:02:07Z
 * Artist: Dark Lunacy
 * Title: Forget Me Not
 * Album: Forget Me Not
 * Track: 11/11
 * Date: 2003
 * Genre: Rock
 * Time: 682
 * Pos: 23
 * Id: 479
 */

/*
 * file: http://lo.death.fm
 * Title: Death.FM (lo.death.fm - 32k aacPlus)
 * Pos: 32
 * Id: 489
 */

