import java.util.*;

/**
 * The "type" and "main source" parts of an assessment result (see docs/ASSESSMENT_METHODOLOGY.md, section 6A).
 *
 * <p>Type: stress is not treated as harmful by default. When the student answered the appraisal items (how motivating,
 * manageable, overwhelming and hard to recover from the demands felt), the type follows the student's own appraisal:
 * {@link Type#DISTRESS} when "strain" is in the higher band, or when a domain or the overall score is in the higher band
 * and strain is at least moderate; {@link Type#EUSTRESS} (stress that feels manageable and can be motivating) when there
 * are demands but strain is lower; {@link Type#LOW} when neither demands nor strain are above the low band. Without
 * appraisal answers (results saved before version 1.1) the type is inferred from the bands alone: higher band is
 * distress, moderate is eustress. These are descriptive labels for a self-reflection tool. They are not diagnoses.
 * {@code motivation} and {@code strain} are 0 to 100 and are null when the appraisal was not answered.
 *
 * <p>Sources: every question is tagged with one source. A source's score is its item points divided by its maximum,
 * as a percent. A source is "reported" when it reaches the moderate cut point. A source with fewer than
 * {@link #MIN_ITEMS_FOR_MODERATE_CUT} items must reach the higher cut point, so one "Sometimes" answer alone does not
 * flag it. The student can have several reported sources at once. The highest one, plus any within
 * {@link AssessmentService#SMALL_CHANGE} points of it, are the main sources. The rest are secondary.
 * If three or more sources are main sources, the result is described as mixed concerns.
 */
