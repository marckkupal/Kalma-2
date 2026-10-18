import java.time.LocalDate;
record CheckIn(String userId, LocalDate date, int mood, int stress, String note) {
    CheckIn(String userId, LocalDate date, int mood, int stress) { this(userId, date, mood, stress, ""); }
}
