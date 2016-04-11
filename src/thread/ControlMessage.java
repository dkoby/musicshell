/*
 *
 */
package mshell.thread;

/*
 *
 */
public class ControlMessage {
    public enum Id {
        GETSTATUS,
        TOGGLEMODE,
        SWITCHTOBROWSER,
        SWITCHTOPLAYLIST,
        ADDTRACKS,
        DELETETRACKS,
        STOP,
        PLAYPAUSE,
        PLAYTRACK,
        LOADCOVER,
        LOADNOCOVER,
        PLAYLISTCLEAR,
        TOGGLEREPEAT,
        TOGGLESINGLE,
        TOGGLERANDOM,
        SETVOLUME,
        UPDATEDB,
    }
    public Id id;
    public Object object;
    /**
     *
     */
    public ControlMessage(Id id) {
        this.id = id;
    }
    /**
     *
     */
    public ControlMessage(Id id, Object object) {
        this.id = id;
        this.object = object;
    }
    /**
     *
     */
    public class SwitchToBrowser {
        public boolean prevDir;
        public String nextDir;
        public SwitchToBrowser(boolean prevDir) {
            this.prevDir = prevDir;
        }
        public SwitchToBrowser(String nextDir) {
            this.nextDir = nextDir;
        }
    }
    /**
     *
     */
    public class AddTracks {
        public String path;
        public AddTracks(String path) {
            this.path = path;
        }
    }
    /**
     *
     */
    public class DeleteTracks {
        public int[] indices;
        public DeleteTracks(int[] indices) {
            this.indices = indices;
        }
    }
}

