/*
 *
 */
package mshell;

/*
 * TODO
 *    * Copy back cover to local directory from CACHE.
 *    * Directly load cover from link provided from LASTFM without save to cache.
 *    * Covers in browser.
 *    * Update only necessary rows at status redraw (for less CPU usage).
 *    * Pop-up help screen with keybindings.
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

