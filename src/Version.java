/*
 *
 */
package mshell;

/*
 * TODO
 *    * Spectrum view.
 *    * Search in browser.
 *    * Search in playlist?
 *    * Volume display.
 *    * Window geometry in command line.
 *    * Command line help (-f option, etc.).
 *    * Pop-up help screen with keybindings.
 *    * MPD playlists support (list, save, load).
 *    * Seek playing track forward.
 *    * Copy back cover to local directory from CACHE.
 *    * Directly load cover from link provided from LASTFM without save to cache.
 *    * Covers in browser.
 *    * Update only necessary rows at status redraw (for less CPU usage).
 *    * Valid handling of update db in non-existiting directory.
 *    * Config dialog or XML config file (JAXB?).
 *    ? Message pop-up (add of tracks, DB udpate, etc.).
 *    * Load NOCOVER if cover not ready yet (downloading from lastFM).
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

