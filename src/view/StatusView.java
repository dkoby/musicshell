/*
 *
 */
package mshell.view;
/* */
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.HashMap;
/* */
import mshell.MusicShell;
import mshell.Config;
import mshell.Config.PlaylistColumnsConfig;
import mshell.mpd.*;

/**
 * Status table
 */
public class StatusView {
    private MusicShell ms;
    private JTable table;
    private StatusTableModel tableModel;
    private HashMap<String, StatusData> statusByKey;
    private StatusData[] statusByRow;
    private Color interlaceColor;

//    private TableColumnModel columnModel;
    /**
     *
     */
    public StatusView(MusicShell ms) {
        this.ms = ms;

        /* Initialize status */
        {
            String[] rows = new String[] {
                "state", /* PLAY, STOP, PAUSE */ 
                "mode",  /* repeat, random, single */ 
                "date",
                "album",
                "artist",
                "track",
                "title",
                "time",
            };

            statusByKey = new HashMap<>(rows.length);
            statusByRow = new StatusData[rows.length];
            for (int i = 0; i < rows.length; i++) {
                StatusData statusData = new StatusData(i, rows[i]);

                statusByKey.put(rows[i], statusData);
                statusByRow[i] = statusData;
            }
        }

        tableModel = new StatusTableModel();
        table = new JTable(tableModel);

        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(false);
        table.setTableHeader(null);
        table.setShowVerticalLines  (ms.debugView);
        table.setShowHorizontalLines(ms.debugView);
        table.setAlignmentX(0.5f);

        final int PADDING = 4;
        table.setIntercellSpacing(new Dimension(0, 8));
        table.setRowHeight(table.getRowHeight() + PADDING * 2);

//        table.setFocusPainted(false);

        table.setFont(ms.config.statusFont);
        table.setDefaultRenderer(Object.class, new StatusTableCellRenderer());

//        columnModel = table.getColumnModel();
    }
    /**
     * Update status
     */
    public void updateStatus(MPDStatusResponse mpdStatus) {
        final int INITIAL_CAPACITY = 128;
        StatusData statusData;

        if (mpdStatus.currentSong.pos == null) {
            statusByKey.forEach((k, v) -> {
                v.set("");
            });
            statusData = statusByKey.get("state");
            statusData.set("-- No track -- ");
        } else {
            /* state */
            {
                StringBuilder data = new StringBuilder(INITIAL_CAPACITY);
                if (mpdStatus.state.equals(MPDStatusResponse.State.PLAY))
                    data.append("[PLAING]");
                else if (mpdStatus.state.equals(MPDStatusResponse.State.PAUSE))
                    data.append("[PAUSED]");
                else if (mpdStatus.state.equals(MPDStatusResponse.State.STOP))
                    data.append("[STOPPED]");

                statusData = statusByKey.get("state");
                statusData.set(data.toString());
            }
            /* mode */
            {
                StringBuilder data = new StringBuilder(INITIAL_CAPACITY);
                if (mpdStatus.repeat)
                    data.append("[repeat]");
                if (mpdStatus.random)
                    data.append("[random]");
                if (mpdStatus.single)
                    data.append("[single]");

                statusData = statusByKey.get("mode");
                statusData.set(data.toString());
            }
            /* date */
            {
                statusData = statusByKey.get("date");
                if (mpdStatus.currentSong.date != null) {
                    statusData.set(mpdStatus.currentSong.date);
                } else {
                    statusData.set("----");
                }
            }
            /* album */
            {
                StringBuilder data = new StringBuilder(INITIAL_CAPACITY);

                statusData = statusByKey.get("album");
                if (mpdStatus.currentSong.album != null) {
                    data.append(mpdStatus.currentSong.album);
                } else {
                    data.append("-");
                }
                statusData.set(data.toString());
            }
            /* artist */
            {
                statusData = statusByKey.get("artist");
                if (mpdStatus.currentSong.artist != null) {
                    statusData.set(mpdStatus.currentSong.artist);
                } else {
                    statusData.set("-");
                }
            }
            /* track */
            {
                StringBuilder data = new StringBuilder(INITIAL_CAPACITY);

                statusData = statusByKey.get("track");
                if (mpdStatus.currentSong.track[0] != null) {
                    data.append(String.format("%02d", mpdStatus.currentSong.track[0]));
                } else {
                    data.append("--");
                }
                data.append("/");
                if (mpdStatus.currentSong.track[1] != null) {
                    data.append(String.format("%02d", mpdStatus.currentSong.track[1]));
                } else {
                    data.append("--");
                }
                statusData.set(data.toString());
            }
            /* title/file */
            {
                statusData = statusByKey.get("title");
                if (mpdStatus.currentSong.title != null) {
                    statusData.set(mpdStatus.currentSong.title);
                } else {
                    statusData.set(mpdStatus.currentSong.file);
                }
            }
            /* time */
            {
                StringBuilder data = new StringBuilder(INITIAL_CAPACITY);

                statusData = statusByKey.get("time");
                data.append(timeToString(mpdStatus.time[0]));
                data.append(" / ");
                data.append(timeToString(mpdStatus.time[1]));
                statusData.set(data.toString());
            }
        }
        tableModel.fireTableDataChanged();
    }
    /**
     *
     */
    public void setColors(Color bgColor, Color fgColor) {
        table.setBackground(bgColor);
        table.setForeground(fgColor);

        float[] hsb = ColorUtil.colorToHSB(fgColor);
        if (hsb[2] > 0.5)
            interlaceColor = fgColor.darker();
        else
            interlaceColor = fgColor.brighter();
    }
    /**
     *
     */
    private String timeToString(Integer value) {
        if (value != null)
        {
            long time = value;
            if (time == 0)
                return new String("");
            if (time / 3600 > 0)
                return new String(String.format("%02d:%02d:%02d", time / 3600, (time % 3600) / 60, time % 60)); 
            else
                return new String(String.format("%02d:%02d", time / 60, time % 60)); 
        } else {
            return new String("--:--");
        }
    }
    /**
     *
     */
    public JTable getComponent() {
        return table;
    }
    /**
     * TableModel for playlist
     */
    class StatusTableModel extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            return 1;
        }
        @Override
        public int getRowCount() {
            return statusByRow.length;
        }
        @Override
        public Class getColumnClass(int column) {
            return String.class;
        }
        @Override
        public Object getValueAt(int row, int column) {
            StatusData statusData = statusByRow[row];

//            System.out.format("update: row %d, column %d%n", row, column);

            return statusData.value.toString();
        }
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
    /**
     *
     */
    class StatusTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
               boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel)super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            setBorder(noFocusBorder);
            label.setHorizontalAlignment(SwingConstants.CENTER);
