import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Student "Resources" (Figma "Student - Resources"): guide cards, a "Need support right now?" panel with the
 * Guidance Office status and the 24/7 lines, the Guidance Office details, and a searchable directory by category.
 * The directory entries are edited by Guidance and Admin on the Maintenance page.
 */
class ResourcesView extends View {
    private static final String ALL = "All";
    private final String query;
    private String category = ALL;
    private final JPanel directory = new HugPanel(new BorderLayout());

    ResourcesView(MainWindow w) {
        super(w);
        query = window.takeSearch().toLowerCase();
        List<Resource> shown = store().visibleResources();

        put(StudentUI.header(w, "Resources", "Guides and tools curated for T.I.P. students."));
        put(guideArea());
        put(supportCard(shown));
        Optional<Resource> office = shown.stream().filter(r -> r.category().equals(Resource.GUIDANCE)).findFirst();
        if (office.isPresent() && query.isEmpty()) put(officeCard(office.get()));
        put(directoryCard(shown));
    }

    // ----- guides -----
    private JComponent guideArea() {
        List<Guide> guides = store().getGuides().stream()
                .filter(Guide::isShared)
                .filter(g -> (g.getTitle() + " " + g.getSummary() + " " + g.getBody()).toLowerCase().contains(query))
                .collect(Collectors.toList());
        if (guides.isEmpty()) {
            if (!query.isEmpty()) return new HugPanel(new BorderLayout()) {{ setOpaque(false); }};
            RoundPanel none = Staff.card(null, null, null);
            none.add(Staff.note("No guides have been shared yet."));
            return none;
        }
        JPanel grid = new HugPanel(new GridLayout(0, 3, 14, 14));
        grid.setOpaque(false);
        for (Guide g : guides) grid.add(StudentUI.actionCard(g.getIcon(), g.getTitle(), g.getSummary(), "Read", () -> read(g)));
        return grid;
    }

    private void read(Guide g) {
        window.message(g.getTitle() + "\n\n" + g.getBody());
    }

