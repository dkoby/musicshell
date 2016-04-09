/**
 * @author Dmitry Kobylin 
 */
package mshell;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.EventQueue;
/* */
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
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
    private void start() {
        threads = new ArrayList<>();

        config = new Config();
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
        new MusicShell().start();
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
}

//        DPrint.hex(new byte[]{
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
//        
//        });
//

