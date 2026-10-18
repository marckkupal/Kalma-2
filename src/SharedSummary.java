import java.time.LocalDateTime;

/**
 * A wellness summary a student chose to share with a Guidance counselor. It is a snapshot of exactly what the student
 * saw on the consent screen ({@code text}), so later check-ins never change what the counselor can read.
 * {@code consentAt} and {@code consentVersion} record that the student agreed, and to which wording.
 * Sharing is voluntary and can be stopped: once {@code revokedAt} is set the counselor can no longer open it.
 */
record SharedSummary(String id, String studentId, String counselorId, String consentVersion, LocalDateTime consentAt,
                     LocalDateTime sharedAt, LocalDateTime revokedAt, String period, String sections, String text) {
    boolean isActive() { return revokedAt == null; }
    SharedSummary revoked(LocalDateTime when) {
        return new SharedSummary(id, studentId, counselorId, consentVersion, consentAt, sharedAt, when, period, sections, text);
    }
}
