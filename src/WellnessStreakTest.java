import java.time.LocalDate;
import java.util.*;

/**
 * Checks Wellness Streaks (docs/WELLNESS_STREAKS.md): the counts, that a gap never costs progress, and that the
 * wording stays supportive. No test framework needed.   java -cp out WellnessStreakTest
 */
public class WellnessStreakTest {
    private static int failures = 0, checks = 0;
    static void check(String name, boolean ok) { checks++; if (!ok) { failures++; System.out.println("FAIL  " + name); } }

    static final LocalDate T = LocalDate.of(2026, 10, 17);
    static CheckIn ci(int daysAgo) { return new CheckIn("u", T.minusDays(daysAgo), 3, 3); }
    static List<CheckIn> days(int... ago) { List<CheckIn> l = new ArrayList<>(); for (int a : ago) l.add(ci(a)); return l; }
    static WellnessStreak.Result of(int... ago) { return WellnessStreak.of(days(ago), T); }

    /** Words that would make a student feel judged for a gap. */
    static final String[] GUILT = {"missed", "miss ", "failed", "forgot", "behind", "should have", "worse", "bad ", "warning",
            "must", "lazy", "disappoint", "lost", "lose", "broke", "reset", "streak", "days ago", "since you", "gap", "again"};

    static boolean gentle(String text) {
        String t = text.toLowerCase();
        for (String g : GUILT) if (t.contains(g)) return false;
        return true;
    }

    public static void main(String[] args) {
        // ----- counts -----
        WellnessStreak.Result a = of(0, 1, 2, 5, 9);
        check("total days", a.totalDays() == 5);
        check("this week counts 4", a.daysThisWeek() == 4);
        check("last week counts 1", a.daysLastWeek() == 1);
        check("run of 3 ending today", a.currentRun() == 3 && a.checkedInToday());
        check("best run", a.bestRun() == 3);
        check("last7 flags, today last", a.last7()[6] && a.last7()[5] && a.last7()[4] && !a.last7()[3] && a.last7()[1] && !a.last7()[0]);
        check("same day twice counts once", WellnessStreak.of(List.of(ci(0), ci(0)), T).totalDays() == 1);
        check("a check-in dated tomorrow is ignored", WellnessStreak.of(List.of(new CheckIn("u", T.plusDays(1), 3, 3)), T).totalDays() == 0);

        // ----- today is never held against anyone -----
        WellnessStreak.Result y = of(1, 2, 3);
        check("run still counts when only today is empty", y.currentRun() == 3 && !y.checkedInToday());
        check("yesterday message", y.message().contains("yesterday"));

        // ----- a gap costs nothing -----
        WellnessStreak.Result gap = of(0, 1, 2, 10, 11, 12, 13, 14);
        check("progress keeps all days", gap.totalDays() == 8);
        WellnessStreak.Result away = of(20, 21, 22, 23);
        check("after a break the run is 0 but totals stay", away.currentRun() == 0 && away.totalDays() == 4);
        check("welcome back wording", away.message().startsWith("Welcome back") && away.message().contains("4 days so far"));
        check("best run is kept after a break", away.bestRun() == 4);

        // ----- milestones only go up -----
        check("no check-ins: no milestone, next is 1", of().reached() == 0 && of().nextMilestone() == 1);
        check("1 day reaches first milestone", of(0).reached() == 1 && of(0).nextMilestone() == 3);
        check("7 days reach 3 milestones", of(0, 1, 2, 3, 4, 5, 6).reached() == 3);
        int[] many = new int[100];
        for (int i = 0; i < 100; i++) many[i] = i;
        check("100 days reach all milestones", of(many).reached() == WellnessStreak.MILESTONES.length && of(many).nextMilestone() == 0);
        check("milestones are the same after a long break", WellnessStreak.of(days(40, 41, 42), T).reached() == 2);

        // ----- progress note -----
        check("more days than last week is celebrated", WellnessStreak.progress(4, 2).contains("more days"));
        check("same pace is steady", WellnessStreak.progress(3, 3).contains("Steady"));
        check("fewer days says nothing", WellnessStreak.progress(1, 5).isEmpty());
        check("nothing to compare says nothing", WellnessStreak.progress(0, 3).isEmpty() && WellnessStreak.progress(3, 0).isEmpty());

        // ----- wording: every message in every situation stays supportive -----
        List<String> all = new ArrayList<>();
        int[][] cases = {{}, {0}, {0, 1}, {0, 1, 2, 3, 4, 5, 6, 7}, {1}, {1, 2, 3}, {5}, {30, 31}, {200}, {0, 5, 6, 7, 8, 9, 10}};
        for (int[] c : cases) { WellnessStreak.Result r = of(c); all.add(r.message()); all.add(r.progress()); }
        all.add(WellnessStreak.progress(4, 2));
        all.add(WellnessStreak.progress(3, 3));
        for (int m : WellnessStreak.MILESTONES) all.add(WellnessStreak.milestoneName(m));
        for (String s : all) check("gentle: " + s, gentle(s));
        check("no check-ins message is welcoming", of().message().contains("no pressure"));

        System.out.println(failures == 0 ? "ALL " + checks + " STREAK CHECKS PASSED" : failures + " of " + checks + " streak checks FAILED");
        if (failures > 0) System.exit(1);
    }
}
