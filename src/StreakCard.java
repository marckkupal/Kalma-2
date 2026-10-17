import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
/**
 * "Wellness Streaks" card for the student's Home (narrow) and Summary (wide) pages: a supportive message, the last
 * 7 days, progress figures that only go up, and milestones. Formal look: no emoji, thin dividers, yellow accents.
 * All numbers and wording come from WellnessStreak.
 */
class StreakCard extends RoundPanel {
    private static final Font BODY = Theme.BASE.deriveFont(12.5f);
    private static final Font SMALL = Theme.BASE.deriveFont(11.5f);

    StreakCard(List<CheckIn> checkIns, LocalDate today, boolean wide) {
        super(null, Color.WHITE, Theme.GRID, 20);
        WellnessStreak.Result r = WellnessStreak.of(checkIns, today);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(16, 20, 16, 20));
        setAlignmentX(0f);

        add(header(wide));
        add(Staff.gap(10));
        add(divider());
        add(Staff.gap(12));

        int textWidth = wide ? 470 : 236;
        JPanel left = vertical();
        left.add(TextWrap.block(r.message(), textWidth, Theme.BASE.deriveFont(13f), Brand.INK));
        left.add(Staff.gap(12));
        left.add(caption("LAST 7 DAYS"));
        left.add(Staff.gap(6));
        Dots dots = new Dots(r.last7(), today);
        dots.setAlignmentX(0f);
        left.add(dots);
        if (!r.progress().isEmpty()) {
            left.add(Staff.gap(10));
            left.add(TextWrap.block(r.progress(), textWidth, SMALL, Brand.MUTED));
        }

        JPanel figures = figures(r, wide);
        if (wide) {
            JPanel row = new JPanel(new BorderLayout(36, 0));
            row.setOpaque(false);
            row.setAlignmentX(0f);
            row.add(left, BorderLayout.WEST);
            row.add(figures, BorderLayout.CENTER);
            add(row);
        } else {
            add(left);
            add(Staff.gap(12));
            add(figures);
        }

