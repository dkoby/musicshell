/*
 *
 */
package mshell.view;
/* */
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.color.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.ScrollPaneConstants;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;
/* */
import mshell.MusicShell;
import mshell.Version;
import mshell.util.DPrint;
import mshell.view.vcomponent.*;
import mshell.view.FindBoxView;
import mshell.mpd.*;
import mshell.thread.ControlMessage;
import mshell.jni.fft.KissFFT;

/*
 *
 */
public class View extends JFrame {
    private MusicShell ms;
    /* */
    private ViewColors colors;
    /* */
    private JPanel mainPanel;
    private JPanel sidePanel;
    private JPanel listPanel;
    private VSpectrumView spectrumView;
    private VProgressBarView progressBarView;
    private VVolumeView volumeView;
    private JLabel mpdConnectStatus;
    private boolean mpdError = false;
    private JScrollPane playlistScrollPane;
    private JScrollPane browserScrollPane;
    private JLabel coverLabel;
    private String prevCoverPath;
    private PlaylistView playlistView;
    public BrowserView browserView;
    private StatusView statusView;
    private FindBoxView findBox;
    private Instant seekTime;
    private boolean zscroll;
    /**
     * @param ms instance of MusicShell (contain shared resources)
     */
    public View(MusicShell ms) {
        super("MusicShell " + Version.getString());
        this.ms = ms;

        colors = new ViewColors(ms.config.baseColor);

        setName("MusicShell " + Version.getString());
        Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        Dimension winSize;

        setUndecorated(true);
        if (ms.config.fullScreen)
            winSize = screenSize;
        else
            winSize = new Dimension(ms.config.windowWidth, ms.config.windowHeight);
        setSize(winSize.width, winSize.height);
        setLocation((screenSize.width - winSize.width) / 2, (screenSize.height - winSize.height) / 2);

        GridBagLayout mainGrid = new GridBagLayout();

        mainPanel = new JPanel();
        mainPanel.setLayout(mainGrid);
        mainPanel.setBorder(makeBorder(ms.config.defaultPadding));

        /*
         * Find Box
         */
        findBox = new FindBoxView(ms);
        setGlassPane(findBox);

        /*
         * Side panel.
         */
        {
            sidePanel = new JPanel();
            sidePanel.setBorder(makeBorder(ms.config.defaultPadding, Color.GREEN));
            sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
            sidePanel.setOpaque(false);

            sidePanel.setMinimumSize(new Dimension((int)(winSize.getWidth() * ms.config.sidePanelWidth), 0));

            {
                Component pad = Box.createVerticalStrut(ms.config.defaultPadding);

                if (ms.config.useSpectrumView) {
                    spectrumView = new VSpectrumView().start();
                    spectrumView.setAlignmentX(Component.CENTER_ALIGNMENT);
                    spectrumView.setBorder(makeBorder(8));
                    spectrumView.setMinimumSize(new Dimension(0, 64));
                    spectrumView.setPreferredSize(new Dimension(0, (int)(winSize.getWidth() / 2)));
                    spectrumView.setMaximumSize(new Dimension((int)winSize.getWidth(), (int)(winSize.getWidth() / 2)));
                }

                progressBarView = new VProgressBarView();
                progressBarView.setAlignmentX(Component.CENTER_ALIGNMENT);
                progressBarView.setMinimumSize(new Dimension(0, 8));
                progressBarView.setPreferredSize(new Dimension(0, 12));
                progressBarView.setMaximumSize(new Dimension((int)winSize.getWidth(), 8));

                volumeView = new VVolumeView();
                volumeView.setAlignmentX(Component.CENTER_ALIGNMENT);
                volumeView.setMinimumSize(new Dimension(0, 32));
                volumeView.setPreferredSize(new Dimension(0, 48));
                volumeView.setMaximumSize(new Dimension((int)winSize.getWidth(), 48));

                mpdConnectStatus = new JLabel("No connection");
                mpdConnectStatus.setBorder(makeBorder(2));
                mpdConnectStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
                mpdConnectStatus.setForeground(Color.WHITE);
                mpdConnectStatus.setOpaque(true);
                mpdConnectStatus.setFont(ms.config.mpdConnectStatusFont);

                coverLabel = new JLabel();

                statusView = new StatusView(ms);

                sidePanel.add(coverLabel);
                sidePanel.add(pad);
                sidePanel.add(Box.createVerticalGlue());
                sidePanel.add(statusView.getComponent());
                sidePanel.add(pad);
                sidePanel.add(progressBarView);
                sidePanel.add(pad);
                sidePanel.add(Box.createVerticalGlue());
                sidePanel.add(volumeView);
                sidePanel.add(pad);
                if (ms.config.useSpectrumView)
                    sidePanel.add(spectrumView);
                sidePanel.add(mpdConnectStatus);
            }

            GridBagConstraints gridConstraint = new GridBagConstraints();
            gridConstraint.gridx   = 0;
            gridConstraint.gridy   = 0;
//            gridConstraint.weightx = ms.config.sidePanelWidth;
//            gridConstraint.weighty = 1.0;
            gridConstraint.anchor  = GridBagConstraints.NORTH;
            gridConstraint.fill    = GridBagConstraints.BOTH;
            mainPanel.add(sidePanel, gridConstraint);

            /* XXX for proper cover resize during startup */
            sidePanel.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    progressBarView.revalidate();
                }
            });
        }
        /*
         * Browser(list) panel.
         */
        {
            listPanel = new JPanel();
            listPanel.setOpaque(false);
            listPanel.setLayout(new GridLayout());

            listPanel.setBorder(makeBorder(ms.config.defaultPadding));

            {
                playlistView = new PlaylistView(ms);

                playlistScrollPane = new JScrollPane(playlistView.getComponent());
                playlistScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                playlistScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
//                playlistScrollPane.setBorder(BorderFactory.createEmptyBorder());
                playlistScrollPane.setBorder(makeBorder(ms.config.defaultPadding, Color.GREEN));
                playlistScrollPane.setOpaque(false);

                browserView = new BrowserView(ms);
                browserScrollPane = new JScrollPane(browserView.getComponent());
                browserScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                browserScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
//                browserScrollPane.setBorder(BorderFactory.createEmptyBorder());
                browserScrollPane.setBorder(makeBorder(ms.config.defaultPadding, Color.GREEN));
                browserScrollPane.setOpaque(false);
            }

//            listPanel.addComponentListener(new ComponentAdapter() {
//                @Override
//                public void componentResized(ComponentEvent e) {
//                    playlistView.resize(playlistScrollPane.getWidth());
//                    browserView.resize(browserScrollPane.getWidth());
//                }
//            });

            GridBagConstraints gridConstraint = new GridBagConstraints();
            gridConstraint.gridx   = 1;
            gridConstraint.gridy   = 0;
//            gridConstraint.weightx = 1.0 - ms.config.sidePanelWidth;
            gridConstraint.weightx = 1.0;
            gridConstraint.weighty = 1.0;
            gridConstraint.anchor  = GridBagConstraints.NORTHWEST;
            gridConstraint.fill    = GridBagConstraints.BOTH;
            mainPanel.add(listPanel, gridConstraint);
        }

        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int newWidth  = mainPanel.getWidth();
                int newHeight = mainPanel.getHeight();

                int sideWidth = (int)(newWidth * ms.config.sidePanelWidth);

                sidePanel.setMinimumSize(new Dimension(sideWidth, 0));
                sidePanel.setPreferredSize(new Dimension(sideWidth, 0));

                int spectrumWidth  = sideWidth;
                int spectrumHeight = spectrumWidth / 2;
                if (ms.config.useSpectrumView)
                    spectrumView.setPreferredSize(new Dimension(spectrumWidth, spectrumHeight));
            }
        });

        KeyboardFocusManager.getCurrentKeyboardFocusManager().
            addKeyEventDispatcher(new ViewKeyDispatcher()); 

        addWindowFocusListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent e) {
                forceFocus();
            }
        });

        addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowActivated(WindowEvent e) {
//                System.out.println("activated");
//            }
            @Override
            public void windowClosing(WindowEvent e) {
                ms.terminate();
            }
        });


