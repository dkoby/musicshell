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

/*
 *
 */
public class VSpectrumView extends JComponent {
    private Color baseColor;
    private Color backingColor;

    public VSpectrumView() {
//        setSize(new Dimension(width, height));
//        setPreferredSize(new Dimension(width, height));
//        setMaximumSize(new Dimension(width, height));

        setColor(Config.baseColor);
    }
    public void setColor(Color color) {
        baseColor = color;
        backingColor = baseColor.brighter();
    }

    @Override
    public void paintComponent(Graphics g) {
//        System.out.println("repaint spectrum");
        g.setColor(backingColor);
//        g.fillRect(0, 0, width - 1, height - 1);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
