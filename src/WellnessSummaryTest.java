import java.time.LocalDate;
import java.util.*;

/**
 * Checks the weekly and monthly summaries (docs/WELLNESS_DASHBOARD.md): the numbers, the comparison rules, and that
 * the wording stays encouraging. No test framework needed.   java -cp out WellnessSummaryTest
 */
public class WellnessSummaryTest {
    private static int failures = 0, checks = 0;

    static void check(String name, boolean ok) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL  " + name); }
    }

    static final LocalDate T = LocalDate.of(2026, 10, 17);
    static CheckIn ci(int daysAgo, int mood, int stress) { return new CheckIn("u", T.minusDays(daysAgo), mood, stress); }

    /** Words that would make a student feel judged for a gap or a change. */
    static final String[] GUILT = {"missed", "failed", "forgot", "behind", "should have", "streak", "worse", "bad ", "warning", "must", "lazy", "disappoint"};

    static boolean gentle(String text) {
        String t = text.toLowerCase();
        for (String g : GUILT) if (t.contains(g)) return false;
        return true;
    }

    public static void main(String[] args) {
        // ----- numbers -----
        List<CheckIn> data = List.of(ci(0, 4, 2), ci(1, 2, 4), ci(3, 5, 1), ci(9, 3, 3), ci(10, 3, 3));
        WellnessSummary.Result w = WellnessSummary.summarize(data, 7, T);
        check("week counts 3 check-ins", w.checkInDays() == 3);
        check("week mood average", Math.abs(w.avgMood() - 11.0 / 3) < 1e-9);
        check("week stress average", Math.abs(w.avgStress() - 7.0 / 3) < 1e-9);
        check("brightest day is the highest mood", w.bestDay().equals(T.minusDays(3)));
        check("older check-ins are not in the week", w.checkInDays() == 3);
        WellnessSummary.Result m = WellnessSummary.summarize(data, 30, T);
        check("month counts all 5", m.checkInDays() == 5);
        check("tie for brightest day takes the latest", WellnessSummary.summarize(List.of(ci(5, 4, 3), ci(2, 4, 3)), 7, T).bestDay().equals(T.minusDays(2)));
        check("a check-in dated tomorrow is ignored", WellnessSummary.summarize(List.of(new CheckIn("u", T.plusDays(1), 5, 1)), 7, T).checkInDays() == 0);
        check("day 7 back is outside the week", WellnessSummary.summarize(List.of(ci(7, 3, 3)), 7, T).checkInDays() == 0);
        check("day 6 back is inside the week", WellnessSummary.summarize(List.of(ci(6, 3, 3)), 7, T).checkInDays() == 1);

        // ----- empty and thin data -----
        WellnessSummary.Result none = WellnessSummary.summarize(List.of(), 7, T);
        check("no check-ins: no averages", none.avgMood() == null && none.avgStress() == null && none.bestDay() == null);
        check("no check-ins: reassuring message", none.message().contains("completely okay"));
        check("no check-ins: no comparison", !none.canCompare());
        WellnessSummary.Result one = WellnessSummary.summarize(List.of(ci(0, 3, 3)), 7, T);
        check("one check-in: thanks and 'on 1 day'", one.message().contains("on 1 day this week"));
        check("one check-in cannot compare", !one.canCompare());
        check("comparison needs 2 days in both periods", !WellnessSummary.summarize(List.of(ci(0, 3, 3), ci(1, 3, 3), ci(8, 3, 3)), 7, T).canCompare());
        check("comparison works with 2 and 2", WellnessSummary.summarize(List.of(ci(0, 3, 3), ci(1, 3, 3), ci(8, 3, 3), ci(9, 3, 3)), 7, T).canCompare());

        // ----- comparison wording -----
        WellnessSummary.Result up = WellnessSummary.summarize(List.of(ci(0, 3, 5), ci(1, 3, 5), ci(8, 3, 2), ci(9, 3, 2)), 7, T);
        check("stress up is described kindly", up.message().contains("heavier") && up.message().contains("normal") && up.message().contains("kind to yourself"));
        WellnessSummary.Result down = WellnessSummary.summarize(List.of(ci(0, 3, 2), ci(1, 3, 2), ci(8, 3, 5), ci(9, 3, 5)), 7, T);
        check("stress down is celebrated", down.message().contains("lighter"));
        WellnessSummary.Result moodDown = WellnessSummary.summarize(List.of(ci(0, 2, 3), ci(1, 2, 3), ci(8, 4, 3), ci(9, 4, 3)), 7, T);
        check("mood down is described kindly", moodDown.message().contains("heavier"));
        WellnessSummary.Result same = WellnessSummary.summarize(List.of(ci(0, 3, 3), ci(1, 3, 3), ci(8, 3, 3), ci(9, 3, 3)), 7, T);
        check("no change is called steady", same.message().contains("steady"));
        check("small change (under 0.3) counts as the same", WellnessSummary.stressChange(3.2, 3.0).equals("about the same"));
        check("bigger change is named", WellnessSummary.stressChange(3.5, 3.0).equals("a little higher") && WellnessSummary.moodChange(2.5, 3.0).equals("a little lower"));

        // ----- the wording is never guilt-inducing, for every kind of data -----
        List<List<CheckIn>> shapes = List.of(List.of(), List.of(ci(0, 1, 5)), up.checkInDays() > 0 ? List.of(ci(0, 3, 5), ci(1, 3, 5), ci(8, 3, 2), ci(9, 3, 2)) : List.of(),
                List.of(ci(0, 2, 3), ci(1, 2, 3), ci(8, 4, 3), ci(9, 4, 3)), List.of(ci(0, 3, 2), ci(1, 3, 2), ci(8, 3, 5), ci(9, 3, 5)),
                List.of(ci(0, 3, 3), ci(1, 3, 3), ci(8, 3, 3), ci(9, 3, 3)), List.of(ci(0, 1, 5), ci(1, 1, 5), ci(8, 5, 1), ci(9, 5, 1)));
        for (int days : new int[]{7, 30})
            for (List<CheckIn> shape : shapes)
                check("gentle wording (" + days + " days, " + shape.size() + " check-ins)", gentle(WellnessSummary.summarize(shape, days, T).message()));
        check("the other student-facing reminder is gentle", gentle(Notifications.all(new Student("x", "X Y", "x@y.z", "pw", "BSIT 1st Year"),
                DataStore.getInstance(), T).stream().filter(n -> n.kind() == Notifications.Kind.CHECKIN).map(Notifications.Notice::text).findFirst().orElse("")));

        System.out.println(failures == 0 ? "ALL " + checks + " WELLNESS CHECKS PASSED" : failures + " of " + checks + " CHECKS FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
