import javax.swing.*;
import java.awt.*;
/** Solid circle used as the logo mark. */
class Dot extends JComponent {
    private final Color color;
    Dot(Color color, int size) {
        this.color = color;
        setPreferredSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
    }
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillOval(0, 0, getWidth(), getHeight());
        g.dispose();
    }
}
