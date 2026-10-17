import java.time.LocalDate;
/** A student's private journal entry for one day. Never shown to staff. */
record JournalEntry(String userId, LocalDate date, String text) {}
