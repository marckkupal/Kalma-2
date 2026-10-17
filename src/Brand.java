import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
/** Brand colors, logo mark and diagonal stripes used by the first pages. */
final class Brand {
    static final Color BLACK = new Color(0x0F0F11);
    static final Color YELLOW = new Color(0xF7D55B);
    static final Color YELLOW_HOVER = new Color(0xF0C93D);
    static final Color YELLOW_SOFT = new Color(0xFCEFB4);
    static final Color YELLOW_STRIPE = new Color(0xFAE48C);
    static final Color OLIVE = new Color(0x7A7557);
    static final Color CREAM = new Color(0xFAFAF5);
    static final Color TAB_BG = new Color(0xEFECE6);
    static final Color LINE = new Color(0xE5E7EB);
    static final Color MUTED = new Color(0x6B7280);
    static final Color INK = new Color(0x111111);

    private Brand() {}

    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** Sun and two leaves, drawn in the brand yellow inside a size x size box at (x, y). */
    static void mark(Graphics2D g0, int x, int y, int size) {
        Graphics2D g = (Graphics2D) g0.create();
        aa(g);
        g.translate(x, y);
        g.scale(size, size);
        g.setColor(YELLOW);
        g.fill(new Ellipse2D.Double(0.36, 0.08, 0.27, 0.27));
        Path2D left = new Path2D.Double();
        left.moveTo(0.48, 0.92);
        left.quadTo(0.06, 0.94, 0.14, 0.46);
        left.quadTo(0.50, 0.52, 0.48, 0.92);
        g.fill(left);
        Path2D right = new Path2D.Double();
        right.moveTo(0.52, 0.92);
        right.quadTo(0.94, 0.94, 0.86, 0.46);
        right.quadTo(0.50, 0.52, 0.52, 0.92);
        g.fill(right);
        g.dispose();
    }

    /** Diagonal band (45 degrees, rising to the right) between x+y = s1 and x+y = s2, clipped to the panel. */
    static void band(Graphics2D g, int w, int h, int s1, int s2, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(s1 - h, h);
        p.lineTo(w, s1 - w);
        p.lineTo(w, s2 - w);
        p.lineTo(s2 - h, h);
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    /** Olive stripe plus a yellow corner wedge, anchored to the bottom-right of a dark panel. */
    static void stripes(Graphics2D g0, int w, int h) {
        Graphics2D g = (Graphics2D) g0.create();
        aa(g);
        g.clipRect(0, 0, w, h);
        int L = Math.round(Math.min(w, h) * 0.42f);
        int corner = w + h;
        band(g, w, h, corner - L - 98, corner - L - 72, OLIVE);
        band(g, w, h, corner - L, corner, YELLOW);
        band(g, w, h, corner - Math.round(L * 0.45f) - 10, corner - Math.round(L * 0.45f), BLACK);
        g.dispose();
    }
}
