/*
 *
 */
package mshell;

/*
 * TODO
 *    * Some documentation (at least README)
 *    * Seek playing track forward.
 *    * Pop-up help screen with keybindings (called with F1).
 *    * Config dialog or XML config file (JAXB?).
 *    * Search in playlist?
 *    * MPD playlists support (list, save, load).
 *    * Copy back cover to local directory from CACHE.
 *    * Directly load cover from link provided from LASTFM without save to cache.
 *    * Covers in browser.
 *    * Update only necessary rows at status redraw (for less CPU usage).
 *    * Valid handling of update db in non-existiting directory.
 *    ? Message pop-up (add of tracks, DB udpate, etc.).
 *    * Load NOCOVER if cover not ready yet (downloading from lastFM).
 *    * Some licence info in source code.
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

