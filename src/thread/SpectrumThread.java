/*
 *
 */
package mshell.thread;
/* */
import mshell.MusicShell;

public class SpectrumThread implements Runnable {
    private MusicShell ms;

    /*
     *
     */
    public SpectrumThread(MusicShell ms) {
        this.ms = ms;
    }
    @Override
    public void run() {

        ms.initReady();
        if (!ms.initWait())
            return;

        while (true) {
            try {
//                System.out.println(getClass().getSimpleName());
                Thread.sleep(3000);
            } catch (Exception e) {
                System.out.println(getClass().getSimpleName() + " interrupted, " + e);
                break;
            }
        }
    }
}

//    if (Thread.interrupted()) {
//        // We've been interrupted: no more crunching.
//        return;
//    }
//
//    t.interrupt();