    // ----- need support right now -----
    /** True while the Guidance Office is staffed: weekdays inside the booking hours, outside the lunch break. */
    static boolean guidanceOpenNow() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek d = now.getDayOfWeek();
        if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) return false;
        LocalTime t = now.toLocalTime();
        return !t.isBefore(BookingRules.OPEN) && t.isBefore(BookingRules.CLOSE)
                && !(!t.isBefore(BookingRules.LUNCH_START) && t.isBefore(BookingRules.LUNCH_END));
    }

    private RoundPanel supportCard(List<Resource> shown) {
        boolean open = guidanceOpenNow();
        RoundPanel help = Staff.card("\u26A0\uFE0F  Need support right now?", null, null);

        RoundPanel office = Staff.row();
        office.add(Staff.twoLines("Campus Guidance Office", open ? "Walk in or book an appointment"
                : "Off duty right now. Use a 24/7 line below, or book for the next working day."), BorderLayout.CENTER);
        office.add(Staff.centered(open ? new Pill("Open", Brand.YELLOW, Brand.INK)
                : new Pill("Closed", Staff.NEUTRAL_BG, Staff.NEUTRAL_FG)), BorderLayout.EAST);
        Staff.click(office, () -> window.open("Guidance Office"));
        help.add(office);

        List<Resource> always = shown.stream().filter(Resource::open247)
                .filter(r -> r.category().equals(Resource.CRISIS)).collect(Collectors.toList());
        if (!always.isEmpty()) {
            help.add(Staff.gap(12));
            JLabel h = new JLabel("Available 24/7, even when Guidance is off duty");
            h.setFont(Theme.BASE.deriveFont(Font.BOLD, 12f));
            h.setForeground(Brand.MUTED);
            h.setAlignmentX(0f);
            help.add(h);
            help.add(Staff.gap(6));
            for (Resource r : always) {
                help.add(entryRow(r, false));
                help.add(Staff.gap(6));
            }
        }
        return help;
    }

    // ----- guidance office -----
    private RoundPanel officeCard(Resource r) {
        RoundPanel card = Staff.card("\uD83C\uDFDB\uFE0F  Guidance Office", null, null);
        card.add(wrap(r.description(), 12.5f, Brand.INK));
        card.add(Staff.gap(10));
        card.add(fact("Contact", r.contact()));
        card.add(fact("Office hours", r.hours()));
        card.add(fact("How to reach us", r.access()));
        card.add(Staff.gap(8));
        JLabel who = new JLabel("Guidance counselors");
        who.setFont(Theme.BASE.deriveFont(Font.BOLD, 13f));
        who.setForeground(Brand.INK);
        who.setAlignmentX(0f);
        card.add(who);
        card.add(Staff.gap(6));
        for (CounselorInfo c : store().getCounselors()) {
            card.add(Staff.personRow(false, initials(c.name()), c.name(), c.role() + "  \u2022  " + c.email(), null));
            card.add(Staff.gap(6));
        }
        YButton book = YButton.yellow("Book a consultation  \u2192");
        book.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        book.setBorder(new EmptyBorder(7, 16, 7, 16));
        book.addActionListener(e -> window.open("Book Consultation"));
        JPanel foot = new HugPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        foot.setOpaque(false);
        foot.setAlignmentX(0f);
        foot.add(book);
        card.add(Staff.gap(4));
        card.add(foot);
        return card;
    }

    private static String initials(String name) {
        String clean = name.replaceAll("^(Ms\\.|Mr\\.|Mrs\\.|Dr\\.)\\s*", "").replaceAll(",.*$", "").trim();
        String[] p = clean.split("\\s+");
        return ("" + p[0].charAt(0) + (p.length > 1 ? p[p.length - 1].charAt(0) : "")).toUpperCase();
    }

    /** Muted label over normal text. */
    private static JPanel fact(String label, String value) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(0f);
        JLabel l = new JLabel(label);
        l.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        l.setForeground(Brand.MUTED);
        l.setAlignmentX(0f);
        p.add(l);
        p.add(wrap(value, 12.5f, Brand.INK));
        p.add(Staff.gap(6));
        return p;
    }

    private static WrapText wrap(String text, float size, Color c) {
        return new WrapText(text, 600, c, size, false);
    }

    // ----- directory -----
    private RoundPanel directoryCard(List<Resource> shown) {
        RoundPanel card = Staff.card("\uD83D\uDCC2  Resource directory", null, null);
        JLabel sub = Staff.note(query.isEmpty() ? "Pick a category, or use the search bar at the top."
                : "Showing results for \"" + query + "\".");
        card.add(sub);
        card.add(Staff.gap(10));

        List<String> cats = new ArrayList<>();
        cats.add(ALL);
        for (Resource r : shown) if (!cats.contains(r.category())) cats.add(r.category());
        JPanel chips = new HugPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        chips.setAlignmentX(0f);
        ButtonGroup group = new ButtonGroup();
        for (String c : cats) {
            PillToggle t = new PillToggle("  " + c + "  ");
            t.setSelected(c.equals(category));
            t.addActionListener(e -> { category = c; fillDirectory(shown); });
            group.add(t);
            chips.add(t);
        }
        card.add(chips);
        card.add(Staff.gap(10));
        directory.setOpaque(false);
        directory.setAlignmentX(0f);
        card.add(directory);
        fillDirectory(shown);
        return card;
    }

    private void fillDirectory(List<Resource> shown) {
        List<Resource> list = shown.stream()
                .filter(r -> category.equals(ALL) || r.category().equals(category))
                .filter(r -> r.searchText().contains(query))
                .collect(Collectors.toList());
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        if (list.isEmpty()) col.add(Staff.note("No resources match. Try another category or search."));
        for (Resource r : list) {
            col.add(entryRow(r, true));
            col.add(Staff.gap(6));
        }
        directory.removeAll();
        directory.add(col, BorderLayout.CENTER);
        directory.revalidate();
        directory.repaint();
    }

    /** One tappable directory row: name, short description, contact, and pills on the right. */
    private RoundPanel entryRow(Resource r, boolean showCategory) {
        RoundPanel row = Staff.row();
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(r.title());
        t.setFont(Theme.BOLD);
        t.setForeground(Brand.INK);
        t.setAlignmentX(0f);
        text.add(t);
        text.add(Box.createVerticalStrut(2));
        if (showCategory) text.add(new WrapText(r.description(), 520, Brand.MUTED, 12f, false));
        JLabel c = new JLabel(r.contact());
        c.setFont(Theme.BASE.deriveFont(Font.BOLD, 12f));
        c.setForeground(Brand.INK);
        c.setAlignmentX(0f);
        text.add(Box.createVerticalStrut(2));
        text.add(c);
        row.add(text, BorderLayout.CENTER);

        JPanel pills = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pills.setOpaque(false);
        if (r.open247() && !(showCategory && r.category().equals(Resource.CRISIS))) pills.add(new Pill("24/7", Staff.GOOD_BG, Staff.GOOD_FG));
        if (showCategory) pills.add(new Pill(r.category(), Staff.NEUTRAL_BG, Staff.NEUTRAL_FG));
        row.add(Staff.centered(pills), BorderLayout.EAST);
        Staff.click(row, () -> details(r));
        return row;
    }

    /** Full information for one resource: what it is, how to contact it, when it is available, how to use it. */
    private void details(Resource r) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(420, 10));
        p.add(wrap(r.description(), 13f, Brand.INK));
        p.add(Staff.gap(10));
        p.add(fact("Contact", r.contact()));
        p.add(fact("When it is available", r.open247() ? "24/7. " + r.hours() : r.hours()));
        p.add(fact("How to access", r.access()));
        p.setPreferredSize(null);
        JOptionPane.showMessageDialog(window, p, r.title(), JOptionPane.PLAIN_MESSAGE);
    }
}
