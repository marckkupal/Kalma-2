import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/** Shared pieces for the student pages that use the large header (Assessment, Check-In, Trends, Summary and more). */
final class StudentUI {
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    private StudentUI() {}

    /** Yellow banner with the page title and a "Back to Home" button. */
    static JPanel header(MainWindow w, String title, String sub) {
        return Staff.header(title, sub, () -> w.open("Home"), true);
    }

    /** Equal-width row of components. */
    static JPanel row(int gap, JComponent... items) {
        JPanel p = new HugPanel(new GridLayout(1, items.length, gap, 0));
        p.setOpaque(false);
        for (JComponent c : items) p.add(c);
        return p;
    }

    /** Keeps a component at its natural size, left-aligned and vertically centred. */
    static JPanel leftMiddle(JComponent c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.weightx = 1;
        p.add(c, gc);
        return p;
    }

    /** White tile: round icon on the left, small label over a big value. */
    static JPanel statTile(String icon, String label, String value) {
        RoundPanel p = new RoundPanel(new BorderLayout(14, 0), Color.WHITE, Theme.GRID, 20);
        p.setBorder(new EmptyBorder(18, 18, 18, 18));
        JLabel l = new JLabel(label);
        l.setFont(Theme.BASE.deriveFont(12f));
        l.setForeground(Brand.MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font("SansSerif", Font.BOLD, 30));
        v.setForeground(Brand.INK);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        l.setAlignmentX(0f);
        v.setAlignmentX(0f);
        col.add(l);
        col.add(v);
        p.add(Staff.centered(new IconBadge(icon, 42)), BorderLayout.WEST);
        p.add(leftMiddle(col), BorderLayout.CENTER);
        return p;
    }

    /** White card: round icon, title, short text, and a small yellow button pinned to the bottom. */
    static RoundPanel actionCard(String icon, String title, String desc, String button, Runnable go) {
        RoundPanel p = new RoundPanel(new BorderLayout(0, 14), Color.WHITE, Theme.GRID, 20);
        p.setBorder(new EmptyBorder(18, 18, 18, 18));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        IconBadge badge = new IconBadge(icon, 42);
        JLabel t = new JLabel(title);
        t.setFont(Theme.BASE.deriveFont(Font.BOLD, 15f));
        t.setForeground(Brand.INK);
        WrapText d = new WrapText(desc, 260, Brand.MUTED, 12f, false);
        badge.setAlignmentX(0f);
        t.setAlignmentX(0f);
        top.add(badge);
        top.add(Staff.gap(10));
        top.add(t);
        top.add(Staff.gap(4));
        top.add(d);
        YButton b = YButton.yellow(button + "  →");
        b.setBorder(new EmptyBorder(7, 16, 7, 16));
        b.addActionListener(e -> go.run());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        foot.setOpaque(false);
        foot.add(b);
        p.add(top, BorderLayout.NORTH);
        p.add(foot, BorderLayout.SOUTH);
        return p;
    }

    /** Grey list row: bold line over a muted line, with a green pill on the right. */
    static RoundPanel listRow(String top, String bottom, String pill) {
        RoundPanel r = Staff.row();
        r.add(Staff.twoLines(top, bottom), BorderLayout.CENTER);
        r.add(Staff.centered(new Pill(pill, Staff.GOOD_BG, Staff.GOOD_FG)), BorderLayout.EAST);
        return r;
    }

    /** Check-ins from the last 7 days, oldest first. */
    static List<CheckIn> lastWeek(List<CheckIn> all) {
        return all.stream().filter(c -> !c.date().isBefore(LocalDate.now().minusDays(6)))
                .sorted(Comparator.comparing(CheckIn::date)).collect(Collectors.toList());
    }
}
