import java.time.LocalDateTime;
/** A short note a student sent to a staff member. */
final class Message {
    private final String toId;
    private final String fromId;
    private final String text;
    private final LocalDateTime at;
    private boolean read;

    Message(String toId, String fromId, String text, LocalDateTime at) {
        this.toId = toId;
        this.fromId = fromId;
        this.text = text;
        this.at = at;
    }

    String getToId() { return toId; }
    String getFromId() { return fromId; }
    String getText() { return text; }
    LocalDateTime getAt() { return at; }
    boolean isRead() { return read; }
    void markRead() { read = true; }
}
