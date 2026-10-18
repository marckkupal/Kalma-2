import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
/** KALMA logo: sun-and-leaves badge with wordmark and optional tagline. */
class Logo extends JComponent {
    private static final String TAG = "A Peer-Support Wellness Companion";
    private final int icon;
    private final boolean tag;
    private final Font wordFont, tagFont;

    Logo(int icon, float word, boolean tag) {
        this.icon = icon;
        this.tag = tag;
        this.wordFont = new Font("SansSerif", Font.BOLD, Math.round(word));
        this.tagFont = Theme.BASE.deriveFont(Font.PLAIN, word * 0.34f);
    }

    @Override public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(wordFont), tm = getFontMetrics(tagFont);
        int textW = Math.max(fm.stringWidth("KALMA"), tag ? tm.stringWidth(TAG) : 0);
        int textH = fm.getHeight() + (tag ? tm.getHeight() : 0);
        return new Dimension(icon + 10 + textW, Math.max(icon, textH));
    }
    @Override public Dimension getMaximumSize() { return getPreferredSize(); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int h = getHeight();
        int iy = (h - icon) / 2;
        g.setColor(new Color(0xF3F4F6));
        g.fillRoundRect(0, iy, icon, icon, icon / 4, icon / 4);
        java.awt.geom.AffineTransform old = g.getTransform();
        g.translate(0, iy);
        g.scale(icon, icon);
        g.setColor(new Color(0xF2B63D));
        g.fill(new Ellipse2D.Double(0.36, 0.14, 0.24, 0.24));
        Path2D left = new Path2D.Double();
        left.moveTo(0.50, 0.86);
        left.quadTo(0.12, 0.88, 0.20, 0.46);
        left.quadTo(0.52, 0.52, 0.50, 0.86);
        g.setColor(new Color(0x6785AD));
        g.fill(left);
        Path2D right = new Path2D.Double();
        right.moveTo(0.52, 0.86);
        right.quadTo(0.88, 0.90, 0.82, 0.50);
        right.quadTo(0.50, 0.52, 0.52, 0.86);
        g.setColor(new Color(0xD5D8DD));
        g.fill(right);
        g.setTransform(old);

        FontMetrics fm = g.getFontMetrics(wordFont), tm = g.getFontMetrics(tagFont);
        int block = fm.getAscent() + fm.getDescent() + (tag ? tm.getAscent() + tm.getDescent() : 0);
        int base = (h - block) / 2 + fm.getAscent();
        int x = icon + 10;
        g.setFont(wordFont);
        g.setColor(new Color(0x1F2937));
        g.drawString("KALMA", x, base);
        if (tag) {
            g.setFont(tagFont);
            g.setColor(new Color(0x4B5563));
            g.drawString(TAG, x, base + fm.getDescent() + tm.getAscent());
        }
        g.dispose();
    }
}
