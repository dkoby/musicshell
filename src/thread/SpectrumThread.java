/*
 *
 */
package mshell.thread;
/* */
import java.util.Arrays;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.time.Instant;
import java.time.Duration;
/* */
import mshell.MusicShell;
import mshell.jni.fft.KissFFT;

public class SpectrumThread implements Runnable {
    private MusicShell ms;

    /*
     * MPD settings:
     *
     * audio_fftOutput {
     *         type            "fifo"
     *         name            "Fifo fftOutput for FFT visualization"
     *         path            "/var/mpd/pcmfifo"
     *        format          "8000:16:1"
     * }
     */
    private final int SAMPLE_WIDTH = 2; /* 16-bit */
    private final int FPS          = 20;
    private final int SAMPLERATE   = 8000;
    private final int NUMSAMPLES   = SAMPLERATE / FPS;
    private final String fifoPath  = "/var/mpd/pcmfifo";
    /* */
    private DataInputStream inStream;
    private KissFFT fft;
    /**
     *
     */
    public SpectrumThread(MusicShell ms) {
        this.ms = ms;
    }
    @Override
    public void run() {
        fft = new KissFFT(NUMSAMPLES, SAMPLE_WIDTH);

        try {
            fft.init();
        } catch (Exception e) {
            System.out.println(getClass().getSimpleName() + " FFT init failed, " + e);
            cleanup();
            return;
        }

        try {
            inStream = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(fifoPath)));
        } catch (Exception e) {
            System.out.println(getClass().getSimpleName() + " failed to open MPD fifo, " + e);
            cleanup();
            return;
        }

        ms.initReady();
        if (!ms.initWait())
            return;

        while (true) {
            /* TODO interrupt read on thread interrupted */

            try {
                inStream.readFully(fft.getInput());
            } catch (EOFException e) {
                System.out.println(getClass().getSimpleName() + " FIFO EOF, " + e);
                break;
            } catch (IOException e) {
                System.out.println(getClass().getSimpleName() + " FIFO IO error, " + e);
                break;
            }
            
            try {
                fft.make();
            } catch (Exception e) {
                System.out.println(getClass().getSimpleName() + " FFT ERROR, " + e);
                break;
            }

            ms.view.drawSpectrum(fft);

            if (Thread.interrupted()) {
                System.out.println(getClass().getSimpleName() + " interrupted");
                break;
            }

        }
        cleanup();
    }
    /**
     *
     */
    private void cleanup() {
        fft.destroy();
        if (inStream != null) {
            try {
                inStream.close();
            } catch (IOException e) {
                System.out.println(getClass().getSimpleName() + " close failed, " + e);
            }
        }
    }
}

