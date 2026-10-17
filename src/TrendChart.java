import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/** Line chart of mood and stress. Student view: green/orange check-ins. Admin view: black/yellow campus averages. */
class TrendChart extends JPanel {
    private String[] labels = new String[0];
    private double[] mood = new double[0];
    private double[] stress = new double[0];
    /** false = no check-in that day. Shown as a gap, never as a mood or stress value. */
    private boolean[] has = new boolean[0];
    private Color moodColor = Theme.GREEN;
    private Color stressColor = Theme.ORANGE;
    private boolean framed = true;
    private boolean legend = true;
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    TrendChart() {
        setOpaque(false);
        setPreferredSize(new Dimension(600, 280));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
    }

    void setFramed(boolean f) { framed = f; repaint(); }

    void setLegend(boolean on) { legend = on; repaint(); }

    void setColors(Color moodColor, Color stressColor) {
        this.moodColor = moodColor;
        this.stressColor = stressColor;
        repaint();
    }

    /** One point per check-in (already sorted by date). */
    void setData(List<CheckIn> data) {
        int n = data.size();
        labels = new String[n];
        mood = new double[n];
        stress = new double[n];
        has = new boolean[n];
        java.util.Arrays.fill(has, true);
        for (int i = 0; i < n; i++) {
            labels[i] = data.get(i).date().format(LABEL);
            mood[i] = data.get(i).mood();
            stress[i] = data.get(i).stress();
        }
        repaint();
    }

    /** One slot for each of the last {@code days} days. Days without a check-in stay empty and are marked "No check-in". */
    void setDaily(List<CheckIn> all, int days) {
        labels = new String[days];
        mood = new double[days];
        stress = new double[days];
        has = new boolean[days];
        for (int i = 0; i < days; i++) {
            LocalDate d = LocalDate.now().minusDays(days - 1 - i);
            labels[i] = d.format(LABEL);
            for (CheckIn c : all) {
                if (c.date().equals(d)) {
                    mood[i] = c.mood();
                    stress[i] = c.stress();
                    has[i] = true;
                }
            }
        }
        repaint();
    }

    /** One point per day: the average of every check-in on that day. days == 0 means all days. */
    void setCampus(List<CheckIn> all, int days) { setCampus(all, days, 1); }

    /** Number of days currently drawn (used by tests). */
    int pointCount() { return labels == null ? 0 : labels.length; }

    /** Campus averages by day. A day with fewer than minGroup different students is left out so no one stands out. */
    void setCampus(List<CheckIn> all, int days, int minGroup) {
        TreeMap<LocalDate, List<CheckIn>> byDay = all.stream()
                .filter(c -> days == 0 || !c.date().isBefore(LocalDate.now().minusDays(days - 1)))
                .collect(Collectors.groupingBy(CheckIn::date, TreeMap::new, Collectors.toList()));
        byDay.values().removeIf(l -> l.stream().map(CheckIn::userId).distinct().count() < minGroup);
        int n = byDay.size();
        labels = new String[n];
        mood = new double[n];
        stress = new double[n];
        has = new boolean[n];
        java.util.Arrays.fill(has, true);
        int i = 0;
        for (Map.Entry<LocalDate, List<CheckIn>> e : byDay.entrySet()) {
            labels[i] = e.getKey().format(LABEL);
            mood[i] = e.getValue().stream().mapToInt(CheckIn::mood).average().orElse(0);
            stress[i] = e.getValue().stream().mapToInt(CheckIn::stress).average().orElse(0);
            i++;
        }
        repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        if (framed) {
            g.setColor(Theme.WHITE);
            g.fillRoundRect(0, 0, w, h, 20, 20);
            g.setColor(Theme.GRID);
            g.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);
        }

        g.setFont(Theme.BASE.deriveFont(11f));
        if (legend) {
            g.setColor(moodColor);
            g.fillOval(18, 14, 8, 8);
            g.setColor(Theme.MUTED);
            g.drawString("Mood", 30, 22);
            g.setColor(stressColor);
            g.fillOval(76, 14, 8, 8);
            g.setColor(Theme.MUTED);
            g.drawString("Stress", 88, 22);
        }

        int left = 40, right = 20, top = legend ? 40 : 16, bottom = 30;
        int pw = w - left - right, ph = h - top - bottom;
        for (int v = 1; v <= 5; v++) {
            int y = top + ph - (v - 1) * ph / 4;
            g.setColor(Theme.GRID);
            g.drawLine(left, y, w - right, y);
            g.setColor(Theme.MUTED);
            g.drawString(String.valueOf(v), 22, y + 4);
        }
        int n = mood.length;
        boolean any = false;
        for (boolean b : has) any |= b;
        if (n == 0 || !any) {
            g.setColor(Theme.INK);
            g.drawString("No check-ins in this period.", left + 10, top + 24);
            g.dispose();
            return;
        }
        int[] xs = new int[n], ms = new int[n], ss = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = n == 1 ? left + pw / 2 : left + 16 + i * (pw - 32) / (n - 1);
            ms[i] = top + ph - (int) Math.round((mood[i] - 1) * ph / 4.0);
            ss[i] = top + ph - (int) Math.round((stress[i] - 1) * ph / 4.0);
        }
        // Days with no check-in get a light column, so a gap is never mistaken for a low or high score.
        int slot = n < 2 ? pw : (pw - 32) / (n - 1);
        g.setFont(Theme.BASE.deriveFont(9f));
        for (int i = 0; i < n; i++) {
            if (has[i]) continue;
            g.setColor(new Color(0xF3F4F6));
            g.fillRoundRect(xs[i] - Math.max(3, slot / 2 - 2), top, Math.max(6, slot - 4), ph, 8, 8);
            String msg = "No check-in";
            if (slot >= 58) {
                g.setColor(new Color(0x9CA3AF));
                g.drawString(msg, xs[i] - g.getFontMetrics().stringWidth(msg) / 2, top + ph / 2);
            }
        }
        g.setFont(Theme.BASE.deriveFont(11f));
        drawSeries(g, xs, ms, has, moodColor);
        drawSeries(g, xs, ss, has, stressColor);
        g.setColor(Theme.MUTED);
        int labelW = g.getFontMetrics().stringWidth("Oct 30") + 8;
        int spacing = n < 2 ? labelW : (pw - 32) / (n - 1);
        int step = Math.max(1, (labelW + spacing - 1) / Math.max(1, spacing));
        for (int i = 0; i < n; i += step) {
            g.drawString(labels[i], xs[i] - g.getFontMetrics().stringWidth(labels[i]) / 2, h - 10);
        }
        g.dispose();
    }

    /** Lines only join two days that both have a check-in. */
    private void drawSeries(Graphics2D g, int[] xs, int[] ys, boolean[] ok, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 1; i < xs.length; i++) if (ok[i - 1] && ok[i]) g.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
        for (int i = 0; i < xs.length; i++) if (ok[i]) g.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
        g.setStroke(new BasicStroke(1f));
    }
}
