import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
/**
 * Main application window.
 * Students: light sidebar, white top bar. Admin and Guidance: black sidebar with a yellow selected item,
 * cream top bar with search, bell and user chip (matches the Figma dashboards).
 */
class MainWindow extends JFrame {
    private User user;
    private JPanel body;
    private String currentKey;
    /** The dark menu on the left. The three-line button in the top bar hides and shows it. */
    private JComponent sidebar;
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    MainWindow() {
        super("Kalma - Peer-Support Wellness Companion");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1360, 780);
        setMinimumSize(new Dimension(1000, 720));
        setLocationRelativeTo(null);
    }

    private String pendingSearch = "";
    void runSearch(String q) {
        pendingSearch = q;
        if (user.isStaff()) open(user.getRole().equals("admin") ? "Users" : "Students");
        else open("Resources");
    }
    String takeSearch() { String q = pendingSearch; pendingSearch = ""; return q; }

    User getUser() { return user; }
    void message(String s) { FormalDialog.show(this, s); }
    boolean confirm(String s) {
        return JOptionPane.showConfirmDialog(this, s, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /** 01 Splash. */
    void showSplash() {
        user = null;
        setContentPane(new SplashPanel(this));
        revalidate();
        repaint();
        setVisible(true);
    }

    /** 02 Tutorial (general, shown before login). */
    void showTutorial() {
        user = null;
        setContentPane(new TutorialPanel(this, null));
        revalidate();
        repaint();
    }

    /** 03 Login. */
    void showLogin() {
        user = null;
        setContentPane(new LoginPanel(this));
        revalidate();
        repaint();
        setVisible(true);
    }

    /** After a successful login: go straight to the role's home page. */
    void afterLogin(User u) {
        enter(u);
    }

    void enter(User u) {
        user = u;
        Notifications.sendEmailDigest(u, DataStore.getInstance(), LocalDate.now());
        body = new JPanel(new BorderLayout());
        body.setBackground(Brand.CREAM);
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Brand.CREAM);
        right.add(buildTopBar(), BorderLayout.NORTH);
        right.add(body, BorderLayout.CENTER);
        JLabel note = new JLabel(Kalma.NOTE);
        note.setFont(Theme.BASE.deriveFont(10f));
        note.setForeground(new Color(0x8A8F98));
        note.setHorizontalAlignment(SwingConstants.CENTER);
        note.setBorder(new EmptyBorder(6, 0, 14, 0));
        right.add(note, BorderLayout.SOUTH);
        JPanel root = new JPanel(new BorderLayout());
        sidebar = buildSidebar();
        root.add(sidebar, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);
        open(u.getMenu()[0]);
        revalidate();
        repaint();
    }

    /** The community a student has open inside Peer Support, or null for the list of communities. */
    private String communityId;
    String communityId() { return communityId; }

    void openCommunity(String id) {
        communityId = id;
        show("Peer Support");
    }

    void open(String key) {
        if (key.equals("Peer Support")) communityId = null;
        show(key);
    }

    private void show(String key) {
        currentKey = key;
        navButtons.forEach((k, b) -> staffSelected(b, k.equals(key)));
        updateBell();
        JScrollPane sp = new JScrollPane(createView(key));
        sp.setBorder(null);
        sp.getViewport().setBackground(Brand.CREAM);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        body.removeAll();
        body.add(sp, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
    }

    void refresh() { show(currentKey); }

    private View createView(String key) {
        return switch (key) {
            case "Home" -> new DashboardView(this);
            case "Assessment" -> new AssessmentView(this);
            case "Daily Check-In" -> new CheckInView(this);
            case "Calendar" -> new CalendarView(this);
            case "Trends" -> new TrendsView(this);
            case "Resources" -> user.isStaff() ? new GuidanceResourcesView(this) : new ResourcesView(this);
            case "Possible Solutions" -> new SolutionsView(this);
            case "Summary" -> new SummaryView(this);
            case "History" -> new HistoryView(this);
            case "Export Summary" -> new ExportView(this);
            case "Shared Summaries" -> new SharedSummariesView(this);
            case "Guidance Office" -> new GuidanceOfficeView(this);
            case "Peer Support" -> new PeerSupportView(this);
            case "Moderation" -> new ModerationView(this);
            case "Profile" -> new ProfileView(this);
            case "Book Consultation" -> new BookingView(this);
            case "My Appointments" -> new AppointmentsView(this);
            case "Appointments" -> new GuidanceAppointmentsView(this);
            case "Dashboard" -> user.getRole().equals("admin") ? new AdminDashboardView(this) : new GuidanceDashboardView(this);
            case "Users" -> new UsersView(this);
            case "Reports" -> new ReportsView(this);
            case "Announcements" -> new AnnouncementsView(this);
            case "Students" -> new StudentsView(this);
            case "Messages" -> new MessagesView(this);
            case "Maintenance" -> new ResourceMaintenanceView(this);
            case "Academic Calendar" -> new AcademicCalendarAdminView(this);
            default -> throw new IllegalArgumentException("Unknown view: " + key);
        };
    }

    /** Everything the bell shows (see Notifications): messages, check-in nudge, appointments, school dates, announcements. */
    private List<Notifications.Notice> notificationLines() {
        return Notifications.active(user, DataStore.getInstance(), LocalDate.now());
    }

    /** Opens the notification list. Each row can open its page or be dismissed; students can reach their preferences. */
    private void showNotifications() {
        JDialog d = new JDialog(this, "Notifications", true);
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Brand.CREAM);
        list.setBorder(new EmptyBorder(14, 16, 8, 16));
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setPreferredSize(new Dimension(580, 320));

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        foot.setBackground(Brand.CREAM);
        YButton dismissAll = YButton.outline("Dismiss all");
        YButton close = YButton.yellow("Close");
        close.addActionListener(e -> d.dispose());
        if (!user.isStaff()) {
            YButton prefs = YButton.outline("Preferences");
            prefs.addActionListener(e -> {
                ProfileView.notificationPreferences(this);
                fill(d, list, dismissAll);
            });
            foot.add(prefs);
        }
        dismissAll.addActionListener(e -> {
            for (Notifications.Notice n : notificationLines()) DataStore.getInstance().dismiss(user.getId(), n.key());
            fill(d, list, dismissAll);
        });
        foot.add(dismissAll);
        foot.add(close);

        d.getContentPane().setLayout(new BorderLayout());
        d.getContentPane().setBackground(Brand.CREAM);
        d.getContentPane().add(sp, BorderLayout.CENTER);
        d.getContentPane().add(foot, BorderLayout.SOUTH);
        fill(d, list, dismissAll);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
        updateBell();
    }

    /** (Re)builds the rows of the notification list and the bell badge. */
    private void fill(JDialog d, JPanel list, JButton dismissAll) {
        list.removeAll();
        List<Notifications.Notice> notices = notificationLines();
        dismissAll.setEnabled(!notices.isEmpty());
        if (notices.isEmpty()) {
            JLabel none = new JLabel("You have no new notifications.");
            none.setFont(Theme.BASE.deriveFont(14f));
            none.setForeground(Brand.MUTED);
            none.setAlignmentX(0f);
            none.setBorder(new EmptyBorder(30, 4, 30, 4));
            list.add(none);
        }
        for (Notifications.Notice n : notices) {
            RoundPanel row = Staff.row();
            JLabel text = new JLabel("<html><div style='width:250px'>" + escapeHtml(n.text()) + "</div></html>");
            text.setFont(Theme.BASE.deriveFont(13f));
            text.setForeground(Brand.INK);
            JLabel icon = new JLabel(n.kind().icon);
            icon.setFont(Theme.BASE.deriveFont(18f));
            icon.setBorder(new EmptyBorder(0, 0, 0, 10));
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            buttons.setOpaque(false);
            if (n.page() != null || n.detail() != null) {
                YButton go = YButton.yellow(n.page() != null ? "Open" : "View");
                go.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
                go.setBorder(new EmptyBorder(5, 14, 5, 14));
                go.addActionListener(e -> {
                    if (n.page() != null) {
                        DataStore.getInstance().dismiss(user.getId(), n.key());
                        d.dispose();
                        open(n.page());
                    } else {
                        message(n.text() + "\n\n" + n.detail());
                        DataStore.getInstance().dismiss(user.getId(), n.key());
                        fill(d, list, dismissAll);
                    }
                });
                buttons.add(go);
            }
            YButton x = YButton.outline("Dismiss");
            x.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
            x.setBorder(new EmptyBorder(5, 12, 5, 12));
            x.addActionListener(e -> {
                DataStore.getInstance().dismiss(user.getId(), n.key());
                fill(d, list, dismissAll);
            });
            buttons.add(x);
            row.setBorder(new EmptyBorder(10, 14, 10, 14));
            row.add(icon, BorderLayout.WEST);
            row.add(text, BorderLayout.CENTER);
            row.add(Staff.centered(buttons), BorderLayout.EAST);
            row.setAlignmentX(0f);
            list.add(row);
            list.add(Staff.gap(6));
        }
        list.revalidate();
        list.repaint();
        updateBell();
        d.revalidate();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** The bell button with a small red count badge. */
    private JButton bell;
    private int bellCount;

    private void updateBell() {
        if (bell == null || user == null) return;
        bellCount = notificationLines().size();
        bell.setToolTipText(bellCount == 0 ? "No new notifications" : bellCount + " notification" + (bellCount == 1 ? "" : "s"));
        bell.repaint();
    }

    /** Cream header for every role: white search pill, bell, black avatar with yellow initials, name and role. */
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Brand.CREAM);
        bar.setBorder(new EmptyBorder(18, 28, 6, 28));

        String hint = switch (user.getRole()) {
            case "admin" -> "Search users, reports, or anything...";
            case "student" -> "Search resources, solutions, or anything...";
            default -> "Search students, appointments, or anything...";
        };
        SearchField search = new SearchField(hint, Color.WHITE);
        search.setPreferredSize(new Dimension(560, 40));
        search.addActionListener(e -> runSearch(search.getText().trim()));
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(menuToggle());
        searchWrap.add(Box.createHorizontalStrut(10));
        searchWrap.add(search);

        JLabel nm = new JLabel(user.getName());
        nm.setFont(Theme.BOLD);
        nm.setForeground(Brand.INK);
        JLabel rl = new JLabel(user.getRoleLabel());
        rl.setFont(Theme.BASE.deriveFont(11f));
        rl.setForeground(Brand.MUTED);
        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(nm);
        names.add(rl);

        bell = new JButton("\uD83D\uDD14") {
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                if (bellCount <= 0) return;
                Graphics2D g = (Graphics2D) g0.create();
                Brand.aa(g);
                String t = bellCount > 9 ? "9+" : String.valueOf(bellCount);
                g.setFont(Theme.BASE.deriveFont(Font.BOLD, 10f));
                int d = Math.max(16, g.getFontMetrics().stringWidth(t) + 8);
                int x = getWidth() - d, y = 2;
                g.setColor(new Color(0xDC2626));
                g.fillOval(x, y, d, 16);
                g.setColor(Color.WHITE);
                g.drawString(t, x + (d - g.getFontMetrics().stringWidth(t)) / 2, y + 12);
                g.dispose();
            }
        };
        bell.setBorderPainted(false);
        bell.setContentAreaFilled(false);
        bell.setFocusPainted(false);
        bell.setFont(Theme.BASE.deriveFont(18f));
        bell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bell.addActionListener(e -> showNotifications());
        JLabel chevron = new JLabel("\u25BE");
        chevron.setForeground(Brand.INK);

        JPanel who = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        who.setOpaque(false);
        who.add(bell);
        who.add(new Avatar(user.getInitials(), Brand.BLACK, Brand.YELLOW));
        who.add(names);
        who.add(chevron);

        bar.add(searchWrap, BorderLayout.WEST);
        bar.add(who, BorderLayout.EAST);
        return bar;
    }

    /** Three-line button that hides or shows the sidebar. */
    private JButton menuToggle() {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Brand.aa(g);
                g.setColor(getModel().isRollover() ? Brand.YELLOW : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g.setColor(Brand.LINE);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g.setColor(Brand.INK);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int x1 = getWidth() / 2 - 8, x2 = getWidth() / 2 + 8, cy = getHeight() / 2;
                for (int dy : new int[]{-6, 0, 6}) g.drawLine(x1, cy + dy, x2, cy + dy);
                g.dispose();
            }
        };
        b.setPreferredSize(new Dimension(40, 40));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText("Hide menu");
        b.addActionListener(e -> {
            if (sidebar == null) return;
            sidebar.setVisible(!sidebar.isVisible());
            b.setToolTipText(sidebar.isVisible() ? "Hide menu" : "Show menu");
            getContentPane().revalidate();
            getContentPane().repaint();
        });
        return b;
    }

    /** Emoji shown beside a sidebar item. */
    private static String iconOf(String key) {
        return switch (key) {
            case "Home", "Dashboard" -> "\uD83C\uDFE0";
            case "Assessment" -> "\uD83D\uDCCB";
            case "Daily Check-In" -> "\uD83D\uDE42";
            case "Calendar" -> "\uD83D\uDDD3\uFE0F";
            case "Trends" -> "\uD83D\uDCC8";
            case "Summary" -> "\uD83D\uDCC4";
            case "History" -> "\uD83D\uDD70\uFE0F";
            case "Export Summary" -> "\uD83D\uDCE4";
            case "Shared Summaries" -> "\uD83D\uDCE5";
            case "Possible Solutions" -> "\uD83D\uDCA1";
            case "Resources" -> "\uD83D\uDCDA";
            case "Maintenance" -> "\uD83D\uDEE0\uFE0F";
            case "Academic Calendar" -> "\uD83C\uDF93";
            case "Guidance Office" -> "\uD83C\uDFDB\uFE0F";
            case "Peer Support" -> "\uD83D\uDC65";
            case "Moderation" -> "\uD83D\uDEE1\uFE0F";
            case "My Appointments", "Appointments" -> "\uD83D\uDCC5";
            case "Profile" -> "\uD83D\uDC64";
            case "Users" -> "\uD83D\uDC65";
            case "Students" -> "\uD83C\uDF93";
            case "Messages" -> "\uD83D\uDCAC";
            case "Reports" -> "\uD83D\uDCCA";
            case "Announcements" -> "\uD83D\uDCE3";
            default -> "";
        };
    }

    // ----- dark sidebar (all roles) -----
    private static final Color STAFF_HOVER = new Color(0x1F1F23);
    private static final Color STAFF_LINE = new Color(0x2A2A2E);

    private JButton staffNav(String text, Runnable action) {
        RoundButton b = new RoundButton(text, Brand.BLACK, STAFF_HOVER, Color.WHITE, null);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setAlignmentX(0f);
        b.addActionListener(e -> action.run());
        staffSelected(b, false);
        return b;
    }

    private void staffSelected(JButton b, boolean on) {
        ((RoundButton) b).setColors(on ? Brand.YELLOW : Brand.BLACK, on ? Brand.YELLOW : STAFF_HOVER,
                on ? Brand.INK : Color.WHITE);
        b.setFont(on ? Theme.BOLD : Theme.BASE);
    }

    private JLabel sectionHeading(String text) {
        JLabel h = new JLabel(text);
        h.setFont(Theme.BASE.deriveFont(11f));
        h.setForeground(new Color(0x9CA3AF));
        h.setBorder(new EmptyBorder(0, 14, 6, 0));
        h.setAlignmentX(0f);
        return h;
    }

    /** Label shown in the sidebar for a page key. */
    private static String labelOf(String key) {
        return key.equals("Daily Check-In") ? "Check-In" : key.equals("Calendar") ? "Calendar & Journal" : key;
    }

    private JComponent buildSidebar() {
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(Brand.BLACK);
        nav.setBorder(new EmptyBorder(24, 16, 20, 16));

        BrandLogo logo = new BrandLogo(36, 24f, "Your Mental Wellness Companion", true);
        logo.setAlignmentX(0f);
        nav.add(logo);
        nav.add(Box.createVerticalStrut(24));

        boolean student = user.getRole().equals("student");
        navButtons.clear();
        for (String key : user.getMenu()) {
            // Students reach "My Appointments" from the Upcoming Appointment card, so it has no sidebar entry.
            if (student && key.equals("My Appointments")) continue;
            String heading = student
                    ? switch (key) {
                        case "Possible Solutions" -> "SUPPORT";
                        case "Peer Support" -> "COMMUNITY";
                        case "Profile" -> "";
                        default -> null;
                    }
                    : key.equals("Resources") || (key.equals("Maintenance") && user.getRole().equals("admin")) ? "RESOURCES" : null;
            if (heading != null) {
                nav.add(Box.createVerticalStrut(heading.isEmpty() ? 12 : 16));
                if (!heading.isEmpty()) nav.add(sectionHeading(heading));
            }
            JButton b = staffNav(iconOf(key) + "  " + labelOf(key), () -> open(key));
            navButtons.put(key, b);
            nav.add(b);
            nav.add(Box.createVerticalStrut(4));
        }
        if (student) {
            nav.add(staffNav("\u21A9  Logout", this::showLogin));
            nav.add(Box.createVerticalGlue());
        } else {
            nav.add(Box.createVerticalGlue());
            nav.add(Box.createVerticalStrut(10));
            nav.add(staffNav("\u21A9  Logout", this::showLogin));
        }
        JScrollPane scroll = new JScrollPane(nav, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Brand.BLACK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scroll.setPreferredSize(new Dimension(240, 0));
        return scroll;
    }
}
