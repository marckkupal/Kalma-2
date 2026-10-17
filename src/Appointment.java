import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
class Appointment {
    private final String studentId;
    private final String counselorId;
    private final String reason;
    private String date;
    private String time;
    private Status status;
    /** How many times the student has moved this appointment. Guidance changes do not count. */
    private int rescheduleCount;

    Appointment(String studentId, String counselorId, String date, String time, String reason, Status status) {
        this.studentId = studentId;
        this.counselorId = counselorId;
        this.date = date;
        this.time = time;
        this.reason = reason;
        this.status = status;
    }

    String getStudentId() { return studentId; }
    String getCounselorId() { return counselorId; }
    String getDate() { return date; }
    String getTime() { return time; }
    String getReason() { return reason; }
    Status getStatus() { return status; }
    int getRescheduleCount() { return rescheduleCount; }
    void setStatus(Status status) { this.status = status; }
    void reschedule(String newDate, String newTime) { reschedule(newDate, newTime, false); }
    void reschedule(String newDate, String newTime, boolean byStudent) {
        this.date = newDate;
        this.time = newTime;
        this.status = Status.RESCHEDULED;
        if (byStudent) rescheduleCount++;
    }
    String getSortKey() { return date + time; }
    LocalDateTime startsAt() { return LocalDate.parse(date).atTime(LocalTime.parse(time)); }
    boolean occupiesSlot() { return status != Status.CANCELLED; }
    boolean isUpcoming() {
        return status != Status.CANCELLED && status != Status.COMPLETED
                && date.compareTo(LocalDate.now().toString()) >= 0;
    }
}
