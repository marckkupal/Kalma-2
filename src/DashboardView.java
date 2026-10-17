import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/** Student home: banner, Latest Mood, Mood Check-In, Quick Actions and a quote strip on the left; appointment, trends and announcements on the right. */
class DashboardView extends View {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    DashboardView(MainWindow w) {
        super(w);
        String uid = user().getId();
        Optional<CheckIn> last = store().checkInsOf(uid).stream().max(Comparator.comparing(CheckIn::date));
        Optional<Appointment> next = store().getAppointments().stream()
                .filter(a -> a.getStudentId().equals(uid) && a.isUpcoming())
                .min(Comparator.comparing(Appointment::getSortKey));

        JPanel topRow = new HugPanel(new GridBagLayout());
        topRow.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.weightx = 0.37;
        gc.insets = new Insets(0, 0, 0, 7);
        topRow.add(latestMoodCard(last), gc);
        gc.gridx = 1;
        gc.weightx = 0.63;
        gc.insets = new Insets(0, 7, 0, 0);
        topRow.add(moodCard(last), gc);

        JPanel left = column();
        stack(left, Staff.banner(user().getFirstName() + "!", "Take a deep breath. You're doing better than you think.",
                "🌿  Try the breathing guide", () -> window.message(Kalma.BREATHING),
                bannerAside()));
        stack(left, topRow);
        stack(left, quickActions());
        stack(left, new QuoteBar("You don't have to be perfect.<br>You just have to keep going.",
                "— T.I.P. Quezon City", window::open,
                new String[]{"🌿", "Mental Health", "Resources"},
                new String[]{"👥", "Peer Support", "Peer Support"},
                new String[]{"🏛️", "Guidance Office", "Guidance Office"},
                new String[]{"❤", "Better You", "Possible Solutions"}));

        JPanel right = column();
        stack(right, appointmentCard(next));
        stack(right, trendsCard());
        stack(right, new StreakCard(store().checkInsOf(uid), LocalDate.now(), false));

        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.setOpaque(false);
        leftWrap.add(left, BorderLayout.NORTH);
        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setOpaque(false);
        rightWrap.add(right, BorderLayout.NORTH);
        rightWrap.setPreferredSize(new Dimension(310, 10));

        JPanel main = new JPanel(new BorderLayout(16, 0));
        main.setOpaque(false);
        main.add(leftWrap, BorderLayout.CENTER);
        main.add(rightWrap, BorderLayout.EAST);
        // Reminders on Home: appointments, school dates and announcements. The check-in nudge and messages stay in the bell.
        List<String> reminders = Notifications.active(user(), store(), java.time.LocalDate.now()).stream()
                .filter(n -> n.kind() != Notifications.Kind.CHECKIN && n.kind() != Notifications.Kind.MESSAGE)
                .map(n -> n.kind().icon + "  " + n.text()).collect(java.util.stream.Collectors.toList());
        if (!reminders.isEmpty()) put(Staff.reminders("Reminders", reminders));
        AcademicPlan.focus(store().academicEvents(), java.time.LocalDate.now()).ifPresent(f -> put(headsUp(f)));
        put(main);
    }

    // ----- helpers -----
    private static JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static void stack(JPanel col, JComponent c) {
        c.setAlignmentX(0f);
        if (col.getComponentCount() > 0) col.add(Staff.gap(14));
        col.add(c);
    }

