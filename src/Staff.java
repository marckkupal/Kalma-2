import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
/** Shared building blocks for the Admin and Guidance pages. */
final class Staff {
    static final Color ROW = new Color(0xF6F6F1);
    static final Color BANNER = new Color(0xFEF6D5);
    static final Color GOOD_BG = new Color(0xDDF3E0), GOOD_FG = new Color(0x2F7D3E);
    static final Color WARN_BG = new Color(0xFCEFB4), WARN_FG = new Color(0x8A6A00);
    static final Color NEUTRAL_BG = new Color(0xE5E7EB), NEUTRAL_FG = new Color(0x4B5563);

    private Staff() {}

    static JComponent gap(int h) {
        JComponent c = (JComponent) Box.createVerticalStrut(h);
        c.setAlignmentX(0f);
        return c;
    }

    static void click(JComponent c, Runnable r) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { r.run(); }
        });
    }

    static String greeting() {
        int h = LocalTime.now().getHour();
        return h < 12 ? "Good morning," : h < 18 ? "Good afternoon," : "Good evening,";
    }

    /** "09:00" becomes "9:00 AM". */
    static String clock(String hhmm) {
        return LocalTime.parse(hhmm).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
    }

    /** Centers a component vertically (keeps pills from stretching to the row height). */
    static JPanel centered(JComponent c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    static Pill statusPill(Status s) {
        switch (s) {
            case CONFIRMED:
            case COMPLETED:
                return new Pill(s.toString(), GOOD_BG, GOOD_FG);
            case PENDING:
            case RESCHEDULED:
                return new Pill(s.toString(), WARN_BG, WARN_FG);
            default:
                return new Pill(s.toString(), NEUTRAL_BG, NEUTRAL_FG);
        }
    }

    /** Muted one-line note, used for empty lists. */
    static JLabel note(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.BASE.deriveFont(12f));
        l.setForeground(Brand.MUTED);
        l.setAlignmentX(0f);
        return l;
    }

    /** Pale yellow in-app reminder box listing each reminder on its own line. */
    static JPanel reminders(java.util.List<String> lines) { return reminders("Appointment reminders", lines); }

    static JPanel reminders(String heading, java.util.List<String> lines) {
        RoundPanel p = new RoundPanel(null, BANNER, new Color(0xF1E3A0), 16);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel title = new JLabel("\u23F0  " + heading);
        title.setFont(Theme.BOLD);
        title.setForeground(Brand.INK);
        title.setAlignmentX(0f);
        p.add(title);
        for (String line : lines) {
            p.add(gap(4));
            JLabel l = new JLabel(line);
            l.setFont(Theme.BASE.deriveFont(12f));
            l.setForeground(Brand.INK);
            l.setAlignmentX(0f);
            p.add(l);
        }
        p.setAlignmentX(0f);
        return p;
    }

    /** Yellow greeting banner with diagonal stripes at the right. */
    static JPanel banner(String name, String sub, String buttonText, Runnable action) {
        return banner(name, sub, buttonText, action, null);
    }

    /** Same banner with an optional component on the right (kept clear of the stripes by its own padding). */
    static JPanel banner(String name, String sub, String buttonText, Runnable action, JComponent aside) {
        JPanel b = new JPanel(new BorderLayout()) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }

            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Brand.aa(g);
                int w = getWidth(), h = getHeight();
                RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, w, h, 24, 24);
                g.setColor(BANNER);
                g.fill(shape);
                Graphics2D s = (Graphics2D) g.create();
                s.clip(shape);
                int c = w + h;
                Brand.band(s, w, h, c - 230, c - 170, Brand.YELLOW_STRIPE);
                Brand.band(s, w, h, c - 150, c - 60, Brand.YELLOW);
                Brand.band(s, w, h, c - 60, c, Brand.BLACK);
                s.dispose();
                g.setColor(new Color(0xF1E3A0));
                g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 24, 24));
                g.dispose();
            }
        };
        b.setOpaque(false);
        b.setBorder(new EmptyBorder(22, 26, 22, 26));

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        JLabel g1 = new JLabel(greeting());
        g1.setFont(Theme.BASE.deriveFont(16f));
        g1.setForeground(Brand.INK);
        JLabel g2 = new JLabel(name);
        g2.setFont(new Font("SansSerif", Font.BOLD, 36));
        g2.setForeground(Brand.INK);
        JLabel g3 = new JLabel(sub);
        g3.setFont(Theme.BASE.deriveFont(13f));
        g3.setForeground(Brand.MUTED);
        YButton go = YButton.yellow(buttonText);
        go.setBorder(new EmptyBorder(8, 18, 8, 18));
        go.addActionListener(e -> action.run());
        for (JComponent c : new JComponent[]{g1, g2, g3, go}) c.setAlignmentX(0f);
        col.add(g1);
        col.add(g2);
        col.add(Box.createVerticalStrut(4));
        col.add(g3);
        col.add(Box.createVerticalStrut(14));
        col.add(go);
        b.add(col, BorderLayout.CENTER);
        if (aside != null) b.add(aside, BorderLayout.EAST);
        return b;
    }

    /** Page header strip used by Reports, Announcements and the Guidance pages. */
    static JPanel header(String title, String sub, Runnable back) { return header(title, sub, back, false); }

    static JPanel header(String title, String sub, Runnable back, boolean large) {
        RoundPanel p = new RoundPanel(new BorderLayout(), BANNER, new Color(0xF1E3A0), 20);
        p.setBorder(new EmptyBorder(18, 24, 18, 24));
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, large ? 30 : 22));
        t.setForeground(Brand.INK);
        JLabel s = new JLabel(sub);
        s.setFont(Theme.BASE.deriveFont(large ? 13f : 12f));
        s.setForeground(Brand.MUTED);
        t.setAlignmentX(0f);
        s.setAlignmentX(0f);
        col.add(t);
        col.add(Box.createVerticalStrut(4));
        col.add(s);
        p.add(col, BorderLayout.CENTER);
        if (back != null) {
            YButton b = YButton.yellow("\uD83C\uDFE0  Back to Home");
            b.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
            b.setBorder(new EmptyBorder(6, 14, 6, 14));
            b.addActionListener(e -> back.run());
            p.add(centered(b), BorderLayout.EAST);
        }
        return p;
    }

    static JButton linkButton(String text, Runnable r) {
        JButton b = new JButton(text);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setForeground(new Color(0xB8860B));
        b.setFont(Theme.BASE.deriveFont(Font.BOLD, 12f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> r.run());
        return b;
    }

    /** White card with an optional title row and an optional link on the right. Add children with add(). */
    static RoundPanel card(String title, String link, Runnable onLink) {
        RoundPanel p = new RoundPanel(null, Color.WHITE, Theme.GRID, 20);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(16, 18, 16, 18));
        p.setAlignmentX(0f);
        if (title != null) {
            JPanel head = new HugPanel(new BorderLayout());
            head.setOpaque(false);
            head.setAlignmentX(0f);
            JLabel t = new JLabel(title);
            t.setFont(Theme.BASE.deriveFont(Font.BOLD, 16f));
            t.setForeground(Brand.INK);
            head.add(t, BorderLayout.WEST);
            if (link != null) head.add(linkButton(link, onLink), BorderLayout.EAST);
            p.add(head);
            p.add(gap(10));
        }
        return p;
    }

    /** Stat tile: icon badge, label and a big number. Clickable when onClick is not null. */
    static JPanel stat(String icon, String title, String value, Runnable onClick) {
        RoundPanel p = new RoundPanel(new BorderLayout(0, 8), Color.WHITE, Theme.GRID, 20);
        p.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(Theme.BASE.deriveFont(12f));
        t.setForeground(Brand.MUTED);
        top.add(new IconBadge(icon), BorderLayout.WEST);
        top.add(t, BorderLayout.CENTER);
        if (onClick != null) {
            JLabel chev = new JLabel("\u203A");
            chev.setFont(Theme.BASE.deriveFont(Font.BOLD, 18f));
            chev.setForeground(Brand.INK);
            top.add(chev, BorderLayout.EAST);
        }
        JLabel v = new JLabel(value);
        v.setFont(new Font("SansSerif", Font.BOLD, 30));
        v.setForeground(Brand.INK);
        v.setBorder(new EmptyBorder(2, 2, 0, 0));
        p.add(top, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        if (onClick != null) click(p, onClick);
        return p;
    }

    /** Light grey list row. A selected row gets a pale yellow fill and a yellow outline. */
    static RoundPanel row(boolean selected) {
        RoundPanel r = new RoundPanel(new BorderLayout(14, 0), selected ? BANNER : ROW,
                selected ? Brand.YELLOW_HOVER : Theme.GRID, 12);
        r.setBorder(new EmptyBorder(10, 14, 10, 14));
        r.setAlignmentX(0f);
        return r;
    }

    static RoundPanel row() { return row(false); }

    /** Bold line over a muted line. */
    static JPanel twoLines(String top, String bottom) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel a = new JLabel(top);
        a.setFont(Theme.BOLD);
        a.setForeground(Brand.INK);
        JLabel b = new JLabel(bottom);
        b.setFont(Theme.BASE.deriveFont(12f));
        b.setForeground(Brand.MUTED);
        a.setAlignmentX(0f);
        b.setAlignmentX(0f);
        p.add(a);
        p.add(Box.createVerticalStrut(2));
        p.add(b);
        return p;
    }

    /** Row with a small black avatar (yellow initials), two text lines and an optional component on the right. */
    static RoundPanel personRow(boolean selected, String initials, String top, String bottom, JComponent right) {
        RoundPanel r = row(selected);
        r.add(centered(new Avatar(initials, Brand.BLACK, Brand.YELLOW, 30)), BorderLayout.WEST);
        r.add(twoLines(top, bottom), BorderLayout.CENTER);
        if (right != null) r.add(centered(right), BorderLayout.EAST);
        return r;
    }

    /** Bar of quick links at the bottom of a dashboard. Each item is {icon, label, page key}. */
    static JPanel linkBar(java.util.function.Consumer<String> open, String[]... items) {
        RoundPanel p = new RoundPanel(new BorderLayout(), Color.WHITE, Theme.GRID, 20);
        p.setBorder(new EmptyBorder(16, 18, 16, 18));
        JPanel grid = new JPanel(new GridLayout(1, items.length, 14, 0));
        grid.setOpaque(false);
        for (String[] it : items) {
            RoundPanel b = new RoundPanel(new BorderLayout(10, 0), ROW, Theme.GRID, 14);
            b.setBorder(new EmptyBorder(14, 16, 14, 16));
            JLabel l = new JLabel(it[0] + "  " + it[1]);
            l.setFont(Theme.BASE.deriveFont(13f));
            l.setForeground(Brand.INK);
            JLabel c = new JLabel("\u203A");
            c.setFont(Theme.BASE.deriveFont(Font.BOLD, 18f));
            c.setForeground(Brand.INK);
            b.add(l, BorderLayout.CENTER);
            b.add(c, BorderLayout.EAST);
            click(b, () -> open.accept(it[2]));
            grid.add(b);
        }
        p.add(grid, BorderLayout.CENTER);
        return p;
    }
}
