/** TIP Quezon City academic programs. One list shared by registration and the profile page. */
enum Program {
    BSIT("BSIT", "Bachelor of Science in Information Technology"),
    BSCS("BSCS", "Bachelor of Science in Computer Science"),
    BSCpE("BSCpE", "Bachelor of Science in Computer Engineering"),
    BSDSA("BSDSA", "Bachelor of Science in Data Science and Analytics"),
    BSIS("BSIS", "Bachelor of Science in Information Systems");

    static final String[] YEARS = {"1st Year", "2nd Year", "3rd Year", "4th Year"};

    final String abbr;
    final String fullName;

    Program(String abbr, String fullName) {
        this.abbr = abbr;
        this.fullName = fullName;
    }

    /** Text shown in dropdown menus, e.g. "BSIT – Bachelor of Science in Information Technology". */
    @Override public String toString() { return abbr + " \u2013 " + fullName; }

    static String[] labels() {
        Program[] all = values();
        String[] out = new String[all.length];
        for (int i = 0; i < all.length; i++) out[i] = all[i].toString();
        return out;
    }

    /** Finds the program from stored text such as "BSCS 3rd Year". Returns null if it is not a known program. */
    static Program fromCourse(String course) {
        String abbr = course == null ? "" : course.trim().split("\\s+")[0];
        for (Program p : values()) if (p.abbr.equalsIgnoreCase(abbr)) return p;
        return null;
    }

    /** The year part of "BSCS 3rd Year", or "" if there is none. */
    static String yearOf(String course) {
        String c = course == null ? "" : course.trim();
        int i = c.indexOf(' ');
        return i < 0 ? "" : c.substring(i + 1).trim();
    }

    static String combine(Program p, String year) { return year == null || year.isBlank() ? p.abbr : p.abbr + " " + year; }
}
