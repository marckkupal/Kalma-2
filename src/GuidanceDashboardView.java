import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/** Guidance home: banner, four stat tiles, today's schedule, check-in highlights and quick links. */
class GuidanceDashboardView extends View {
    GuidanceDashboardView(MainWindow w) {
        super(w);
        DataStore d = store();
        String today = LocalDate.now().toString();
        List<Appointment> todays = d.getAppointments().stream()
                .filter(a -> a.getDate().equals(today) && a.occupiesSlot())
                .sorted(Comparator.comparing(Appointment::getSortKey)).collect(Collectors.toList());
        long pending = d.getAppointments().stream().filter(a -> a.getStatus() == Status.PENDING).count();

        JPanel banner = Staff.banner(user().getGreetingName() + "!",
                "You have " + todays.size() + (todays.size() == 1 ? " appointment" : " appointments") + " scheduled today.",
                "View appointments  \u2192", () -> window.open("Appointments"));

        JPanel stats = new HugPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.add(Staff.stat("\uD83D\uDCC5", "Today's Appointments", String.valueOf(todays.size()), () -> window.open("Appointments")));
        stats.add(Staff.stat("\uD83D\uDD14", "Pending Requests", String.valueOf(pending), () -> window.open("Appointments")));
        stats.add(Staff.stat("\uD83D\uDC65", "Following Up", String.valueOf(d.followUpCount()), () -> window.open("Students")));
        stats.add(Staff.stat("\uD83D\uDCAC", "Unread Messages", String.valueOf(d.unreadCount(user().getId())), () -> window.open("Messages")));

        RoundPanel schedule = Staff.card("\uD83D\uDCC5  Today's Schedule", "\u203A", () -> window.open("Appointments"));
        if (todays.isEmpty()) schedule.add(Staff.note("No appointments today."));
        for (Appointment a : todays) {
            RoundPanel r = Staff.row();
            JLabel time = new JLabel(Staff.clock(a.getTime()));
            time.setFont(Theme.BOLD.deriveFont(12f));
            time.setForeground(Brand.INK);
            time.setPreferredSize(new Dimension(74, 20));
            r.add(time, BorderLayout.WEST);
            r.add(Staff.twoLines(d.nameOf(a.getStudentId()), "In-Person \u00B7 " + a.getReason()), BorderLayout.CENTER);
            r.add(Staff.centered(Staff.statusPill(a.getStatus())), BorderLayout.EAST);
            schedule.add(r);
            schedule.add(Staff.gap(6));
        }

        RoundPanel highlights = Staff.card("\u26A0\uFE0F  Check-in Highlights", "\u203A", () -> window.open("Students"));
        List<String[]> notes = d.highlights();
        if (notes.isEmpty()) highlights.add(Staff.note("No highlights this week."));
        for (int i = 0; i < Math.min(4, notes.size()); i++) {
            RoundPanel r = Staff.row();
            r.add(Staff.twoLines(notes.get(i)[0], notes.get(i)[1]), BorderLayout.CENTER);
            highlights.add(r);
            highlights.add(Staff.gap(6));
        }

        JPanel mid = new HugPanel(new GridBagLayout());
        mid.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.weightx = 0.64;
        gc.insets = new Insets(0, 0, 0, 7);
        mid.add(schedule, gc);
        gc.gridx = 1;
        gc.weightx = 0.36;
        gc.insets = new Insets(0, 7, 0, 0);
        mid.add(highlights, gc);

        JPanel links = Staff.linkBar(window::open,
                new String[]{"\uD83D\uDCC5", "Appointments", "Appointments"},
                new String[]{"\uD83D\uDC65", "Students", "Students"},
                new String[]{"\uD83D\uDCAC", "Messages", "Messages"},
                new String[]{"\uD83D\uDEE1\uFE0F", "Moderation", "Moderation"},
                new String[]{"\uD83D\uDCDA", "Resources", "Resources"});

        put(banner, stats, mid, links);
    }
}