//        disableEvents(KeyEvent.MOUSE_EVENT_MASK);
        switchColors(ms.config.baseColor);
        setContentPane(mainPanel);
        setVisible(true);
    }
    /**
     *
     */
    public void forceFocus() {
        if (findBox.isVisible()) {
            findBox.requestFocus();
        } else {
            if (isBrowserMode()) {
                browserView.getComponent().requestFocusInWindow();
            } else {
                playlistView.getComponent().requestFocusInWindow();
            }
        }
    }
    /**
     *
     */
    public void drawMPDConnectStatus(String value, boolean error) {
        runLater(() -> {
            if (error) {
                mpdError = true;
                mpdConnectStatus.setBackground(Color.RED);
            } else {
                mpdError = false;
                mpdConnectStatus.setBackground(colors.bgColor);
            }
            mpdConnectStatus.setText(value);
        });
    }
    /**
     *
     */
    public boolean isPlaylistMode() {
        if (listPanel.isAncestorOf(playlistScrollPane))
            return true;
        else
            return false;
    }
    /**
     *
     */
    public boolean isBrowserMode() {
        return !isPlaylistMode();
    }
    /**
     * Draw status
     */
    public void drawStatus(MPDStatusResponse status) {
        runLater(() -> {
            statusView.updateStatus(status);
            playlistView.setCurrent(status);
            if (status.time[0] != null && status.time[1] != 0) {
                progressBarView.setValue(100 * status.time[0] / status.time[1]);
            } else {
                progressBarView.setValue(-1);
            }
            if (status.volume != null)
                volumeView.setValue(status.volume);
        });
    }
    /**
     * Load playlist
     */
    public void loadPlaylist(MPDPlaylistResponse response) {
        runLater(() -> {
            if (listPanel.isAncestorOf(browserScrollPane)) {
                browserView.saveCursor();
                listPanel.remove(browserScrollPane);
            }
            if (!listPanel.isAncestorOf(playlistScrollPane)) {
                listPanel.add(playlistScrollPane, BorderLayout.CENTER);
            }
            playlistView.rebuild(response);
            listPanel.revalidate();
            listPanel.repaint();

            runLater(() -> {
                playlistView.resize(playlistScrollPane.getWidth());
            });
        });
    }
    /**
     * Load browser
     */
    public void loadBrowser(File currentPath, MPDDBFilesResponse response) {
        runLater(() -> {
            if (listPanel.isAncestorOf(playlistScrollPane)) {
                playlistView.saveCursor();
                listPanel.remove(playlistScrollPane);
            }
            if (!listPanel.isAncestorOf(browserScrollPane)) {
                listPanel.add(browserScrollPane, BorderLayout.CENTER);
            }
            browserView.rebuild(currentPath, response);
            listPanel.revalidate();
            listPanel.repaint();

            runLater(() -> {
                browserView.resize(browserScrollPane.getWidth());
            });
        });
    }
    /**
     *
     */
    public void loadNoCover() {
        loadCover(null);
    }
    /**
     *
     */
    public void loadCover(String pathToLoad) {
        final String noCoverString = "NOCOVER";
        runLater(() -> {
            String coverPath;
            if (pathToLoad == null)
                coverPath = noCoverString;
            else
                coverPath = pathToLoad;

            if (prevCoverPath == null || !prevCoverPath.equals(coverPath)) {
                int width = progressBarView.getWidth();

                Image image = null;
                try {
                    InputStream inputStream;
                    if (coverPath.equals(noCoverString))
                        inputStream = getClass().getResourceAsStream("/res/img/nocover.png");
                    else
                        inputStream = new FileInputStream(new File(coverPath));
                    image = loadImage(inputStream, width, width);
                } catch (Exception e) {
                    DPrint.format(DPrint.Level.EXCEPTION, "Failed to load image \"%s\", %s%n",
                        coverPath, e.toString());
                    e.printStackTrace();
                }
                if (image != null)
                    coverLabel.setIcon(new ImageIcon(image));
                prevCoverPath = coverPath;
            }
        });
    }
    /**
     *
     */
    public void switchColors(Color newColor) {
        colors.setAll(newColor);
        colors.apply();
    }
    /**
     *
     */
    public void drawSpectrum(KissFFT fft) {
        spectrumView.drawSpectrum(fft);
    }

    /**
     * Used for interface color manipulation
     */
    private class ViewColors {
        private Color bgColor;
        private Color fgColor;
        private Color listSelBgColor;
        private Color listSelFgColor;
        private Color listCurFgColor;
        private Color listCurSelFgColor;
        /**
         *
         */
        private ViewColors(Color baseColor) {
            setAll(baseColor);
        }
        /**
         *
         */
        private void setAll(Color newColor) {
            /* adjust background color */
            {
                float[] hsb = ColorUtil.colorToHSB(newColor);
                final float BRIGHTNESS_MAX = 0.6f;
                final float BRIGHTNESS_MIN = 0.05f;
                if (hsb[2] > BRIGHTNESS_MAX) {
                    hsb[2] = BRIGHTNESS_MAX;
                } else if (hsb[2] < BRIGHTNESS_MIN) {
                    hsb[2] = BRIGHTNESS_MIN;
                }

                bgColor = ColorUtil.hsbToColor(hsb);

//                System.out.println(
//                        " R " + bgColor.getRed() +   
//                        " G " + bgColor.getGreen() +   
//                        " B " + bgColor.getBlue());
//                System.out.println(
//                        " H " + hsb[0] +
//                        " S " + hsb[1] + 
//                        " B " + hsb[2]);
            }
            /* adjust foreground(font) color */
            {
                fgColor = ColorUtil.blurColor(bgColor);

                float[] hsb = ColorUtil.colorToHSB(fgColor);
                if (hsb[2] < 0.6f)
                    hsb[2] = 0.9f;
                else
                    hsb[2] = 0.1f;
                fgColor = ColorUtil.hsbToColor(hsb);
            }
            /* adjust selection, current track color */
            {
                float[] hsb = ColorUtil.colorToHSB(newColor);
                if (hsb[2] < 0.5) {
                    listSelBgColor    = Color.WHITE;
                    listCurSelFgColor = Color.PINK;
                } else {
                    listSelBgColor    = Color.BLACK;
                    listCurSelFgColor = Color.YELLOW;
                }
                listSelFgColor = ColorUtil.invertColor(listSelBgColor);

                if (fgColor.getRed() > 200 && fgColor.getGreen() > 200 && fgColor.getBlue() < 200)
                {
                    listCurFgColor = Color.WHITE;
                } else {
                    listCurFgColor = Color.YELLOW;
                }
            }
        }
        /**
         *
         */
        private void apply() {
            mainPanel.setBackground(bgColor);
            if (ms.config.useSpectrumView)
                spectrumView.setColors(bgColor);
            progressBarView.setColors(fgColor, bgColor);
            volumeView.setColors(fgColor, bgColor);
            playlistScrollPane.getViewport().setBackground(bgColor);
            browserScrollPane.getViewport().setBackground(bgColor);
            playlistView.setColors(bgColor, fgColor, listSelBgColor,
                    listSelFgColor, listCurFgColor, listCurSelFgColor);
            browserView.setColors(bgColor, fgColor, listSelBgColor, listSelFgColor, listCurFgColor);
            statusView.setColors(bgColor, fgColor);
            findBox.setColors(bgColor, fgColor);
            if (mpdError)
                mpdConnectStatus.setBackground(Color.RED);
            else
                mpdConnectStatus.setBackground(bgColor);
            /* this should trigger other components repaint */
            mainPanel.repaint();
        }
    }
    /**
     * Used for keypress processing
     */
    private class ViewKeyDispatcher implements KeyEventDispatcher {
        Runnable prevDir = new Runnable() {
            @Override
            public void run() {
                browserView.saveCursor();
                ControlMessage message;

                message = new ControlMessage(ControlMessage.Id.SWITCHTOBROWSER);
                message.object = message.new SwitchToBrowser(true); 
                ms.putControlMessage(message);
            }
        };
        Runnable playTrack = new Runnable() {
            @Override
            public void run() {
                int row = playlistView.getSelectedRow();
                if (row < 0)
                    return;
                ControlMessage message;

                message = new ControlMessage(ControlMessage.Id.PLAYTRACK);
                message.object = new Integer(row);
                ms.putControlMessage(message);
            }
        };
        Runnable dirEnter = new Runnable() {
            @Override
            public void run() {
                FileDescription file = browserView.getFileUnderCursor();
                if (file.type.equals(FileDescription.Type.DIR))
                {
                    if (file.name.equals(".."))
                    {
                        prevDir.run();
                    } else {
                        browserView.saveCursor();
                        new GoNextDirectory(file.name).run();
                    }
                }
            }
        };

        class GoNextDirectory implements Runnable {
            String nextDir;
            private GoNextDirectory(String nextDir) {
                this.nextDir = nextDir;
            }
            @Override
            public void run() {
                ControlMessage message;

                message = new ControlMessage(ControlMessage.Id.SWITCHTOBROWSER);
                message.object = message.new SwitchToBrowser(nextDir); 
                ms.putControlMessage(message);
            }
        }
        class SimulateKey {
            private SimulateKey(JComponent component, int modifiers, int keyCode) {
                KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                KeyEvent event = new KeyEvent(component, 
                    KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, 
                    keyCode, KeyEvent.CHAR_UNDEFINED);
                component.dispatchEvent(event);
            }
        };
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (findBox.isVisible()) {
                AtomicBoolean proceed = new AtomicBoolean(false);
                boolean ret = findBox.dispatchKeyEvent(e, proceed);
                if (!proceed.get())
                    return ret;

//                System.out.println("PROCEED");
//                /* XXX */
//                dirEnter.run();
//                return true;
            }

            if (e.getID() == KeyEvent.KEY_PRESSED) {
//                    DPrint.format(DPrint.Level.VERBOSE4,
//                        "%s \"%c\" 0x%02x%n", "Got key typed", e.getKeyChar(), (int)e.getKeyChar());
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_TAB:
                        ms.putControlMessage(new ControlMessage(ControlMessage.Id.TOGGLEMODE));
                        e.consume();
                        return true;
                    case KeyEvent.VK_BACK_SPACE:
                        if (isBrowserMode()) {
                            prevDir.run();
                        }
                        return true;
                    case KeyEvent.VK_ENTER:
                        if (isBrowserMode()) {
                            dirEnter.run();
                        } else if (isPlaylistMode()) {
                            playTrack.run();
                        }
                        return true;
                    case KeyEvent.VK_M:
                        if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                            if (isPlaylistMode()) {
                                playTrack.run();
                            }
                        }
                        return true;
                    case KeyEvent.VK_SPACE:
                        if (isBrowserMode()) {
                            FileDescription file = browserView.getFileUnderCursor();
                            if (file != null) {
                                ControlMessage message = new ControlMessage(ControlMessage.Id.ADDTRACKS);
                                message.object = message.new AddTracks(file.name);
                                ms.putControlMessage(message);
                            }
                        }
                        return true;
                    case KeyEvent.VK_D:
                        if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                            new SimulateKey((JComponent)e.getSource(), 0, KeyEvent.VK_PAGE_DOWN);
                        }
                        return true;
                    case KeyEvent.VK_U:
                        if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                            new SimulateKey((JComponent)e.getSource(), 0, KeyEvent.VK_PAGE_UP);
                        } else {
                            if (isBrowserMode()) {
                                ms.putControlMessage(new ControlMessage(ControlMessage.Id.UPDATEDB));
                            }
                        }
                        return true;
                    case KeyEvent.VK_J:
                        new SimulateKey((JComponent)e.getSource(), e.getModifiers(), KeyEvent.VK_DOWN);
                        return true;
                    case KeyEvent.VK_K:
                        new SimulateKey((JComponent)e.getSource(), e.getModifiers(), KeyEvent.VK_UP);
                        return true;
                    case KeyEvent.VK_N:
                        if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                            new SimulateKey((JComponent)e.getSource(), 0, KeyEvent.VK_DOWN);
                        }
                        return true;
                    case KeyEvent.VK_P:
                        if (e.getModifiers() == 0) {
                            ms.putControlMessage(new ControlMessage(ControlMessage.Id.PLAYPAUSE));
                        } else if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                            new SimulateKey((JComponent)e.getSource(), 0, KeyEvent.VK_UP);
                        }
                        return true;
                    case KeyEvent.VK_R:
                        if ((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
                            ms.putControlMessage(new ControlMessage(ControlMessage.Id.TOGGLERANDOM));
                        } else if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                            ms.putControlMessage(new ControlMessage(ControlMessage.Id.TOGGLESINGLE));
                        } else {
                            ms.putControlMessage(new ControlMessage(ControlMessage.Id.TOGGLEREPEAT));
                        }
                        return true;
                    case KeyEvent.VK_O:
                        if (isPlaylistMode()) {
                            playlistView.scrollToCurrent();
                        }
                        return true;
                    case KeyEvent.VK_X:
                    case KeyEvent.VK_DELETE:
                        if (isPlaylistMode()) {
                            playlistView.saveCursor();
                            ControlMessage message = new ControlMessage(ControlMessage.Id.DELETETRACKS);
                            message.object = message.new DeleteTracks(playlistView.getSelectedIndices());
                            ms.putControlMessage(message);
                        }
                        return true;
                    case KeyEvent.VK_S:
                        ms.putControlMessage(new ControlMessage(ControlMessage.Id.STOP));
                        return true;
                    case KeyEvent.VK_C:
                        if (isPlaylistMode()) {
                            if (e.getModifiers() == 0)
                                ms.putControlMessage(new ControlMessage(ControlMessage.Id.PLAYLISTCLEAR));
                        }
                        return true;
                    case KeyEvent.VK_Q:
                        ms.putControlMessage(new ControlMessage(ControlMessage.Id.SETVOLUME, (Object)new Integer(1)));
                        return true;
                    case KeyEvent.VK_A:
                        ms.putControlMessage(new ControlMessage(ControlMessage.Id.SETVOLUME, (Object)new Integer(-1)));
                        return true;
                    case KeyEvent.VK_SLASH:
                        if (isBrowserMode()) {
                            findBox.popUp();
                        }
                        return true;
                    case KeyEvent.VK_F:
                        if (seekTime == null || Duration.between(seekTime, Instant.now()).toMillis() > 100) {
                            seekTime = Instant.now();
                            Integer adjust = ms.config.seekSeconds;
                            if ((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0)
                                adjust = adjust * (-1);
                            ms.putControlMessage(
                                    new ControlMessage(ControlMessage.Id.SEEKSONG,
                                        (Object)new Integer(adjust)));
                        }
                        return true;
                    case KeyEvent.VK_Z:
                        if (!zscroll) {
                            zscroll = true;
                            return true;
                        }
                        zscroll = false;
                        if (isPlaylistMode()) {
                            playlistView.scrollToCenter();
                        } else {
                            browserView.scrollToCenter();
                        }
                        return true;
                    default:
                        return false;
                }
            }
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_PAGE_UP:
                case KeyEvent.VK_PAGE_DOWN:
                    return false;
            }
            return true;
        }
    }
    /**
     *
     */
    private Border makeBorder(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }
    /**
     *
     */
    private Border makeBorder(int width) {
        if (ms.debugView)
            return BorderFactory.createLineBorder(Color.RED, width);
        else
            return BorderFactory.createEmptyBorder(width, width, width, width);
    }
    /**
     *
     */
    private Border makeBorder(int width, Color color) {
        if (ms.debugView)
            return BorderFactory.createLineBorder(color, width);
        else
            return BorderFactory.createEmptyBorder(width, width, width, width);
    }
    /**
     *
     */
    private Border makeDebugBorder() {
        if (ms.debugView)
            return BorderFactory.createLineBorder(Color.RED, 1);
        else
            return BorderFactory.createEmptyBorder();
    }
    /**
     * Wrapper for run runnable in event dispatch thread
     */
    private static void runLater(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }
    /**
     *
     */
    private Image loadImage(InputStream inputStream, int width, int height) {
        BufferedImage bImage;

        try {
            bImage = ImageIO.read(inputStream);
        } catch (Exception e) {
            DPrint.format(DPrint.Level.EXCEPTION, "Failed to load image, %s%n", e);
            e.printStackTrace();
            return null;
        }

        switchColors(getColorsBaseCover(bImage));

        return bImage.getScaledInstance(width, height, Image.SCALE_DEFAULT);
    }
    /**
     * Switch colors based on cover
     */
    private Color getColorsBaseCover(BufferedImage bImage) {
        int width = bImage.getWidth();
        int height = bImage.getHeight();
        /*
         * ┏━━━━━━━━━━━━━━┓
         * ┃   ▄▄▄▄▄▄▄▄   ┃
         * ┃  █        █  ┃
         * ┃  █        █<-┃-- border
         * ┃  █        █  ┃
         * ┃   ▀▀▀▀▀▀▀▀   ┃
         * ┗━━━━━━━━━━━━━━┛
         */
        final int BORDER_OFFSET  = 8;
        final int BORDER_SIZE    = 8;
        final int BORDER_WIDTH   = width  - 2 * (BORDER_OFFSET + BORDER_SIZE);
        final int BORDER_HEIGHT  = height - 2 * (BORDER_OFFSET + BORDER_SIZE);

        if (BORDER_WIDTH <= 0 || BORDER_HEIGHT <= 0)
            return ms.config.baseColor;

        AverageColor averageColor = new AverageColor(bImage);

        /* top */
        averageColor.summRegion(BORDER_OFFSET + BORDER_SIZE,
                                BORDER_OFFSET,
                                BORDER_WIDTH, BORDER_SIZE);
        /* bottom */
        averageColor.summRegion(BORDER_OFFSET + BORDER_SIZE,
                                BORDER_OFFSET + BORDER_SIZE + BORDER_HEIGHT,
                                BORDER_WIDTH , BORDER_SIZE);
        /* left */
        averageColor.summRegion(BORDER_OFFSET,
                                BORDER_OFFSET + BORDER_SIZE,
                                BORDER_SIZE , BORDER_HEIGHT);
        /* right */
        averageColor.summRegion(BORDER_OFFSET + BORDER_SIZE + BORDER_WIDTH,
                                BORDER_OFFSET + BORDER_SIZE,
                                BORDER_SIZE , BORDER_HEIGHT);
        Color baseColor = averageColor.getResult();

        float[] hsb = Color.RGBtoHSB(
                baseColor.getRed(),
                baseColor.getGreen(), 
                baseColor.getBlue(),
                null);

//        DPrint.format(DPrint.Level.VERBOSE4, "HSB: %f %f %f%n", hsb[0], hsb[1], hsb[2]);
        final float BRIGHTNESS_MAX = 0.5f;
        if (hsb[2] > BRIGHTNESS_MAX) {
            hsb[2] = BRIGHTNESS_MAX;
            baseColor = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
        }

        return baseColor;
    }
}

