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
public class VVolumeView extends JComponent {
    private Color borderColor;
    private Color fgColor;
    private Color bgColor;
    private Color bgBaseColor;
    int percents;
    /* */
    private final int BORDER_WIDTH = 2;
    private final int BAR_PAD   = 4;
    private final int BAR_WIDTH = 10;
    private BasicStroke borderStroke;
    /**
     *
     */
    public VVolumeView() {
        borderColor = Color.WHITE;
        fgColor     = Color.BLACK;
        bgColor     = Color.YELLOW;
        borderStroke = new BasicStroke((float)BORDER_WIDTH);
        percents = 0;
    }
    /**
     *
     */
    public void setColors(Color fgColor, Color bgColor) {
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
        int width = getWidth();
        int height = getHeight();

        final int MIN_HEIGHT = height / 4;
        final int NBARS      = width / (BAR_WIDTH + BAR_PAD);
        final int HINCR      = (height - MIN_HEIGHT) / NBARS;

        int nbars      = NBARS;
        int x          = (width - NBARS * (BAR_WIDTH + BAR_PAD)) / 2;
        int bheight    = MIN_HEIGHT;
        Color barColor = fgColor;

        while (nbars-- > 0) {
            /* draw bar outline */
//            g2d.setStroke(borderStroke);
            g.setColor(borderColor);
            g.fillRect(x, height - bheight,
                    BAR_WIDTH, bheight);


            if ((100 * nbars / NBARS) >= (100 - percents))
                barColor = fgColor;
            else
                barColor = bgColor;
            g.setColor(barColor);
            g.fillRect(x + BORDER_WIDTH, height - bheight + BORDER_WIDTH,
                    BAR_WIDTH - BORDER_WIDTH * 2, bheight - BORDER_WIDTH * 2);

            x += BAR_WIDTH + BAR_PAD;
            bheight += HINCR;
        }
    }
}

