import java.awt.Color;
import java.time.LocalDate;
/** An important school date (or date range) such as Prelims, Midterms, Finals, a holiday, or another deadline. */
record AcademicEvent(LocalDate start, LocalDate end, String type, String title) {
    static final String PRELIMS = "Prelims", MIDTERMS = "Midterms", FINALS = "Finals", HOLIDAY = "Holiday", OTHER = "Other";

    boolean covers(LocalDate d) { return !d.isBefore(start) && !d.isAfter(end); }

    boolean isExam() { return type.equals(PRELIMS) || type.equals(MIDTERMS) || type.equals(FINALS); }

    Color bg() { return isExam() ? new Color(0xFEF3C7) : type.equals(HOLIDAY) ? new Color(0xDCFCE7) : new Color(0xE0E7FF); }
    Color fg() { return isExam() ? new Color(0x92400E) : type.equals(HOLIDAY) ? new Color(0x166534) : new Color(0x3730A3); }

    String range() {
        java.time.format.DateTimeFormatter f = StudentUI.SHORT_DAY;
        return start.equals(end) ? start.format(f) : start.format(f) + " \u2013 " + end.format(f);
    }
}
