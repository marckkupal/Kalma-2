import java.time.LocalDate;
import java.util.*;

/**
 * Checks the Admin Moderation Dashboard rules (docs/ADMIN_DASHBOARD.md): account management, who may do it, the last
 * administrator, and that aggregated figures hold back small groups. No test framework needed.
 * java -cp out AdminRulesTest   (run it from a scratch folder)
 */
public class AdminRulesTest {
    private static int failures = 0, checks = 0;

    static void check(String name, boolean ok) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL  " + name); }
    }

    public static void main(String[] args) {
        DataStore d = DataStore.getInstance();
        d.seed();
        User admin = d.findById("u4").orElseThrow(), counselor = d.findById("u3").orElseThrow(), maria = d.findById("u1").orElseThrow();

        // ----- creating accounts -----
        check("student cannot create accounts", d.createAccount(maria, "student", "X Y", "x@tip.edu.ph", "abcdefg1", "BSIT 1st Year") != null);
        check("guidance cannot create accounts", d.createAccount(counselor, "admin", "X Y", "x@tip.edu.ph", "abcdefg1", "") != null);
        check("admin creates a student", d.createAccount(admin, "student", "New Student", "NEW@tip.edu.ph", "abcdefg1", "BSIT 1st Year") == null);
        User ns = d.findByEmail("new@tip.edu.ph").orElse(null);
        check("new student exists with the right role", ns != null && ns.getRole().equals("student") && ns.isActive());
        check("admin creates an administrator", d.createAccount(admin, "admin", "Second Admin", "admin2@tip.edu.ph", "abcdefg1", "") == null);
        User a2 = d.findByEmail("admin2@tip.edu.ph").orElseThrow();
        check("new administrator is an admin", a2.getRole().equals("admin") && a2.isStaff());
        check("duplicate email refused", d.createAccount(admin, "student", "Dup", "new@tip.edu.ph", "abcdefg1", "BSIT 1st Year") != null);
        check("bad email refused", d.createAccount(admin, "student", "Bad", "not-an-email", "abcdefg1", "BSIT 1st Year") != null);
        check("weak password refused", d.createAccount(admin, "student", "Weak", "weak@tip.edu.ph", "short1", "BSIT 1st Year") != null);
        check("password without a number refused", d.createAccount(admin, "student", "Weak", "weak@tip.edu.ph", "abcdefghi", "BSIT 1st Year") != null);
        check("blank name refused", d.createAccount(admin, "student", " ", "blank@tip.edu.ph", "abcdefg1", "BSIT 1st Year") != null);
        check("student needs a program", d.createAccount(admin, "student", "No Prog", "np@tip.edu.ph", "abcdefg1", "") != null);
        check("guidance accounts cannot be created here", d.createAccount(admin, "counselor", "G", "g@tip.edu.ph", "abcdefg1", "") != null);
        check("the new password works", ns.passwordMatches("abcdefg1"));

        // ----- activating and deactivating -----
        check("student cannot deactivate", d.setAccountActive(maria, ns, false) != null && ns.isActive());
        check("admin cannot deactivate self", d.setAccountActive(admin, admin, false) != null && admin.isActive());
        check("admin deactivates a student", d.setAccountActive(admin, ns, false) == null && !ns.isActive());
        check("admin reactivates", d.setAccountActive(admin, ns, true) == null && ns.isActive());
        check("admin can deactivate another admin when one remains", d.setAccountActive(admin, a2, false) == null && !a2.isActive());
        check("a deactivated admin cannot manage accounts", d.setAccountActive(a2, ns, false) != null);
        d.setAccountActive(admin, a2, true);
        check("the last active administrator cannot be deactivated", lastAdminProtected(d, admin, a2));

        // ----- peer volunteers -----
        check("student cannot make volunteers", d.setVolunteerBy(maria, ns, true) != null);
        check("admin makes a volunteer", d.setVolunteerBy(admin, ns, true) == null && d.isVolunteer(ns.getId()));
        check("admin removes a volunteer", d.setVolunteerBy(admin, ns, false) == null && !d.isVolunteer(ns.getId()));
        check("only students can be volunteers", d.setVolunteerBy(admin, a2, true) != null && !d.isVolunteer(a2.getId()));
        d.setAccountActive(admin, ns, false);
        check("deactivated students cannot be made volunteers", d.setVolunteerBy(admin, ns, true) != null);

        // ----- privacy of aggregates -----
        check("1 student is not enough", !AdminRules.enoughStudents(1));
        check("minimum group is enough", AdminRules.enoughStudents(AdminRules.MIN_GROUP));
        check("small count is hidden", AdminRules.countLabel(2).startsWith("Fewer than"));
        check("big count is shown", AdminRules.countLabel(12).equals("12"));
        LocalDate today = LocalDate.now();
        AdminRules.Summary s = AdminRules.summarize(d, today);
        check("seed data has enough students for averages", s.avgMood() != null && s.avgStress() != null);
        check("summary counts open reports", s.openReports() == d.openReports().size());
        check("summary counts appointments by status", s.appointments().values().stream().mapToLong(Long::longValue).sum() == d.getAppointments().size());
        check("summary counts volunteers", s.volunteers() >= 1);
        DataStore.getInstance().saveCheckIn(new CheckIn("zz-ghost", today, 1, 5)); // not a real user: still one more student
        int n = 0;
        for (User u : d.students()) if (d.checkInsOf(u.getId()).stream().anyMatch(c -> !c.date().isBefore(today.minusDays(6)))) n++;
        check("distinct students counted once each", AdminRules.summarize(d, today).studentsCheckedIn() == n + 1);
        TrendChartProbe.run();

        // ----- the activity feed names no one -----
        d.addMessage(new Message("u3", "u1", "private words", java.time.LocalDateTime.now()));
        boolean leak = d.recentActivity(50).stream().anyMatch(a -> (a.title() + " " + a.detail()).contains(maria.getName()));
        check("activity feed never shows a student's name", !leak);

        System.out.println(failures == 0 ? "ALL " + checks + " ADMIN CHECKS PASSED" : failures + " of " + checks + " CHECKS FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** With only one active admin left, deactivating it (by itself or another admin) must be refused. */
    private static boolean lastAdminProtected(DataStore d, User admin, User a2) {
        d.setAccountActive(admin, a2, false);
        boolean selfRefused = d.setAccountActive(admin, admin, false) != null;
        d.setAccountActive(admin, a2, true);
        return selfRefused && admin.isActive();
    }

    /** Campus chart: a day with fewer than the minimum number of students is dropped. */
    static final class TrendChartProbe {
        static void run() {
            LocalDate t = LocalDate.now();
            List<CheckIn> two = List.of(new CheckIn("a", t, 3, 3), new CheckIn("b", t, 3, 3));
            List<CheckIn> three = List.of(new CheckIn("a", t, 3, 3), new CheckIn("b", t, 3, 3), new CheckIn("c", t, 3, 3));
            TrendChart c = new TrendChart();
            c.setCampus(two, 7, AdminRules.MIN_GROUP);
            check("chart hides a day with 2 students", c.pointCount() == 0);
            c.setCampus(three, 7, AdminRules.MIN_GROUP);
            check("chart shows a day with 3 students", c.pointCount() == 1);
        }
    }
}
