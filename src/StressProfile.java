import java.util.*;

/**
 * The "type" and "main source" parts of an assessment result (see docs/ASSESSMENT_METHODOLOGY.md, section 6A).
 *
 * <p>Type: {@link Type#DISTRESS} when the overall score or any domain is in the higher band, {@link Type#EUSTRESS}
 * (manageable stress) when the highest band reached is moderate, and {@link Type#LOW} when nothing is above the low band.
 * These are descriptive labels for a self-reflection tool. They are not diagnoses.
 *
 * <p>Sources: every question is tagged with one source. A source's score is its item points divided by its maximum,
 * as a percent. A source is "reported" when it reaches the moderate cut point. A source with fewer than
 * {@link #MIN_ITEMS_FOR_MODERATE_CUT} items must reach the higher cut point, so one "Sometimes" answer alone does not
 * flag it. The student can have several reported sources at once. The highest one, plus any within
 * {@link AssessmentService#SMALL_CHANGE} points of it, are the main sources. The rest are secondary.
 * If three or more sources are main sources, the result is described as mixed concerns.
 */
record StressProfile(Type type, Map<String, Integer> sourceScores, List<String> main, List<String> secondary) {
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
        return new StressProfile(type, scores, main, rest);
    }

    static Type typeOf(AssessmentResult r) {
        int top = AssessmentService.classify(r.overall()).rank();
        for (String d : AssessmentService.DOMAINS) top = Math.max(top, AssessmentService.classify(r.score(d)).rank());
        return top >= 2 ? Type.DISTRESS : top == 1 ? Type.EUSTRESS : Type.LOW;
    }

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
        return switch (type) {
            case LOW -> "Your answers suggest very little stress right now. Stress is a normal part of student life "
                    + "and is not always a bad thing.";
            case EUSTRESS -> "Your answers suggest stress that feels manageable. Stress like this can be motivating "
                    + "when it comes with enough rest and support.";
            case DISTRESS -> "Your answers suggest stress that feels overwhelming or hard to manage at the moment. "
                    + "This can affect focus, sleep, motivation and daily activities, and support is available.";
        };
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
