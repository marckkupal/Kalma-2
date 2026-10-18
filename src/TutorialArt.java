import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
/** Yellow tutorial art: pale diagonal stripe, black circle with a line icon, black corner triangle. */
class TutorialArt extends JComponent {
    private String icon = "smiley";

    void setIcon(String icon) { this.icon = icon; repaint(); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        int w = getWidth(), h = getHeight();
        g.setColor(Brand.YELLOW);
        g.fillRect(0, 0, w, h);

        Graphics2D s = (Graphics2D) g.create();
        s.clipRect(0, 0, w, h);
        Brand.band(s, w, h, h + 8, h + 34, Brand.YELLOW_STRIPE);
        s.dispose();

        Path2D tri = new Path2D.Double();
        tri.moveTo(w, h);
        tri.lineTo(w, h - 76);
        tri.lineTo(w - 76, h);
        tri.closePath();
        g.setColor(Brand.BLACK);
        g.fill(tri);

        int cx = w / 2, cy = h / 2;
        g.fill(new Ellipse2D.Double(cx - 46, cy - 46, 92, 92));
        g.setColor(Brand.YELLOW);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (icon) {
            case "bulb" -> bulb(g, cx, cy);
            case "people" -> people(g, cx, cy);
            case "chart" -> chart(g, cx, cy);
            case "bell" -> bell(g, cx, cy);
            case "calendar" -> calendar(g, cx, cy);
            case "book" -> book(g, cx, cy);
            default -> smiley(g, cx, cy);
        }
        g.dispose();
    }

    private void smiley(Graphics2D g, int cx, int cy) {
        g.draw(new Ellipse2D.Double(cx - 20, cy - 20, 40, 40));
        g.fill(new Ellipse2D.Double(cx - 8, cy - 7, 2.5, 2.5));
        g.fill(new Ellipse2D.Double(cx + 6, cy - 7, 2.5, 2.5));
        g.draw(new java.awt.geom.Arc2D.Double(cx - 10, cy - 6, 20, 16, 205, 130, java.awt.geom.Arc2D.OPEN));
    }

    private void bulb(Graphics2D g, int cx, int cy) {
        g.draw(new Ellipse2D.Double(cx - 13, cy - 24, 26, 26));
        g.drawLine(cx - 7, cy + 1, cx - 7, cy + 8);
        g.drawLine(cx + 7, cy + 1, cx + 7, cy + 8);
        g.drawLine(cx - 7, cy + 8, cx + 7, cy + 8);
        g.drawLine(cx - 6, cy + 14, cx + 6, cy + 14);
        g.drawLine(cx - 3, cy + 20, cx + 3, cy + 20);
    }

    private void people(Graphics2D g, int cx, int cy) {
        g.draw(new Ellipse2D.Double(cx - 22, cy - 20, 16, 16));
        g.draw(new java.awt.geom.Arc2D.Double(cx - 28, cy - 2, 28, 26, 0, 180, java.awt.geom.Arc2D.OPEN));
        g.draw(new java.awt.geom.Arc2D.Double(cx + 2, cy - 22, 14, 16, 270, 180, java.awt.geom.Arc2D.OPEN));
        g.draw(new java.awt.geom.Arc2D.Double(cx + 4, cy - 2, 24, 26, 0, 180, java.awt.geom.Arc2D.OPEN));
    }

    private void chart(Graphics2D g, int cx, int cy) {
        g.drawLine(cx - 18, cy - 20, cx - 18, cy + 18);      // y axis
        g.drawLine(cx - 18, cy + 18, cx + 20, cy + 18);      // x axis
        g.drawLine(cx - 8, cy + 12, cx - 8, cy + 2);
        g.drawLine(cx + 2, cy + 12, cx + 2, cy - 10);
        g.drawLine(cx + 12, cy + 12, cx + 12, cy - 2);
    }

    private void bell(Graphics2D g, int cx, int cy) {
        g.draw(new java.awt.geom.Arc2D.Double(cx - 12, cy - 20, 24, 24, 0, 180, java.awt.geom.Arc2D.OPEN));
        g.drawLine(cx - 12, cy - 8, cx - 14, cy + 8);
        g.drawLine(cx + 12, cy - 8, cx + 14, cy + 8);
        g.drawLine(cx - 17, cy + 8, cx + 17, cy + 8);
        g.draw(new java.awt.geom.Arc2D.Double(cx - 4, cy + 4, 8, 10, 180, 180, java.awt.geom.Arc2D.OPEN));
    }

    private void calendar(Graphics2D g, int cx, int cy) {
        g.draw(new java.awt.geom.RoundRectangle2D.Double(cx - 18, cy - 16, 36, 34, 6, 6));
        g.drawLine(cx - 18, cy - 6, cx + 18, cy - 6);
        g.drawLine(cx - 8, cy - 21, cx - 8, cy - 12);
        g.drawLine(cx + 8, cy - 21, cx + 8, cy - 12);
        g.drawLine(cx - 7, cy + 6, cx - 2, cy + 11);         // check mark
        g.drawLine(cx - 2, cy + 11, cx + 8, cy + 1);
    }

    private void book(Graphics2D g, int cx, int cy) {
        g.draw(new java.awt.geom.RoundRectangle2D.Double(cx - 22, cy - 17, 44, 34, 3, 3));
        g.drawLine(cx, cy - 17, cx, cy + 17);
    }
}
