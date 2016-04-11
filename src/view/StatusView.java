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
                "album",
                "date",
                "artist",
                "track",
                "title",
                "time",
                "state", /* PLAY, STOP, PAUSE */ 
                "mode",  /* repeat, random, single */ 
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

                if (mpdStatus.updating_db != null)
                    data.append("[UPDATING DB]");

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
                return new String("--:--");
            else if (time / 3600 > 0)
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
//            return statusByRow.length + 1;
        }
        @Override
        public Class getColumnClass(int column) {
            return String.class;
        }
        @Override
        public Object getValueAt(int row, int column) {
            if (row < statusByRow.length) {
                StatusData statusData = statusByRow[row];
                return statusData.value.toString();
            } else {
                return "";
            }
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

//            if (row < statusByRow.length) {
                JLabel label;
                label = (JLabel)super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                setBorder(noFocusBorder);
                label.setHorizontalAlignment(SwingConstants.CENTER);
    //            label.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

                /* XXX new Color */
                if ((row % 2) == 0)
                    label.setForeground(table.getForeground());
                else
                    label.setForeground(interlaceColor);
                return label;
//            } else {
//                JLabel label;
//                label = (ProgressBarView)super.getTableCellRendererComponent(
//                        table, value, isSelected, hasFocus, row, column);
//                return label;
//            }

//            cellSpacingLabel .setBorder(new CompoundBorder(new EmptyBorder(new Insets(1, 4, 1, 4)), oLabel.getBorder()));

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

