import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

/**
 * Wellness and Stress Assessment: question bank, scoring, result bands, explanations and recommendations.
 * The formula, thresholds and their rationale are documented in docs/ASSESSMENT_METHODOLOGY.md.
 * This is an informational self-reflection tool. It is not a diagnostic or clinical instrument.
 */
final class AssessmentService {
    /** Bump this whenever the items, formula or default thresholds change, so stored results stay interpretable. */
    static final String VERSION = "v1.1";
    static final String[] DOMAINS = {"Academic", "Social", "Personal"};
    static final int ITEMS_PER_DOMAIN = 6;
    static final int MAX_POINTS_PER_ITEM = 3;

    static final String[] OPTIONS = {"Never", "Sometimes", "Often", "Almost always"};
    /** Same four choices, same order, same score (0 to 3) as OPTIONS. */
    static final String[] OPTIONS_TL = {"Hindi kailanman", "Paminsan-minsan", "Madalas", "Halos palagi"};
    static String[] options(boolean filipino) { return filipino ? OPTIONS_TL : OPTIONS; }

    static final String CRISIS_NOTE = "If you ever feel unsafe or in crisis, call NCMH 1553 or emergency services 911, "
            + "or go to the Guidance Office right away.";
    static final String NOT_DIAGNOSIS = "This is a self-reflection tool, not a diagnosis or a clinical test.";

    // ----- thresholds (percent of the maximum possible score) -----
    private static final Path CONFIG = Paths.get("data", "assessment-thresholds.properties");
    static final int DEFAULT_MODERATE = 33;
    static final int DEFAULT_HIGHER = 66;
    /** A change smaller than this many points between two assessments is described as "about the same". */
    static final int SMALL_CHANGE = 10;
    private static int moderateCut = DEFAULT_MODERATE;
    private static int higherCut = DEFAULT_HIGHER;
    static { loadThresholds(CONFIG); }

    private AssessmentService() {}

    /**
     * Reads "moderate" and "higher" cut points from a properties file if one exists, so the Guidance Office can
     * adjust them after review without changing code. Invalid files are ignored and the defaults are used.
     */
    static void loadThresholds(Path file) {
        moderateCut = DEFAULT_MODERATE;
        higherCut = DEFAULT_HIGHER;
        if (file == null || !Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            Properties p = new Properties();
            p.load(in);
            int m = Integer.parseInt(p.getProperty("moderate", "" + DEFAULT_MODERATE).trim());
            int h = Integer.parseInt(p.getProperty("higher", "" + DEFAULT_HIGHER).trim());
            if (m > 0 && m < h && h < 100) { moderateCut = m; higherCut = h; }
            else System.err.println("Ignoring assessment thresholds: need 0 < moderate < higher < 100.");
        } catch (IOException | RuntimeException e) {
            System.err.println("Could not read assessment thresholds: " + e.getMessage());
        }
    }
    static int moderateCut() { return moderateCut; }
    static int higherCut() { return higherCut; }

