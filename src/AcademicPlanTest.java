import java.time.LocalDate;
import java.util.*;

/**
 * Checks the academic calendar suggestions and the exam-time stress comparison (docs/ACADEMIC_CALENDAR.md).
 * No test framework needed.   java -cp out AcademicPlanTest
 */
public class AcademicPlanTest {
    private static int failures = 0, checks = 0;

    static void check(String name, boolean ok) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL  " + name); }
    }

    static final LocalDate T = LocalDate.of(2026, 10, 17);
    static AcademicEvent ev(String type, int fromDay, int toDay) {
        return new AcademicEvent(T.plusDays(fromDay), T.plusDays(toDay), type, type + " " + fromDay);
    }
    static AcademicPlan.Phase phase(AcademicEvent... e) {
        return AcademicPlan.focus(List.of(e), T).map(AcademicPlan.Focus::phase).orElse(null);
    }
    static CheckIn ci(int dayOffset, int stress) { return new CheckIn("u1", T.plusDays(dayOffset), 3, stress); }

    public static void main(String[] args) {
        // ----- which phase -----
        check("exam today is DURING", phase(ev(AcademicEvent.MIDTERMS, -1, 3)) == AcademicPlan.Phase.DURING);
        check("exam in 7 days is PREPARE", phase(ev(AcademicEvent.FINALS, 7, 10)) == AcademicPlan.Phase.PREPARE);
        check("exam in 8 days is PLAN", phase(ev(AcademicEvent.FINALS, 8, 10)) == AcademicPlan.Phase.PLAN);
        check("exam in 21 days is PLAN", phase(ev(AcademicEvent.FINALS, 21, 23)) == AcademicPlan.Phase.PLAN);
        check("exam in 22 days gives nothing", phase(ev(AcademicEvent.FINALS, 22, 24)) == null);
        check("exam that ended 2 days ago is RECOVER", phase(ev(AcademicEvent.PRELIMS, -6, -2)) == AcademicPlan.Phase.RECOVER);
        check("exam that ended 4 days ago gives nothing", phase(ev(AcademicEvent.PRELIMS, -9, -4)) == null);
        check("exam soon beats recovery", phase(ev(AcademicEvent.PRELIMS, -6, -2), ev(AcademicEvent.MIDTERMS, 3, 6)) == AcademicPlan.Phase.PREPARE);
        check("recovery beats a far-off exam", phase(ev(AcademicEvent.PRELIMS, -6, -2), ev(AcademicEvent.FINALS, 15, 17)) == AcademicPlan.Phase.RECOVER);
        check("deadline within a week is PREPARE", phase(ev(AcademicEvent.OTHER, 5, 5)) == AcademicPlan.Phase.PREPARE);
        check("deadline in 10 days gives nothing", phase(ev(AcademicEvent.OTHER, 10, 10)) == null);
        check("holiday today is BREAK", phase(ev(AcademicEvent.HOLIDAY, 0, 0)) == AcademicPlan.Phase.BREAK);
        check("holiday in 5 days is BREAK", phase(ev(AcademicEvent.HOLIDAY, 5, 5)) == AcademicPlan.Phase.BREAK);
        check("holiday in 12 days gives nothing", phase(ev(AcademicEvent.HOLIDAY, 12, 12)) == null);
        check("exam beats holiday", phase(ev(AcademicEvent.HOLIDAY, 1, 1), ev(AcademicEvent.FINALS, 4, 6)) == AcademicPlan.Phase.PREPARE);
        check("empty calendar gives nothing", AcademicPlan.focus(List.of(), T).isEmpty());
        check("past events only give nothing", phase(ev(AcademicEvent.FINALS, -60, -55)) == null);

        // ----- suggestions are complete for every phase -----
        for (AcademicPlan.Phase ph : AcademicPlan.Phase.values()) {
            AcademicPlan.Focus f = new AcademicPlan.Focus(ev(AcademicEvent.FINALS, 3, 5), ph, 3);
            check(ph + ": has 3 tips", AcademicPlan.tips(f).size() == 3);
            check(ph + ": has an activity", !AcademicPlan.activityIds(f).isEmpty());
            for (String id : AcademicPlan.activityIds(f))
                check(ph + ": activity " + id + " exists", GroundingActivity.ALL.stream().anyMatch(a -> a.id().equals(id)));
            check(ph + ": has resource categories", !AcademicPlan.resourceCategories(f).isEmpty());
        }
        check("booking offered 3 days ahead", AcademicPlan.canStillBook(new AcademicPlan.Focus(ev(AcademicEvent.FINALS, 3, 5), AcademicPlan.Phase.PREPARE, 3)));
        check("booking not offered tomorrow (24h notice)", !AcademicPlan.canStillBook(new AcademicPlan.Focus(ev(AcademicEvent.FINALS, 1, 3), AcademicPlan.Phase.PREPARE, 1)));
        check("booking not offered during exams", !AcademicPlan.canStillBook(new AcademicPlan.Focus(ev(AcademicEvent.FINALS, -1, 3), AcademicPlan.Phase.DURING, 0)));

        // ----- exam time window -----
        List<AcademicEvent> exam = List.of(ev(AcademicEvent.MIDTERMS, 10, 14));
        check("7 days before the exam is exam time", AcademicPlan.inExamTime(T.plusDays(3), exam));
        check("8 days before the exam is not", !AcademicPlan.inExamTime(T.plusDays(2), exam));
        check("last exam day is exam time", AcademicPlan.inExamTime(T.plusDays(14), exam));
        check("day after the exam is not", !AcademicPlan.inExamTime(T.plusDays(15), exam));
        check("holidays are not exam time", !AcademicPlan.inExamTime(T, List.of(ev(AcademicEvent.HOLIDAY, 0, 0))));

        // ----- comparison -----
        List<AcademicEvent> past = List.of(ev(AcademicEvent.PRELIMS, -20, -16));   // exam time: day -27 to -16
        List<CheckIn> data = new ArrayList<>();
        for (int d : new int[]{-26, -24, -20, -17}) data.add(ci(d, 4));            // exam time
        for (int d : new int[]{-8, -6, -4, -2}) data.add(ci(d, 2));                // other days
        Optional<AcademicPlan.Comparison> c = AcademicPlan.compare(data, past, T);
        check("comparison appears with enough days", c.isPresent());
        check("exam average", c.isPresent() && Math.abs(c.get().examStress() - 4.0) < 1e-9 && c.get().examDays() == 4);
        check("other average", c.isPresent() && Math.abs(c.get().otherStress() - 2.0) < 1e-9 && c.get().otherDays() == 4);
        check("direction higher", c.isPresent() && c.get().direction().equals("higher"));
        check("too few exam days: no comparison", AcademicPlan.compare(data.subList(2, 8), past, T).isEmpty());
        check("too few other days: no comparison", AcademicPlan.compare(data.subList(0, 6), past, T).isEmpty());
        check("no check-ins: no comparison", AcademicPlan.compare(List.of(), past, T).isEmpty());
        check("no exams: no comparison", AcademicPlan.compare(data, List.of(), T).isEmpty());
        List<CheckIn> flat = new ArrayList<>();
        for (int d : new int[]{-26, -24, -20}) flat.add(ci(d, 3));
        for (int d : new int[]{-8, -6, -4}) flat.add(ci(d, 3));
        check("same stress reads as about the same", AcademicPlan.compare(flat, past, T).get().direction().equals("about the same"));
        List<CheckIn> lower = new ArrayList<>();
        for (int d : new int[]{-26, -24, -20}) lower.add(ci(d, 2));
        for (int d : new int[]{-8, -6, -4}) lower.add(ci(d, 4));
        check("lower direction", AcademicPlan.compare(lower, past, T).get().direction().equals("lower"));
        List<CheckIn> future = new ArrayList<>(data);
        future.add(ci(5, 5));
        check("future check-ins are ignored", AcademicPlan.compare(future, past, T).get().otherDays() == 4);

        // ----- per exam -----
        List<AcademicPlan.PerEvent> per = AcademicPlan.perEvent(data, past, T);
        check("one exam period listed", per.size() == 1 && per.get(0).days() == 4 && Math.abs(per.get(0).stress() - 4.0) < 1e-9);
        check("upcoming exams are not listed", AcademicPlan.perEvent(data, List.of(ev(AcademicEvent.FINALS, 5, 8)), T).isEmpty());
        check("needs 2 check-ins", AcademicPlan.perEvent(List.of(ci(-20, 4)), past, T).isEmpty());

        // ----- reflection prompts -----
        check("prompts without data", AcademicPlan.prompts(Optional.empty()).size() == 3);
        check("prompts do not state a cause", AcademicPlan.prompts(c).stream().noneMatch(p -> p.toLowerCase().contains("because of exams")));

        System.out.println(failures == 0 ? "ALL " + checks + " CHECKS PASSED" : failures + " of " + checks + " CHECKS FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
