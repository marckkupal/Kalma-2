import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
/** Selectable rounded "pill" (mood, stress, period). With an icon, the emoji sits above the label. */
class PillToggle extends JToggleButton {
    private final String icon;

    PillToggle(String text) { this(text, null); }

    PillToggle(String text, String icon) {
        super(text);
        this.icon = icon;
        setFont(Theme.BASE.deriveFont(11f));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(icon == null ? 7 : 6, 6, icon == null ? 7 : 6, 6));
    }

    @Override public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        Insets in = getInsets();
        int w = Math.max(fm.stringWidth(getText()), icon == null ? 0 : 22) + in.left + in.right;
        int h = fm.getHeight() + in.top + in.bottom + (icon == null ? 0 : 22);
        return new Dimension(w, h);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean on = isSelected();
        g.setColor(on ? Theme.INK : getModel().isRollover() ? Theme.MINT : Theme.WHITE);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        if (!on) {
            g.setColor(Theme.GRID);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        }
        Insets in = getInsets();
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        if (icon != null) {
            Font emoji = getFont().deriveFont(16f);
            g.setFont(emoji);
            g.setColor(Theme.INK);
            g.drawString(icon, (getWidth() - g.getFontMetrics().stringWidth(icon)) / 2, in.top + 17);
            g.setFont(getFont());
        }
        g.setColor(on ? Theme.WHITE : Theme.INK);
        int y = icon == null ? (getHeight() + fm.getAscent() - fm.getDescent()) / 2
                : getHeight() - in.bottom - fm.getDescent();
        g.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, y);
        g.dispose();
    }
}
