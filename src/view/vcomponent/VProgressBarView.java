/*
 *
 */
package mshell.view.vcomponent;
/* */
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
/* */
import mshell.Config;
import mshell.view.ColorUtil;

/*
 *
 */
public class VProgressBarView extends JComponent {
    private Color borderColor;
    private Color fgColor;
    private Color bgColor;
    private Color bgBaseColor;
    int percents;
    /* */
    private final int BORDER_WIDTH = 2;
    private BasicStroke borderStroke;
    /**
     *
     */
    public VProgressBarView() {
        borderColor = Color.WHITE;
        fgColor     = Color.BLACK;
        bgColor     = Color.YELLOW;
        borderStroke = new BasicStroke((float)BORDER_WIDTH);
        percents = 0;
    }
    /**
     *
     */
    public void setColor(Color fgColor, Color bgColor) {
        this.bgBaseColor = bgColor;
        this.fgColor = fgColor;

        float[] hsb = ColorUtil.colorToHSB(bgColor);
        if (hsb[2] > 0.5f) {
            this.bgColor = bgColor.darker();
            hsb[2] = 0;
            this.borderColor = ColorUtil.hsbToColor(hsb);
        } else {
            this.bgColor = bgColor.brighter();
            hsb[2] = 1.0f;
            this.borderColor = ColorUtil.hsbToColor(hsb);
        }
    }
    /**
     *
     */
    public void setValue(int percents) {
        if (percents > 100)
            percents = 100;
        if (percents != this.percents)
        {
            this.percents = percents;
            repaint();
        }
    }
    /**
     *
     */
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;

        int width = getWidth() - BORDER_WIDTH * 2;
        if (width < 0)
            return;

        if (percents < 0) {
            g.setColor(bgBaseColor);
            g.fillRect(0, 0, getWidth(), getHeight());
            return;
        }

        int part1 = width * percents / 100;
        int part2 = width - part1;
        int x = BORDER_WIDTH;

        if (part1 > 0) {
            g.setColor(fgColor);
            g.fillRect(x, BORDER_WIDTH, part1, getHeight() - BORDER_WIDTH * 2);
            x += part1;
        }
        if (part2 > 0) {
            g.setColor(bgColor);
            g.fillRect(x, BORDER_WIDTH, part2, getHeight() - BORDER_WIDTH * 2);
        }
        g2d.setStroke(borderStroke);
        g.setColor(borderColor);
        g.drawRect(0, 0, getWidth(), getHeight());
    }
}