    // ----- question bank (original wording; see the methodology document for the basis of each domain) -----
    /** Ordered so the three domains alternate and the positively worded (reverse-scored) items are spread out. */
    static final List<Question> QUESTIONS = List.of(
            q("I felt I had more schoolwork than I could realistically finish.",
              "Pakiramdam ko ay mas marami ang gawain ko sa pag-aaral kaysa sa kaya kong tapusin.", "Academic", StressProfile.ACADEMIC),
            q("I felt left out, or like I did not belong, in my classes or groups.",
              "Pakiramdam ko ay hindi ako kasama o hindi ako nabibilang sa aking mga klase o grupo.", "Social", StressProfile.SOCIAL),
            q("Worries about my family or home affected my focus on other things.",
              "Naapektuhan ang aking pokus sa ibang bagay ng mga alalahanin ko tungkol sa aking pamilya o tahanan.", "Personal", StressProfile.PERSONAL),
            q("Upcoming exams or deadlines made it hard for me to relax.",
              "Nahirapan akong magpahinga dahil sa mga paparating na pagsusulit o deadline.", "Academic", StressProfile.ACADEMIC),
            q("I felt pressure to act or come across in a certain way to fit in.",
              "Nakaramdam ako ng presyon na kumilos o lumitaw sa isang tiyak na paraan para makibagay.", "Social", StressProfile.SOCIAL),
            q("Money worries, such as tuition, allowance, or daily expenses, added to my stress.",
              "Nadagdagan ang aking stress dahil sa mga alalahanin sa pera, tulad ng tuition, baon, o pang-araw-araw na gastusin.", "Personal", StressProfile.FINANCIAL),
            r("I felt I had enough time and energy to do my schoolwork well.",
              "Pakiramdam ko ay sapat ang aking oras at lakas para magawa nang maayos ang aking mga gawain sa pag-aaral.", "Academic", StressProfile.TIME),
            q("Tension or disagreements with friends, classmates, or groupmates weighed on me.",
              "Nabigatan ako sa tensyon o hindi pagkakasundo namin ng mga kaibigan, kaklase, o ka-grupo.", "Social", StressProfile.SOCIAL),
            q("A recent change in my life, such as a move, a new routine, or a loss, was hard to handle.",
              "Mahirap harapin ang isang kamakailang pagbabago sa buhay ko, tulad ng paglipat, bagong gawain, o pagkawala.", "Personal", StressProfile.PERSONAL),
            q("I worried that my results would not meet what I, my family, or my teachers expect.",
              "Nag-alala ako na hindi maaabot ng aking mga resulta ang inaasahan ko, ng aking pamilya, o ng aking mga guro.", "Academic", StressProfile.ACADEMIC),
            r("I felt comfortable and accepted when I was with people at school.",
              "Pakiramdam ko ay komportable ako at tanggap kapag kasama ko ang mga tao sa paaralan.", "Social", StressProfile.SOCIAL),
            q("My responsibilities left me little time for rest or for things I enjoy.",
              "Dahil sa aking mga responsibilidad, kaunti na lang ang oras ko para magpahinga o gawin ang mga bagay na gusto ko.", "Personal", StressProfile.TIME),
            q("I put off schoolwork and then felt pressured to catch up.",
              "Ipinagpaliban ko ang mga gawain sa pag-aaral at pagkatapos ay nakaramdam ako ng presyon na humabol.", "Academic", StressProfile.TIME),
            q("I avoided social situations because they felt tiring or uncomfortable.",
              "Iniwasan ko ang mga sosyal na sitwasyon dahil nakakapagod o hindi komportable ang mga ito.", "Social", StressProfile.SOCIAL),
            r("I felt I could cope with the personal challenges that came up.",
              "Pakiramdam ko ay kaya kong harapin ang mga personal na hamong dumating.", "Personal", StressProfile.PERSONAL),
            q("I had trouble keeping up with the lessons in one or more subjects.",
              "Nahirapan akong sabayan ang mga aralin sa isa o higit pang asignatura.", "Academic", StressProfile.ACADEMIC),
            q("I felt I had no one to turn to when I needed to talk.",
              "Pakiramdam ko ay wala akong malalapitan kapag kailangan kong makipag-usap.", "Social", StressProfile.SOCIAL),
            q("I felt my personal problems were too much to handle on my own.",
              "Pakiramdam ko ay sobra na ang aking mga personal na problema para harapin nang mag-isa.", "Personal", StressProfile.PERSONAL));

    // ----- stress appraisal items (eustress and distress; see docs/ASSESSMENT_METHODOLOGY.md, section 6A) -----
    /** Appraisal themes. They describe how the student experienced the demands, not what the demands were. */
    static final String MOTIVATING = "Motivating";
    static final String MANAGEABLE = "Manageable";
    static final String OVERWHELMING = "Overwhelming";
    static final String RECOVERY = "Hard to recover";

