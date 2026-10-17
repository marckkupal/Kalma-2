import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
/** Admin home: banner, four stat tiles, campus trends, recent activity and quick links. */
class AdminDashboardView extends View {
    AdminDashboardView(MainWindow w) {
        super(w);
        DataStore d = store();
        LocalDate today = LocalDate.now();
        long students = d.students().size();
        long todays = d.getCheckIns().stream().filter(c -> c.date().equals(today)).count();
        long pending = d.getAppointments().stream().filter(a -> a.getStatus() == Status.PENDING).count();

        JPanel banner = Staff.banner(user().getGreetingName() + "!",
                "Here is how Kalma is doing across campus today.", "View reports  \u2192", () -> window.open("Reports"));

        JPanel stats = new HugPanel(new GridLayout(1, 5, 14, 0));
        stats.setOpaque(false);
        stats.add(Staff.stat("\uD83C\uDF93", "Active Students", String.format("%,d", students), () -> window.open("Users")));
        stats.add(Staff.stat("\uD83D\uDE42", "Check-ins Today", String.format("%,d", todays), () -> window.open("Reports")));
        stats.add(Staff.stat("\uD83D\uDCC5", "Appointments", String.valueOf(d.getAppointments().size()), () -> window.open("Appointments")));
        stats.add(Staff.stat("\u23F3", "Pending Requests", String.valueOf(pending), () -> window.open("Appointments")));
        stats.add(Staff.stat("\u26A0\uFE0F", "Open Reports", String.valueOf(d.openReports().size()), () -> window.open("Moderation")));

        RoundPanel trends = Staff.card("\uD83D\uDCC8  Campus Wellness Trends", "View All", () -> window.open("Reports"));
        TrendChart chart = new TrendChart();
        chart.setFramed(false);
        chart.setColors(Brand.BLACK, Brand.YELLOW_HOVER);
        chart.setPreferredSize(new Dimension(420, 250));
        chart.setAlignmentX(0f);
        chart.setCampus(d.getCheckIns(), 7, AdminRules.MIN_GROUP);
        trends.add(chart);

        RoundPanel activity = Staff.card("\uD83D\uDD52  Recent Activity", null, null);
        for (Activity a : d.recentActivity(4)) {
            RoundPanel r = Staff.row();
            r.add(Staff.twoLines(a.title(), a.detail() + " \u00B7 " + DataStore.ago(a.at())), BorderLayout.CENTER);
            activity.add(r);
            activity.add(Staff.gap(6));
        }

        JPanel mid = new HugPanel(new GridBagLayout());
        mid.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.weightx = 0.64;
        gc.insets = new Insets(0, 0, 0, 7);
        mid.add(trends, gc);
        gc.gridx = 1;
        gc.weightx = 0.36;
        gc.insets = new Insets(0, 7, 0, 0);
        mid.add(activity, gc);

        JPanel links = Staff.linkBar(window::open,
                new String[]{"\uD83D\uDC65", "Manage Users", "Users"},
                new String[]{"\uD83D\uDCCA", "View Reports", "Reports"},
                new String[]{"\uD83D\uDEE1\uFE0F", "Moderation", "Moderation"},
                new String[]{"\uD83D\uDCE3", "Post Announcement", "Announcements"});

        put(banner, stats, mid, links);
    }
}
