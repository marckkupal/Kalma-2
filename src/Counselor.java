final class Counselor extends User {
    Counselor(String id, String name, String email, String password) {
        super(id, name, email, password, "");
    }
    @Override String getRole() { return "counselor"; }
    @Override String[] getMenu() { return new String[]{"Dashboard", "Appointments", "Students", "Messages", "Shared Summaries", "Moderation", "Resources", "Maintenance", "Academic Calendar"}; }
    @Override boolean isStaff() { return true; }
    @Override String getRoleLabel() { return "Guidance Counselor"; }
    @Override String getGreetingName() {
        String[] p = getName().trim().split("\\s+");
        return "Ms. " + p[p.length - 1];
    }
}
