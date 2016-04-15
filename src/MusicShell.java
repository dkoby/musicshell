/**
 * @author Dmitry Kobylin 
 */
package mshell;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.Map;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
/* */
import mshell.view.View;
import mshell.util.DPrint;
import mshell.thread.*;

/*
 *
 */
public class MusicShell {
    public static final boolean debugView = false;
    public static Config config;
    public static View view;
    private static CountDownLatch startLatch;
    private static ControlThread controlThread;
    public static CoverManager coverManager;
    private ArrayList<Thread> threads;
    /**
     *
     */
    private void start(String... args) {
        config = new Config();

        /* parse args */
        for (String arg: args) {
            if (arg.equals("-h")) {
                System.out.println("Command line options: ");
                System.out.println("    -f                             Set full screen mode.");
                System.out.println("    --geometry=<WIDTH>x<HEIGHT>    Window geometry. ");
                System.exit(1);
            }
            if (arg.equals("-f"))
            {
                config.fullScreen = true;
                continue;
            }
            Pattern pattern = Pattern.compile("^--geometry=(\\d+)x(\\d+)");
            Matcher matcher = pattern.matcher(arg);
            if (matcher.find()) {
                if (matcher.start(1) >= 0 && matcher.start(2) >= 0) {
                    int width  = Integer.decode(matcher.group(1));
                    int height = Integer.decode(matcher.group(2));

                    config.windowWidth  = width;
                    config.windowHeight = height;
                    continue;
                }
            }
            System.err.println("Unknown command line option \"" + arg + "\"");
            System.exit(1);
        }

        try {
            mkDirs();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }

        threads = new ArrayList<>();
        controlThread = new ControlThread(this);
        coverManager = new CoverManager(this);

        threads.add(new Thread((Runnable)controlThread));
        threads.add(new Thread((Runnable)coverManager));
        threads.add(new Thread((Runnable)new SpectrumThread(this)));
        startLatch = new CountDownLatch(threads.size() + 1);
        for (Thread thread: threads)
            thread.start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view = new View(MusicShell.this);
                startLatch.countDown();
            }
        });
    }
    /**
     *
     */
    public static void main(String... args) {
        new MusicShell().start(args);
    }
    /**
     * Wait when other threads become ready
     * @return <tt>true</tt> if waiting was success, <tt>false</tt> if thread was interrupted
     */
    public boolean initWait() {
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }
    /**
     * Used for signal other threads that current thread ready
     */
    public void initReady() {
        startLatch.countDown();
    }
    /**
     * Send cotrol message to control thread
     * @param msg message to send to control thread
     */
    public void putControlMessage(ControlMessage msg) {
        controlThread.putMessage(msg);
    }
    /**
     * Called when program was terminated (by window close event).
     */
    public void terminate() {
        for (Thread thread: threads)
            thread.interrupt();
        System.exit(0);
    }
    /**
     * Create necessary directories used by program.
     */
    public void mkDirs() throws Exception {
        Map<String, String> env = System.getenv();

        String homeEnv = env.get("HOME");
        if (homeEnv == null) {
            throw new Exception("Failed to get HOME environment variable");
        }

        File homePath = new File(homeEnv);
        File msPath = new File(homePath, ".mshell");

        if (msPath.exists()) {
            if (!msPath.isDirectory()) {
                throw new Exception("Failed to create \"" + msPath.toString() + "\", " +
                        "regular file already exists with such name");
            }
        } else {
            if (!msPath.mkdir()) {
                throw new Exception("Failed to create \"" + msPath.toString() + "\" directory");
            }
// = new String("/home/pine/.mshell/cover");
        }

        if (config.coverCacheDirectory == null) {
            File coverPath = new File(msPath, "cover");
            if (coverPath.exists()) {
                if (!coverPath.isDirectory())
                    throw new Exception("Failed to create \"" + coverPath.toString() + "\", " +
                            "regular file already exists with such name");
            } else {
                if (!coverPath.mkdir())
                    throw new Exception("Failed to create \"" + coverPath.toString() + "\" directory");
            }
            config.coverCacheDirectory = coverPath.toString();
        }
    }
}

