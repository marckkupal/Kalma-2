import javax.swing.*;
import java.awt.*;
/** Circle with the user's initials. Large avatars (64 px and up) get a larger font, as on the Profile page. */
class Avatar extends JComponent {
    private final String initials;
    private final Color bg, fg;
    private final int size;

    Avatar(String initials) { this(initials, Theme.GREEN_DARK, Theme.WHITE); }

    Avatar(String initials, Color bg, Color fg) { this(initials, bg, fg, 38); }

    Avatar(String initials, Color bg, Color fg, int size) {
        this.initials = initials;
        this.bg = bg;
        this.fg = fg;
        this.size = size;
        Dimension d = new Dimension(size, size);
        setPreferredSize(d);
        setMaximumSize(d);
        setMinimumSize(d);
    }
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(bg);
        g.fillOval(0, 0, getWidth(), getHeight());
        g.setColor(fg);
        g.setFont(size < 32 ? Theme.BASE.deriveFont(Font.BOLD, 10f)
                : size >= 64 ? Theme.BOLD.deriveFont(size * 0.36f) : Theme.BOLD);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(initials, (getWidth() - fm.stringWidth(initials)) / 2,
                (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }
}
