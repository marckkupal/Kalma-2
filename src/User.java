abstract class User {
    private final String id;
    private final String name;
    private final String email;
    private final String password;
    private String course;
    private boolean active = true;

    protected User(String id, String name, String email, String password, String course) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.course = course;
    }

    String getId() { return id; }
    String getName() { return name; }
    String getEmail() { return email; }
    String getCourse() { return course; }
    void setCourse(String course) { this.course = course; }
    boolean isActive() { return active; }
    void setActive(boolean active) { this.active = active; }
    boolean passwordMatches(String attempt) { return password.equals(attempt); }
    String getFirstName() { return name.split(" ")[0]; }
    String getInitials() {
        String[] p = name.trim().split("\\s+");
        return p.length == 1 ? p[0].substring(0, 1).toUpperCase()
                : ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase();
    }

    abstract String getRole();
    abstract String[] getMenu();
    boolean isStaff() { return false; }

    /** Role text shown under the name in the top bar. */
    String getRoleLabel() { return getRole().substring(0, 1).toUpperCase() + getRole().substring(1); }
    /** Name used in the dashboard greeting banner. */
    String getGreetingName() { return getFirstName(); }
}
