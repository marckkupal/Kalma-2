import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;
/**
 * One day in the calendar. Three clearly different looks:
 *  - completed check-in: green tint, check mark, mood and stress written out
 *  - missing check-in (a past day): grey with a dashed border and the words "No check-in"
 *  - upcoming day: plain white
 * Today has a yellow number badge. A notebook emoji means a journal entry exists. The mood is shown as an emoji.
 * School dates show as tags with an emoji for exams, holidays or other dates.
 */
final class DayCell extends JComponent {
    static final Color[] MOOD = {new Color(0xE5736B), new Color(0xF0A36B), new Color(0xF2D96B), new Color(0xA7D17B), new Color(0x5FB57A)};
    private static final Color GREEN = new Color(0x2F7D3E);

    private final LocalDate date;
    private final CheckIn ci;
    private final boolean journal, expanded, inPeriod, selected;
    private final List<AcademicEvent> events;

    DayCell(LocalDate date, CheckIn ci, boolean journal, List<AcademicEvent> events,
            boolean expanded, boolean inPeriod, boolean selected, Runnable onPick) {
        this.date = date;
        this.ci = ci;
        this.journal = journal;
        this.events = events;
        this.expanded = expanded;
        this.inPeriod = inPeriod;
        this.selected = selected;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onPick.run(); }
        });
        setToolTipText(describe());
    }

    private String describe() {
        StringBuilder b = new StringBuilder(date.format(StudentUI.DAY)).append(": ");
        if (ci != null) b.append(Emoji.mood(ci.mood())).append(" Mood ").append(Kalma.MOODS[ci.mood() - 1].substring(4)).append(", stress ").append(CheckInView.STRESS[ci.stress() - 1]);
        else if (date.isBefore(LocalDate.now())) b.append("No check-in");
        else if (date.equals(LocalDate.now())) b.append("Today\u2019s check-in is open");
        else b.append("Upcoming");
        if (journal) b.append(" \u2022 journal entry");
        for (AcademicEvent e : events) b.append(" \u2022 ").append(e.title());
        return b.toString();
    }

    @Override public Dimension getPreferredSize() { return new Dimension(100, expanded ? 190 : 92); }

    private static String fit(String s, FontMetrics fm, int max) {
        if (fm.stringWidth(s) <= max) return s;
        while (s.length() > 1 && fm.stringWidth(s + "\u2026") > max) s = s.substring(0, s.length() - 1);
        return s + "\u2026";
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        int w = getWidth(), h = getHeight();
        LocalDate today = LocalDate.now();
        boolean isToday = date.equals(today), past = date.isBefore(today);
        boolean missing = ci == null && past;

        Color bg = !inPeriod ? new Color(0xFAFAFA) : ci != null ? new Color(0xF0FDF4) : missing ? new Color(0xF3F4F6) : Color.WHITE;
        g.setColor(bg);
        g.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
        if (missing && inPeriod) {
            g.setColor(new Color(0x9CA3AF));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f));
        } else {
            g.setColor(ci != null && inPeriod ? new Color(0xA7E3B7) : Brand.LINE);
        }
        g.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
        g.setStroke(new BasicStroke(1f));
        if (selected) {
            g.setColor(Brand.INK);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);
            g.setStroke(new BasicStroke(1f));
        }

        // day number
        Font num = Theme.BASE.deriveFont(Font.BOLD, 12f);
        g.setFont(num);
        FontMetrics fm = g.getFontMetrics();
        String n = String.valueOf(date.getDayOfMonth());
        if (isToday) {
            g.setColor(Brand.YELLOW);
            g.fillOval(6, 6, 22, 22);
        }
        g.setColor(inPeriod ? Brand.INK : new Color(0xB0B5BD));
        g.drawString(n, 17 - fm.stringWidth(n) / 2, 21);

        // completed check mark and journal icon (top right)
        int x = w - 22;
        if (ci != null && inPeriod) {
            g.setColor(GREEN);
            g.fillOval(x, 8, 14, 14);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x + 3, 15, x + 6, 18);
            g.drawLine(x + 6, 18, x + 11, 11);
            g.setStroke(new BasicStroke(1f));
            x -= 20;
        }
        if (journal && inPeriod) {
            g.setFont(Theme.BASE.deriveFont(13f));
            g.setColor(Brand.INK);
            g.drawString(Emoji.JOURNAL, x - 2, 20);
        }

        // body
        int fs = expanded ? 12 : 11;
        g.setFont(Theme.BASE.deriveFont((float) fs));
        fm = g.getFontMetrics();
        int y = expanded ? 52 : 42;
        int textW = w - 24;
        if (inPeriod && ci != null) {
            g.setColor(Brand.INK);
            g.drawString(fit(Emoji.mood(ci.mood()) + " Mood: " + Kalma.MOODS[ci.mood() - 1].substring(4), fm, textW + 12), 9, y);
            y += expanded ? 20 : 15;
            g.setColor(Brand.MUTED);
            g.drawString(fit("Stress: " + CheckInView.STRESS[ci.stress() - 1], fm, textW + 12), 9, y);
            y += expanded ? 20 : 15;
        } else if (inPeriod && missing) {
            g.setFont(Theme.BASE.deriveFont(Font.ITALIC, (float) fs));
            fm = g.getFontMetrics();
            g.setColor(new Color(0x6B7280));
            g.drawString("No check-in", 9, y);
            y += expanded ? 20 : 15;
        } else if (inPeriod && isToday) {
            g.setColor(Brand.MUTED);
            g.drawString("Today", 9, y);
            y += expanded ? 20 : 15;
        }

        // school dates as tags
        if (inPeriod && !events.isEmpty()) {
            g.setFont(Theme.BASE.deriveFont(Font.BOLD, 10f));
            fm = g.getFontMetrics();
            int max = expanded ? 3 : 1;
            int ty = expanded ? Math.max(y, 100) : h - 22;
            for (int i = 0; i < Math.min(max, events.size()); i++) {
                AcademicEvent e = events.get(i);
                String t = Emoji.event(e) + " " + e.title() + (!expanded && events.size() > 1 ? " +" + (events.size() - 1) : "");
                t = fit(t, fm, w - 24);
                int tw = fm.stringWidth(t) + 12;
                g.setColor(e.bg());
                g.fillRoundRect(6, ty, tw, 16, 16, 16);
                g.setColor(e.fg());
                g.drawString(t, 12, ty + 12);
                ty += 20;
            }
        }
        g.dispose();
    }
}
