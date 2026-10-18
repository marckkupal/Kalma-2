import java.time.LocalDateTime;
/** A discussion post, or a reply when parentId is not null. Moderators can hide a post without deleting it. */
final class CommunityPost {
    private final String id;
    private final String communityId;
    private final String parentId;
    private final String authorId;
    private final String text;
    private final LocalDateTime at;
    private final boolean anonymous;
    private boolean hidden;
    private String hiddenBy;

    CommunityPost(String id, String communityId, String parentId, String authorId, String text,
                  LocalDateTime at, boolean anonymous) {
        this.id = id;
        this.communityId = communityId;
        this.parentId = parentId;
        this.authorId = authorId;
        this.text = text;
        this.at = at;
        this.anonymous = anonymous;
    }

    String getId() { return id; }
    String getCommunityId() { return communityId; }
    String getParentId() { return parentId; }
    boolean isReply() { return parentId != null; }
    /** The real author. Other students never see this for an anonymous post; moderators do. */
    String getAuthorId() { return authorId; }
    String getText() { return text; }
    LocalDateTime getAt() { return at; }
    boolean isAnonymous() { return anonymous; }
    boolean isHidden() { return hidden; }
    String getHiddenBy() { return hiddenBy; }
    void hide(String moderatorId) { hidden = true; hiddenBy = moderatorId; }
    void restore() { hidden = false; hiddenBy = null; }
}
