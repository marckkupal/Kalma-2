import javax.swing.*;
import java.awt.*;
/** Small yellow circle holding an emoji icon. */
class IconBadge extends JComponent {
    private final String icon;
    private final int size;

    IconBadge(String icon) { this(icon, 34); }

    IconBadge(String icon, int size) {
        this.icon = icon;
        this.size = size;
        Dimension d = new Dimension(size, size);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        g.setColor(Brand.YELLOW_SOFT);
        g.fillOval(0, 0, getWidth(), getHeight());
        g.setColor(Brand.INK);
        g.setFont(Theme.BASE.deriveFont(size * 0.44f));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(icon, (getWidth() - fm.stringWidth(icon)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }
}
