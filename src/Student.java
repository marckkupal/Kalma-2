final class Student extends User {
    Student(String id, String name, String email, String password, String course) {
        super(id, name, email, password, course);
    }
    /** "en" or "tl". Used only for assessment questions; the rest of the app stays in English. */
    private String assessmentLanguage = "en";
    String getAssessmentLanguage() { return assessmentLanguage; }
    void setAssessmentLanguage(String lang) { assessmentLanguage = "tl".equals(lang) ? "tl" : "en"; }
    @Override String getRole() { return "student"; }
    @Override String[] getMenu() {
        return new String[]{"Home", "Assessment", "Daily Check-In", "Calendar", "Trends", "Summary", "History", "Export Summary",
                "Possible Solutions", "Resources", "Guidance Office",
                "Peer Support", "My Appointments", "Profile"};
    }
}
