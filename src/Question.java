/**
 * One assessment statement in English and Filipino. Both versions carry the same meaning and the same scoring.
 * {@code category} is the stress domain: "Academic", "Social" or "Personal".
 * {@code source} is the stress source it speaks to (see {@link StressProfile}): Academic, Financial, Social,
 * Personal or family, or Time management. It is used only for the "main source" part of the result and never
 * changes the domain scores.
 * {@code reverse} is true for positively worded items (for example "I felt accepted"). Their score is flipped
 * (0 becomes 3, 1 becomes 2 ...) so that a higher number always means more stress.
 */
record Question(String text, String textTl, String category, boolean reverse, String source) {
    Question(String text, String textTl, String category, boolean reverse) {
        this(text, textTl, category, reverse, defaultSource(category));
    }
    Question(String text, String textTl, String category) { this(text, textTl, category, false); }
    private static String defaultSource(String category) {
        return switch (category) {
            case "Academic" -> StressProfile.ACADEMIC;
            case "Social" -> StressProfile.SOCIAL;
            default -> StressProfile.PERSONAL;
        };
    }
    String text(boolean filipino) { return filipino ? textTl : text; }
    /** Stress points (0 to 3) for a raw answer (0 to 3). */
    int points(int answer) { return reverse ? 3 - answer : answer; }
}
