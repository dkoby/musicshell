/*
 *
 */
package mshell.view;
/* */
import java.awt.Color;

/**
 *
 */
public class ColorUtil {
    /**
     *
     */
    public static float[] colorToHSB(Color color) {
        return Color.RGBtoHSB(
            color.getRed(),
            color.getGreen(), 
            color.getBlue(),
            null);
    }
    /*
     *
     */
    public static int[] colorToRGB(Color color) {
        int[] rgb = new int[3];
        rgb[0] = color.getRed();
        rgb[1] = color.getGreen();
        rgb[2] = color.getBlue();
        return rgb;
    }
    /**
     *
     */
    public static Color hsbToColor(float[] hsb) {
        return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    }
    /**
     *
     */
    public static Color rgbToColor(int[] rgb) {
        return new Color(rgb[0], rgb[1], rgb[2]);
    }
    /**
     *
     */
    public static float normalize(float val) {
        if (val > 1.0f)
            val = 1.0f;
        else if (val < 0)
            val = 1;
        return val;
    }
    /**
     *
     */
    public static Color invertColor(Color color) {
        return new Color(
                color.getRed() ^ 0xff,
                color.getGreen() ^ 0xff,
                color.getBlue() ^ 0xff
                );
    }
    /**
     *
     */
    public static Color blurColor(Color color) {
        int[] rgb = colorToRGB(color);
        int average = (rgb[0] + rgb[1] + rgb[2]) / 3;

        rgb[0] = (rgb[0] + average * 2) / 3;
        rgb[1] = (rgb[1] + average * 2) / 3;
        rgb[2] = (rgb[2] + average * 2) / 3;

        return rgbToColor(rgb);
    }
}