record StressProfile(Type type, Map<String, Integer> sourceScores, List<String> main, List<String> secondary,
                     Integer motivation, Integer strain) {
    enum Type { LOW, EUSTRESS, DISTRESS }

    static final String ACADEMIC = "Academic";
    static final String FINANCIAL = "Financial";
    static final String SOCIAL = "Social";
    static final String PERSONAL = "Personal or family";
    static final String TIME = "Time management";
    /** Shown when the main sources are spread across three or more categories. No question maps only to it. */
    static final String OTHER = "Other or mixed concerns";
    /** The categories that questions can belong to, in display order. */
    static final List<String> SOURCES = List.of(ACADEMIC, FINANCIAL, SOCIAL, PERSONAL, TIME);
    static final int MIN_ITEMS_FOR_MODERATE_CUT = 3;

    /** Profile without appraisal scores (version 1.0 results and band-only inference). */
    StressProfile(Type type, Map<String, Integer> sourceScores, List<String> main, List<String> secondary) {
        this(type, sourceScores, main, secondary, null, null);
    }

    StressProfile {
        sourceScores = Collections.unmodifiableMap(new LinkedHashMap<>(sourceScores));
        main = List.copyOf(main);
        secondary = List.copyOf(secondary);
    }

    boolean mixed() { return main.size() >= 3; }

    /** The categories to show as "main source". A mixed result shows the single "Other or mixed concerns" label. */
    List<String> categories() {
        if (mixed()) return List.of(OTHER);
        List<String> all = new ArrayList<>(main);
        all.addAll(secondary);
        return all;
    }

    // ----- building -----
    /** Builds the profile for one completed assessment. {@code base} supplies the domain and overall scores. */
    static StressProfile of(List<Question> qs, int[] answers, AssessmentResult base) {
        Map<String, Integer> pts = new HashMap<>();
        Map<String, Integer> count = new HashMap<>();
        for (int i = 0; i < qs.size(); i++) {
            pts.merge(qs.get(i).source(), qs.get(i).points(answers[i]), Integer::sum);
            count.merge(qs.get(i).source(), 1, Integer::sum);
        }
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (String s : SOURCES)
            if (count.containsKey(s)) scores.put(s, AssessmentService.percent(pts.get(s), count.get(s)));
        return fromScores(typeOf(base), scores);
    }

    /** Rebuilds a profile from saved values. Which sources are main or secondary is always re-derived. */
    static StressProfile fromScores(Type type, Map<String, Integer> scores) {
        return fromScores(type, scores, null, null);
    }

    static StressProfile fromScores(Type type, Map<String, Integer> scores, Integer motivation, Integer strain) {
        List<String> reported = new ArrayList<>();
        for (String s : SOURCES) {
            Integer p = scores.get(s);
            if (p != null && p >= elevationCut(s)) reported.add(s);
        }
        reported.sort(Comparator.comparingInt((String s) -> scores.get(s)).reversed());   // stable: ties keep SOURCES order
        List<String> main = new ArrayList<>(), rest = new ArrayList<>();
        if (!reported.isEmpty()) {
            int top = scores.get(reported.get(0));
            for (String s : reported)
                (top - scores.get(s) <= AssessmentService.SMALL_CHANGE ? main : rest).add(s);
        }
        return new StressProfile(type, scores, main, rest, motivation, strain);
    }

    /** Type of stress for a result. Uses the saved profile (which includes the student's appraisal) when there is one. */
    static Type typeOf(AssessmentResult r) {
        return r.profile() != null ? r.profile().type() : typeFromBands(r);
    }

    /** Band-only inference, used when the appraisal items were not answered. */
    static Type typeFromBands(AssessmentResult r) {
        int top = topBand(r);
        return top >= 2 ? Type.DISTRESS : top == 1 ? Type.EUSTRESS : Type.LOW;
    }

    /** Highest band (0 low, 1 moderate, 2 higher) reached by the overall score or any domain. */
    static int topBand(AssessmentResult r) {
        int top = AssessmentService.classify(r.overall()).rank();
        for (String d : AssessmentService.DOMAINS) top = Math.max(top, AssessmentService.classify(r.score(d)).rank());
        return top;
    }

    /**
     * Type of stress when the appraisal was answered. {@code strain} (0 to 100) combines "overwhelming", "hard to
     * recover" and "not manageable". A heavy workload alone does not make the result distress if the student feels
     * able to manage it; feeling overwhelmed does, even when the demands look moderate.
     */
    static Type typeFrom(int topBand, int strain) {
        if (strain >= AssessmentService.higherCut()) return Type.DISTRESS;
        if (topBand >= 2 && strain >= AssessmentService.moderateCut()) return Type.DISTRESS;
        if (topBand >= 1 || strain >= AssessmentService.moderateCut()) return Type.EUSTRESS;
        return Type.LOW;
    }

    /**
     * Adds the appraisal to a profile. Appraisal items are in {@link AssessmentService#APPRAISAL} order:
     * 2 motivating, 1 manageable (reverse), 1 overwhelming, 1 hard to recover.
     */
    static StressProfile withAppraisal(StressProfile sources, AssessmentResult base, int[] a) {
        int motivation = AssessmentService.percent(a[0] + a[1], 2);
        int strain = AssessmentService.percent((3 - a[2]) + a[3] + a[4], 3);
        return fromScores(typeFrom(topBand(base), strain), sources.sourceScores(), motivation, strain);
    }

    boolean hasAppraisal() { return motivation != null && strain != null; }

    /** Percent a source must reach to count as reported. Sources with very few items need a stronger signal. */
    static int elevationCut(String source) {
        int items = 0;
        for (Question q : AssessmentService.QUESTIONS) if (q.source().equals(source)) items++;
        return items >= MIN_ITEMS_FOR_MODERATE_CUT ? AssessmentService.moderateCut() : AssessmentService.higherCut();
    }

    // ----- wording -----
    String typeLabel() {
        return switch (type) {
            case LOW -> "Low reported stress";
            case EUSTRESS -> "Eustress (manageable stress)";
            case DISTRESS -> "Distress (stress that feels heavy)";
        };
    }

    String typeMessage() {
        boolean motivated = motivation != null && motivation >= AssessmentService.moderateCut();
        return switch (type) {
            case LOW -> "Your answers suggest very little stress right now. Stress is a normal part of student life "
                    + "and is not always a bad thing.";
            case EUSTRESS -> "Your answers suggest stress that feels manageable. Stress like this can be motivating "
                    + "when it comes with enough rest and support."
                    + (motivated ? " You also said that some of the pressure has been helping you move forward." : "");
            case DISTRESS -> "Your answers suggest stress that feels overwhelming or hard to manage at the moment. "
                    + "This can affect focus, sleep, motivation and daily activities, and support is available.";
        };
    }

    /** One line on how the demands felt (motivating, manageable, overwhelming, hard to recover), or null without appraisal. */
    String appraisalLine() {
        if (!hasAppraisal()) return null;
        String m = motivation >= AssessmentService.higherCut() ? "often motivating"
                : motivation >= AssessmentService.moderateCut() ? "sometimes motivating" : "not very motivating";
        String h = strain >= AssessmentService.higherCut() ? "overwhelming and hard to recover from"
                : strain >= AssessmentService.moderateCut() ? "sometimes heavy, but mostly manageable" : "manageable";
        return "You described your demands as " + m + " and " + h + ". Motivation " + motivation + "%, strain " + strain + "%.";
    }

    /** For example: "Moderate reported stress \u2014 primarily academic, with some financial concerns." */
    String summary(AssessmentResult r) {
        int rank = AssessmentService.classify(r.overall()).rank();
        String level = (rank == 0 ? "Low" : rank == 1 ? "Moderate" : "Higher") + " reported stress";
        if (main.isEmpty()) return level + " \u2014 no single source stands out.";
        String s = level + " \u2014 " + (mixed() ? "mixed concerns across " + join(lower(main)) : "primarily " + join(lower(main)));
        if (!secondary.isEmpty()) s += ", with some " + join(lower(secondary)) + " concerns";
        return s + ".";
    }

    private static List<String> lower(List<String> l) { return l.stream().map(String::toLowerCase).toList(); }

    private static String join(List<String> l) {
        if (l.size() == 1) return l.get(0);
        return String.join(", ", l.subList(0, l.size() - 1)) + " and " + l.get(l.size() - 1);
    }

    // ----- saving (two extra columns in data/assessments.tsv) -----
    String encodeScores() {
        StringJoiner j = new StringJoiner(";");
        sourceScores.forEach((k, v) -> j.add(k + "=" + v));
        return j.toString();
    }

    /** Saved appraisal scores, for example "Motivation=50;Strain=22". Empty when the appraisal was not answered. */
    String encodeAppraisal() {
        return hasAppraisal() ? "Motivation=" + motivation + ";Strain=" + strain : "";
    }

    /** Like {@link #decode(String, String)}, with the optional saved appraisal. A damaged appraisal is ignored. */
    static StressProfile decode(String type, String scores, String appraisal) {
        StressProfile base = decode(type, scores);
        if (base == null || appraisal == null || appraisal.isBlank()) return base;
        try {
            Integer mot = null, str = null;
            for (String part : appraisal.split(";")) {
                String[] kv = part.split("=", 2);
                if (kv[0].equals("Motivation")) mot = Integer.parseInt(kv[1]);
                if (kv[0].equals("Strain")) str = Integer.parseInt(kv[1]);
            }
            if (mot == null || str == null) return base;
            return fromScores(base.type(), base.sourceScores(), mot, str);
        } catch (RuntimeException e) {
            return base;
        }
    }

    /** Returns null when the saved text cannot be read, so an old or damaged line still loads without a profile. */
    static StressProfile decode(String type, String scores) {
        try {
            Map<String, Integer> m = new LinkedHashMap<>();
            for (String part : scores.split(";")) {
                String[] kv = part.split("=", 2);
                if (SOURCES.contains(kv[0])) m.put(kv[0], Integer.parseInt(kv[1]));
            }
            return m.isEmpty() ? null : fromScores(Type.valueOf(type), m);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
