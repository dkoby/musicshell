/*
 *
 */
package mshell.mpd;
/* */
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
/* */
import mshell.util.DPrint;
import mshell.Config;

/*
 *
 */
public class MPDClient {
    /* */
    private Config config;
    /* */
    private int    connectTimeout  = 500;
    private int    readTimeout     = 1000;
    /* */
    private Socket socket;
    private BufferedReader in;
    private PrintStream out;
    /* */
    public int[] mpdVersion = new int[3];
    private ArrayList<String> supported;
    /* */

    /*
     *
     */
    public MPDClient(Config config) throws MPDException {
        this.config = config;
        socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(InetAddress.getByName(config.mpdHost), config.mpdPort), connectTimeout);
            socket.setSoTimeout(readTimeout);
        } catch (Exception e) {
            throw (MPDException)new MPDException(e.toString()).initCause(e);
        }

        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception e) {
            try {
                socket.close();
            } catch (Exception eclose) {
                throw (MPDException)new MPDException(eclose.toString()).initCause(eclose);
            }
            throw (MPDException)new MPDException(e.toString()).initCause(e);
        }

        try {
            out = new PrintStream(socket.getOutputStream(), true);
        } catch (Exception e) {
            try {
                in.close();
                socket.close();
            } catch (Exception eclose) {
                throw (MPDException)new MPDException(eclose.toString()).initCause(eclose);
            }
            throw (MPDException)new MPDException(e.toString()).initCause(e);
        }

        try {
            Arrays.fill(mpdVersion, 0);

            Pattern pattern = Pattern.compile("^OK\\s+MPD\\s+(\\d+)\\.(\\d+)\\.(\\d+)");
            Matcher matcher = pattern.matcher(in.readLine());
            if (matcher.find()) {
                if (matcher.start(1) >= 0) mpdVersion[0] = Integer.decode(matcher.group(1));
                if (matcher.start(2) >= 0) mpdVersion[1] = Integer.decode(matcher.group(2));
                if (matcher.start(3) >= 0) mpdVersion[2] = Integer.decode(matcher.group(3));
                DPrint.format(DPrint.Level.VERBOSE0, "Version %d.%d.%d%n",
                        mpdVersion[0], mpdVersion[1], mpdVersion[2]);
            } else {
                throw new MPDException("Invalid MPD Response");
            }
        } catch (Exception e) {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (Exception eclose) {
                throw (MPDException)new MPDException(eclose.toString()).initCause(eclose);
            }
            throw (MPDException)new MPDException(e.toString()).initCause(e);
        }
    }
    /**
     * Execute MPD command and return lines of response
     */
    public ArrayList<String> query(String cmd, String ... args) throws MPDException {
        StringBuilder request = new StringBuilder();
        ArrayList<String> response = new ArrayList<String>();

        request.append(cmd);
        for (String arg: args)
        {
            request.append(' ');
            request.append(arg);
        }
        DPrint.format(DPrint.Level.VERBOSE0, "Query \"%s\"%n", request.toString());
        request.append('\n');

        try {
            out.print(request.toString());
        } catch (Exception e) {
            System.out.println("Send error");
            throw (MPDException)new MPDException(e.toString()).initCause(e);
        }

        while (true)
        {
            String line;
            try {
                line = in.readLine();
                DPrint.format(DPrint.Level.VERBOSE0, "Response \"%s\"%n", line);
                if (line.matches("ACK.*"))
                    throw new MPDException("ACK");
                if (line.matches("OK"))
                    break;
                response.add(line);
            } catch (SocketTimeoutException e) {
                throw (MPDException)new MPDException("Timeout").initCause(e);
            } catch (Exception e) {
                throw (MPDException)new MPDException("Timeout").initCause(e);
            }
        } 

        return response;
    }
    /**
     * Get supported file types (audio decoders)
     */
    public void getSupported() throws MPDException {
        supported = new ArrayList<>();
        Matcher matcher;

        ArrayList<String> response = query("decoders");
        for (String line: response) {
            matcher = Pattern.compile("^suffix:\\s+(.+)").matcher(line);
            if (matcher.find() && matcher.start(1) >= 0) {
                supported.add(matcher.group(1));
                continue;
            }
        }
        /* XXX add pls */
        supported.add("pls");
    }
    /**
     * Get version of MPD in string format
     */
    public String mpdVersionString() {
        StringBuilder version = new StringBuilder();
        version.append(mpdVersion[0]);
        version.append(".");
        version.append(mpdVersion[1]);
        version.append(".");
        version.append(mpdVersion[2]);

        return version.toString();
    }
    /**
     * Get status
     */
    public MPDStatusResponse getStatus() throws MPDException {
        Pattern pattern;
        Matcher matcher;
        MPDStatusResponse status = new MPDStatusResponse();

        ArrayList<String> response = query("status");
        for (String line: response) {
            DPrint.format(DPrint.Level.VERBOSE1, "line: %s%n", line);
            /*
             * volume: 59
             * repeat: 0
             * random: 0
             * single: 0
             * consume: 0
             * playlist: 35
             * playlistlength: 176
             * mixrampdb: 0.000000
             * state: pause
             * song: 136
             * songid: 137
             * time: 20:265
             * elapsed: 19.678
             * bitrate: 0
             * audio: 44100:24:2
             * nextsong: 137
             * nextsongid: 138
             */
            if (status.volume == null)
            {
                pattern = Pattern.compile("^volume:\\s+(\\d+)");
                matcher = pattern.matcher(line);
                if (matcher.find() && matcher.start(1) >= 0)
                {
                    status.volume = Integer.decode(matcher.group(1));
                    continue;
                }
            }
            if (status.repeat == null) {
                pattern = Pattern.compile("^repeat:\\s+([01]+)");
                matcher = pattern.matcher(line);
                if (matcher.find() && matcher.start(1) >= 0)
                {
                    if (Integer.decode(matcher.group(1)) == 0)
                        status.repeat = false;
                    else
                        status.repeat = true;
                    continue;
                }
            }
            if (status.random == null) {
                pattern = Pattern.compile("^random:\\s+([01]+)");
                matcher = pattern.matcher(line);
                if (matcher.find() && matcher.start(1) >= 0)
                {
                    if (Integer.decode(matcher.group(1)) == 0)
                        status.random = false;
                    else
                        status.random = true;
                    continue;
                }
            }
            if (status.single == null) {
                pattern = Pattern.compile("^single:\\s+([01]+)");
                matcher = pattern.matcher(line);
                if (matcher.find() && matcher.start(1) >= 0)
                {
                    if (Integer.decode(matcher.group(1)) == 0)
                        status.single = false;
                    else
                        status.single = true;
                    continue;
                }
            }
            if (status.pos == null) {
                pattern = Pattern.compile("^song:\\s+(\\d+)");
                matcher = pattern.matcher(line);
                if (matcher.find() && matcher.start(1) >= 0)
                {
                    status.pos = Integer.decode(matcher.group(1));
                    continue;
                }
            }
            if (status.id == null) {
                pattern = Pattern.compile("^songid:\\s+(\\d+)");
                matcher = pattern.matcher(line);
                if (matcher.find() && matcher.start(1) >= 0)
                {
                    status.id = Integer.decode(matcher.group(1));
                    continue;
                }
            }
            if (status.time[0] == null) {
                matcher = Pattern.compile("^time:\\s+(\\d+):(\\d+)").matcher(line);
                if (matcher.find()) {
                    if (matcher.start(1) >= 0) {
                        status.time[0] = Integer.decode(matcher.group(1));
                    }
                    if (matcher.start(2) >= 0) {
                        status.time[1] = Integer.decode(matcher.group(2));
                    }
                    continue;
                }
            }
            if (status.state == null) {
                pattern = Pattern.compile("^state:\\s+(\\w+)");
                matcher = pattern.matcher(line);
                if (matcher.find() && matcher.start(1) >= 0)
                {
                    if (matcher.group(1).equals("stop"))
                        status.state = MPDStatusResponse.State.STOP;
                    else if (matcher.group(1).equals("pause"))
                        status.state = MPDStatusResponse.State.PAUSE;
                    else if (matcher.group(1).equals("play"))
                        status.state = MPDStatusResponse.State.PLAY;
                    continue;
                }
            }
            if (status.updating_db == null) {
                pattern = Pattern.compile("^updating_db:");
                matcher = pattern.matcher(line);
                if (matcher.find())
                    status.updating_db = true;
            }
        }
        /*
         * file: mp3/Atheist/1990 - Piece Of Time/02-Unholy War.mp3
         * Last-Modified: 2000-12-21T15:18:34Z
         * Artist: Atheist
         * Title: Unholy War
         * Album: Piece Of Time
         * Date: 1990
         * Genre: Death Metal
         * Time: 138
         * Pos: 53
         * Id: 143
         * OK
         */
        status.currentSong = status.new CurrentSong();
        MPDStatusResponse.CurrentSong currentSong = status.new CurrentSong();
        response = query("currentsong");
        for (String line: response) {
            DPrint.format(DPrint.Level.VERBOSE1, "line: %s%n", line);
            if (currentSong.file == null) {
                matcher = Pattern.compile("^file:\\s+(.+)").matcher(line);
                if (matcher.find() && matcher.start(1) >= 0) {
                    currentSong.file = matcher.group(1);
                    continue;
                }
            }
            if (currentSong.artist == null) {
                matcher = Pattern.compile("^Artist:\\s+(.+)").matcher(line);
                if (matcher.find() && matcher.start(1) >= 0) {
                    currentSong.artist = matcher.group(1);
                    continue;
                }
            }
            if (currentSong.title == null) {
                matcher = Pattern.compile("^Title:\\s+(.+)").matcher(line);
                if (matcher.find() && matcher.start(1) >= 0) {
                    currentSong.title = matcher.group(1);
                    continue;
                }
            }
            if (currentSong.album == null) {
                matcher = Pattern.compile("^Album:\\s+(.+)").matcher(line);
                if (matcher.find() && matcher.start(1) >= 0) {
                    currentSong.album = matcher.group(1);
                    continue;
                }
            }
            if (currentSong.track[0] == null) {
                matcher = Pattern.compile("^Track:\\s+0*(\\d+)(/0*(\\d+))?").matcher(line);
                if (matcher.find()) {
                    if (matcher.start(1) >= 0) {
                        currentSong.track[0] = Integer.decode(matcher.group(1));
                    }
                    if (matcher.start(3) >= 0) {
                        currentSong.track[1] = Integer.decode(matcher.group(3));
                    }
                    continue;
                }
            }
            if (currentSong.date == null) {
                matcher = Pattern.compile("^Date:\\s+(.+)").matcher(line);
                if (matcher.find() && matcher.start(1) >= 0) {
                    currentSong.date = matcher.group(1);
                    continue;
                }
            }
            if (currentSong.time == null) {
                matcher = Pattern.compile("^Time:\\s+(.+)").matcher(line);
                if (matcher.find() && matcher.start(1) >= 0) {
                    currentSong.time = Integer.decode(matcher.group(1));
                    continue;
                }
            }
            if (currentSong.pos == null) {
                pattern = Pattern.compile("^Pos:\\s+(\\d+)");
                matcher = pattern.matcher(line);
                if (matcher.find() && matcher.start(1) >= 0)
                {
                    currentSong.pos = Integer.decode(matcher.group(1));
                    continue;
                }
            }
            if (currentSong.id == null) {
                pattern = Pattern.compile("^Id:\\s+(\\d+)");
                matcher = pattern.matcher(line);
                if (matcher.find() && matcher.start(1) >= 0)
                {
                    currentSong.id = Integer.decode(matcher.group(1));
                    continue;
                }
            }
        }

        if (currentSong.id != null && currentSong.id.equals(status.id))
            status.currentSong = currentSong;
        return status;
    }
    /**
     * Get playlist
     */
    public MPDPlaylistResponse getPlaylist() throws MPDException {
        Matcher matcher;

        MPDPlaylistResponse playlist = new MPDPlaylistResponse();
        MPDPlaylistResponse.TrackInfo trackInfo = null;

        ArrayList<String> response = query("playlistinfo");
        for (String line: response)
        {
            DPrint.format(DPrint.Level.VERBOSE0, "line: %s%n", line);

            if (trackInfo == null) {
                matcher = Pattern.compile("^file:\\s+(.+)").matcher(line);
                if (matcher.find() && matcher.start(1) >= 0) {
                    trackInfo = playlist.new TrackInfo();
                    trackInfo.file = matcher.group(1);
                }
            } else {
                do {
                    if (trackInfo.artist == null) {
                        matcher = Pattern.compile("^Artist:\\s+(.+)").matcher(line);
                        if (matcher.find() && matcher.start(1) >= 0) {
                            trackInfo.artist = matcher.group(1);
                            break;
                        }
                    }
                    if (trackInfo.title == null) {
                        matcher = Pattern.compile("^Title:\\s+(.+)").matcher(line);
                        if (matcher.find() && matcher.start(1) >= 0) {
                            trackInfo.title = matcher.group(1);
                            break;
                        }
                    }
                    if (trackInfo.album == null) {
                        matcher = Pattern.compile("^Album:\\s+(.+)").matcher(line);
                        if (matcher.find() && matcher.start(1) >= 0) {
                            trackInfo.album = matcher.group(1);
                            break;
                        }
                    } 
                    if (trackInfo.track[0] == null) {
                        matcher = Pattern.compile("^Track:\\s+0*(\\d+)(/0*(\\d+))?").matcher(line);
                        if (matcher.find()) {
                            if (matcher.start(1) >= 0) {
                                trackInfo.track[0] = Integer.decode(matcher.group(1));
                            }
                            if (matcher.start(3) >= 0) {
                                trackInfo.track[1] = Integer.decode(matcher.group(3));
                            }
                            break;
                        }
                    }
                    if (trackInfo.date == null) {
                        matcher = Pattern.compile("^Date:\\s+(.+)").matcher(line);
                        if (matcher.find() && matcher.start(1) >= 0) {
                            trackInfo.date = matcher.group(1);
                            break;
                        }
                    }
                    if (trackInfo.time == null) {
                        matcher = Pattern.compile("^Time:\\s+(\\d+)").matcher(line);
                        if (matcher.find() && matcher.start(1) >= 0) {
                            trackInfo.time = Integer.decode(matcher.group(1));
                            break;
                        }
                    }
                    if (trackInfo.pos == null) {
                        matcher = Pattern.compile("^Pos:\\s+(\\d+)").matcher(line);
                        if (matcher.find() && matcher.start(1) >= 0) {
                            trackInfo.pos = Integer.decode(matcher.group(1));
                            break;
                        }
                    }
                    matcher = Pattern.compile("^Id:\\s+(\\d+)").matcher(line);
                    if (matcher.find() && matcher.start(1) >= 0) {
                        trackInfo.id = Integer.decode(matcher.group(1));
                        playlist.addTrackInfo(trackInfo);
                        trackInfo = null;
                    }
                } while (false);
            }
        }
        return playlist;
    }
    /**
     * List files in database directory 
     */
    public MPDDBFilesResponse getDBFiles(String uri) throws MPDException {
        MPDDBFilesResponse dbFiles = new MPDDBFilesResponse();
        Matcher matcher;

//        System.out.println("URI: \"" + uri + "\"");

        if (!uri.equals("/"))
            dbFiles.addEntry(MPDDBFilesResponse.EntryType.DIR, new String(".."));

        uri = uri.substring(1); /* XXX remove leading slash */

//        System.out.println("URI_: \"" + uri + "\"");
        ArrayList<String> response = query("lsinfo", "\"" + uri + "\"");
//        ArrayList<String> response = query("listfiles", "\"" + uri + "\"");

        for (String line: response)
        {
            String fileName = null;

            DPrint.format(DPrint.Level.VERBOSE1, "line: %s%n", line);

            while (true) {
                matcher = Pattern.compile("^directory:\\s+(.+)").matcher(line);
                if (matcher.find() && matcher.start(1) >= 0) {
                    fileName = matcher.group(1);
                    break;
                }
                matcher = Pattern.compile("^file:\\s+(.+)").matcher(line);
                if (!matcher.find(0))
                    matcher = Pattern.compile("^playlist:\\s+(.+)").matcher(line);
                if (matcher.find(0) && matcher.start(1) >= 0) {
                    String match = matcher.group(1);
//                    System.out.println("match: \"" + match + "\"");
                    for (String suffix: supported) {
                        if (match.endsWith(suffix))
//                        if (match.regionMatches(true, match.length() - suffix.length(),
//                                    suffix, 0, suffix.length()))
                        {
                            fileName = match;
                            break;
                        }
                    }
                    break;
                }
                break;
            }

            if (fileName != null) {
                /* remove initial dir from file */
                if (uri.length() > 0 && fileName.startsWith(uri)) {
                    fileName = fileName.substring(uri.length() + 1 /* XXX */);
                }
                dbFiles.addEntry(MPDDBFilesResponse.EntryType.DIR, fileName);
            }
        }

        dbFiles.entries.sort((entry1, entry2) -> {
            if (entry1.name.equals(".."))
                return -1;
            if (entry2.name.equals(".."))
                return 1;

            if (entry1.type.equals(MPDDBFilesResponse.EntryType.DIR) && 
                entry2.type.equals(MPDDBFilesResponse.EntryType.FILE))
                return 1;
            if (entry1.type.equals(MPDDBFilesResponse.EntryType.FILE) && 
                entry2.type.equals(MPDDBFilesResponse.EntryType.DIR))
                return -1;

            return entry1.name.compareTo(entry2.name);
        });

        return dbFiles;
    }
    /**
     *
     */
    public void addFiles(String uri) throws MPDException {
        if (uri.endsWith("pls")) {
            query("load", "\"" + uri.substring(1) + "\"");
        } else {
            query("add", "\"" + uri.substring(1) + "\"");
        }
    }
    /**
     *
     */
    public void deleteFiles(int[] indices) throws MPDException {
        if (indices.length == 0)
            return;

        boolean sequence = true;
        int prev = indices[0];
        for (int i = 1; i < indices.length; i++) {
            if (indices[i] != prev + 1)
            {
                sequence = false;
                break;
            }
            prev = indices[i];
        }

        if (sequence && indices.length > 0) {
            query("delete", new Integer(indices[0]).toString() +
                    ":" + new Integer(indices[indices.length - 1] + 1 /* XXX */).toString());
        } else {
            for (Integer pos: indices) {
                query("delete", new Integer(pos).toString());
            }
        }
    }
    /**
     *
     */
    public void stop() throws MPDException {
        query("stop");
    }
    /**
     *
     */
    public void clearPlaylist() throws MPDException {
        query("clear");
    }
    /**
     *
     */
    public void playPause() throws MPDException {
        MPDStatusResponse status = getStatus();

        if (status.state.equals(MPDStatusResponse.State.PLAY))
            query("pause", "1");
        else if (status.state.equals(MPDStatusResponse.State.PAUSE))
            query("pause", "0");
    }
    /**
     *
     */
    public void repeatToggle() throws MPDException {
        MPDStatusResponse status = getStatus();

        if (status.repeat)
            query("repeat", "0");
        else
            query("repeat", "1");
    }
    /**
     *
     */
    public void randomToggle() throws MPDException {
        MPDStatusResponse status = getStatus();

        if (status.random)
            query("random", "0");
        else
            query("random", "1");
    }
    /**
     *
     */
    public void singleToggle() throws MPDException {
        MPDStatusResponse status = getStatus();

        if (status.single)
            query("single", "0");
        else
            query("single", "1");
    }
    /**
     *
     */
    public void setVolume(Integer direction) throws MPDException {
        MPDStatusResponse status = getStatus();

        if (status.volume == null)
            return;

        int volume;
        if (direction > 0)
            volume = status.volume + config.volumeStep;
        else
            volume = status.volume - config.volumeStep;
        if (volume > 100)
            volume = 100;
        else if (volume < 0)
            volume = 0;

        if (status.single)
            query("setvol", new Integer(volume).toString());
        else
            query("setvol", new Integer(volume).toString());
    }
    /**
     *
     */
    public void seekSong(Integer adjust) throws MPDException {
        query("seekcur", String.format("%+d", adjust));
    }
    /**
     *
     */
    public void playTrack(Integer trackPos) throws MPDException {
        query("play", trackPos.toString());
    }
    /**
     *
     */
    public void updateDB(String path) throws MPDException {
        query("update", "\"" + path.substring(1) + "\"");
    }
    /**
     * Close connection to MPD
     */
    public void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch(Exception e) {
            System.out.println("Failed to close: " + e.getMessage());
        }
    }
    /**
     * Thrown when MPD returned ACK response (error)
     */
    public class MPDException extends Exception {
        public MPDException(String msg) {
            super(msg);
        }
    }
}

