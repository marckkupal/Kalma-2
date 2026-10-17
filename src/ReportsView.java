import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
/**
 * Admin reports: aggregated system activity and anonymized campus numbers. Only counts are shown, never names,
 * individual scores, assessment results or appointment reasons, and check-in figures need at least
 * AdminRules.MIN_GROUP different students (see docs/ADMIN_DASHBOARD.md).
 */
class ReportsView extends View {
    ReportsView(MainWindow w) {
        super(w);
        DataStore d = store();
        AdminRules.Summary s = AdminRules.summarize(d, LocalDate.now());

        JPanel stats = new HugPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(Staff.stat("🙂", "Avg. Campus Mood (7 days)", s.avgMood() == null ? "Not enough data" : String.format("%.1f / 5", s.avgMood()), null));
        stats.add(Staff.stat("📋", "Students Checking In (7 days)", AdminRules.countLabel(s.studentsCheckedIn()), null));
        stats.add(Staff.stat("📅", "Appointments", String.valueOf(d.getAppointments().size()), null));

        RoundPanel card = Staff.card("📈  Campus Mood and Stress", null, null);
        TrendChart chart = new TrendChart();
        chart.setFramed(false);
        chart.setColors(Brand.BLACK, Brand.YELLOW_HOVER);
        chart.setPreferredSize(new Dimension(600, 300));
        chart.setAlignmentX(0f);
        chart.setCampus(d.getCheckIns(), 7, AdminRules.MIN_GROUP);
        card.add(chart);
        card.add(Staff.gap(6));
        card.add(Staff.note("Each day shows only when at least " + AdminRules.MIN_GROUP + " different students checked in, so no one can be picked out."));

        RoundPanel accounts = Staff.card("👥  Accounts", "Manage Users", () -> window.open("Users"));
        accounts.add(line("Active students", s.activeStudents()));
        accounts.add(line("Peer Volunteers", s.volunteers()));
        accounts.add(line("Active administrators", s.activeAdmins()));
        accounts.add(line("Deactivated accounts", s.deactivatedAccounts()));

        RoundPanel appts = Staff.card("📅  Appointments", "Open", () -> window.open("Appointments"));
        appts.add(line("Today", s.appointmentsToday()));
        for (Status st : Status.values()) appts.add(line(st.toString(), s.appointments().get(st)));

        RoundPanel peer = Staff.card("🛡️  Peer Support moderation", "Open", () -> window.open("Moderation"));
        peer.add(line("Open reports", s.openReports()));
        peer.add(line("Urgent open reports", s.urgentReports()));
        peer.add(line("Hidden posts", s.hiddenPosts()));
        peer.add(line("Posts and replies, last 7 days", s.postsThisWeek()));

        JPanel counts = new HugPanel(new GridLayout(1, 3, 14, 0));
        counts.setOpaque(false);
        counts.add(accounts);
        counts.add(appts);
        counts.add(peer);

        put(Staff.header("Reports", "Aggregated system activity. Private student wellness information is never shown here.", () -> window.open("Dashboard")),
                stats, card, counts);
    }

    private static JPanel line(String label, long value) {
        RoundPanel r = Staff.row();
        JLabel l = new JLabel(label);
        l.setFont(Theme.BASE.deriveFont(13f));
        l.setForeground(Brand.INK);
        JLabel v = new JLabel(String.valueOf(value));
        v.setFont(Theme.BOLD);
        v.setForeground(Brand.INK);
        r.add(l, BorderLayout.CENTER);
        r.add(v, BorderLayout.EAST);
        JPanel wrap = new HugPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setAlignmentX(0f);
        wrap.setBorder(new javax.swing.border.EmptyBorder(0, 0, 6, 0));
        wrap.add(r);
        return wrap;
    }
}