/*
 * TODO
 *   * out of bounds exception
 *   * npix zero exception
 */
class AverageColor {
    BufferedImage image;
    int[] rgbArray;
    int[] rgb;
    int npix;
    /**
     *
     */
    public AverageColor(BufferedImage image) {
        this.image  = image;
        rgbArray = new int[image.getWidth() * image.getHeight()];
        rgb = new int[3];
        npix = 0;
    }
    /**
     *
     */
    public void reset() {
        npix = 0;
        java.util.Arrays.fill(rgb, 0);
    }
    /**
     *
     */
    public void summRegion(int startX, int startY, int width, int height) {
        image.getRGB(startX, startY, width, height, rgbArray, 0, width);
        for (int i = 0; i < width * height; i++) {
            rgb[0] += (rgbArray[i] >> 16) & 0xff;
            rgb[1] += (rgbArray[i] >> 8 ) & 0xff;
            rgb[2] += (rgbArray[i] >> 0 ) & 0xff;
        }
        npix += width * height;
        /* for testing - draw region on image */
        if (false) {
            java.util.Arrays.fill(rgbArray, 0xffffff);
            image.setRGB(startX, startY, width, height, rgbArray, 0, width);
        }
    }
    /**
     *
     */
    public Color getResult() {
        rgb[0] /= npix;
        rgb[1] /= npix;
        rgb[2] /= npix;
        return new Color(rgb[0], rgb[1], rgb[2]);
    }
};

