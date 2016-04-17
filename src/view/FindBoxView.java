/*
 *
 */
package mshell.view;
/* */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;
/* */
import mshell.MusicShell;

/**
 *
 */
public class FindBoxView extends JComponent {
    private MusicShell ms;
    private JTextField textField;
    private boolean hide = true;
    private Thread hideThread;
    /**
     *
     */
    public FindBoxView(MusicShell ms) {
        super(); /* XXX */

        this.ms = ms;

        textField = new JTextField(20);
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        textField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));

        setLayout(null);
        add(textField);

        addMouseListener(new ThisMouseAdapter());

        setVisible(false);
        setOpaque(false);
    }
    /**
     *
     */
    public void setColors(Color bgColor, Color fgColor) {
        textField.setBackground(fgColor);
        textField.setForeground(bgColor);
        textField.setBorder(BorderFactory.createLineBorder(bgColor.brighter(), 2));
    }
    /**
     *
     */
    public void popUp() {
        Dimension tsize = textField.getPreferredSize();

        final int PAD = 16;
        textField.setBounds(
                getWidth() - tsize.width - PAD, getHeight() - tsize.height - PAD,
                tsize.width, tsize.height);
        textField.setText("");

        hideThread = new Thread(() -> {
            while (true) {
                synchronized (this) {hide = true;}

                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    break;
                }

                synchronized (this) {
                    if (hide) {
                        dispose();
                        break;
                    }
                }
            }
        });
        hideThread.setDaemon(true);
        hideThread.start();

        setVisible(true);
        textField.requestFocus();
    }
    /**
     *
     */
    public void dispose() {
        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            ms.view.forceFocus();
        });
    }
    /**
     * @return true if event was processed
     */
    public boolean dispatchKeyEvent(KeyEvent e, AtomicBoolean proceed) {
        if (e.getID() == KeyEvent.KEY_TYPED) {
            switch (e.getKeyChar()) {
                case '/':
                case '\r':
                case '\n':
                    return true;
                case 0x08:
                    return false;
            }
            synchronized (this) {hide = false;}

            String newText = textField.getText() + e.getKeyChar();
            if (ms.view.browserView.searchFor(newText))
                return false;

            return true;
        } else if (e.getID() == KeyEvent.KEY_PRESSED) {
            switch(e.getKeyCode()) {
                case KeyEvent.VK_BACK_SPACE:
                    return false;
                case KeyEvent.VK_ENTER:
                    proceed.set(true);
                case KeyEvent.VK_ESCAPE:
                    hideThread.interrupt();
                    dispose();
                    return true;
                default:
                    return true;
            }
        }
        return true;
    }
    /**
     *
     */
    @Override
    public void requestFocus() {
        textField.requestFocus();
    }
    /**
     *
     */
    private class ThisMouseAdapter extends MouseAdapter {
        /**
         *
         */
        @Override
        public void mouseClicked(MouseEvent e) {

        }
    }
}

