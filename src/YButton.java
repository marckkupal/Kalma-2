import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
/** Fully rounded (pill) button. Used for the yellow call-to-action and the outlined secondary buttons. */
class YButton extends JButton {
    private final Color bg, hover, line;

    YButton(String text, Color bg, Color hover, Color fg, Color line) {
        super(text);
        this.bg = bg;
        this.hover = hover;
        this.line = line;
        setForeground(fg);
        setFont(Theme.BOLD.deriveFont(12f));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 22, 10, 22));
    }

    static YButton yellow(String text) {
        return new YButton(text, Brand.YELLOW, Brand.YELLOW_HOVER, Brand.INK, null);
    }

    static YButton outline(String text) {
        return new YButton(text, Color.WHITE, new Color(0xF3F4F6), Brand.INK, Brand.LINE);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        int arc = getHeight();
        g.setColor(getModel().isPressed() ? hover.darker() : getModel().isRollover() ? hover : bg);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        if (line != null) {
            g.setColor(line);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
