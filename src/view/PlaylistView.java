/*
 *
 */
package mshell.view;
/* */
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
/* */
import mshell.MusicShell;
import mshell.Config;
import mshell.Config.PlaylistColumnsConfig;
import mshell.mpd.*;

/**
 * Play list table
 */
public class PlaylistView {
    private MusicShell ms;
    private JTable table;
    private PlaylistTableModel tableModel;
    private TableColumnModel columnModel;
    private Color currentTrackFgColor;
    private Color currentTrackSelFgColor;

    private MPDPlaylistResponse.TrackInfo[] playlist;
    private MPDStatusResponse currentStatus;
    int cursor = 0;
    /**
     *
     */
    public PlaylistView(MusicShell ms) {
        this.ms = ms;
        tableModel = new PlaylistTableModel(ms.config);
        table = new JTable(tableModel);

        table.setShowVerticalLines  (ms.debugView);
        table.setShowHorizontalLines(ms.debugView);
        table.setFont(ms.config.listFont);
        table.setTableHeader(null);
        table.setDefaultRenderer(Object.class, new PlaylistTableCellRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
//        table.setCellSelectionEnabled(false);

        columnModel = table.getColumnModel();
    }
    /**
     *
     */
    public void rebuild(MPDPlaylistResponse mpdPlaylist) {
        playlist = new MPDPlaylistResponse.TrackInfo[mpdPlaylist.tracks.size()];
        mpdPlaylist.tracks.toArray(playlist);

        tableModel.fireTableDataChanged();

        table.requestFocus();

        if (playlist.length > cursor) {
            setCursor(cursor);
        } else {
            if (cursor >= playlist.length)
                setCursor(playlist.length - 1);
            else
                setCursor(0);
        }
        scrollToCurrent();
    }
    /**
     *
     */
    private void setCursor(int pos) {
        table.setRowSelectionInterval(pos, pos);
    } 
    /**
     *
     */
    public void saveCursor() {
        ListSelectionModel selectionModel = table.getSelectionModel();
        if (!selectionModel.isSelectionEmpty()) {
            int cursor = selectionModel.getMaxSelectionIndex();
            if (cursor >= 0) {
                this.cursor = cursor;
            }
        }
    }
    /**
     *
     */
    public void scrollToCurrent() {
        if (currentStatus != null && currentStatus.pos != null && currentStatus.pos < playlist.length) {
            int x       = 0;
            int width   = table.getWidth();
            int y       = currentStatus.pos * table.getRowHeight();
            int height  = table.getRowHeight();

            table.scrollRectToVisible(new Rectangle(x, y, width, height));
            setCursor(currentStatus.pos);
        }
    }
    /**
     *
     */
    public int[] getSelectedIndices() {
        return table.getSelectedRows();
    }
    /**
     *
     */
    public int getSelectedRow() {
        return table.getSelectedRow();
    }
    /**
     *
     */
    public void setColors(
            Color bgColor, Color fgColor,
            Color selBgColor, Color selFgColor,
            Color currentTrackFgColor, Color currentTrackSelFgColor) {
        table.setBackground(bgColor);
        table.setForeground(fgColor);

        table.setSelectionBackground(selBgColor);
        table.setSelectionForeground(selFgColor);

        this.currentTrackFgColor = currentTrackFgColor;
        this.currentTrackSelFgColor = currentTrackSelFgColor;
    }
    /**
     * Set current song
     */
    public void setCurrent(MPDStatusResponse status) {
        if (playlist == null || playlist.length == 0)
            return;
        /*
         * Update current track if necessary
         */
        if (currentStatus == null || status.pos != currentStatus.pos) {
            if (currentStatus != null && currentStatus.pos != null)
                tableModel.fireTableRowsUpdated(currentStatus.pos, currentStatus.pos);
            if (status.pos != null)
                tableModel.fireTableRowsUpdated(status.pos, status.pos);

            boolean scrollToCurrent = false;
            if (currentStatus == null)
                scrollToCurrent = true;
            currentStatus = status;
            if (scrollToCurrent)
                scrollToCurrent();
        }
        /*
         * Update song info if necessary
         */
        if (currentStatus.pos == null)
            return;

        boolean update = false;
        MPDPlaylistResponse.TrackInfo track = playlist[currentStatus.pos];
        MPDStatusResponse.CurrentSong currentSong = currentStatus.currentSong;

        if (track.title == null && currentSong.title != null) {
            track.title = currentSong.title;
            update = true;
        }
        if (track.album == null && currentSong.album != null) {
            track.album = currentSong.album;
            update = true;
        }
        if (track.artist == null && currentSong.artist != null) {
            track.artist = currentSong.artist;
            update = true;
        }
        if (track.date == null && currentSong.date != null) {
            track.date = currentSong.date;
            update = true;
        }
        if (track.track[0] == null && currentSong.track[0] != null) {
            track.track[0] = currentSong.track[0];
            update = true;
        }

        if (update)
        {
            System.out.println("UPDATE");
            tableModel.fireTableRowsUpdated(track.pos, track.pos);
        }
    }
    /**
     * Resize columns of table in proportions.
     */
    public void resize(int width) {
        int idx = 0;
        for (Config.PlaylistColumnsConfig cc: ms.config.playlistColumnConfig) {
            columnModel.getColumn(idx++).setMaxWidth(width * cc.width / 100);
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
    class PlaylistTableModel extends AbstractTableModel {
        private int columnCount = 0;

        public PlaylistTableModel(Config config) {
            columnCount = config.playlistColumnConfig.length;
        }

        @Override
        public int getColumnCount() {
            return columnCount;
        }
        @Override
        public int getRowCount() {
            if (playlist != null)
                return playlist.length;
            else
                return 0;
        }
        @Override
        public Class getColumnClass(int column) {
            return String.class;
        }
        @Override
        public Object getValueAt(int row, int column) {
            if (playlist == null)
                return new String("");

            MPDPlaylistResponse.TrackInfo track = playlist[row];
            PlaylistColumnsConfig config = ms.config.playlistColumnConfig[column];

            if (config.name.equals("artist")) {
                if (track.artist != null)
                    return new String(track.artist);
            } else if (config.name.equals("date")) {
                if (track.date != null)
                    return new String(track.date);
            } else if (config.name.equals("album")) {
                if (track.album != null)
                    return new String(track.album);
            } else if (config.name.equals("track")) {
                if (track.track[0] != null)
                    return String.format("%02d", track.track[0]);
            } else if (config.name.equals("title")) {
                if (track.title != null)
                    return new String(track.title); 
                else
                    return new String(track.file); 
            } else if (config.name.equals("time")) {
                if (track.time != null)
                {
                    long time = track.time;
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
            return new String("");
        }
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
    /**
     *
     */
    class PlaylistTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
               boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel)super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            MPDPlaylistResponse.TrackInfo track = playlist[row];
            if (currentStatus == null) {
                label.setForeground(table.getForeground());
            } else {
                if (currentStatus.pos.equals(track.pos)) {
                    if (table.isRowSelected(row)) {
                        label.setForeground(currentTrackSelFgColor);
                    } else {
                        label.setForeground(currentTrackFgColor);
                    }
                } else {
                    if (table.isRowSelected(row)) {
                        label.setForeground(table.getSelectionForeground());
                    } else {
                        label.setForeground(table.getForeground());
                    }
                }
            }

            return label;
        }
    }
}

