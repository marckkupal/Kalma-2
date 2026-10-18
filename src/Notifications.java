import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Everything the notification bell shows (docs/NOTIFICATIONS.md). The rules live here, away from the screens,
 * so NotificationTest can check them without opening a window.
 *
 * A student's list can hold: unread messages, the daily check-in nudge, appointment reminders, important school
 * dates, and new announcements. Each kind (except messages) can be switched off in Profile > Notification
 * preferences. Guidance and Admin get unread messages and appointment reminders.
 */
final class Notifications {
    /** School dates are announced this many days ahead (and while they are happening). */
    static final int ACADEMIC_DAYS = 7;
    /** Announcements stay in the list for this many days after they are published. */
    static final int ACTIVITY_DAYS = 14;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    enum Kind {
        MESSAGE("💬"), CHECKIN("🙂"), APPOINTMENT("📅"),
        ACADEMIC("🎓"), ACTIVITY("📣");
        final String icon;
        Kind(String icon) { this.icon = icon; }
    }

    /**
     * One line in the bell. {@code key} identifies it so a dismissed notice stays dismissed. {@code page} is the
     * page its Open button goes to (null for none) and {@code detail} is extra text shown by View (null for none).
     */
    record Notice(Kind kind, String key, String text, String page, String detail) {}

    /** Preference keys a student can change: {key, label}. All are on by default except the opt-in email option. */
    static final String[][] STUDENT_OPTIONS = {
            {"remind-checkin", "Remind me to check in each day"},
            {"remind-appt", "Remind me about upcoming appointments (today and the next " + BookingRules.REMIND_DAYS + " days)"},
            {"remind-academic", "Tell me about important school dates (exams, holidays and deadlines in the next " + ACADEMIC_DAYS + " days)"},
            {"notify-activities", "Tell me about new announcements and campus activities"}};
    static final String EMAIL_OPTION = "email-reminders";

    private Notifications() {}

    // ----- the list -----
    /** Notices for this person that they have not dismissed, most urgent first. */
    static List<Notice> active(User u, DataStore ds, LocalDate today) {
        return all(u, ds, today).stream().filter(n -> !ds.isDismissed(u.getId(), n.key())).collect(Collectors.toList());
    }

    /** Every notice that applies today, dismissed or not. */
    static List<Notice> all(User u, DataStore ds, LocalDate today) {
        List<Notice> out = new ArrayList<>();
        String uid = u.getId();
        boolean student = !u.isStaff();

        long unread = ds.unreadCount(uid);
        if (unread > 0) {
            out.add(new Notice(Kind.MESSAGE, "msg|" + unread, "You have " + unread + " unread message" + (unread == 1 ? "" : "s") + ".",
                    u.isStaff() ? "Messages" : null, null));
        }
        if (student && ds.pref(uid, "remind-checkin") && ds.checkInsOf(uid).stream().noneMatch(c -> c.date().equals(today))) {
            out.add(new Notice(Kind.CHECKIN, "checkin|" + today, "Ready for a quick check-in today? It only takes a minute.", "Daily Check-In", null));
        }
        for (String line : ds.appointmentReminders(u)) {
            out.add(new Notice(Kind.APPOINTMENT, "appt|" + today + "|" + line, line, u.isStaff() ? "Appointments" : "My Appointments", null));
        }
        if (student && ds.pref(uid, "remind-academic")) out.addAll(academic(ds.academicEvents(), today));
        if (student && ds.pref(uid, "notify-activities")) out.addAll(activities(ds.getAnnouncements(), today));
        return out;
    }

    /** School dates that are happening now or start within {@link #ACADEMIC_DAYS} days, soonest first. */
    static List<Notice> academic(List<AcademicEvent> events, LocalDate today) {
        List<Notice> out = new ArrayList<>();
        events.stream().sorted(Comparator.comparing(AcademicEvent::start).thenComparing(AcademicEvent::title)).forEach(e -> {
            if (e.end().isBefore(today)) return;
            long days = ChronoUnit.DAYS.between(today, e.start());
            if (days > ACADEMIC_DAYS) return;
            boolean multi = !e.start().equals(e.end());
            String text;
            if (days <= 0) {
                text = e.title() + (multi ? " is happening now (until " + e.end().format(DAY) + ")." : " is today.");
            } else if (days == 1) {
                text = e.title() + " is tomorrow" + (multi ? " (" + e.range() + ")." : ".");
            } else {
                text = e.title() + (multi ? " starts in " : " is in ") + days + " days (" + e.range() + ").";
            }
            // The key changes when a date goes from "coming up" to "happening now", so that stage shows again.
            String key = "acad|" + e.title() + "|" + e.start() + "|" + (days <= 0 ? "now" : "soon");
            out.add(new Notice(Kind.ACADEMIC, key, text, "Calendar", null));
        });
        return out;
    }

    /** Announcements published in the last {@link #ACTIVITY_DAYS} days, newest first. */
    static List<Notice> activities(List<Announcement> announcements, LocalDate today) {
        return announcements.stream()
                .filter(a -> !a.date().isAfter(today) && ChronoUnit.DAYS.between(a.date(), today) <= ACTIVITY_DAYS)
                .sorted(Comparator.comparing(Announcement::date).reversed())
                .map(a -> new Notice(Kind.ACTIVITY, "ann|" + a.title() + "|" + a.date(), "New announcement: " + a.title(), null, a.body()))
                .collect(Collectors.toList());
    }

    // ----- optional email reminders -----
    /** An outgoing mail service. None is built in; plug one in with {@link #setEmailService} if the school has one. */
    interface EmailService {
        boolean available();
        void send(String to, String subject, String body) throws Exception;
    }

    private static EmailService email;
    private static final Map<String, LocalDate> emailedOn = new HashMap<>();

    static void setEmailService(EmailService s) { email = s; }
    /** True only when a mail service has been plugged in and says it is ready. The preference is hidden otherwise. */
    static boolean emailAvailable() { return email != null && email.available(); }

    /**
     * Emails today's reminders to a student who turned on email reminders, at most once a day. Messages are left
     * out (they are private to the app). Returns true if an email was sent. Never throws: a failure only means
     * the in-app reminders are the only ones.
     */
    static boolean sendEmailDigest(User u, DataStore ds, LocalDate today) {
        if (u.isStaff() || !emailAvailable() || !ds.pref(u.getId(), EMAIL_OPTION)) return false;
        if (today.equals(emailedOn.get(u.getId()))) return false;
        List<Notice> lines = active(u, ds, today).stream().filter(n -> n.kind() != Kind.MESSAGE).collect(Collectors.toList());
        if (lines.isEmpty()) return false;
        StringBuilder body = new StringBuilder("Hi " + u.getFirstName() + ",\n\nHere are your Kalma reminders for "
                + today.format(DAY) + ":\n\n");
        for (Notice n : lines) body.append("• ").append(n.text()).append('\n');
        body.append("\nYou can change these emails any time in Kalma: Profile > Notification preferences.\n");
        try {
            email.send(u.getEmail(), "Your Kalma reminders for " + today.format(DAY), body.toString());
            emailedOn.put(u.getId(), today);
            return true;
        } catch (Exception e) {
            System.err.println("Could not send reminder email: " + e.getMessage());
            return false;
        }
    }
}
