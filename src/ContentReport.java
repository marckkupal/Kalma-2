import java.time.LocalDateTime;
/** A report on a community post, waiting for (or already handled by) Admin or Guidance. */
final class ContentReport {
    enum State { OPEN, ACTIONED, DISMISSED }

    /** Reporter id used when the system files a report itself (for example, crisis wording in a post). */
    static final String SYSTEM = "system";
    static final String URGENT_REASON = "Someone may be in danger or at risk of self-harm";
    static final String[] REASONS = {
            URGENT_REASON,
            "Harassment, bullying or hate",
            "Harmful advice (medical, medication or therapy-style advice)",
            "Shares private information",
            "Spam or advertising",
            "Something else"};

    private final String id;
    private final String postId;
    private final String reporterId;
    private final String reason;
    private final String details;
    private final LocalDateTime at;
    private State state = State.OPEN;
    private String handledBy = "";
    private String note = "";
    private LocalDateTime handledAt;

    ContentReport(String id, String postId, String reporterId, String reason, String details, LocalDateTime at) {
        this.id = id;
        this.postId = postId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.details = details == null ? "" : details;
        this.at = at;
    }

    String getId() { return id; }
    String getPostId() { return postId; }
    String getReporterId() { return reporterId; }
    String getReason() { return reason; }
    String getDetails() { return details; }
    LocalDateTime getAt() { return at; }
    State getState() { return state; }
    boolean isOpen() { return state == State.OPEN; }
    boolean isUrgent() { return reason.equals(URGENT_REASON); }
    String getHandledBy() { return handledBy; }
    String getNote() { return note; }
    LocalDateTime getHandledAt() { return handledAt; }

    void resolve(State outcome, String moderatorId, String note) {
        this.state = outcome;
        this.handledBy = moderatorId;
        this.note = note == null ? "" : note;
        this.handledAt = LocalDateTime.now();
    }
}
