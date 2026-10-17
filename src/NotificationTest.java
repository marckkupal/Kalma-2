import java.time.LocalDate;
import java.util.*;

/**
 * Checks the reminders and notifications (docs/NOTIFICATIONS.md): which notices a student sees, school date
 * wording and timing, preferences, dismissing, saving to disk, and the optional email digest.
 * No test framework needed.   java -cp out NotificationTest   (run it from a scratch folder: it writes data/)
 */
public class NotificationTest {
    private static int failures = 0, checks = 0;

    static void check(String name, boolean ok) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL  " + name); }
    }

    static final LocalDate T = LocalDate.of(2026, 10, 17);
    static AcademicEvent ev(String type, int fromDay, int toDay, String title) {
        return new AcademicEvent(T.plusDays(fromDay), T.plusDays(toDay), type, title);
    }
    static List<String> texts(List<Notifications.Notice> l) {
        List<String> out = new ArrayList<>();
        for (Notifications.Notice n : l) out.add(n.text());
        return out;
    }

    public static void main(String[] args) {
        // ----- school dates -----
        check("exam in 7 days is announced", texts(Notifications.academic(List.of(ev(AcademicEvent.FINALS, 7, 9, "Finals")), T)).size() == 1);
        check("exam in 8 days is not", Notifications.academic(List.of(ev(AcademicEvent.FINALS, 8, 9, "Finals")), T).isEmpty());
        check("date that ended yesterday is not", Notifications.academic(List.of(ev(AcademicEvent.FINALS, -3, -1, "Finals")), T).isEmpty());
        check("running exam says happening now", texts(Notifications.academic(List.of(ev(AcademicEvent.MIDTERMS, -1, 3, "Midterms")), T)).get(0).contains("happening now"));
        check("one-day holiday today says today", texts(Notifications.academic(List.of(ev(AcademicEvent.HOLIDAY, 0, 0, "Holiday")), T)).get(0).equals("Holiday is today."));
        check("tomorrow is worded as tomorrow", texts(Notifications.academic(List.of(ev(AcademicEvent.OTHER, 1, 1, "Deadline")), T)).get(0).equals("Deadline is tomorrow."));
        check("later date counts the days", texts(Notifications.academic(List.of(ev(AcademicEvent.FINALS, 5, 7, "Finals")), T)).get(0).startsWith("Finals starts in 5 days"));
        List<Notifications.Notice> two = Notifications.academic(List.of(ev(AcademicEvent.FINALS, 5, 7, "B"), ev(AcademicEvent.OTHER, 2, 2, "A")), T);
        check("soonest first", two.get(0).text().startsWith("A"));
        check("coming up and happening now have different keys",
                !Notifications.academic(List.of(ev(AcademicEvent.FINALS, 2, 4, "F")), T).get(0).key()
                        .equals(Notifications.academic(List.of(ev(AcademicEvent.FINALS, 2, 4, "F")), T.plusDays(2)).get(0).key()));

        // ----- announcements -----
        List<Announcement> anns = List.of(new Announcement("New", "Body", T.minusDays(1)), new Announcement("Old", "Body", T.minusDays(15)),
                new Announcement("Edge", "Body", T.minusDays(14)));
        List<String> a = texts(Notifications.activities(anns, T));
        check("recent announcement shown", a.contains("New announcement: New"));
        check("14-day-old announcement still shown", a.contains("New announcement: Edge"));
        check("15-day-old announcement hidden", !a.contains("New announcement: Old"));
        check("newest announcement first", a.get(0).endsWith("New"));
        check("announcement keeps its body for View", Notifications.activities(anns, T).get(0).detail().equals("Body"));

        // ----- per person -----
        DataStore d = DataStore.getInstance();
        d.seed();
        User maria = d.findById("u1").orElseThrow(), juan = d.findById("u2").orElseThrow(), counselor = d.findById("u3").orElseThrow();
        LocalDate today = LocalDate.now();
        Set<Notifications.Kind> kinds = new HashSet<>();
        for (Notifications.Notice n : Notifications.all(juan, d, today)) kinds.add(n.kind());
        check("a student who has not checked in gets the nudge", Notifications.all(maria, d, today).stream().anyMatch(n -> n.kind() == Notifications.Kind.CHECKIN)
                || d.checkInsOf("u1").stream().anyMatch(c -> c.date().equals(today)));
        check("seed announcements reach students", Notifications.all(juan, d, today).stream().anyMatch(n -> n.kind() == Notifications.Kind.ACTIVITY));
        check("staff get no check-in, school-date or announcement notices", Notifications.all(counselor, d, today).stream()
                .noneMatch(n -> n.kind() == Notifications.Kind.CHECKIN || n.kind() == Notifications.Kind.ACADEMIC || n.kind() == Notifications.Kind.ACTIVITY));
        check("staff still get appointment reminders", Notifications.all(counselor, d, today).stream().anyMatch(n -> n.kind() == Notifications.Kind.APPOINTMENT));

        // ----- preferences -----
        for (String[] o : Notifications.STUDENT_OPTIONS) check(o[0] + " is on by default", d.pref("u2", o[0]));
        check("email reminders are off by default", !d.pref("u2", Notifications.EMAIL_OPTION));
        d.setPref("u2", "notify-activities", false);
        check("announcements switch off", Notifications.all(juan, d, today).stream().noneMatch(n -> n.kind() == Notifications.Kind.ACTIVITY));
        d.setPref("u2", "notify-activities", true);
        check("announcements switch back on", Notifications.all(juan, d, today).stream().anyMatch(n -> n.kind() == Notifications.Kind.ACTIVITY));
        d.setPref("u2", "remind-checkin", false);
        check("check-in nudge switches off", Notifications.all(juan, d, today).stream().noneMatch(n -> n.kind() == Notifications.Kind.CHECKIN));
        d.setPref("u2", "remind-checkin", true);
        d.setPref("u2", "remind-academic", false);
        check("school dates switch off", Notifications.all(juan, d, today).stream().noneMatch(n -> n.kind() == Notifications.Kind.ACADEMIC));
        d.setPref("u2", "remind-academic", true);
        check("one student's choice does not change another's", d.pref("u1", "notify-activities"));

        // ----- dismissing -----
        Notifications.Notice first = Notifications.all(juan, d, today).stream().filter(n -> n.kind() == Notifications.Kind.ACTIVITY).findFirst().orElseThrow();
        d.dismiss("u2", first.key());
        check("dismissed notice leaves the list", Notifications.active(juan, d, today).stream().noneMatch(n -> n.key().equals(first.key())));
        check("dismissed notice is still in 'all'", Notifications.all(juan, d, today).stream().anyMatch(n -> n.key().equals(first.key())));
        check("dismissing is per person", Notifications.active(maria, d, today).stream().anyMatch(n -> n.key().equals(first.key())));

        // ----- saved settings survive a restart -----
        d.setPref("u2", "remind-appt", false);
        d.setPref("u2", Notifications.EMAIL_OPTION, true);
        try {
            List<String> saved = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("data", "notification-settings.tsv"));
            check("off pref is written", saved.contains("off\tu2\tremind-appt"));
            check("opt-in pref is written", saved.contains("on\tu2\t" + Notifications.EMAIL_OPTION));
            check("dismissed notice is written", saved.stream().anyMatch(l -> l.startsWith("dismissed\tu2\tann|")));
        } catch (java.io.IOException e) {
            check("settings file readable", false);
        }
        d.setPref("u2", "remind-appt", true);
        d.setPref("u2", Notifications.EMAIL_OPTION, false);

        // ----- optional email -----
        check("no email service means nothing is sent", !Notifications.sendEmailDigest(juan, d, today) && !Notifications.emailAvailable());
        List<String> sent = new ArrayList<>();
        Notifications.setEmailService(new Notifications.EmailService() {
            public boolean available() { return true; }
            public void send(String to, String subject, String body) { sent.add(to + "|" + body); }
        });
        check("email needs the student to opt in", !Notifications.sendEmailDigest(juan, d, today));
        d.setPref("u2", Notifications.EMAIL_OPTION, true);
        check("opted-in student gets one email", Notifications.sendEmailDigest(juan, d, today) && sent.size() == 1);
        check("email goes to the student's address", sent.get(0).startsWith(juan.getEmail() + "|"));
        check("email leaves out private messages", !sent.get(0).contains("unread message"));
        check("only one email a day", !Notifications.sendEmailDigest(juan, d, today) && sent.size() == 1);
        check("staff never get the digest", !Notifications.sendEmailDigest(counselor, d, today));
        Notifications.setEmailService(new Notifications.EmailService() {
            public boolean available() { return true; }
            public void send(String to, String subject, String body) throws Exception { throw new Exception("down"); }
        });
        check("a failing mail service does not throw", !Notifications.sendEmailDigest(juan, d, today.plusDays(1)));
        Notifications.setEmailService(null);
        d.setPref("u2", Notifications.EMAIL_OPTION, false);

        System.out.println(failures == 0 ? "ALL " + checks + " CHECKS PASSED" : failures + " of " + checks + " CHECKS FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