    /**
     * Five short items asked after the 18 statements. They are NOT part of the Academic, Social and Personal scores or
     * of the overall score, so the domain formula is unchanged. They only decide the type of stress (see
     * {@link StressProfile}) and the wording of the result. Motivating and Manageable are positively worded, so they
     * are reverse-scored for "strain" and read directly for "motivation".
     */
    static final List<Question> APPRAISAL = List.of(
            new Question("The pressure I felt helped me get started or stay focused on what mattered.",
                    "Nakatulong ang presyon na naramdaman ko para magsimula o manatiling nakatuon sa mahahalagang bagay.", MOTIVATING, false, MOTIVATING),
            new Question("I saw some of my demands as a challenge I wanted to meet.",
                    "Nakita ko ang ilan sa aking mga pangangailangan bilang hamon na gusto kong harapin.", MOTIVATING, false, MOTIVATING),
            new Question("I felt I could manage my demands with the time, skills and support I had.",
                    "Pakiramdam ko ay kaya kong asikasuhin ang aking mga responsibilidad gamit ang oras, kakayahan, at suportang mayroon ako.", MANAGEABLE, false, MANAGEABLE),
            new Question("My demands felt overwhelming, as if there was too much to handle at once.",
                    "Pakiramdam ko ay sobra-sobra ang aking mga responsibilidad, na parang napakarami para sabay-sabay na harapin.", OVERWHELMING, false, OVERWHELMING),
            new Question("It was hard to recover or bounce back after stressful days.",
                    "Nahirapan akong makabawi o makabangon pagkatapos ng mga nakakapagod na araw.", RECOVERY, false, RECOVERY));

    /** The 18 statements followed by the appraisal items, in the order the student sees them. */
    static List<Question> allQuestions() {
        List<Question> all = new ArrayList<>(QUESTIONS);
        all.addAll(APPRAISAL);
        return all;
    }

    private static Question q(String en, String tl, String domain, String source) { return new Question(en, tl, domain, false, source); }
    private static Question r(String en, String tl, String domain, String source) { return new Question(en, tl, domain, true, source); }

    // ----- scoring -----
    /**
     * Scores one completed assessment.
     * Domain score  = (sum of the domain's item points) / (6 items x 3) x 100, rounded.
     * Overall score = (sum of all 18 item points) / (18 items x 3) x 100, rounded. Because every domain has the same
     * number of items, this equals the plain average of the three (unrounded) domain scores, so no domain counts more.
     * Positively worded items are reverse-scored first (see {@link Question#points}).
     *
     * @param answers raw answers 0 to 3, in the same order as {@code qs}
     */
    static AssessmentResult score(String userId, LocalDate date, List<Question> qs, int[] answers) {
        if (qs.size() != answers.length) throw new IllegalArgumentException("one answer per question is required");
        Map<String, Integer> sum = new HashMap<>();
        Map<String, Integer> count = new HashMap<>();
        int total = 0;
        for (int i = 0; i < qs.size(); i++) {
            if (answers[i] < 0 || answers[i] > MAX_POINTS_PER_ITEM) throw new IllegalArgumentException("answer out of range");
            int pts = qs.get(i).points(answers[i]);
            sum.merge(qs.get(i).category(), pts, Integer::sum);
            count.merge(qs.get(i).category(), 1, Integer::sum);
            total += pts;
        }
        int[] d = new int[DOMAINS.length];
        for (int i = 0; i < DOMAINS.length; i++)
            d[i] = percent(sum.getOrDefault(DOMAINS[i], 0), count.getOrDefault(DOMAINS[i], 1));
        AssessmentResult base = new AssessmentResult(userId, date, d[0], d[1], d[2], percent(total, qs.size()), VERSION);
        return base.withProfile(StressProfile.of(qs, answers, base));
    }

    /**
     * Scores the 18 statements plus the appraisal items. {@code appraisalAnswers} may be null (for example for older
     * callers); the stress type is then inferred from the result bands alone, as in version 1.0.
     */
    static AssessmentResult score(String userId, LocalDate date, List<Question> qs, int[] answers, int[] appraisalAnswers) {
        AssessmentResult base = score(userId, date, qs, answers);
        if (appraisalAnswers == null) return base;
        if (appraisalAnswers.length != APPRAISAL.size()) throw new IllegalArgumentException("one appraisal answer per item is required");
        for (int a : appraisalAnswers)
            if (a < 0 || a > MAX_POINTS_PER_ITEM) throw new IllegalArgumentException("answer out of range");
        return base.withProfile(StressProfile.withAppraisal(base.profile(), base, appraisalAnswers));
    }

    static int percent(int points, int itemCount) {
        return Math.round(points * 100f / (itemCount * MAX_POINTS_PER_ITEM));
    }

    // ----- result bands -----
    static ConcernLevel classify(int percent) {
        if (percent < moderateCut) return new ConcernLevel(0, "Low concern",
                "Your answers suggest that things feel mostly manageable right now. Keep up the habits that help you feel balanced.");
        if (percent < higherCut) return new ConcernLevel(1, "Moderate concern",
                "Some parts of student life seem to be weighing on you. Small steps and talking with someone you trust can help.");
        return new ConcernLevel(2, "Higher concern",
                "A lot seems to be weighing on you lately. You do not have to handle this alone. The Guidance Office is here to listen.");
    }

