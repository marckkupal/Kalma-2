import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/** A student's own appointments, split into Upcoming and Previous. (Guidance uses GuidanceAppointmentsView.) */
class AppointmentsView extends View {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);
    private final List<Appointment> upcoming;
    private final JTable upcomingTable;

    AppointmentsView(MainWindow w) {
        super(w);
        List<Appointment> all = store().appointmentsFor(user());
        upcoming = all.stream().filter(Appointment::isUpcoming).collect(Collectors.toList());
        List<Appointment> previous = all.stream().filter(a -> !a.isUpcoming())
                .sorted(Comparator.comparing(Appointment::getSortKey).reversed()).collect(Collectors.toList());

        upcomingTable = Theme.table(new String[]{"When", "Counselor", "Reason", "Status"}, rowsOf(upcoming), 200, 200, 200, 100);
        upcomingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane upcomingScroll = Theme.scroll(upcomingTable);
        upcomingScroll.setPreferredSize(new Dimension(600, 30 * Math.min(Math.max(upcoming.size(), 2), 6) + 34));

        JTable previousTable = Theme.table(new String[]{"When", "Counselor", "Reason", "Status"}, rowsOf(previous), 200, 200, 200, 100);
        JScrollPane previousScroll = Theme.scroll(previousTable);
        previousScroll.setPreferredSize(new Dimension(600, 30 * Math.min(Math.max(previous.size(), 2), 6) + 34));

        JPanel bar = new HugPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setOpaque(false);
        bar.add(Theme.primary("Book new", () -> window.open("Book Consultation")));
        bar.add(Box.createHorizontalStrut(8));
        bar.add(Theme.secondary("Reschedule", this::reschedule));
        bar.add(Box.createHorizontalStrut(8));
        bar.add(Theme.secondary("Cancel appointment", this::cancel));

        List<String> reminders = store().appointmentReminders(user());
        if (!reminders.isEmpty()) put(Staff.reminders(reminders));
        put(Theme.heading("My Appointments"),
                Theme.wrapLabel("All consultations are in person at the Guidance Office. Select an upcoming appointment, then choose an action. "
                        + BookingRules.studentRulesText(), 640, Theme.INK, 13f, false),
                Theme.subheading("Upcoming"),
                upcoming.isEmpty() ? Staff.note("No upcoming appointments. Choose \"Book new\" to request one.") : upcomingScroll,
                bar,
                Theme.subheading("Previous (completed, cancelled, or past)"),
                previous.isEmpty() ? Staff.note("No previous appointments yet.") : previousScroll);
    }

    private List<Object[]> rowsOf(List<Appointment> list) {
        return list.stream().map(a -> new Object[]{
                LocalDate.parse(a.getDate()).format(DAY) + "  " + Staff.clock(a.getTime()),
                store().counselorName(a.getCounselorId()), a.getReason(), a.getStatus()}).collect(Collectors.toList());
    }

    private Appointment selected() {
        int r = upcomingTable.getSelectedRow();
        if (r < 0) { window.message("Select an upcoming appointment first."); return null; }
        return upcoming.get(r);
    }

    private void cancel() {
        Appointment a = selected();
        if (a == null) return;
        String blocker = BookingRules.changeBlocker(a, user(), false);
        if (blocker != null) { window.message(blocker); return; }
        if (!window.confirm("Cancel your appointment on " + LocalDate.parse(a.getDate()).format(DAY) + " at "
                + Staff.clock(a.getTime()) + "?")) return;
        String problem = store().cancelAppointment(a, user());
        if (problem != null) { window.message(problem); return; }
        window.refresh();
    }

    private void reschedule() {
        Appointment a = selected();
        if (a == null) return;
        String blocker = BookingRules.changeBlocker(a, user(), true);
        if (blocker != null) { window.message(blocker); return; }
        String[] slot = BookingView.pickNewSlot(window, a, BookingRules.BOOKING_WINDOW_WEEKDAYS);
        if (slot == null) return;
        String problem = store().rescheduleAppointment(a, slot[0], slot[1], user());
        if (problem != null) { window.message(problem); return; }
        window.message("Your appointment was moved to " + BookingView.dayLabel(slot[0]) + " at " + Staff.clock(slot[1])
                + ". Guidance will confirm the new time.");
        window.refresh();
    }
}
