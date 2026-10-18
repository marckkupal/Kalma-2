import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Line chart of a student's assessment scores over time: overall plus the three domains, on a 0 to 100 scale.
 * Faint lines mark where "moderate" and "higher" concern begin, so the chart reads the same way as the result labels.
 */
class ScoreHistoryChart extends JPanel {
    static final Color C_OVERALL = Brand.INK;
    static final Color C_ACADEMIC = Theme.ORANGE;
    static final Color C_SOCIAL = Theme.GREEN;
    static final Color C_PERSONAL = new Color(0x6B7FD7);
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private List<AssessmentResult> data = List.of();

    ScoreHistoryChart() {
        setOpaque(false);
        setPreferredSize(new Dimension(600, 260));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        getAccessibleContext().setAccessibleName("Assessment scores over time");
    }

    /** Results oldest first. */
    void setData(List<AssessmentResult> chronological) {
        data = List.copyOf(chronological);
        repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        int w = getWidth(), h = getHeight();
        int left = 38, right = 16, top = 34, bottom = 28;
        int pw = w - left - right, ph = h - top - bottom;
        if (pw <= 0 || ph <= 0) { g.dispose(); return; }

        // legend
        String[] names = {"Overall", "Academic", "Social", "Personal"};
        Color[] cols = {C_OVERALL, C_ACADEMIC, C_SOCIAL, C_PERSONAL};
        g.setFont(Theme.BASE.deriveFont(11f));
        int lx = left;
        for (int i = 0; i < names.length; i++) {
            g.setColor(cols[i]);
            g.fillOval(lx, 10, 9, 9);
            g.setColor(Brand.MUTED);
            g.drawString(names[i], lx + 14, 19);
            lx += 14 + g.getFontMetrics().stringWidth(names[i]) + 18;
        }

        // grid and band lines
        g.setFont(Theme.BASE.deriveFont(10f));
        for (int v = 0; v <= 100; v += 25) {
            int y = top + ph - ph * v / 100;
            g.setColor(Theme.GRID);
            g.drawLine(left, y, left + pw, y);
            g.setColor(Brand.MUTED);
            g.drawString("" + v, left - 28, y + 4);
        }
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
        int[] cuts = {AssessmentService.moderateCut(), AssessmentService.higherCut()};
        String[] cutNames = {"Moderate", "Higher"};
        for (int i = 0; i < 2; i++) {
            int y = top + ph - ph * cuts[i] / 100;
            g.setColor(new Color(0xC9CDD4));
            g.drawLine(left, y, left + pw, y);
            g.setColor(Brand.MUTED);
            g.drawString(cutNames[i], left + pw - g.getFontMetrics().stringWidth(cutNames[i]) - 2, y - 3);
        }
        g.setStroke(new BasicStroke(1f));

        int n = data.size();
        if (n == 0) { g.dispose(); return; }
        int[] xs = new int[n];
        for (int i = 0; i < n; i++) xs[i] = n == 1 ? left + pw / 2 : left + pw * i / (n - 1);

        // x labels (thinned so they never overlap)
        g.setColor(Brand.MUTED);
        int lastEnd = -1;
        for (int i = 0; i < n; i++) {
            String s = data.get(i).date().format(LABEL);
            int sw = g.getFontMetrics().stringWidth(s);
            int x0 = Math.max(0, Math.min(w - sw, xs[i] - sw / 2));
            if (x0 > lastEnd + 6) { g.drawString(s, x0, h - 8); lastEnd = x0 + sw; }
        }

        // series: domains first, overall on top
        String[] keys = {"Academic", "Social", "Personal", "Overall"};
        Color[] sc = {C_ACADEMIC, C_SOCIAL, C_PERSONAL, C_OVERALL};
        for (int s = 0; s < keys.length; s++) {
            boolean overall = s == 3;
            g.setColor(sc[s]);
            g.setStroke(new BasicStroke(overall ? 3f : 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Float path = new Path2D.Float();
            for (int i = 0; i < n; i++) {
                float y = top + ph - ph * data.get(i).score(keys[s]) / 100f;
                if (i == 0) path.moveTo(xs[i], y); else path.lineTo(xs[i], y);
            }
            if (n > 1) g.draw(path);
            for (int i = 0; i < n; i++) {
                int y = Math.round(top + ph - ph * data.get(i).score(keys[s]) / 100f);
                int r = overall ? 5 : 4;
                g.fillOval(xs[i] - r, y - r, r * 2, r * 2);
            }
        }
        g.dispose();
    }
}
