/**
 *
 */
package mshell.jni.fft;
/**
 *
 */
public class KissFFT {
    private byte[] input;
    private int[] output;
    private int numSamples;
    private int sampleWidth;

    static {
        System.loadLibrary("fft");
    }
    /**
     * @param numSamples  number of input samples
     * @param sampleWidth width of one sample in bytes
     *
     */
    public KissFFT(int numSamples, int sampleWidth) {
        this.numSamples = numSamples;
        input  = new byte[numSamples * sampleWidth];
        output = new int[numSamples / 2];
    }
    /**
     * Get input array.
     */
    public byte[] getInput() {
        return input;
    }
    /**
     * Get output array.
     */
    public int[] getOutput() {
        return output;
    }
    /**
     * Initialize fft.
     *
     * @param numSamples number of fft samples to process
     */
    public native void init() throws Exception;
    /**
     * Free resources used by fft.
     */
    public native void destroy();
    /**
     * Perform FFT on input data
     */
    public native void make() throws Exception;
}

