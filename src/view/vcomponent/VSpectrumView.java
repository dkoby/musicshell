/*
 *
 */
package mshell.view.vcomponent;
/* */
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
/* */
import mshell.Config;
import mshell.view.View;
import mshell.view.ColorUtil;
import mshell.jni.fft.KissFFT;
/*
 *
 */
public class VSpectrumView extends JComponent {
    private Color backingColor;
    private Color barColor;
    /* */
    private final int BAR_WIDTH = 4;
    private final int BAR_PAD   = 1;
    /* */
    private KissFFT fft;
    private AverageFIFO fifo;
    /* */
    private CalibrateMax calibrateMax;
    /* */
    private Thread clearThread;
    private boolean clear = true;
    /**
     *
     */
    public VSpectrumView() {
        calibrateMax = new CalibrateMax();

        backingColor = Config.baseColor.brighter();
        barColor     = Color.YELLOW;

        setDoubleBuffered(false);
        setOpaque(true);

        /* XXX from constructor ? */
        clearThread = new Thread(() -> {
            final int CLEAR_TIMEOUT = 1000;

            while (true) {
                synchronized (VSpectrumView.this) {
                    clear = true;
                }

                try {
                    Thread.sleep(CLEAR_TIMEOUT);
                } catch (Exception e) {break;}

                synchronized (VSpectrumView.this) {
                    if (clear)
                        repaintSwing();
                }
            }
        });
        clearThread.setDaemon(true);
        clearThread.start();
    }
    /**
     *
     */
    public void setColor(Color color) {
        backingColor = color.brighter();

        float[] hsb = ColorUtil.colorToHSB(backingColor);
        hsb[2] = 1.0f;
        barColor = ColorUtil.hsbToColor(hsb);
    }
    /**
     *
     */
    private class AverageFIFO {
        private int[] buffer;
        private int p;
        private int c;
        public AverageFIFO(int depth) {
            buffer = new int[depth];
            p = 0;
            c = 0;
        }
        public void reset() {
            p = 0;
            c = 0;
        }
        /**
         *
         * @return True if FIFO is full
         */
        public boolean putData(int data) {
            buffer[p++] = data;
            if (p >= buffer.length)
                p = 0;
            if (c < buffer.length)
                c++;

            if (c >= buffer.length)
                return true;
            return false;
        }
        public int getAverage() {
            if (c == 0)
                return 0;

            long average = 0;
            for (int e: buffer) {
                average += e;
            }
            return (int)(average / c);
        }
        public int getDepth() {
            return buffer.length;
        }
    }
    /**
     *
     */
    private class CalibrateMax {
        private final int MAXIMUM_VALUE      = 100; /* XXX */
        private final int CALIBRATE_INTERVAL = 2000;
        public int max;
        private int interval;

        public CalibrateMax() {
            max = MAXIMUM_VALUE;
        }
        public void setPoint(int point) {
            if (point > max)
                max = point;
            if (++interval > CALIBRATE_INTERVAL) {
                interval = 0;
                max = max * 4 / 5;
                if (max < MAXIMUM_VALUE)
                    max = MAXIMUM_VALUE;
            }
        }
    }
    /**
     *
     */
    @Override
    public void paintComponent(Graphics g) {
        clearScreen(g);
        synchronized (this) {
            if (fft == null)
                return;
            if (clear)
                return;
        }

        synchronized (fft) {
            drawFFT(g);
        }
    }
    /**
     *
     */
    private void clearScreen(Graphics g) {
        g.setColor(backingColor);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
    /**
     *
     */
    private void drawFFT(Graphics g) {
        int width  = getWidth();
        int height = getHeight();

        final int DATA_OFFSET = 2;

//        System.out.println(getClass().getSimpleName() + " points " + java.util.Arrays.toString(fft.getOutput()));

        int[] fftData = fft.getOutput();
        int depth = 2 * (fftData.length - DATA_OFFSET) / (width / (BAR_WIDTH + BAR_PAD));

//        System.out.println(getClass().getSimpleName() +
//                " length " + fftData.length +
//                " depth " + depth +
//                " width " + width
//                );

        if (depth == 0) {
            /* TODO if not enough points - make duplicate points */
            System.out.println(getClass().getSimpleName() + " not enough points");
            return;
        }

        if (fifo == null) {
            fifo = new AverageFIFO(depth);
        } else {
            if (fifo.getDepth() != depth) {
                System.out.println(getClass().getSimpleName() + " FIFO depth changed");
                fifo = new AverageFIFO(depth);
            }
        }

        int x = 0;
        int n = 0;
        g.setColor(barColor);
        for (int i = DATA_OFFSET; i < fftData.length; i++) {
            fifo.putData(fftData[i]);
            calibrateMax.setPoint(fftData[i]);
            if (++n < fifo.getDepth() / 2)
                continue;
            n = 0;

            int average = fifo.getAverage();


//            System.out.println("AVERAGE: " + average);
            if (average > calibrateMax.max)
                average = calibrateMax.max;

//            System.out.println(getClass().getSimpleName() + " X " + x);

            int barheight = (int)(height * average / calibrateMax.max);
            g.fillRect(x, height - barheight - 1, BAR_WIDTH, barheight);
            x += BAR_WIDTH + BAR_PAD;
            if (x >= width)
                break;
        }
    }
    /**
     *
     */
    public void drawSpectrum(KissFFT fft) {
        synchronized (this) {
            this.fft = fft;
            this.clear = false;
        }
        repaintSwing();
    }
    /**
     *
     */
    private void repaintSwing() {
        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    } 
    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }
}

