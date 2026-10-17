import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
/**
 * What the Admin Moderation Dashboard may show (docs/ADMIN_DASHBOARD.md). Administrators see counts about the
 * system, never an individual student's wellness data, so every figure built from check-ins is held back until
 * at least {@link #MIN_GROUP} different students are behind it. AdminRulesTest checks these rules.
 */
final class AdminRules {
    /** A check-in figure is only shown when at least this many different students contributed to it. */
    static final int MIN_GROUP = 3;

    private AdminRules() {}

    static boolean enoughStudents(long distinctStudents) { return distinctStudents >= MIN_GROUP; }

    /** "12", or "Fewer than 3" when the count is too small to show without pointing at individuals. */
    static String countLabel(long distinctStudents) {
        return enoughStudents(distinctStudents) ? String.format("%,d", distinctStudents) : "Fewer than " + MIN_GROUP;
    }

    /** Plain counts only. Mood and stress are null when too few students checked in to show an average safely. */
    record Summary(long activeStudents, long deactivatedAccounts, long activeAdmins, long volunteers,
                   Map<Status, Long> appointments, long appointmentsToday,
                   long studentsCheckedIn, Double avgMood, Double avgStress,
                   long openReports, long urgentReports, long hiddenPosts, long postsThisWeek) {}

    static Summary summarize(DataStore d, LocalDate today) {
        LocalDate from = today.minusDays(6);
        List<CheckIn> week = d.getCheckIns().stream()
                .filter(c -> !c.date().isBefore(from) && !c.date().isAfter(today)).collect(Collectors.toList());
        long distinct = week.stream().map(CheckIn::userId).distinct().count();
        boolean show = enoughStudents(distinct);
        Map<Status, Long> appts = new EnumMap<>(Status.class);
        for (Status s : Status.values()) appts.put(s, 0L);
        for (Appointment a : d.getAppointments()) appts.merge(a.getStatus(), 1L, Long::sum);
        long apptToday = d.getAppointments().stream().filter(a -> a.isUpcoming() && a.getDate().equals(today.toString())).count();
        return new Summary(
                d.students().size(),
                d.getUsers().stream().filter(u -> !u.isActive()).count(),
                d.getUsers().stream().filter(u -> u.getRole().equals("admin") && u.isActive()).count(),
                d.getUsers().stream().filter(u -> d.isVolunteer(u.getId())).count(),
                appts, apptToday, distinct,
                show ? week.stream().mapToInt(CheckIn::mood).average().orElse(0) : null,
                show ? week.stream().mapToInt(CheckIn::stress).average().orElse(0) : null,
                d.openReports().size(),
                d.openReports().stream().filter(ContentReport::isUrgent).count(),
                d.hiddenPostCount(),
                d.allPosts().stream().filter(p -> !p.getAt().toLocalDate().isBefore(from)).count());
    }

    /** Password rule used for accounts an administrator creates (same as student sign-up). Null when fine. */
    static String passwordProblem(String pass) {
        return pass == null || pass.length() < 8 || !pass.matches(".*\\d.*") ? "Password needs 8+ characters and at least one number." : null;
    }
}
