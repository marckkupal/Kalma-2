import java.time.LocalDate;
/**
 * One completed assessment. Every score is a percentage from 0 to 100 (see docs/ASSESSMENT_METHODOLOGY.md).
 * {@code version} records which scoring formula produced the numbers, so old results stay interpretable if the
 * formula or thresholds are ever revised after review.
 * {@code profile} holds the stress type and main sources. It is null for results saved before it existed.
 */
record AssessmentResult(String userId, LocalDate date, int academic, int social, int personal, int overall,
                        String version, StressProfile profile) {
    AssessmentResult(String userId, LocalDate date, int academic, int social, int personal, int overall, String version) {
        this(userId, date, academic, social, personal, overall, version, null);
    }
    AssessmentResult withProfile(StressProfile p) {
        return new AssessmentResult(userId, date, academic, social, personal, overall, version, p);
    }
    int score(String domain) {
        return switch (domain) {
            case "Academic" -> academic;
            case "Social" -> social;
            case "Personal" -> personal;
            default -> overall;
        };
    }
}