    private static JLabel small(String s, Color c) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.BASE.deriveFont(12f));
        l.setForeground(c);
        l.setAlignmentX(0f);
        return l;
    }

    private static JLabel chevron() {
        JLabel c = new JLabel("›");
        c.setFont(Theme.BASE.deriveFont(Font.BOLD, 20f));
        c.setForeground(Brand.INK);
        return c;
    }

    // ----- banner -----
    /** Sun and motto shown on the right side of the banner, behind a thin divider. */
    private JComponent bannerAside() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new CompoundBorder(new MatteBorder(0, 1, 0, 0, new Color(0xE9DFB5)),
                new EmptyBorder(10, 30, 10, 84)));
        JLabel sun = new JLabel("☀");
        sun.setFont(Theme.BASE.deriveFont(26f));
        sun.setForeground(Brand.YELLOW_HOVER);
        JLabel text = new JLabel("<html><div style='text-align:center;width:150px'>Small steps still lead to big progress.</div></html>");
        text.setFont(Theme.BASE.deriveFont(16f));
        text.setForeground(Brand.INK);
        sun.setAlignmentX(0.5f);
        text.setAlignmentX(0.5f);
        p.add(sun);
        p.add(Box.createVerticalStrut(6));
        p.add(text);
        return Staff.centered(p);
    }

    // ----- latest mood -----
    private JPanel latestMoodCard(Optional<CheckIn> ci) {
        RoundPanel p = new RoundPanel(new BorderLayout(14, 0), Color.WHITE, Theme.GRID, 20);
        p.setBorder(new EmptyBorder(18, 18, 18, 18));
        int mood = ci.map(CheckIn::mood).orElse(3);

        JPanel icon = new JPanel(new BorderLayout());
        icon.setOpaque(false);
        icon.add(new FaceIcon(44, mood), BorderLayout.NORTH);

        JLabel title = new JLabel("Latest Mood");
        title.setFont(Theme.BASE.deriveFont(13f));
        title.setForeground(Brand.INK);
        JLabel value = new JLabel(ci.map(c -> Kalma.MOODS[c.mood() - 1].substring(4)).orElse("None yet"));
        value.setFont(new Font("SansSerif", Font.BOLD, 24));
        value.setForeground(Brand.INK);
        JLabel date = small(ci.map(c -> c.date().format(DAY)).orElse("Save a check-in to see it here"), Brand.MUTED);
        JPanel col = column();
        title.setAlignmentX(0f);
        value.setAlignmentX(0f);
        col.add(title);
        col.add(value);
        col.add(Staff.gap(6));
        col.add(date);

        p.add(icon, BorderLayout.WEST);
        p.add(col, BorderLayout.CENTER);
        p.add(Staff.centered(chevron()), BorderLayout.EAST);
        Staff.click(p, () -> window.open("Daily Check-In"));
        return p;
    }

    // ----- mood check-in -----
    private JPanel moodCard(Optional<CheckIn> ci) {
        RoundPanel p = new RoundPanel(null, Color.WHITE, Theme.GRID, 20);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel head = new HugPanel(new BorderLayout(12, 0));
        head.setOpaque(false);
        head.setAlignmentX(0f);
        JLabel t = new JLabel("Mood Check-In");
        t.setFont(Theme.BASE.deriveFont(Font.BOLD, 16f));
        t.setForeground(Brand.INK);
        head.add(Staff.centered(new FaceIcon(28, 4)), BorderLayout.WEST);
        head.add(t, BorderLayout.CENTER);
        head.add(Staff.centered(chevron()), BorderLayout.EAST);
        Staff.click(head, () -> window.open("Daily Check-In"));

        JLabel sub = new JLabel("How are you feeling today?");
        sub.setFont(Theme.BASE.deriveFont(13f));
        sub.setForeground(Brand.MUTED);
        sub.setAlignmentX(0f);

        int picked = ci.map(CheckIn::mood).orElse(0);
        JPanel tiles = new HugPanel(new GridLayout(1, 5, 12, 0));
        tiles.setOpaque(false);
        tiles.setAlignmentX(0f);
        for (int i = 0; i < Kalma.MOODS.length; i++) {
            int mood = i + 1;
            tiles.add(new MoodTile(mood, Kalma.MOODS[i].substring(4),
                    picked == mood, () -> saveMood(mood)));
        }

        p.add(head);
        p.add(Staff.gap(8));
        p.add(sub);
        p.add(Staff.gap(14));
        p.add(tiles);
        return p;
    }

    /** Tapping a face opens the Daily Check-In with that mood selected. Nothing is saved until stress is chosen too. */
    private void saveMood(int mood) {
        CheckInView.preselectMood = mood;
        window.open("Daily Check-In");
    }

    // ----- quick actions -----
    private JPanel quickActions() {
        RoundPanel p = Staff.card("⚡  Quick Actions", null, null);
        JPanel grid = new HugPanel(new GridLayout(1, 4, 14, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(0f);
        String[][] items = {
                {"Assessment", "Assessment", "📋"}, {"Resources", "Resources", "📚"},
                {"Guidance", "Guidance Office", "🏛️"}, {"Peer Support", "Peer Support", "👥"}};
        for (String[] it : items) {
            RoundPanel tile = new RoundPanel(new BorderLayout(0, 18), Staff.ROW, Theme.GRID, 14);
            tile.setBorder(new EmptyBorder(16, 16, 14, 16));
            JLabel ic = new JLabel(it[2]);
            ic.setFont(Theme.BASE.deriveFont(24f));
            JLabel lb = new JLabel(it[0]);
            lb.setFont(Theme.BASE.deriveFont(13f));
            lb.setForeground(Brand.INK);
            JPanel foot = new JPanel(new BorderLayout());
            foot.setOpaque(false);
            foot.add(lb, BorderLayout.WEST);
            foot.add(chevron(), BorderLayout.EAST);
            JPanel icWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            icWrap.setOpaque(false);
            icWrap.add(ic);
            tile.add(icWrap, BorderLayout.NORTH);
            tile.add(foot, BorderLayout.SOUTH);
            Staff.click(tile, () -> window.open(it[1]));
            grid.add(tile);
        }
        p.add(grid);
        return p;
    }

    // ----- right column -----
    private JPanel appointmentCard(Optional<Appointment> next) {
        RoundPanel p = Staff.card("📅  Upcoming Appointment", "›", () -> window.open("My Appointments"));
        if (next.isEmpty()) {
            p.add(small("None scheduled.", Brand.MUTED));
            p.add(Staff.gap(10));
            YButton book = YButton.yellow("Book Consultation");
            book.setAlignmentX(0f);
            book.addActionListener(e -> window.open("Book Consultation"));
            p.add(book);
            return p;
        }
        Appointment a = next.get();
        RoundPanel inner = new RoundPanel(null, Staff.ROW, null, 14);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(new EmptyBorder(14, 14, 14, 14));
        inner.setAlignmentX(0f);

        JPanel top = new HugPanel(new BorderLayout());
        top.setOpaque(false);
        top.setAlignmentX(0f);
        JLabel kind = new JLabel("In-Person Consultation");
        kind.setFont(Theme.BOLD);
        kind.setForeground(Brand.INK);
        top.add(kind, BorderLayout.WEST);
        top.add(Staff.centered(Staff.statusPill(a.getStatus())), BorderLayout.EAST);

        JLabel when = small("📅  " + LocalDate.parse(a.getDate()).format(DAY) + "  •  "
                + Staff.clock(a.getTime()), Brand.MUTED);
        WrapText where = new WrapText("📍  T.I.P. QC Guidance Office (In-Person)", 240, Brand.MUTED, 12f, false);
        inner.add(top);
        inner.add(Staff.gap(8));
        inner.add(when);
        inner.add(Staff.gap(6));
        inner.add(where);
        p.add(inner);
        p.add(Staff.gap(10));

        YButton details = YButton.outline("View Details  →");
        details.setMaximumSize(new Dimension(Integer.MAX_VALUE, details.getPreferredSize().height));
        details.setAlignmentX(0f);
        details.addActionListener(e -> window.open("My Appointments"));
        p.add(details);
        return p;
    }

    private JPanel trendsCard() {
        RoundPanel p = Staff.card("📈  Wellness Trends", "View All", () -> window.open("Trends"));
        TrendChart chart = new TrendChart();
        chart.setFramed(false);
        chart.setColors(Brand.YELLOW, Brand.BLACK);
        chart.setPreferredSize(new Dimension(260, 210));
        chart.setAlignmentX(0f);
        chart.setDaily(store().checkInsOf(user().getId()), 7);
        p.add(chart);
        return p;
    }

    /** One-line heads-up for the next important school date, with a link to the suggestions on the Calendar page. */
    private JPanel headsUp(AcademicPlan.Focus f) {
        RoundPanel p = new RoundPanel(new BorderLayout(14, 0), Staff.BANNER, new Color(0xF1E3A0), 16);
        p.setBorder(new EmptyBorder(12, 16, 12, 16));
        p.add(Staff.twoLines("\uD83D\uDCC5  " + f.event().title() + "  \u2022  " + f.when(),
                f.event().range() + ". See wellness suggestions to help you get through it."), BorderLayout.CENTER);
        YButton go = YButton.yellow("Get ready  \u2192");
        go.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        go.setBorder(new EmptyBorder(6, 14, 6, 14));
        go.addActionListener(e -> window.open("Calendar"));
        p.add(Staff.centered(go), BorderLayout.EAST);
        return p;
    }
}
