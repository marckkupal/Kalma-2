import javax.swing.*;
import java.awt.*;
/** KALMA wordmark with the leaf mark and an optional tagline. */
class BrandLogo extends JComponent {
    private final int icon;
    private final String tag;
    private final boolean dark;
    private final Font wordFont, tagFont;

    BrandLogo(int icon, float word, String tag, boolean dark) {
        this.icon = icon;
        this.tag = tag;
        this.dark = dark;
        this.wordFont = new Font("SansSerif", Font.BOLD, Math.round(word));
        this.tagFont = Theme.BASE.deriveFont(Font.PLAIN, Math.max(9f, word * 0.36f));
    }

    @Override public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(wordFont), tm = getFontMetrics(tagFont);
        int textW = Math.max(fm.stringWidth("KALMA"), tag == null ? 0 : tm.stringWidth(tag));
        int textH = fm.getHeight() + (tag == null ? 0 : tm.getHeight());
        return new Dimension(icon + 12 + textW, Math.max(icon, textH));
    }
    @Override public Dimension getMaximumSize() { return getPreferredSize(); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        int h = getHeight();
        Brand.mark(g, 0, (h - icon) / 2, icon);
        FontMetrics fm = g.getFontMetrics(wordFont), tm = g.getFontMetrics(tagFont);
        int block = fm.getAscent() + fm.getDescent() + (tag == null ? 0 : tm.getAscent() + tm.getDescent());
        int base = (h - block) / 2 + fm.getAscent();
        int x = icon + 12;
        g.setFont(wordFont);
        g.setColor(dark ? Color.WHITE : Brand.INK);
        g.drawString("KALMA", x, base);
        if (tag != null) {
            g.setFont(tagFont);
            g.setColor(dark ? new Color(0xE5E5E5) : Brand.MUTED);
            g.drawString(tag, x, base + fm.getDescent() + tm.getAscent());
        }
        g.dispose();
    }
}
