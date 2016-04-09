/*
 *
 */
package mshell;

/*
 * TODO
 *    * Copy back cover to local directory from CACHE.
 *    * Directly load cover from link provided from LASTFM without save to cache.
 *    * Use JPEG instead of PNG to save to CACHE, need to evade ImageIO writing JPEG files bug.
 *    * use HOME environment variable to make direcotries for CACHE, config, etc.
 */

/*
 *
 */
public class Version {
    private static final int MAJOR = 0;
    private static final int MINOR = 1;
    private static final int BUILD = 0;

    public static String getString() {
        return "v" + MAJOR + "." + MINOR + "." + BUILD;
    }
}

