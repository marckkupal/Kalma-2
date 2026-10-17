import java.util.List;
/**
 * One entry in the Resources directory (hotline, office, self-help material ...).
 * Guidance and Admin edit these from the Maintenance page; students see the visible ones on Resources.
 */
final class Resource {
    static final String GUIDANCE = "Guidance Office";
    static final String CRISIS = "24/7 Crisis Support";
    static final String SELF_HELP = "Self-Help";
    static final String ACADEMIC = "Academic Support";
    static final String WELLNESS = "Wellness Materials";
    /** Suggested categories. Admins can also type a new one when adding or editing an entry. */
    static final List<String> CATEGORIES = List.of(GUIDANCE, CRISIS, SELF_HELP, ACADEMIC, WELLNESS);

    private String title, category, description, contact, hours, access;
    private boolean open247, visible;

    Resource(String title, String category, String description, String contact, String hours, String access,
             boolean open247, boolean visible) {
        update(title, category, description, contact, hours, access, open247, visible);
    }

    void update(String title, String category, String description, String contact, String hours, String access,
                boolean open247, boolean visible) {
        this.title = title;
        this.category = category;
        this.description = description;
        this.contact = contact;
        this.hours = hours;
        this.access = access;
        this.open247 = open247;
        this.visible = visible;
    }

    String title() { return title; }
    String category() { return category; }
    String description() { return description; }
    String contact() { return contact; }
    /** Opening hours as text, e.g. "Mon to Fri, 9:00 AM to 4:00 PM". */
    String hours() { return hours; }
    /** Plain-language steps for reaching this resource. */
    String access() { return access; }
    boolean open247() { return open247; }
    /** Hidden entries stay in Maintenance but students do not see them. */
    boolean visible() { return visible; }

    String searchText() {
        return (title + " " + category + " " + description + " " + contact + " " + hours + " " + access).toLowerCase();
    }
}
