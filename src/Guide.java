/** A guide written by Guidance. Shared guides show up as cards on the students' Resources page. */
final class Guide {
    private String title;
    private String summary;
    private String body;
    private boolean shared;
    private final String icon;

    Guide(String title, String summary, String body, boolean shared, String icon) {
        this.title = title;
        this.summary = summary;
        this.body = body;
        this.shared = shared;
        this.icon = icon;
    }

    String getTitle() { return title; }
    String getSummary() { return summary; }
    String getBody() { return body; }
    boolean isShared() { return shared; }
    String getIcon() { return icon; }
    void update(String title, String summary, String body, boolean shared) {
        this.title = title;
        this.summary = summary;
        this.body = body;
        this.shared = shared;
    }
}
