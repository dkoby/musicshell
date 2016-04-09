/*
 *
 */
package mshell.thread;
/* */
import java.util.Arrays;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.io.InputStream;
import java.time.Instant;
import java.time.Duration;
import javax.imageio.ImageIO;
import java.net.URL;
import java.net.URLConnection;
import java.awt.image.BufferedImage;
/* */
import mshell.MusicShell;
import mshell.util.DPrint;
import mshell.lastfm.LastFM;

/**
 *
 */
public class CoverManager implements Runnable {
    private MusicShell ms;
    private final int QUEUE_CAPACITY = 8;
    private ArrayBlockingQueue<CoverRequest> requestQueue;
    private Instant prevLastFMAccessTime;

    /**
     *
     */
    public class CoverRequest {
        String filePath;
        String artist;
        String album;
        public CoverRequest(String filePath, String artist, String album) {
            this.filePath = filePath;
            this.artist   = artist;
            this.album    = album;
        }
    }
    /**
     *
     */
    public CoverManager(MusicShell ms) {
        this.ms = ms;
        requestQueue = new ArrayBlockingQueue<CoverRequest>(QUEUE_CAPACITY);
    }
    /**
     *
     */
    @Override
    public void run() {
        ms.initReady();
        if (!ms.initWait())
            return;
        while (true) {
            /* Try to connect */
            try {
                processRequest(requestQueue.take());
            } catch (InterruptedException e) {
                System.out.println(getClass().getSimpleName() + " interrupted, " + e);
                break;
            }
        }
    }
    /**
     * Push request to queue
     */
    public void putRequest(CoverRequest coverRequest) {
        if (!requestQueue.offer(coverRequest)) {
            System.out.println(getClass().getName() + ", queue overflow");
            requestQueue.clear();
            try {
                requestQueue.put(coverRequest);
            } catch (InterruptedException e) {};
        }
    }
    /**
     * Process request for retrieve cover
     */
    private void processRequest(CoverRequest coverRequest) {
        String coverPath = null;

        DPrint.format(DPrint.Level.VERBOSE4,
                "Look cover for:%n" +
                " -- file     \"%s\"%n" +
                " -- artist   \"%s\"%n" +
                " -- album    \"%s\"%n",
                coverRequest.filePath,
                coverRequest.artist != null ? coverRequest.artist : "NULL",
                coverRequest.album != null ? coverRequest.album : "NULL");

        for (String source: ms.config.coverPriority) {
            DPrint.format(DPrint.Level.VERBOSE4, "Check source: \"%s\"%n", source);
            if (source.equals("CACHE")) {
                Path path = getCacheCoverPath(coverRequest.artist, coverRequest.album);
                if (path.toFile().exists())
                    coverPath = path.toString();


            } else if (source.equals("LASTFM")) {
                /* we can query for last.fm only if we know artist and album */
                if (coverRequest.artist == null || coverRequest.album == null)
                    continue;

                /* Insert some delay if we access last.fm service recently */
                if (prevLastFMAccessTime != null) {
                    Instant now = Instant.now();
                    long dt = Duration.between(prevLastFMAccessTime, now).toMillis();
                    if (dt < ms.config.lastFMDelay) {
                        DPrint.format(DPrint.Level.VERBOSE4, "Wait for access last.fm for \"%d\" ms%n", dt);
                        try {
                            Thread.sleep(dt);
                        } catch (InterruptedException e) {
                            System.out.println(getClass().getSimpleName() + "interrupted, " + e);
                            break;
                        }
                    }
                }
                prevLastFMAccessTime = Instant.now();

                LastFM lastFM = null;
                try {
                    lastFM = new LastFM();
                } catch (Exception e) {
                    DPrint.format(DPrint.Level.EXCEPTION, "Failed to connect to last.fm service, %s%n", e);
                    continue;
                }

                String url;
                try {
                    url = lastFM.query(coverRequest.artist, coverRequest.album);
                } catch (Exception e) {
                    DPrint.format(DPrint.Level.EXCEPTION, "Failed to get cover from last.fm service, %s%n", e);
                    continue;
                }
                lastFM.close();

                if (url == null)
                    continue;

                DPrint.format(DPrint.Level.VERBOSE4, "LASTFM URL: \"%s\"%n", url);
                try {
                    coverPath = saveURLToCache(coverRequest.artist, coverRequest.album, url);
                } catch (Exception e) {
                    DPrint.format(DPrint.Level.EXCEPTION, "Failed to download cover to cache, %s%n", e);
                    continue;
                }
            } else {
                /* Check if file look's like URI with scheme, if not then it is local file */
                Matcher matcher = Pattern.compile("^[\\w\\+\\.\\-]+://.*").matcher(coverRequest.filePath);
                if (matcher.find()) {
                    DPrint.format(DPrint.Level.VERBOSE4, "Remote file, can not use glob pattern, try next source%n");
                    continue;
                }
                coverPath = lookLocal(coverRequest, source);
            }
            if (coverPath != null)
                break;
            /* */
            DPrint.format(DPrint.Level.VERBOSE4, "Not found%n");
        }

        if (coverPath != null) {
            DPrint.format(DPrint.Level.VERBOSE4, "Find cover \"%s\"%n", coverPath);
            ControlMessage message;
            message = new ControlMessage(ControlMessage.Id.LOADCOVER);
            message.object = coverPath;
            ms.putControlMessage(message);
        } else {
            ControlMessage message;
            message = new ControlMessage(ControlMessage.Id.LOADNOCOVER);
            ms.putControlMessage(message);
            DPrint.format(DPrint.Level.VERBOSE4, "Failed to get cover%n");
        }
    }
    /**
     * Look for local cover
     */
    private String lookLocal(CoverRequest coverRequest, String glob) {
        String result = null;
        FileSystem fileSystem = FileSystems.getDefault();
        Path lookupDir = fileSystem.getPath(ms.config.musicDirectory, coverRequest.filePath).getParent();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(lookupDir, glob)) {
            for (Path entry: stream) {
                result = entry.toString();
                break;
            }
        } catch (Exception e) {
            DPrint.format(DPrint.Level.EXCEPTION, "Failed to iterate over \"%s\" directory%n", lookupDir.toString());
            return null;
        }
    
