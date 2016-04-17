/*
 *
 */
package mshell.thread;
/* */
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
/* */
import java.io.File;
import java.util.Arrays;
/* */
import mshell.MusicShell;
import mshell.Config;
import mshell.view.View;
import mshell.util.DPrint;
import mshell.mpd.*;

public class ControlThread implements Runnable {
    enum Mode {
        BROWSER,
        PLAYLIST,
    };
    /* */
    private final int MESSAGE_QUEUE_CAPACITY = 16;
    private final int MPD_POLL_TIME = 1000;
    private final int MPD_RECONNECT_TIME = 2000;
    /* */
    private MusicShell ms;
    private BlockingQueue<ControlMessage> messageQueue;
    private MPDClient mpdClient;
    private Mode mode = Mode.PLAYLIST;
    private static final String ROOTDIR = "/";
    private File currentPath;
    MPDStatusResponse prevStatus = null;
    /* */
    public ControlThread(MusicShell ms) {
        this.ms = ms;
        messageQueue = new ArrayBlockingQueue<ControlMessage>(MESSAGE_QUEUE_CAPACITY);
    }
    /* */
    @Override
    public void run() {
        ms.initReady();
        if (!ms.initWait())
            return;
        while (true) {
            currentPath = new File(ROOTDIR);
            /* Try to connect */
            try {
                mpdClient = new MPDClient(ms.config);
                mpdClient.getSupported();
                ms.view.drawMPDConnectStatus("MPD " + mpdClient.mpdVersionString(), false);
            } catch (Exception e) {
                setError("MPD connect error");
                DPrint.format(DPrint.Level.EXCEPTION, "failed to connect to MPD: %s%n", e);
                e.printStackTrace();
            }
            /* On error wait some time and reconnect */
            if (mpdClient == null) {
                if (!sleep(MPD_RECONNECT_TIME))
                    break;
                continue;
            }
            /* Clear message queue */
            messageQueue.clear();
            /* Initiate message sender with GETSTATUS message */
            MessageSender messageSender =
                new MessageSender(new ControlMessage(ControlMessage.Id.GETSTATUS), MPD_POLL_TIME);
            messageSender.exec();
            switch (mode) {
                case PLAYLIST:
                    putMessage(new ControlMessage(ControlMessage.Id.SWITCHTOPLAYLIST));
                    break;
                case BROWSER:
                    putMessage(new ControlMessage(ControlMessage.Id.SWITCHTOBROWSER));
                    break;
            }

            while (true) {
                /* Get message from queue */
                ControlMessage message;
                try {
                    message = messageQueue.take();
                } catch (InterruptedException e) {
                    System.out.println(getClass().getSimpleName() + " interrupted, " + e);
                    break;
                }
                /* Process */
                try {
                    processMessage(message);
                } catch (MPDClient.MPDException e) {
                    System.out.println(getClass().getSimpleName() + " MPD error");
                    e.printStackTrace();
                    setError("MPD error");
                    break;
                }
            }
            setError("MPD error");
            messageSender.interrupt();
            if (!sleep(MPD_RECONNECT_TIME))
                break;
        }
    }
    /**
     *
     */
    private void processStatus(MPDStatusResponse status) throws MPDClient.MPDException {
        boolean loadCover = false;
        /* Status changed - try to load cover anyway */
        if (prevStatus == null) {
            loadCover = true;
        } else {
            if (prevStatus.currentSong.file == null && status.currentSong.file != null)
                loadCover = true;
            if (prevStatus.currentSong.file != null && status.currentSong.file == null) 
                loadCover = true;

            /* Nothing to do if we know nothing about file (only possible on MPD stop state) */
            if (prevStatus.currentSong.file != null && status.currentSong.file != null) {
                /* If file changed we should update cover */
                if (!prevStatus.currentSong.file.equals(status.currentSong.file))
                    loadCover = true;
                /* If we now know about artist and album we can load cover from remote source. */
                if ((prevStatus.currentSong.artist == null && prevStatus.currentSong.album == null) &&
                    (status.currentSong.artist != null && status.currentSong.album != null)) {
                    loadCover = true;
                }
            }

//            if (prevStatus.currentSong.file == null && status.currentSong.file != null) {
//                loadCover = true;
//            } else if (prevStatus.currentSong.file != null && status.currentSong.file == null) {
//                loadCover = true;
//            } else {
//                /* Nothing to do if we know nothing about file (only possible on MPD stop state) */
//                if (prevStatus.currentSong.file != null && status.currentSong.file != null) {
//                    /* If file changed we should update cover */
//                    if (!prevStatus.currentSong.file.equals(status.currentSong.file)) {
//                        loadCover = true;
//                    } else {
//                        /* If we now know about artist and album we can load cover from remote source. */
//                        if ((prevStatus.currentSong.artist == null && prevStatus.currentSong.album == null) &&
//                            (status.currentSong.artist != null && prevStatus.currentSong.album != null)) {
//                            loadCover = true;
//                        }
//                    }
//                }
//            }
        }
        if (loadCover) {
            ms.coverManager.putRequest(ms.coverManager.new CoverRequest(
                        status.currentSong.file,
                        status.currentSong.artist,
                        status.currentSong.album)
                    );
        }

        /* reload browser/playlist on db update finish */
        if (prevStatus != null && prevStatus.updating_db != null && status.updating_db == null) {
            if (mode == Mode.BROWSER)
                ms.view.loadBrowser(currentPath, mpdClient.getDBFiles(currentPath.toString()));
            else
                ms.view.loadPlaylist(mpdClient.getPlaylist());
        }
        ms.view.drawStatus(status);

        prevStatus = status;
    }
    /**
     * Process incoming message of control thread
     */
    private void processMessage(ControlMessage message) throws MPDClient.MPDException {
        DPrint.format(DPrint.Level.VERBOSE3, "process message: %s%n", message.id.name());
        switch (message.id) {
            case GETSTATUS:
                processStatus(mpdClient.getStatus());
                break;
            case TOGGLEMODE:
            case SWITCHTOBROWSER:
            case SWITCHTOPLAYLIST:
                switch (message.id) {
                    case TOGGLEMODE:
                        if (mode == Mode.BROWSER)
                            mode = Mode.PLAYLIST;
                        else
                            mode = Mode.BROWSER;
                        break;
                    case SWITCHTOPLAYLIST:
                        mode = Mode.PLAYLIST;
                        break;
                    case SWITCHTOBROWSER:
                        mode = Mode.BROWSER;
                        break;
                }
                if (mode.equals(Mode.PLAYLIST)) {
                    ms.view.loadPlaylist(mpdClient.getPlaylist());
                } else {
                    if (message.object instanceof ControlMessage.SwitchToBrowser) 
                    {
                        ControlMessage.SwitchToBrowser toBrowser = (ControlMessage.SwitchToBrowser)message.object;
                        if (toBrowser.prevDir) {
                            String parent = currentPath.getParent();
                            if (parent != null)
                                currentPath = new File(parent);
                        } else if (toBrowser.nextDir != null) {
                            currentPath = new File(currentPath, toBrowser.nextDir);
                        }
                    }
                    ms.view.loadBrowser(currentPath, mpdClient.getDBFiles(currentPath.toString()));
                }
                break;
            case ADDTRACKS:
                ControlMessage.AddTracks addtracks = (ControlMessage.AddTracks)message.object;
                mpdClient.addFiles(new File(currentPath, addtracks.path).toString());
                break;
            case DELETETRACKS:
                int[] indicies = ((ControlMessage.DeleteTracks)message.object).indices;
                if (indicies.length > 0) {
                    mpdClient.deleteFiles(indicies);
                    putMessage(new ControlMessage(ControlMessage.Id.SWITCHTOPLAYLIST));
                }
                break;
            case STOP:
                mpdClient.stop();
                break;
            case PLAYPAUSE:
                mpdClient.playPause();
                break;
            case PLAYTRACK:
                mpdClient.playTrack((Integer)message.object);
                processStatus(mpdClient.getStatus());
                break;
            case LOADCOVER:
                ms.view.loadCover((String)message.object);
                break;
            case LOADNOCOVER:
                ms.view.loadNoCover();
                break;
            case PLAYLISTCLEAR:
                mpdClient.clearPlaylist();
                ms.view.loadPlaylist(mpdClient.getPlaylist());
                break;
            case TOGGLEREPEAT:
                mpdClient.repeatToggle();
                processStatus(mpdClient.getStatus());
                break;
            case TOGGLESINGLE:
                mpdClient.singleToggle();
                processStatus(mpdClient.getStatus());
                break;
            case TOGGLERANDOM:
                mpdClient.randomToggle();
                processStatus(mpdClient.getStatus());
                break;
            case SETVOLUME:
                mpdClient.setVolume((Integer)message.object);
                processStatus(mpdClient.getStatus());
                break;
            case UPDATEDB:
                mpdClient.updateDB(currentPath.toString());
                processStatus(mpdClient.getStatus());
                break;
        }
    }
    /**
     * Set error state of mpd
     */
    private void setError(String... args) {
        String out;

        if (args.length == 0)
            out = "MPD Error";
        else
            out = args[0];
        ms.view.drawMPDConnectStatus(out, true);
        if (mpdClient != null)
            mpdClient.close();
        mpdClient = null;
    }
    /**
     * Put message to queue control thread
     */
    public void putMessage(ControlMessage.Id id) {
        putMessage(new ControlMessage(id));
    }
    /**
     * Put message to queue control thread
     */
    public void putMessage(ControlMessage message) {
        if (!messageQueue.offer(message)) {
            System.out.println(getClass().getName() + ", queue overflow");
            try {
                messageQueue.put(message);
            } catch (InterruptedException e) {};
        }
    }
    /**
     * Send same message to control true in equal intervals
     */
    private class MessageSender implements Runnable {
        private ControlMessage message;
        private long ms;
        private Thread thread;
        public MessageSender(ControlMessage message, long ms) {
            this.message = message;
            this.ms      = ms;
        }
        public void exec() {
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
        public void interrupt() {
            thread.interrupt();
        }
        @Override
        public void run() {
            while (true) {
                try {
                    putMessage(message);
                    Thread.sleep(ms);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
    /**
     * Wrapper for Thread.sleep() with exception handling
     */
    private boolean sleep(long ms) {
        try {
            Thread.sleep(MPD_RECONNECT_TIME);
        } catch (InterruptedException e) {
            System.out.println(getClass().getSimpleName() + ", interrupted: " + e);
            return false;
        }
        return true;
    }
}

