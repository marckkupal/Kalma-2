import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
/** Line faces used by the mood tiles. mood runs from 1 (very bad) to 5 (very good). */
final class MoodFace {
    private MoodFace() {}

    static void paint(Graphics2D g0, double x, double y, double d, int mood, boolean filled) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        Ellipse2D head = new Ellipse2D.Double(x, y, d, d);
        if (filled) {
            g.setColor(Brand.YELLOW);
            g.fill(head);
        }
        g.setColor(Brand.INK);
        g.setStroke(new BasicStroke((float) Math.max(1.4, d / 20), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (!filled) g.draw(head);
        double eye = d * 0.09;
        g.fill(new Ellipse2D.Double(x + d * 0.34 - eye / 2, y + d * 0.40 - eye / 2, eye, eye));
        g.fill(new Ellipse2D.Double(x + d * 0.66 - eye / 2, y + d * 0.40 - eye / 2, eye, eye));
        switch (mood) {
            case 1 -> g.draw(new Arc2D.Double(x + d * 0.28, y + d * 0.58, d * 0.44, d * 0.30, 20, 140, Arc2D.OPEN));
            case 2 -> g.draw(new Arc2D.Double(x + d * 0.30, y + d * 0.64, d * 0.40, d * 0.20, 25, 130, Arc2D.OPEN));
            case 3 -> g.draw(new Line2D.Double(x + d * 0.32, y + d * 0.70, x + d * 0.68, y + d * 0.70));
            case 4 -> g.draw(new Arc2D.Double(x + d * 0.28, y + d * 0.40, d * 0.44, d * 0.34, 200, 140, Arc2D.OPEN));
            default -> g.draw(new Arc2D.Double(x + d * 0.26, y + d * 0.36, d * 0.48, d * 0.40, 190, 160, Arc2D.CHORD));
        }
        g.dispose();
    }
}
