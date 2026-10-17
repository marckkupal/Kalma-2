import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
/** Pill-shaped button with hover state. Colors can be changed later (used by the sidebar). */
class RoundButton extends JButton {
    private Color bg, hover;
    private final Color border;

    RoundButton(String text, Color bg, Color hover, Color fg, Color border) {
        super(text);
        this.bg = bg;
        this.hover = hover;
        this.border = border;
        setForeground(fg);
        setFont(Theme.BOLD);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(9, 20, 9, 20));
    }

    void setColors(Color bg, Color hover, Color fg) {
        this.bg = bg;
        this.hover = hover;
        setForeground(fg);
        repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getModel().isPressed() ? hover.darker() : getModel().isRollover() ? hover : bg);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        if (border != null) {
            g.setColor(border);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
