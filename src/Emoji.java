/**
 * Emoji used in the Calendar and Journal. They are written as unicode escape codes so the file compiles the same on every computer.
 * mood runs from 1 (very bad) to 5 (very good), the same scale as Kalma.MOODS.
 */
final class Emoji {
    private Emoji() {}

    static final String JOURNAL = "\uD83D\uDCD3";   // notebook
    private static final String[] MOOD = {
            "\uD83D\uDE1E",   // 1 disappointed face
            "\uD83D\uDE41",   // 2 slightly frowning face
            "\uD83D\uDE10",   // 3 neutral face
            "\uD83D\uDE42",   // 4 slightly smiling face
            "\uD83D\uDE04"};  // 5 grinning face with smiling eyes

    static String mood(int mood) { return MOOD[Math.max(1, Math.min(5, mood)) - 1]; }

    /** Exam periods, holidays and everything else. */
    static String event(AcademicEvent e) {
        return e.isExam() ? "\uD83D\uDCDD"                                  // memo
                : e.type().equals(AcademicEvent.HOLIDAY) ? "\uD83C\uDF89"   // party popper
                : "\uD83D\uDCCC";                                           // pushpin
    }
}
