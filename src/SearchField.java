import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
/** Rounded search box with a magnifier and placeholder text. */
class SearchField extends JTextField {
    private final String hint;
    private final Color fill;

    SearchField(String hint) { this(hint, Theme.BG); }

    SearchField(String hint, Color fill) {
        this.hint = hint;
        this.fill = fill;
        setOpaque(false);
        setBorder(new EmptyBorder(9, 38, 9, 16));
        setForeground(Theme.INK);
        setCaretColor(Theme.INK);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(fill);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g.setColor(Theme.GRID);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
        g.dispose();
        super.paintComponent(g0);
        Graphics2D t = (Graphics2D) g0.create();
        t.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        t.setFont(getFont());
        FontMetrics fm = t.getFontMetrics();
        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        t.setColor(Theme.MUTED);
        t.drawString("\uD83D\uDD0D", 14, y);
        if (getText().isEmpty()) t.drawString(hint, 38, y);
        t.dispose();
    }
}
