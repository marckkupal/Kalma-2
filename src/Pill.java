import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
/** Small rounded label such as "Confirmed", "Pending" or "Live". */
class Pill extends JLabel {
    private final Color bg;

    Pill(String text, Color bg, Color fg) {
        super(text);
        this.bg = bg;
        setForeground(fg);
        setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        setBorder(new EmptyBorder(4, 12, 4, 12));
        setOpaque(false);
    }

    @Override public Dimension getMaximumSize() { return getPreferredSize(); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        g.setColor(bg);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g.dispose();
        super.paintComponent(g0);
    }
}
