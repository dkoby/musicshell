/*
 *
 */
package mshell.view;
/* */
import java.awt.*;
import javax.swing.*;
//import javax.swing.tree.TreePath;
//import javax.swing.event.*;
//import javax.swing.border.*;
import javax.swing.table.*;
//import javax.swing.ScrollPaneConstants;
/* */
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.io.File;
/* */
import mshell.util.DPrint;
import mshell.MusicShell;
import mshell.Config;
import mshell.mpd.*;

/**
 * Browser table
 */
public class BrowserView {
    private MusicShell ms;
    private JTable table;
    private BrowserTableModel tableModel;
    private TableColumnModel columnModel;
//    private DefaultListSelectionModel selectionModel;
    private Color curFgColor;
    private HashMap<File, CurrentDir> dirs;
    private CurrentDir currentDir;
    /**
     *
     */
    private class CurrentDir {
        int cursor = 0;
        public MPDDBFilesResponse.Entry[] entries;
    } 
    /**
     *
     */
    public BrowserView(MusicShell ms) {
        this.ms = ms;
        tableModel = new BrowserTableModel();
        table = new JTable(tableModel);

        table.setShowVerticalLines  (ms.debugView);
        table.setShowHorizontalLines(ms.debugView);
        table.setFont(ms.config.listFont);
        table.setTableHeader(null);
        table.setDefaultRenderer(Object.class, new BrowserTableCellRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        columnModel = table.getColumnModel();

        dirs = new HashMap<>();
    }
    /**
     *
     */
    public void rebuild(File path, MPDDBFilesResponse response) {
        MPDDBFilesResponse.Entry[] entries = new MPDDBFilesResponse.Entry[response.entries.size()];
        response.entries.toArray(entries);

        if (!dirs.containsKey(path)) {
            DPrint.format(DPrint.Level.VERBOSE3, "%s%n", "new entry " + path);
            dirs.put(path, new CurrentDir());
        }
        currentDir = dirs.get(path);
        currentDir.entries = entries;

        tableModel.fireTableDataChanged();

        table.requestFocus();
        if (currentDir.entries.length > currentDir.cursor) {
            table.setRowSelectionInterval(0, currentDir.cursor);
        } else {
            if (currentDir.entries.length > 0)
                table.setRowSelectionInterval(0, 0);
        }
        scrollToSelected();
    }
    /**
     *
     */
    public void scrollToCenter() {
        Rectangle visRect = table.getVisibleRect();

        int visRows = (int)(visRect.getHeight() / table.getRowHeight());

        int x       = 0;
        int width   = table.getWidth();
        int y       = (int)((table.getSelectedRow() - visRows / 2) * table.getRowHeight());
        int height  = (visRows - 1) * table.getRowHeight();

        table.scrollRectToVisible(new Rectangle(x, y, width, height));
    }
    /**
     *
     */
    public void setColors(
            Color bgColor, Color fgColor,
            Color selBgColor, Color selFgColor,
            Color curFgColor) {

        table.setBackground(bgColor);
        table.setForeground(fgColor);
        table.setSelectionBackground(selBgColor);
        table.setSelectionForeground(selFgColor);

        this.curFgColor = curFgColor;
//        fireTableRowsUpdated
    }
    /**
     *
     */
    public FileDescription getFileUnderCursor() {
        int idx = table.getSelectedRow();
        if (idx < 0)
            return null;

        MPDDBFilesResponse.Entry entry = currentDir.entries[idx];

        if (idx >= 0) {
            if (entry.type.equals(MPDDBFilesResponse.EntryType.DIR))
                return new FileDescription(entry.name, FileDescription.Type.DIR);
            else
                return new FileDescription(entry.name, FileDescription.Type.FILE);
        }

        return null;
    }
    /**
     *
     */
    public void saveCursor() {
        ListSelectionModel selectionModel = table.getSelectionModel();
        if (!selectionModel.isSelectionEmpty()) {
            int cursor = selectionModel.getMaxSelectionIndex();
            if (cursor >= 0) {
                currentDir.cursor = cursor;
            }
        }
    }
    /**
     *
     */
    public void scrollToSelected() {
        if (currentDir.entries.length > currentDir.cursor) {
            int x       = 0;
            int width   = table.getWidth();
            int y       = currentDir.cursor * table.getRowHeight();
            int height  = table.getRowHeight();

            table.scrollRectToVisible(new Rectangle(x, y, width, height));
        }
    }
    /**
     *
     */
    public void scrollTo(int pos) {
        int x       = 0;
        int width   = table.getWidth();
        int y       = pos * table.getRowHeight();
        int height  = table.getRowHeight();

        table.scrollRectToVisible(new Rectangle(x, y, width, height));
    }
    /**
     * Search and scroll.
     *
     * @param text text to search for
     *
     * @return true if something found.
     */
    public boolean searchFor(String text) {
//        System.out.println("search for \"" + text + "\"");

        int idx = 0;
        for (MPDDBFilesResponse.Entry entry: currentDir.entries) {
            String name = entry.name;

            if (name.regionMatches(true, 0, text, 0, text.length())) {
                scrollTo(idx);
                table.setRowSelectionInterval(0, idx);
                return true;
            }
            idx++;
        }
        return false;
    }
    /**
     * Resize columns of table in proportions.
     */
    public void resize(int width) {
        columnModel.getColumn(0).setMaxWidth(width);
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
    class BrowserTableModel extends AbstractTableModel {
        private int columnCount = 1;

        public BrowserTableModel() {
        }

        @Override
        public int getColumnCount() {
            return columnCount;
        }
        @Override
        public int getRowCount() {
            if (currentDir != null)
                return currentDir.entries.length;
            else
                return 0;
        }
        @Override
        public Class getColumnClass(int column) {
            return String.class;
        }
        @Override
        public Object getValueAt(int row, int column) {
            if (currentDir == null || currentDir.entries.length == 0)
                return new String("");

            return currentDir.entries[row].name;
        }
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
    /**
     *
     */
    class BrowserTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
               boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel)super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            setBorder(noFocusBorder);

            if (currentDir.entries[row].type.equals(MPDDBFilesResponse.EntryType.FILE)) {
                if (table.isRowSelected(row)) {
                    label.setForeground(table.getSelectionForeground());
                } else {
                    label.setForeground(curFgColor);
                }
            } else {
                if (table.isRowSelected(row)) {
                    label.setForeground(table.getSelectionForeground());
                } else {
                    label.setForeground(table.getForeground());
                }
            }

            return label;
        }
    }
}

