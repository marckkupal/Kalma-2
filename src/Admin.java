final class Admin extends User {
    Admin(String id, String name, String email, String password) {
        super(id, name, email, password, "");
    }
    @Override String getRole() { return "admin"; }
    @Override String[] getMenu() { return new String[]{"Dashboard", "Users", "Reports", "Appointments", "Moderation", "Announcements", "Maintenance", "Academic Calendar"}; }
    @Override boolean isStaff() { return true; }
    @Override String getRoleLabel() { return "Administrator"; }
}