//            label.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

            /* XXX new Color */
            if ((row % 2) == 0)
                label.setForeground(table.getForeground());
            else
                label.setForeground(interlaceColor);


//            cellSpacingLabel .setBorder(new CompoundBorder(new EmptyBorder(new Insets(1, 4, 1, 4)), oLabel.getBorder()));

            return label;
        }
    }
    /**
     *
     */
    class StatusData {
        int row;
        String key;
        StringBuilder value;
        private final int INITIAL_CAPACITY = 128;

        StatusData(int row, String key) {
            this.row = row;
            this.key = key;
            value = new StringBuilder(INITIAL_CAPACITY);
        }
        void set(String newValue) {
            if (value.length() > 0)
                value.delete(0, value.length());
            value.append(newValue);
        }
    }
}

//            if (playlist == null)
//                return new String("");
//
//            MPDPlaylistResponse.TrackInfo track = playlist[row];
//            PlaylistColumnsConfig config = ms.config.playlistColumnConfig[column];
//
//            if (config.name.equals("artist")) {
//                if (track.artist != null)
//                    return new String(track.artist);
//            } else if (config.name.equals("date")) {
//                if (track.date != null)
//                    return new String(track.date);
//            } else if (config.name.equals("album")) {
//                if (track.album != null)
//                    return new String(track.album);
//            } else if (config.name.equals("track")) {
//                if (track.track[0] != null)
//                    return String.format("%02d", track.track[0]);
//            } else if (config.name.equals("title")) {
//                if (track.title != null)
//                    return new String(track.title); 
//                else
//                    return new String(track.file); 
//            } else if (config.name.equals("time")) {
//                if (track.time != null)
//                {
//                    long time = track.time;
//                    if (time == 0)
//                        return new String("");
//                    if (time / 3600 > 0)
//                        return new String(String.format("%02d:%02d:%02d", time / 3600, (time % 3600) / 60, time % 60)); 
//                    else
//                        return new String(String.format("%02d:%02d", time / 60, time % 60)); 
//                } else {
//                    return new String("--:--");
//                }
//            }

//    /**
//     * Set current song
//     */
//    public void setCurrent(MPDStatusResponse status) {
//        if (playlist == null || playlist.length == 0)
//            return;
//        /*
//         * Update current track if necessary
//         */
//        if (currentStatus == null || status.pos != currentStatus.pos) {
//            if (currentStatus != null && currentStatus.pos != null)
//                tableModel.fireTableRowsUpdated(currentStatus.pos, currentStatus.pos);
//            if (status.pos != null)
//                tableModel.fireTableRowsUpdated(status.pos, status.pos);
//
//            boolean scrollToCurrent = false;
//            if (currentStatus == null)
//                scrollToCurrent = true;
//            currentStatus = status;
//            if (scrollToCurrent)
//                scrollToCurrent();
//        }
//        /*
//         * Update song info if necessary
//         */
//        if (currentStatus.pos == null)
//            return;
//
//        boolean update = false;
//        MPDPlaylistResponse.TrackInfo track = playlist[currentStatus.pos];
//        MPDStatusResponse.CurrentSong currentSong = currentStatus.currentSong;
//
//        if (track.title == null && currentSong.title != null) {
//            track.title = currentSong.title;
//            update = true;
//        }
//        if (track.album == null && currentSong.album != null) {
//            track.album = currentSong.album;
//            update = true;
//        }
//        if (track.artist == null && currentSong.artist != null) {
//            track.artist = currentSong.artist;
//            update = true;
//        }
//        if (track.date == null && currentSong.date != null) {
//            track.date = currentSong.date;
//            update = true;
//        }
//        if (track.track[0] == null && currentSong.track[0] != null) {
//            track.track[0] = currentSong.track[0];
//            update = true;
//        }
//
//        if (update)
//        {
//            System.out.println("UPDATE");
//            tableModel.fireTableRowsUpdated(track.pos, track.pos);
//        }
//    }