        add(Staff.gap(14));
        add(divider());
        add(Staff.gap(10));
        add(caption("MILESTONES"));
        add(Staff.gap(6));
        Milestones ms = new Milestones(r.reached());
        ms.setAlignmentX(0f);
        add(ms);
        add(Staff.gap(6));
        String next = r.nextMilestone() > 0 ? "Next milestone: " + WellnessStreak.milestoneName(r.nextMilestone()) + ". " : "All milestones reached. ";
        add(TextWrap.block(next + "Milestones count total check-in days, so they are never taken away. "
                + "Days without a check-in are simply left blank.", wide ? 700 : 236, SMALL, Brand.MUTED));
    }

    private static JComponent header(boolean wide) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setAlignmentX(0f);
        JLabel t = new JLabel("Wellness Streaks");
        t.setFont(Theme.BASE.deriveFont(Font.BOLD, 16f));
        t.setForeground(Brand.INK);
        h.add(t, BorderLayout.WEST);
        if (wide) {
            JLabel s = new JLabel("Your consistency with daily check-ins");
            s.setFont(SMALL);
            s.setForeground(Brand.MUTED);
            h.add(s, BorderLayout.EAST);
        }
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return h;
    }

    /** Four progress figures. Wide: a 2 x 2 grid of big values. Narrow: label and value rows. */
    private static JPanel figures(WellnessStreak.Result r, boolean wide) {
        String[][] f = {
                {"This week", r.daysThisWeek() + " of 7 days"},
                {"This month", r.daysThisMonth() + " days"},
                {"All together", r.totalDays() + " days"},
                {"Longest run in a row", r.bestRun() >= 3 ? r.bestRun() + " days" : "Building"}};
        JPanel p = new JPanel(wide ? new GridLayout(2, 2, 16, 12) : new GridLayout(4, 1, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(0f);
        for (String[] x : f) p.add(wide ? tile(x[0], x[1]) : row(x[0], x[1]));
        if (!wide) p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4 * 26));
        return p;
    }

    private static JPanel tile(String label, String value) {
        JPanel t = vertical();
        t.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.MatteBorder(0, 3, 0, 0, Brand.YELLOW), new EmptyBorder(2, 10, 2, 0)));
        JLabel v = new JLabel(value);
        v.setFont(Theme.BASE.deriveFont(Font.BOLD, 18f));
        v.setForeground(Brand.INK);
        v.setAlignmentX(0f);
        JLabel l = new JLabel(label);
        l.setFont(SMALL);
        l.setForeground(Brand.MUTED);
        l.setAlignmentX(0f);
        t.add(v);
        t.add(l);
        return t;
    }

    private static JPanel row(String label, String value) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        r.setBorder(new javax.swing.border.MatteBorder(0, 0, 1, 0, Brand.LINE));
        JLabel l = new JLabel(label);
        l.setFont(BODY);
        l.setForeground(Brand.MUTED);
        JLabel v = new JLabel(value);
        v.setFont(BODY.deriveFont(Font.BOLD));
        v.setForeground(Brand.INK);
        r.add(l, BorderLayout.WEST);
        r.add(v, BorderLayout.EAST);
        return r;
    }

    private static JPanel vertical() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(0f);
        return p;
    }

    private static JComponent divider() {
        JPanel d = new JPanel();
        d.setBackground(Brand.LINE);
        d.setPreferredSize(new Dimension(10, 1));
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        d.setAlignmentX(0f);
        return d;
    }

    private static JLabel caption(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.BASE.deriveFont(Font.BOLD, 10.5f));
        l.setForeground(Brand.OLIVE);
        l.setAlignmentX(0f);
        return l;
    }

    /** Seven round dots, oldest to today. Filled with a check = checked in; plain outline = no check-in that day. */
    private static class Dots extends JComponent {
        private final boolean[] on;
        private final LocalDate today;
        Dots(boolean[] on, LocalDate today) {
            this.on = on;
            this.today = today;
            setPreferredSize(new Dimension(236, 48));
            setMaximumSize(new Dimension(236, 48));
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(3, 3);
            int d = 24, gap = (236 - 6 - 7 * d) / 6;
            for (int i = 0; i < 7; i++) {
                int x = i * (d + gap);
                if (on[i]) {
                    g.setColor(Brand.YELLOW);
                    g.fillOval(x, 0, d, d);
                    g.setColor(Brand.INK);
                    g.setStroke(new BasicStroke(2f));
                    g.drawLine(x + 7, 12, x + 11, 16);
                    g.drawLine(x + 11, 16, x + 17, 8);
                } else {
                    g.setColor(Brand.LINE);
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawOval(x, 0, d, d);
                }
                if (i == 6) { g.setColor(Brand.YELLOW_HOVER); g.setStroke(new BasicStroke(1.5f)); g.drawOval(x - 3, -3, d + 6, d + 6); }
                g.setFont(Theme.BASE.deriveFont(10f));
                g.setColor(Brand.MUTED);
                String s = today.minusDays(6 - i).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                g.drawString(s, x + (d - g.getFontMetrics().stringWidth(s)) / 2, 40);
            }
            g.dispose();
        }
    }

    /** One chip per milestone: yellow when reached, outline when still ahead. */
    private static class Milestones extends JComponent {
        private final int reached;
        Milestones(int reached) {
            this.reached = reached;
            setPreferredSize(new Dimension(236, 28));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
            int x = 0;
            for (int i = 0; i < WellnessStreak.MILESTONES.length; i++) {
                String s = String.valueOf(WellnessStreak.MILESTONES[i]);
                int w = g.getFontMetrics().stringWidth(s) + 18;
                boolean done = i < reached;
                g.setColor(done ? Brand.YELLOW : Color.WHITE);
                g.fillRoundRect(x, 2, w, 22, 22, 22);
                g.setColor(done ? Brand.YELLOW_HOVER : Brand.LINE);
                g.drawRoundRect(x, 2, w, 22, 22, 22);
                g.setColor(done ? Brand.INK : Brand.MUTED);
                g.drawString(s, x + 9, 17);
                x += w + 6;
            }
            g.dispose();
        }
    }
}
