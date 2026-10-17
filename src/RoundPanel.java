import javax.swing.*;
import java.awt.*;
/** Rounded panel with optional border and optional gradient. Hugs its preferred height. */
class RoundPanel extends JPanel {
    private final Color fill, fill2, line;
    private final int arc;

    RoundPanel(LayoutManager layout, Color fill, Color line, int arc) { this(layout, fill, null, line, arc); }

    RoundPanel(LayoutManager layout, Color fill, Color fill2, Color line, int arc) {
        super(layout);
        this.fill = fill;
        this.fill2 = fill2;
        this.line = line;
        this.arc = arc;
        setOpaque(false);
    }

    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(fill2 == null ? fill : new GradientPaint(0, 0, fill, getWidth(), getHeight(), fill2));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        if (line != null) {
            g.setColor(line);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
