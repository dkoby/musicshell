/**
 *
 */
package mshell.mpd;
/* */
import java.util.ArrayList;

public class MPDDBFilesResponse {
    public ArrayList<Entry> entries;
    /**
     *
     */
    public MPDDBFilesResponse() {
        entries = new ArrayList<>();
    }
    /**
     *
     */
    public enum EntryType {
        DIR,
        FILE,
    }
    /**
     *
     */
    public class Entry {
        public EntryType type;
        public String name;
        /**
         *
         */
        public Entry(EntryType type, String name) {
            this.type = type;
            this.name = name;
        }
    }
    /**
     *
     */
    public void addEntry(EntryType type, String name) {
        entries.add(new Entry(type, name));
    }
}