    /** One sentence about a single domain, written to be supportive rather than alarming. */
    static String domainMessage(String domain, ConcernLevel level) {
        return switch (domain) {
            case "Academic" -> switch (level.rank()) {
                case 0 -> "Schoolwork seems manageable at the moment.";
                case 1 -> "Workload, deadlines or expectations seem to be adding noticeable pressure.";
                default -> "Academic pressure seems to be weighing on you heavily. Support with workload and planning is available.";
            };
            case "Social" -> switch (level.rank()) {
                case 0 -> "You seem to feel reasonably connected and comfortable with the people around you.";
                case 1 -> "Some social situations or relationships seem to be taking energy from you.";
                default -> "Feeling disconnected or under social pressure seems to be a big part of your week. Talking to someone can help.";
            };
            default -> switch (level.rank()) {
                case 0 -> "Your personal circumstances seem to feel manageable right now.";
                case 1 -> "Things outside of school, such as family, money or changes in your life, seem to be adding to your stress.";
                default -> "Personal concerns seem to be weighing on you heavily. A confidential conversation can make them feel lighter.";
            };
        };
    }

    // ----- change over time -----
    /** The result just before {@code r} for the same student, or null if {@code r} is the first. */
    static AssessmentResult previousOf(List<AssessmentResult> chronological, AssessmentResult r) {
        int i = chronological.indexOf(r);
        return i > 0 ? chronological.get(i - 1) : null;
    }

    /** "Down 8 points", "Up 12 points" or "About the same", for one score. */
    static String changeText(int now, int before) {
        int diff = now - before;
        if (Math.abs(diff) < SMALL_CHANGE) return "About the same";
        return (diff < 0 ? "Lower by " : "Higher by ") + Math.abs(diff) + " points";
    }

    /** One supportive sentence comparing this result with the previous one. Lower stress is good news. */
    static String trendSentence(AssessmentResult now, AssessmentResult before) {
        if (before == null) return "This is your first assessment. Take it again in a few weeks to see how things change.";
        int diff = now.overall() - before.overall();
        if (Math.abs(diff) < SMALL_CHANGE)
            return "Your overall score is about the same as last time (" + before.overall() + " to " + now.overall() + ").";
        if (diff < 0) return "Your overall score is lower than last time (" + before.overall() + " to " + now.overall()
                + "). Whatever you have been doing seems to be helping.";
        return "Your overall score is higher than last time (" + before.overall() + " to " + now.overall()
                + "). Stress often rises during busy periods, and it is a good moment to use some support.";
    }

    // ----- recommendations -----
    /** {@code target} is a sidebar page key to open (for example "Book Consultation"), or null for tips with no page. */
    record Recommendation(String title, String detail, String target) {}

