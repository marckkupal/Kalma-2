import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/** The consultation rules in one place. Change a constant here and booking, reminders and the pages follow. */
final class BookingRules {
    static final LocalTime OPEN = LocalTime.of(9, 0);
    static final LocalTime CLOSE = LocalTime.of(16, 0);
    /** Nothing can be booked during lunch. A slot that overlaps this window is dropped. */
    static final LocalTime LUNCH_START = LocalTime.of(11, 0);
    static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    static final int SLOT_MINUTES = 60;
    /** How many weekdays ahead (starting tomorrow) a student can book. */
    static final int BOOKING_WINDOW_WEEKDAYS = 10;
    /** Students must cancel or reschedule at least this long before the appointment. */
    static final int MIN_NOTICE_HOURS = 24;
    /** How many times a student may move one appointment. */
    static final int MAX_RESCHEDULES = 2;
    /** Reminders appear for appointments today and this many days ahead. */
    static final int REMIND_DAYS = 2;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private BookingRules() {}

    /** Start times a counselor can be booked, e.g. 09:00, 10:00, 13:00, 14:00, 15:00. Lunch is already removed. */
    static List<String> slots() {
        List<String> out = new ArrayList<>();
        for (LocalTime t = OPEN; !t.plusMinutes(SLOT_MINUTES).isAfter(CLOSE); t = t.plusMinutes(SLOT_MINUTES)) {
            if (!overlapsLunch(t)) out.add(t.toString());
        }
        return out;
    }

    static boolean overlapsLunch(LocalTime start) {
        return start.isBefore(LUNCH_END) && start.plusMinutes(SLOT_MINUTES).isAfter(LUNCH_START);
    }

    static String clock(String hhmm) { return LocalTime.parse(hhmm).format(CLOCK); }
    static String lunchLabel() { return LUNCH_START.format(CLOCK) + " to " + LUNCH_END.format(CLOCK); }

    /** One sentence describing the student change rules, shown on the booking and appointments pages. */
    static String studentRulesText() {
        return "You can cancel or reschedule up to " + MIN_NOTICE_HOURS + " hours before your appointment, "
                + "and reschedule each appointment up to " + MAX_RESCHEDULES + " times. "
                + "Closer than that, please visit or message the Guidance Office.";
    }

    /**
     * Why this person may not cancel or reschedule the appointment, or null when they may.
     * Guidance and Admin are not held to the notice or reschedule limit, because they manage the schedule.
     */
    static String changeBlocker(Appointment a, User actor, boolean reschedule) {
        if (a.getStatus() == Status.CANCELLED) return "This appointment is already cancelled.";
        if (a.getStatus() == Status.COMPLETED) return "Completed appointments cannot be changed.";
        if (actor.isStaff()) return null;
        if (!a.getStudentId().equals(actor.getId())) return "You can only change your own appointments.";
        if (!a.isUpcoming()) return "Past appointments cannot be changed.";
        if (LocalDateTime.now().plusHours(MIN_NOTICE_HOURS).isAfter(a.startsAt())) {
            return "Changes must be made at least " + MIN_NOTICE_HOURS + " hours before the appointment. "
                    + "Please visit or message the Guidance Office.";
        }
        if (reschedule && a.getRescheduleCount() >= MAX_RESCHEDULES) {
            return "This appointment has already been rescheduled " + MAX_RESCHEDULES
                    + " times. You can cancel it and book a new time.";
        }
        return null;
    }
}
