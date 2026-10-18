import java.time.LocalDate;
import java.util.*;

/**
 * Checks the grounding activities: content, which ones are matched to a check-in or assessment, and when the
 * Guidance Office prompt is shown (docs/ASSESSMENT_METHODOLOGY.md, section 7A). No test framework needed.
 *   java -cp out GroundingTest
 */
public class GroundingTest {
    private static int failures = 0, checks = 0;

    static void check(String name, boolean ok) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL  " + name); }
    }

    static List<String> ids(List<GroundingActivity.Pick> p) { return p.stream().map(x -> x.activity().id()).toList(); }
    static CheckIn ci(int daysAgo, int mood, int stress) { return new CheckIn("u1", LocalDate.now().minusDays(daysAgo), mood, stress); }
    static AssessmentResult res(int a, int s, int p, int o) {
        return new AssessmentResult("u1", LocalDate.now(), a, s, p, o, AssessmentService.VERSION);
    }

    public static void main(String[] args) {
        // ----- content -----
        check("four activities", GroundingActivity.ALL.size() == 4);
        check("the four named techniques", GroundingActivity.ALL.stream().map(GroundingActivity::title).toList().equals(
                List.of("Breathing Exercise", "Countdown of Colors", "Progressive Muscle Relaxation", "Five-Senses Exercise")));
        check("unique ids", GroundingActivity.ALL.stream().map(GroundingActivity::id).distinct().count() == 4);
        for (GroundingActivity a : GroundingActivity.ALL) {
            check(a.title() + ": at least 5 steps", a.steps().size() >= 5);
            check(a.title() + ": no blank step", a.steps().stream().noneMatch(String::isBlank));
            check(a.title() + ": has a duration", a.minutes() > 0);
        }
        check("breathing matches the 4-4-6 pattern used elsewhere",
                GroundingActivity.byId("breathing").steps().stream().anyMatch(s -> s.contains("4 counts"))
                        && GroundingActivity.byId("breathing").steps().stream().anyMatch(s -> s.contains("6 counts")));
        check("muscle relaxation warns against pain",
                GroundingActivity.byId("muscles").steps().get(0).contains("never to the point of pain"));
        check("five senses covers sight, hearing, touch, smell, taste",
                List.of("See:", "Hear:", "Feel:", "Smell:", "Taste:").stream()
                        .allMatch(k -> GroundingActivity.byId("senses").steps().stream().anyMatch(s -> s.startsWith(k))));
        String all = GroundingActivity.ALL.stream().flatMap(a -> a.steps().stream()).reduce("", String::concat).toLowerCase();
        check("no diagnostic wording in the steps", !all.contains("anxiety disorder") && !all.contains("depress") && !all.contains("diagnos"));

        // ----- matching -----
        check("high stress -> breathing, senses", ids(GroundingActivity.suggest(ci(0, 4, 4), null)).equals(List.of("breathing", "senses")));
        check("very high stress + low mood -> 3 picks", ids(GroundingActivity.suggest(ci(0, 2, 5), null)).equals(List.of("breathing", "senses", "colors")));
        check("low mood only -> colors", ids(GroundingActivity.suggest(ci(0, 1, 2), null)).equals(List.of("colors")));
        check("moderate stress -> breathing", ids(GroundingActivity.suggest(ci(0, 4, 3), null)).equals(List.of("breathing")));
        check("calm check-in -> nothing claimed", GroundingActivity.suggest(ci(0, 4, 2), null).isEmpty());
        check("no data -> nothing claimed", GroundingActivity.suggest(null, null).isEmpty());
        check("low assessment adds nothing", GroundingActivity.suggest(null, res(10, 10, 10, 10)).isEmpty());
        check("distress assessment -> muscle relaxation", ids(GroundingActivity.suggest(null, res(90, 10, 10, 37))).equals(List.of("muscles")));
        check("never more than 3", GroundingActivity.suggest(ci(0, 1, 5), res(90, 90, 90, 90)).size() == 3);
        check("every pick has a reason", GroundingActivity.suggest(ci(0, 1, 5), res(90, 90, 90, 90)).stream().allMatch(p -> !p.reason().isBlank()));

        // ----- recency -----
        check("check-in from 3 days ago is used", GroundingActivity.latestCheckIn(List.of(ci(3, 3, 3))) != null);
        check("check-in from 8 days ago is ignored", GroundingActivity.latestCheckIn(List.of(ci(8, 3, 3))) == null);
        check("newest check-in wins", GroundingActivity.latestCheckIn(List.of(ci(3, 1, 1), ci(1, 5, 5), ci(2, 2, 2))).stress() == 5);
        check("no check-ins -> null", GroundingActivity.latestCheckIn(List.of()) == null);

        // ----- Guidance Office prompt -----
        check("higher domain -> prompt", GroundingActivity.suggestGuidance(List.of(), res(90, 10, 10, 37)));
        check("higher overall -> prompt", GroundingActivity.suggestGuidance(List.of(), res(100, 100, 100, 100)));
        check("low assessment, no check-ins -> no prompt", !GroundingActivity.suggestGuidance(List.of(), res(10, 10, 10, 10)));
        check("no data at all -> no prompt", !GroundingActivity.suggestGuidance(List.of(), null));
        check("3 hard days in a row -> prompt", GroundingActivity.suggestGuidance(List.of(ci(0, 3, 4), ci(1, 3, 5), ci(2, 2, 2)), null));
        check("2 hard days is not enough", !GroundingActivity.suggestGuidance(List.of(ci(0, 3, 4), ci(1, 3, 5)), null));
        check("a good day breaks the run", !GroundingActivity.suggestGuidance(List.of(ci(0, 3, 4), ci(1, 5, 1), ci(2, 3, 5)), null));
        check("old hard days are ignored", !GroundingActivity.suggestGuidance(List.of(ci(8, 1, 5), ci(9, 1, 5), ci(10, 1, 5)), null));
        check("demo student (stress 4,4,3 lately) gets no prompt",
                !GroundingActivity.suggestGuidance(List.of(ci(2, 3, 4), ci(1, 4, 4), ci(0, 4, 3)), null));

        System.out.println(failures == 0 ? "ALL " + checks + " CHECKS PASSED" : failures + " OF " + checks + " CHECKS FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