    /**
     * Suggests activities and support based on the domain bands. Rules (also listed in the methodology document):
     * any domain at "higher" or an overall "higher" result puts a Guidance Office consultation first; a "moderate"
     * domain gets self-help activities for that domain; all "low" gets a keep-it-up suggestion. At most 6 are returned.
     */
    static List<Recommendation> recommend(AssessmentResult r) {
        Map<String, Recommendation> out = new LinkedHashMap<>();
        boolean higher = classify(r.overall()).rank() == 2;
        for (String d : DOMAINS) higher |= classify(r.score(d)).rank() == 2;
        if (higher) {
            add(out, "Book a Guidance Office consultation",
                    "A counselor can listen, help you plan next steps, and point you to other support. It is confidential and free.",
                    "Book Consultation");
        }
        // Manageable stress is not treated as a problem: offer a way to use it well, ahead of the domain tips.
        if (r.profile() != null && r.profile().type() == StressProfile.Type.EUSTRESS && StressProfile.topBand(r) >= 1)
            add(out, "Use manageable pressure well",
                    "Some pressure can keep you moving. Set one small goal for the week and schedule rest and recovery after it.",
                    "Calendar");
        // Highest-scoring domains first so the most useful suggestions are not cut off.
        List<String> byScore = new ArrayList<>(Arrays.asList(DOMAINS));
        byScore.sort(Comparator.comparingInt((String d) -> r.score(d)).reversed());
        for (String d : byScore) {
            int rank = classify(r.score(d)).rank();
            if (rank == 0) continue;
            switch (d) {
                case "Academic" -> {
                    add(out, "Break big tasks into small steps",
                            "List every deliverable, split each into 25-minute steps, and start with the smallest.", "Possible Solutions");
                    add(out, "Peer Tutoring Program", "Get help with a hard subject from fellow students.", "Resources");
                    if (rank == 2) add(out, "Talk to your professor early",
                            "Ask about deadlines, extensions or where to focus. Most teachers prefer hearing early.", null);
                }
                case "Social" -> {
                    add(out, "Reach out in a small way",
                            "Message one classmate, or join a peer support community this week.", "Peer Support");
                    add(out, "Join a peer community", "Talk with students who understand student life, anonymously if you prefer.", "Peer Support");
                }
                default -> {
                    add(out, "Write it down in your journal",
                            "A few private lines about what is on your mind can make it feel less heavy.", "Calendar");
                    add(out, "Take a short breathing break", "Breathe in for 4 counts, hold for 4, out for 6.", "Possible Solutions");
                    if (rank == 2) add(out, "Ask the Guidance Office about other help",
                            "For family, money or life-change concerns, the Guidance Office can talk it through "
                                    + "and tell you what student support is available.", "Guidance Office");
                }
            }
        }
        // Suggestions for the main sources (Financial and Time management have no domain of their own).
        StressProfile p = r.profile();
        if (p != null) {
            for (String src : p.categories().equals(List.of(StressProfile.OTHER)) ? p.main() : p.categories()) {
                if (src.equals(StressProfile.FINANCIAL))
                    add(out, "Look into budgeting and financial help",
                            "Plan a simple weekly budget, and ask the Guidance Office about financial assistance or scholarships.",
                            "Guidance Office");
                if (src.equals(StressProfile.TIME))
                    add(out, "Plan your week in time blocks",
                            "Put classes, study, rest and the things you enjoy on your calendar so nothing gets squeezed out.",
                            "Calendar");
            }
            if (p.type() == StressProfile.Type.DISTRESS)
                add(out, "Try a short relaxation exercise",
                        "Slow breathing or a few minutes of stretching can help your body settle when stress feels heavy.",
                        "Possible Solutions");
        }
        if (!higher && out.isEmpty()) {
            add(out, "Keep doing what works", "Protect your rest, keep time for friends, and plan study and break blocks.", "Resources");
            add(out, "Check in on yourself regularly", "A quick daily check-in helps you notice changes early.", "Daily Check-In");
        } else if (!higher && classify(r.overall()).rank() == 1) {
            add(out, "Visit the Guidance Office if this continues",
                    "If these feelings last for a few weeks, a short chat with a counselor can help.", "Guidance Office");
        }
        return out.values().stream().limit(6).toList();
    }

    private static void add(Map<String, Recommendation> m, String title, String detail, String target) {
        m.putIfAbsent(title, new Recommendation(title, detail, target));
    }

    /** Plain-language description of the scoring, shown to students in the app. */
    static String howScoringWorks() {
        return "How your scores work\n\n"
                + "\u2022  The check has 18 statements, 6 each about Academic, Social and Personal stress.\n"
                + "\u2022  You choose how often each happened in the past 2 weeks: Never (0), Sometimes (1), Often (2), Almost always (3).\n"
                + "\u2022  A few statements are positive (for example feeling accepted). Those are scored the other way round.\n"
                + "\u2022  Each area score is your points divided by the most possible points (18), as a percent.\n"
                + "\u2022  The overall score is the average of the three areas, so no area counts more than another.\n"
                + "\u2022  After the 18 statements, 5 short questions ask whether the pressure felt motivating, manageable, overwhelming or hard to recover from. "
                + "They are not part of your area scores. They help describe your stress as eustress (manageable, can be motivating) or distress (overwhelming or hard to manage), "
                + "so stress is not treated as bad by default.\n"
                + "\u2022  Below " + moderateCut + "% is low concern, " + moderateCut + "% to " + (higherCut - 1)
                + "% is moderate concern, and " + higherCut + "% or more is higher concern.\n\n"
                + "These bands are meant to help you reflect, and they have not been clinically validated. "
                + NOT_DIAGNOSIS;
    }
}
