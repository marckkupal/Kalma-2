import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Guidance - Appointments (Figma frame): header strip, Today / Pending / This Week tiles and an
 * "Upcoming" list. Click a row to select it, then Confirm, Complete, Reschedule or Cancel.
 */
class GuidanceAppointmentsView extends View {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private final JPanel list = new JPanel();
    private Appointment selected;

    GuidanceAppointmentsView(MainWindow w) {
        super(w);
        DataStore d = store();
        LocalDate today = LocalDate.now();
        String monday = today.with(java.time.DayOfWeek.MONDAY).toString();
        String sunday = today.with(java.time.DayOfWeek.MONDAY).plusDays(6).toString();
        long todayN = d.getAppointments().stream()
                .filter(a -> a.getDate().equals(today.toString()) && a.occupiesSlot()).count();
        long pending = d.getAppointments().stream().filter(a -> a.getStatus() == Status.PENDING).count();
        long weekN = d.getAppointments().stream().filter(a -> a.occupiesSlot()
                && a.getDate().compareTo(monday) >= 0 && a.getDate().compareTo(sunday) <= 0).count();

        JPanel stats = new HugPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(Staff.stat("\uD83D\uDCC5", "Today", String.valueOf(todayN), null));
        stats.add(Staff.stat("\uD83D\uDD14", "Pending", String.valueOf(pending), null));
        stats.add(Staff.stat("\uD83D\uDDD3", "This Week", String.valueOf(weekN), null));

        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setAlignmentX(0f);

        JPanel bar = new HugPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.setAlignmentX(0f);
        bar.add(action("Confirm", true, () -> change(Status.CONFIRMED)));
        bar.add(action("Complete", false, () -> change(Status.COMPLETED)));
        bar.add(action("Reschedule", false, this::reschedule));
        bar.add(action("Cancel", false, () -> change(Status.CANCELLED)));
        bar.add(action("Manage Schedule", false, this::manageSchedule));

        RoundPanel upcoming = Staff.card("\uD83D\uDCC5  Upcoming", null, null);
        upcoming.add(list);
        upcoming.add(Staff.gap(6));
        upcoming.add(Staff.note("Select an appointment, then choose an action."));
        upcoming.add(Staff.gap(8));
        upcoming.add(bar);
        fill();

        RoundPanel history = Staff.card("\uD83D\uDD52  Completed and Cancelled", null, null);
        List<Appointment> past = d.appointmentsFor(user()).stream()
                .filter(a -> !a.isUpcoming()).collect(Collectors.toList());
        for (Appointment a : past) {
            history.add(row(a, false));
            history.add(Staff.gap(6));
        }

        put(Staff.header("Appointments", "Review, confirm, and manage student consultations.",
                () -> window.open("Dashboard")), stats, upcoming);
        if (!past.isEmpty()) put(history);
    }

    private static YButton action(String text, boolean primary, Runnable r) {
        YButton b = primary ? YButton.yellow(text) : YButton.outline(text);
        b.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        b.setBorder(new EmptyBorder(7, 16, 7, 16));
        b.addActionListener(e -> r.run());
        return b;
    }

    private void fill() {
        list.removeAll();
        List<Appointment> up = store().appointmentsFor(user()).stream()
                .filter(Appointment::isUpcoming).collect(Collectors.toList());
        if (selected != null && !up.contains(selected)) selected = null;
        if (up.isEmpty()) list.add(Staff.note("No upcoming appointments."));
        for (Appointment a : up) {
            list.add(row(a, true));
            list.add(Staff.gap(6));
        }
        list.revalidate();
        list.repaint();
    }

    private RoundPanel row(Appointment a, boolean pickable) {
        String sub = LocalDate.parse(a.getDate()).format(DAY) + " \u2022 " + Staff.clock(a.getTime()) + " \u2022 In-Person";
        RoundPanel r = Staff.personRow(pickable && a == selected, store().initialsOf(a.getStudentId()),
                store().nameOf(a.getStudentId()), sub, Staff.statusPill(a.getStatus()));
        r.setToolTipText(user().getRole().equals("admin") ? "The reason is private to Guidance." : "Reason: " + a.getReason());
        if (pickable) Staff.click(r, () -> { selected = a; fill(); });
        return r;
    }

    private Appointment need() {
        if (selected == null) window.message("Select an appointment first.");
        return selected;
    }

    private void change(Status s) {
        Appointment a = need();
        if (a == null || !window.confirm("Mark this appointment as " + s + "?")) return;
        String problem = store().updateStatus(a, s, user());
        if (problem != null) { window.message(problem); return; }
        window.refresh();
    }

    private void reschedule() {
        Appointment a = need();
        if (a == null) return;
        String[] slot = BookingView.pickNewSlot(window, a, 14);
        if (slot == null) return;
        String problem = store().rescheduleAppointment(a, slot[0], slot[1], user());
        if (problem != null) { window.message(problem); return; }
        window.refresh();
    }

    /** Close or reopen times so students cannot book them (meetings, leave, events). Booked times must be moved first. */
    private void manageSchedule() {
        DataStore d = store();
        List<CounselorInfo> counselors = d.getCounselors();
        List<String> dates = DataStore.nextWeekdays(14);
        List<String> slots = BookingRules.slots();
        JComboBox<String> who = Theme.combo(counselors.stream().map(CounselorInfo::name).toArray(String[]::new));
        JComboBox<String> day = Theme.combo(dates.stream().map(BookingView::dayLabel).toArray(String[]::new));
        List<String> timeLabels = new ArrayList<>();
        timeLabels.add("Whole day");
        slots.forEach(t -> timeLabels.add(Staff.clock(t)));
        JComboBox<String> time = Theme.combo(timeLabels.toArray(new String[0]));
        JTextArea blocked = new JTextArea(6, 34);
        blocked.setEditable(false);
        blocked.setFont(Theme.BASE);
        Runnable show = () -> {
            List<String[]> rows = d.blockedFromToday();
            blocked.setText(rows.isEmpty() ? "Nothing is blocked. Students can book every free time."
                    : rows.stream().map(p -> d.counselorName(p[0]) + "  |  " + BookingView.dayLabel(p[1]) + "  |  "
                            + Staff.clock(p[2])).collect(Collectors.joining("\n")));
            blocked.setCaretPosition(0);
        };
        show.run();
        String[] options = {"Block", "Unblock", "Close"};
        while (true) {
            int pick = JOptionPane.showOptionDialog(window, new Object[]{"Counselor", who, "Date", day, "Time", time,
                            "Blocked times (students cannot book these)", new JScrollPane(blocked)},
                    "Manage Schedule", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
            if (pick != 0 && pick != 1) break;
            String cid = counselors.get(who.getSelectedIndex()).id();
            String date = dates.get(day.getSelectedIndex());
            String t = time.getSelectedIndex() == 0 ? null : slots.get(time.getSelectedIndex() - 1);
            if (pick == 0) {
                String problem = d.blockTime(cid, date, t);
                if (problem != null) window.message(problem);
            } else {
                d.unblockTime(cid, date, t);
            }
            show.run();
        }
        window.refresh();
    }
}
