import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Checks the assessment scoring, result bands, recommendations and saving. No test framework needed.
 *   java AssessmentTest            runs all checks (writes a test result to ./data/assessments.tsv)
 *   java AssessmentTest reload     second run: confirms the saved result is loaded again
 * Run it from an empty scratch folder so it does not touch real data.
 */
public class AssessmentTest {
    private static int failures = 0, checks = 0;

    static void check(String name, boolean ok) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL  " + name); }
    }

    static int[] fill(List<Question> qs, boolean worst) {
        int[] a = new int[qs.size()];
        for (int i = 0; i < a.length; i++) a[i] = (worst != qs.get(i).reverse()) ? 3 : 0;   // worst: 3, but 0 on reverse items
        return a;
    }

    static int[] all(List<Question> qs, int v) { int[] a = new int[qs.size()]; Arrays.fill(a, v); return a; }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("reload")) { reload(); return; }
        List<Question> qs = AssessmentService.QUESTIONS;

        // ----- question bank -----
        check("18 questions", qs.size() == 18);
        for (String d : AssessmentService.DOMAINS)
            check("6 items in " + d, qs.stream().filter(q -> q.category().equals(d)).count() == 6);
        check("3 reverse-scored items, one per domain",
                qs.stream().filter(Question::reverse).map(Question::category).distinct().count() == 3
                        && qs.stream().filter(Question::reverse).count() == 3);
        check("no duplicate statements", qs.stream().map(Question::text).distinct().count() == 18);
        check("every item has a Filipino version", qs.stream().allMatch(q -> !q.textTl().isBlank() && !q.textTl().equals(q.text())));
        check("same number of answer options in both languages",
                AssessmentService.OPTIONS.length == 4 && AssessmentService.OPTIONS_TL.length == 4);
        // Original wording: none of the statements should be the PHQ/DASS/PSS-style stock phrasing.
        String[] banned = {"felt down", "little interest", "nervous and stressed", "felt hopeless", "life is not worth"};
        for (Question q : qs) for (String b : banned) check("no stock phrase '" + b + "'", !q.text().toLowerCase().contains(b));

        // ----- scoring -----
        LocalDate d = LocalDate.of(2026, 10, 15);
        AssessmentResult worst = AssessmentService.score("t", d, qs, fill(qs, true));
        AssessmentResult best = AssessmentService.score("t", d, qs, fill(qs, false));
        check("worst case = 100 everywhere", worst.academic() == 100 && worst.social() == 100
                && worst.personal() == 100 && worst.overall() == 100);
        check("best case = 0 everywhere", best.academic() == 0 && best.social() == 0
                && best.personal() == 0 && best.overall() == 0);
        AssessmentResult allNever = AssessmentService.score("t", d, qs, all(qs, 0));
        check("answering Never to everything is low, because positive items flip",
                allNever.academic() == 17 && AssessmentService.classify(allNever.overall()).rank() == 0);
        AssessmentResult allSometimes = AssessmentService.score("t", d, qs, all(qs, 1));
        check("all Sometimes: 5x1 + 1x2 = 7 of 18 = 39%", allSometimes.academic() == 39 && allSometimes.overall() == 39);
        AssessmentResult allAlmost = AssessmentService.score("t", d, qs, all(qs, 3));
        check("all Almost always: 5x3 + 1x0 = 15 of 18 = 83%", allAlmost.social() == 83 && allAlmost.overall() == 83);
        check("percent(): 6/18 = 33, 12/18 = 67, 5/18 = 28", AssessmentService.percent(6, 6) == 33
                && AssessmentService.percent(12, 6) == 67 && AssessmentService.percent(5, 6) == 28);

        // Overall is the plain average of the unrounded domain scores: raise only Academic to the maximum.
        int[] a = fill(qs, false);
        for (int i = 0; i < qs.size(); i++) if (qs.get(i).category().equals("Academic")) a[i] = qs.get(i).reverse() ? 0 : 3;
        AssessmentResult onlyAcademic = AssessmentService.score("t", d, qs, a);
        check("only Academic maxed: A=100, S=0, P=0, overall=33",
                onlyAcademic.academic() == 100 && onlyAcademic.social() == 0 && onlyAcademic.overall() == 33);
        check("results store the formula version", worst.version().equals(AssessmentService.VERSION));
        boolean threw = false;
        try { AssessmentService.score("t", d, qs, new int[3]); } catch (IllegalArgumentException e) { threw = true; }
        check("wrong number of answers is rejected", threw);
        threw = false;
        int[] bad = all(qs, 0); bad[0] = 4;
        try { AssessmentService.score("t", d, qs, bad); } catch (IllegalArgumentException e) { threw = true; }
        check("out-of-range answer is rejected", threw);

        // ----- bands (defaults 33 / 66) -----
        check("default cut points", AssessmentService.moderateCut() == 33 && AssessmentService.higherCut() == 66);
        check("32 is low", AssessmentService.classify(32).rank() == 0);
        check("33 is moderate", AssessmentService.classify(33).rank() == 1);
        check("65 is moderate", AssessmentService.classify(65).rank() == 1);
        check("66 is higher", AssessmentService.classify(66).rank() == 2);
        check("labels", AssessmentService.classify(0).label().equals("Low concern")
                && AssessmentService.classify(50).label().equals("Moderate concern")
                && AssessmentService.classify(90).label().equals("Higher concern"));
        for (int p = 0; p <= 100; p++) {
            ConcernLevel lv = AssessmentService.classify(p);
            String t = (lv.label() + lv.message()).toLowerCase();
            check("no clinical wording at " + p, !t.contains("depress") && !t.contains("disorder") && !t.contains("diagnos"));
        }

        // ----- thresholds from a properties file -----
        Path tmp = Files.createTempFile("thr", ".properties");
        Files.writeString(tmp, "moderate=40\nhigher=70\n");
        AssessmentService.loadThresholds(tmp);
        check("custom cut points load", AssessmentService.moderateCut() == 40 && AssessmentService.higherCut() == 70);
        check("custom cut points apply", AssessmentService.classify(39).rank() == 0 && AssessmentService.classify(70).rank() == 2);
        Files.writeString(tmp, "moderate=80\nhigher=20\n");
        AssessmentService.loadThresholds(tmp);
        check("invalid cut points fall back to defaults", AssessmentService.moderateCut() == 33 && AssessmentService.higherCut() == 66);
        Files.writeString(tmp, "moderate=abc\n");
        AssessmentService.loadThresholds(tmp);
        check("unreadable cut points fall back to defaults", AssessmentService.moderateCut() == 33);
        AssessmentService.loadThresholds(Paths.get("does-not-exist.properties"));
        check("missing file uses defaults", AssessmentService.higherCut() == 66);
        Files.deleteIfExists(tmp);

        // ----- change over time -----
        AssessmentResult r1 = new AssessmentResult("t", d, 50, 30, 30, 37, "v1.0");
        AssessmentResult r2 = new AssessmentResult("t", d.plusDays(14), 70, 30, 30, 43, "v1.0");
        AssessmentResult r3 = new AssessmentResult("t", d.plusDays(28), 30, 30, 30, 30, "v1.0");
        check("small change is 'about the same'", AssessmentService.changeText(38, 33).equals("About the same"));
        check("bigger rise", AssessmentService.changeText(50, 33).equals("Higher by 17 points"));
        check("bigger drop", AssessmentService.changeText(20, 33).equals("Lower by 13 points"));
        check("first assessment sentence", AssessmentService.trendSentence(r1, null).contains("first assessment"));
        check("about-the-same sentence", AssessmentService.trendSentence(r2, r1).contains("about the same"));
        check("drop sentence is encouraging", AssessmentService.trendSentence(r3, r2).contains("helping"));
        check("previousOf", AssessmentService.previousOf(List.of(r1, r2, r3), r3) == r2
                && AssessmentService.previousOf(List.of(r1, r2, r3), r1) == null);

        // ----- recommendations -----
        List<AssessmentService.Recommendation> low = AssessmentService.recommend(best);
        check("low everywhere: keep-it-up suggestions, no consultation push",
                low.get(0).title().equals("Keep doing what works")
                        && low.stream().noneMatch(r -> "Book Consultation".equals(r.target())));
        List<AssessmentService.Recommendation> acad = AssessmentService.recommend(
                new AssessmentResult("t", d, 50, 10, 10, 23, "v1.0"));
        check("moderate academic gets academic help", acad.stream().anyMatch(r -> r.title().startsWith("Break big tasks")));
        check("moderate academic gets no social or personal activities",
                acad.stream().noneMatch(r -> r.title().equals("Join a peer community") || r.title().startsWith("Write it down")));
        check("moderate academic alone does not push a consultation first", !acad.get(0).title().startsWith("Book a Guidance"));
        List<AssessmentService.Recommendation> oneHigh = AssessmentService.recommend(
                new AssessmentResult("t", d, 10, 10, 80, 33, "v1.0"));
        check("one higher domain puts the Guidance Office first, even if overall is only moderate",
                oneHigh.get(0).title().startsWith("Book a Guidance") && "Book Consultation".equals(oneHigh.get(0).target()));
        check("higher personal mentions Guidance Office help", oneHigh.stream().anyMatch(r -> r.title().startsWith("Ask the Guidance")));
        check("recommendations capped at 6 and unique",
                AssessmentService.recommend(worst).size() <= 6
                        && AssessmentService.recommend(worst).stream().map(r -> r.title()).distinct().count()
                        == AssessmentService.recommend(worst).size());
        Set<String> pages = new HashSet<>(Arrays.asList(new Student("x", "x", "x", "x", "x").getMenu()));
        pages.add("Book Consultation");
        for (AssessmentResult r : List.of(best, worst, allSometimes, onlyAcademic, r1, r2))
            for (AssessmentService.Recommendation rec : AssessmentService.recommend(r))
                check("recommendation target exists: " + rec.target(), rec.target() == null || pages.contains(rec.target()));
        check("every result shows a crisis line", AssessmentService.CRISIS_NOTE.contains("1553") && AssessmentService.CRISIS_NOTE.contains("911"));
        check("scoring explanation names the cut points", AssessmentService.howScoringWorks().contains("33%")
                && AssessmentService.howScoringWorks().contains("66%"));

        // ----- stress type and main sources -----
        for (String s : StressProfile.SOURCES) {
            int expected = switch (s) { case "Academic", "Personal or family" -> 4; case "Financial" -> 1; case "Social" -> 6; default -> 3; };
            check("source " + s + " has " + expected + " items", qs.stream().filter(q -> q.source().equals(s)).count() == expected);
        }
        check("every item has a known source", qs.stream().allMatch(q -> StressProfile.SOURCES.contains(q.source())));

        AssessmentResult worstR = AssessmentService.score("t", LocalDate.now(), qs, fill(qs, true));
        AssessmentResult bestR = AssessmentService.score("t", LocalDate.now(), qs, fill(qs, false));
        AssessmentResult someR = AssessmentService.score("t", LocalDate.now(), qs, all(qs, 1));
        check("worst case is distress", worstR.profile().type() == StressProfile.Type.DISTRESS);
        check("best case is low", bestR.profile().type() == StressProfile.Type.LOW);
        check("all Sometimes (39%) is manageable stress (eustress)", someR.profile().type() == StressProfile.Type.EUSTRESS);
        check("low result: no main source and a plain summary", bestR.profile().main().isEmpty()
                && bestR.profile().summary(bestR).equals("Low reported stress \u2014 no single source stands out."));
        check("worst case is mixed: three or more main sources", worstR.profile().mixed()
                && worstR.profile().categories().equals(List.of(StressProfile.OTHER))
                && worstR.profile().summary(worstR).contains("mixed concerns"));

        int[] fin = fill(qs, false);
        int finIdx = -1;
        for (int i = 0; i < qs.size(); i++) if (qs.get(i).source().equals(StressProfile.FINANCIAL)) finIdx = i;
        fin[finIdx] = 3;
        AssessmentResult finR = AssessmentService.score("t", LocalDate.now(), qs, fin);
        check("one 'Almost always' on the money item reports Financial", finR.profile().main().equals(List.of("Financial")));
        fin[finIdx] = 1;
        check("one 'Sometimes' on the single money item does not report Financial",
                AssessmentService.score("t", LocalDate.now(), qs, fin).profile().main().isEmpty());

        int[] acadOnly = fill(qs, false);
        for (int i = 0; i < qs.size(); i++) if (qs.get(i).category().equals("Academic")) acadOnly[i] = qs.get(i).reverse() ? 0 : 3;
        AssessmentResult acadR = AssessmentService.score("t", LocalDate.now(), qs, acadOnly);
        check("academic maxed: Academic is the main source", acadR.profile().main().equals(List.of("Academic")));
        check("academic maxed: its time-management items make Time a secondary source",
                acadR.profile().secondary().equals(List.of("Time management")));
        check("summary names main then secondary", acadR.profile().summary(acadR)
                .equals("Moderate reported stress \u2014 primarily academic, with some time management concerns."));

        StressProfile example = StressProfile.fromScores(StressProfile.Type.EUSTRESS,
                new LinkedHashMap<>(Map.of("Academic", 89, "Financial", 66, "Social", 10)));
        check("example sentence from the specification",
                example.summary(someR).equals("Moderate reported stress \u2014 primarily academic, with some financial concerns."));
        check("several categories can be reported at once", example.categories().equals(List.of("Academic", "Financial")));
        StressProfile back = StressProfile.decode(worstR.profile().type().name(), worstR.profile().encodeScores());
        check("profile survives save and load", back != null && back.equals(worstR.profile()));
        check("damaged saved profile loads as null", StressProfile.decode("NOPE", "x") == null
                && StressProfile.decode("LOW", "Academic=abc") == null);
        check("distress adds a relaxation suggestion (one higher domain is enough)", AssessmentService.recommend(acadR).stream()
                .anyMatch(r -> r.title().startsWith("Try a short relaxation")));
        check("distress still puts the Guidance Office first", AssessmentService.recommend(worstR).get(0).title().startsWith("Book a Guidance"));
        check("financial source adds budgeting help", AssessmentService.recommend(finR).stream()
                .anyMatch(r -> r.title().startsWith("Look into budgeting")));
        check("time source adds time-block planning", AssessmentService.recommend(acadR).stream()
                .anyMatch(r -> r.title().startsWith("Plan your week")));
        check("results without a profile still get recommendations", !AssessmentService.recommend(
                new AssessmentResult("t", LocalDate.now(), 20, 20, 20, 20, "v1.0")).isEmpty());
        check("recommendations stay capped at 6", AssessmentService.recommend(worstR).size() <= 6);

        // ----- eustress and distress appraisal -----
        List<Question> ap = AssessmentService.APPRAISAL;
        check("5 appraisal items: 2 motivating, 1 manageable, 1 overwhelming, 1 recovery",
                ap.size() == 5 && ap.stream().filter(q -> q.category().equals(AssessmentService.MOTIVATING)).count() == 2);
        check("every appraisal item has a Filipino version", ap.stream().allMatch(q -> !q.textTl().isBlank() && !q.textTl().equals(q.text())));
        check("allQuestions = 18 statements then the appraisal", AssessmentService.allQuestions().size() == 23
                && AssessmentService.allQuestions().get(18) == ap.get(0));
        // appraisal order: motivating, motivating, manageable, overwhelming, recovery
        int[] calm = {3, 3, 3, 0, 0};          // motivated, manageable, not overwhelmed, recovers easily
        int[] swamped = {0, 0, 0, 3, 3};       // not motivated, not manageable, overwhelmed, hard to recover
        int[] mid = {1, 1, 2, 1, 1};
        int[] heavyAnswers = fill(qs, true), lowAnswers = fill(qs, false);
        AssessmentResult heavyCalm = AssessmentService.score("t", d, qs, heavyAnswers, calm);
        AssessmentResult heavySwamped = AssessmentService.score("t", d, qs, heavyAnswers, swamped);
        AssessmentResult lowSwamped = AssessmentService.score("t", d, qs, lowAnswers, swamped);
        AssessmentResult lowCalm = AssessmentService.score("t", d, qs, lowAnswers, calm);
        check("appraisal never changes the domain or overall scores",
                heavyCalm.academic() == worst.academic() && heavyCalm.overall() == worst.overall()
                        && lowSwamped.overall() == best.overall() && lowSwamped.personal() == best.personal());
        check("null appraisal behaves like version 1.0", AssessmentService.score("t", d, qs, heavyAnswers, null).profile().type()
                == StressProfile.Type.DISTRESS && !AssessmentService.score("t", d, qs, heavyAnswers, null).profile().hasAppraisal());
        check("motivation and strain scores", heavyCalm.profile().motivation() == 100 && heavyCalm.profile().strain() == 0
                && heavySwamped.profile().motivation() == 0 && heavySwamped.profile().strain() == 100);
        check("heavy demands the student feels able to manage are eustress, not automatically distress",
                heavyCalm.profile().type() == StressProfile.Type.EUSTRESS);
        check("heavy demands that feel overwhelming are distress", heavySwamped.profile().type() == StressProfile.Type.DISTRESS);
        check("feeling overwhelmed is distress even when the scored demands are low", lowSwamped.profile().type() == StressProfile.Type.DISTRESS);
        check("low demands and a calm appraisal is low", lowCalm.profile().type() == StressProfile.Type.LOW);
        check("mid appraisal with moderate demands is eustress", AssessmentService.score("t", d, qs, all(qs, 1), mid).profile().type()
                == StressProfile.Type.EUSTRESS);
        check("higher band with moderate strain is distress", StressProfile.typeFrom(2, AssessmentService.moderateCut()) == StressProfile.Type.DISTRESS);
        check("higher band with low strain is eustress", StressProfile.typeFrom(2, AssessmentService.moderateCut() - 1) == StressProfile.Type.EUSTRESS);
        check("a higher band still puts the Guidance Office first, whatever the appraisal",
                AssessmentService.recommend(heavyCalm).get(0).title().startsWith("Book a Guidance"));
        check("eustress with demands suggests using the pressure well, and no relaxation push",
                AssessmentService.recommend(AssessmentService.score("t", d, qs, all(qs, 1), calm)).stream()
                        .anyMatch(r -> r.title().startsWith("Use manageable pressure"))
                        && AssessmentService.recommend(AssessmentService.score("t", d, qs, all(qs, 1), calm)).stream()
                        .noneMatch(r -> r.title().startsWith("Try a short relaxation")));
        check("motivated eustress message mentions the pressure helping", heavyCalm.profile().typeMessage().contains("helping you"));
        check("appraisal line is shown only with appraisal", heavyCalm.profile().appraisalLine().contains("Motivation 100%")
                && worstR.profile().appraisalLine() == null);
        for (StressProfile sp : List.of(heavyCalm.profile(), heavySwamped.profile(), lowCalm.profile())) {
            String t = (sp.typeLabel() + sp.typeMessage() + sp.appraisalLine()).toLowerCase();
            check("no clinical wording in type text", !t.contains("depress") && !t.contains("disorder") && !t.contains("diagnos"));
        }
        StressProfile ap2 = StressProfile.decode(heavyCalm.profile().type().name(), heavyCalm.profile().encodeScores(), heavyCalm.profile().encodeAppraisal());
        check("appraisal survives save and load", ap2 != null && ap2.equals(heavyCalm.profile()));
        check("damaged appraisal is ignored but the profile still loads",
                StressProfile.decode("EUSTRESS", "Academic=50", "Motivation=x").equals(StressProfile.decode("EUSTRESS", "Academic=50")));
        check("scoring explanation mentions eustress and distress", AssessmentService.howScoringWorks().contains("eustress")
                && AssessmentService.howScoringWorks().contains("distress"));
        boolean appThrew = false;
        try { AssessmentService.score("t", d, qs, heavyAnswers, new int[2]); } catch (IllegalArgumentException e) { appThrew = true; }
        check("wrong number of appraisal answers is rejected", appThrew);

        // ----- saving (needs ./data to be writable) -----
        DataStore store = DataStore.getInstance();
        store.seed();
        int before = store.resultsOf("u1").size();
        check("demo history is present", before >= 3);
        List<AssessmentResult> demo = store.resultsOf("u1");
        boolean sorted = true;
        for (int i = 1; i < demo.size(); i++) sorted &= !demo.get(i).date().isBefore(demo.get(i - 1).date());
        check("history is oldest first", sorted);
        store.addResult(new AssessmentResult("u2", LocalDate.of(2026, 10, 1), 61, 22, 44, 42, AssessmentService.VERSION));
        check("added result is returned", store.resultsOf("u2").size() == 1);
        check("other students' results stay separate", store.resultsOf("u1").size() == before);
        List<String> lines = Files.readAllLines(Paths.get("data", "assessments.tsv"));
        check("one line saved", lines.size() == 1 && lines.get(0).equals("u2\t2026-10-01\t61\t22\t44\t42\t" + AssessmentService.VERSION));
        check("demo data is not written to disk", lines.stream().noneMatch(l -> l.startsWith("u1")));
        AssessmentResult withP = AssessmentService.score("u3", LocalDate.of(2026, 10, 2), qs, fill(qs, true));
        store.addResult(withP);
        lines = Files.readAllLines(Paths.get("data", "assessments.tsv"));
        check("result with a profile saves two extra columns", lines.size() == 2 && lines.get(1).split("\t", -1).length == 9
                && lines.get(1).contains("DISTRESS"));
        AssessmentResult withA = AssessmentService.score("u4", LocalDate.of(2026, 10, 3), qs, fill(qs, true), calm);
        store.addResult(withA);
        lines = Files.readAllLines(Paths.get("data", "assessments.tsv"));
        check("result with appraisal saves a tenth column", lines.size() == 3 && lines.get(2).split("\t", -1).length == 10
                && lines.get(2).contains("Motivation=100;Strain=0") && lines.get(2).contains("EUSTRESS"));
        check("old 7-column line is unchanged by the new columns", lines.get(0).equals("u2\t2026-10-01\t61\t22\t44\t42\t" + AssessmentService.VERSION));

        System.out.println(failures == 0 ? "ALL " + checks + " CHECKS PASSED" : failures + " OF " + checks + " CHECKS FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static boolean r2noProfile(List<AssessmentResult> r) { return r.get(0).profile() == null; }

    private static void reload() {
        DataStore store = DataStore.getInstance();
        store.seed();
        List<AssessmentResult> r = store.resultsOf("u2");
        boolean ok = r.size() == 1 && r.get(0).academic() == 61 && r.get(0).social() == 22
                && r.get(0).personal() == 44 && r.get(0).overall() == 42 && r.get(0).date().equals(LocalDate.of(2026, 10, 1));
        List<AssessmentResult> r3 = store.resultsOf("u3");
        List<AssessmentResult> r4 = store.resultsOf("u4");
        ok &= r4.size() == 1 && r4.get(0).profile() != null && r4.get(0).profile().hasAppraisal()
                && r4.get(0).profile().type() == StressProfile.Type.EUSTRESS && r4.get(0).profile().strain() == 0;
        boolean p = r3.size() == 1 && r3.get(0).profile() != null && r3.get(0).profile().type() == StressProfile.Type.DISTRESS
                && r3.get(0).profile().motivation() == null && r2noProfile(r);
        ok &= p;
        System.out.println(ok ? "RELOAD OK: saved result was loaded again" : "RELOAD FAILED: " + r + " " + r3);
        System.exit(ok ? 0 : 1);
    }
}