        return result;
    }
    /**
     *
     */
    private String saveURLToCache(String artist, String album, String urlPath) throws Exception {
        String cachePath = null;
        URL url = null;
        URLConnection con = null;
        InputStream in = null;
        BufferedImage img = null;

        try {
            url = new URL(urlPath);

            con = url.openConnection();
            /* XXX move timeout values to config */
            con.setConnectTimeout(2000);
            con.setReadTimeout(10000);

            in = con.getInputStream();

            img = ImageIO.read(in);
        } catch (Exception e) {
            DPrint.format(DPrint.Level.EXCEPTION, "Failed to download file \"%s\", %s%n", urlPath, e);
            throw e;
        } finally {
            if (in != null) {
                try {
                     in.close();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
        try {
            Path path = getCacheCoverPath(artist, album);
            cachePath = path.toString();

            if (!path.toFile().exists())
                ImageIO.write(img, ms.config.coverCacheFormat, path.toFile());
        } catch (Exception e) {
            DPrint.format(DPrint.Level.EXCEPTION, "Failed to save image \"%s\", %s%n", urlPath, e);
            throw e;
        }

        return cachePath;
    }
    /**
     *
     */
    private Path getCacheCoverPath(String artist, String album) {
        FileSystem fileSystem = FileSystems.getDefault();
        Path path = fileSystem.getPath(ms.config.coverCacheDirectory,
                makeHashString(artist, album) + "." + ms.config.coverCacheFormat);
        return path;
    }
    /**
     *
     */
    private void copyBack(String path) {

    }
    /**
     *
     */
    private String makeHashString(String artist, String album) {
        StringBuilder resultHashString = new StringBuilder();
        String stringToHash = artist + "+" + album;

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            digest.update(stringToHash.getBytes("UTF-8"));
            for (byte b: digest.digest())
                resultHashString.append(String.format("%02X", b));
        } catch (Exception e) {
            System.out.println("MessageDigest exception, " + e);
            resultHashString = new StringBuilder(stringToHash);
        }
        return resultHashString.toString();
    }
}

//    System.out.println("MD5: ", Arrays.toString(java.security.MessageDigest.getInstance("MD5").digest()););

