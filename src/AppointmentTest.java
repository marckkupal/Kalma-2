import java.time.LocalDate;
import java.util.*;

/**
 * Checks the Appointment Booking rules: slots and lunch, double-booking, upcoming vs previous, cancel and
 * reschedule rules, Guidance schedule management and in-app reminders. No test framework needed:  java AppointmentTest
 */
public class AppointmentTest {
    private static int failures = 0, checks = 0;

    static void check(String name, boolean ok) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL  " + name); }
    }

    public static void main(String[] args) {
        DataStore d = DataStore.getInstance();
        d.seed();
        User maria = d.findById("u1").orElseThrow();
        User juan = d.findById("u2").orElseThrow();
        User ana = d.findById("u5").orElseThrow();
        User counselor = d.findById("u3").orElseThrow();
        User admin = d.findById("u4").orElseThrow();
        List<String> days = DataStore.nextWeekdays(12);
        String far = days.get(9), far2 = days.get(10), near = days.get(0);

        // ----- slots and lunch -----
        check("slots skip the lunch break", BookingRules.slots().equals(List.of("09:00", "10:00", "13:00", "14:00", "15:00")));
        check("no slot overlaps lunch", BookingRules.slots().stream().noneMatch(t -> BookingRules.overlapsLunch(java.time.LocalTime.parse(t))));
        check("free slots on a clear day are all the slots", d.freeSlots("c2", far).equals(BookingRules.slots()));
        check("weekends offer no slots", d.freeSlots("c1", nextWeekend()).isEmpty());
        check("past dates offer no slots", d.freeSlots("c1", LocalDate.now().minusDays(3).toString()).isEmpty());
        String lunchTry = d.addAppointment(new Appointment("u1", "c2", far, "12:00", "Lunch attempt", Status.PENDING));
        check("booking during lunch is refused", lunchTry != null && lunchTry.toLowerCase().contains("lunch"));
        check("booking after hours is refused", d.addAppointment(new Appointment("u1", "c2", far, "17:00", "Late", Status.PENDING)) != null);
        check("booking on a weekend is refused", d.addAppointment(new Appointment("u1", "c2", nextWeekend(), "09:00", "Weekend", Status.PENDING)) != null);
        check("booking in the past is refused", d.addAppointment(new Appointment("u1", "c2", LocalDate.now().minusDays(1).toString(), "09:00", "Past", Status.PENDING)) != null);

        // ----- booking and double-booking -----
        Appointment first = new Appointment("u1", "c2", far, "09:00", "Academic pressure", Status.PENDING);
        check("a free slot can be booked", d.addAppointment(first) == null && d.getAppointments().contains(first));
        check("booked slot disappears from the list", !d.freeSlots("c2", far).contains("09:00"));
        check("same counselor, same slot is refused", d.addAppointment(new Appointment("u2", "c2", far, "09:00", "Same slot", Status.PENDING)) != null);
        check("same student cannot hold two counselors at one time",
                d.addAppointment(new Appointment("u1", "c1", far, "09:00", "Clash", Status.PENDING)) != null);
        check("another counselor, another time is fine", d.addAppointment(new Appointment("u1", "c1", far, "10:00", "Other time", Status.PENDING)) == null);

        // ----- upcoming vs previous -----
        List<Appointment> mine = d.appointmentsFor(maria);
        check("student sees only their own appointments", mine.stream().allMatch(a -> a.getStudentId().equals("u1")));
        check("seeded completed visit is previous", mine.stream().anyMatch(a -> !a.isUpcoming() && a.getStatus() == Status.COMPLETED));
        check("new booking is upcoming", mine.contains(first) && first.isUpcoming());

        // ----- cancelling -----
        Appointment todays = mine.stream().filter(a -> a.getDate().equals(LocalDate.now().toString())).findFirst().orElseThrow();
        check("student cannot cancel inside the notice window", d.cancelAppointment(todays, maria) != null && todays.getStatus() != Status.CANCELLED);
        check("student cannot change someone else's appointment", d.cancelAppointment(first, juan) != null);
        Appointment completed = mine.stream().filter(a -> a.getStatus() == Status.COMPLETED).findFirst().orElseThrow();
        check("completed appointments cannot be cancelled", d.cancelAppointment(completed, maria) != null);
        Appointment toCancel = new Appointment("u1", "c2", far2, "13:00", "To cancel", Status.PENDING);
        d.addAppointment(toCancel);
        check("student can cancel a far-off appointment", d.cancelAppointment(toCancel, maria) == null && toCancel.getStatus() == Status.CANCELLED);
        check("cancelled slot is free again", d.freeSlots("c2", far2).contains("13:00"));
        check("cancelled appointment becomes previous", !toCancel.isUpcoming());
        check("cancelling twice is refused", d.cancelAppointment(toCancel, maria) != null);

        // ----- rescheduling -----
        check("student cannot reschedule inside the notice window", d.rescheduleAppointment(todays, far2, "14:00", maria) != null);
        check("reschedule into lunch is refused", d.rescheduleAppointment(first, far2, "11:00", maria) != null);
        check("reschedule to the same time is refused", d.rescheduleAppointment(first, far, "09:00", maria) != null);
        check("reschedule onto a taken slot is refused", d.rescheduleAppointment(first, far, "10:00", maria) != null);
        check("student can reschedule once", d.rescheduleAppointment(first, far2, "14:00", maria) == null
                && first.getDate().equals(far2) && first.getTime().equals("14:00") && first.getStatus() == Status.RESCHEDULED);
        check("old slot is freed by rescheduling", d.freeSlots("c2", far).contains("09:00"));
        check("new slot is held by rescheduling", !d.freeSlots("c2", far2).contains("14:00"));
        check("student can reschedule a second time", d.rescheduleAppointment(first, far2, "15:00", maria) == null);
        check("reschedule count is tracked", first.getRescheduleCount() == 2);
        check("third student reschedule is refused", d.rescheduleAppointment(first, far, "09:00", maria) != null);
        check("Guidance can still reschedule past the limit", d.rescheduleAppointment(first, far, "09:00", counselor) == null);
        check("Guidance changes do not use up the student's count", first.getRescheduleCount() == 2);
        check("Guidance can cancel inside the notice window", d.cancelAppointment(ana == null ? todays : d.appointmentsFor(ana).get(0), counselor) == null);

        // ----- Guidance / Admin manage requests -----
        Appointment pending = new Appointment("u2", "c2", far2, "09:00", "Request", Status.PENDING);
        d.addAppointment(pending);
        check("students cannot confirm", d.updateStatus(pending, Status.CONFIRMED, juan) != null);
        check("Guidance can confirm a pending request", d.updateStatus(pending, Status.CONFIRMED, counselor) == null && pending.getStatus() == Status.CONFIRMED);
        check("a confirmed request cannot be confirmed again", d.updateStatus(pending, Status.CONFIRMED, counselor) != null);
        check("a future visit cannot be marked completed", d.updateStatus(pending, Status.COMPLETED, admin) != null);
        check("Admin can manage a request too", d.updateStatus(pending, Status.CANCELLED, admin) == null && pending.getStatus() == Status.CANCELLED);
        Appointment unconfirmed = new Appointment("u2", "c2", far2, "10:00", "Unconfirmed", Status.PENDING);
        d.addAppointment(unconfirmed);
        check("pending cannot be completed before confirming", d.updateStatus(unconfirmed, Status.COMPLETED, counselor) != null);
        check("today's confirmed visit can be completed", d.updateStatus(todays, Status.COMPLETED, counselor) == null && todays.getStatus() == Status.COMPLETED);
        check("Admin menu has the Appointments page", Arrays.asList(admin.getMenu()).contains("Appointments"));

        // ----- schedule management -----
        check("Guidance can block a free time", d.blockTime("c1", near, "13:00") == null && d.isBlocked("c1", near, "13:00"));
        check("blocked time is hidden from students", !d.freeSlots("c1", near).contains("13:00"));
        check("students cannot book a blocked time", d.addAppointment(new Appointment("u2", "c1", near, "13:00", "Blocked try", Status.PENDING)) != null);
        check("blocking one counselor leaves the other open", d.freeSlots("c2", near).contains("13:00") || near.equals(far));
        check("blocked times are listed", d.blockedFromToday().stream().anyMatch(p -> p[0].equals("c1") && p[1].equals(near) && p[2].equals("13:00")));
        d.unblockTime("c1", near, "13:00");
        check("unblocking reopens the time", d.freeSlots("c1", near).contains("13:00"));
        check("a whole day with a booking cannot be blocked", d.blockTime("c1", far, null) != null && d.freeSlots("c1", far).contains("13:00"));
        String dayOff = days.get(11);
        check("blocking a whole day empties it", d.blockTime("c1", dayOff, null) == null && d.freeSlots("c1", dayOff).isEmpty());
        d.unblockTime("c1", dayOff, null);
        check("unblocking the day reopens it", d.freeSlots("c1", dayOff).size() == BookingRules.slots().size());
        check("a booked time cannot be blocked", d.blockTime("c2", far2, "10:00") != null && !d.isBlocked("c2", far2, "10:00"));

        // ----- reminders -----
        check("staff are reminded of pending requests", d.appointmentReminders(counselor).stream().anyMatch(s -> s.contains("waiting for review")));
        Appointment soon = null;
        for (int add = 1; add <= BookingRules.REMIND_DAYS && soon == null; add++) {
            LocalDate day = LocalDate.now().plusDays(add);
            if (day.getDayOfWeek().getValue() < 6) {
                soon = new Appointment("u5", "c2", day.toString(), "15:00", "Reminder test", Status.CONFIRMED);
                check("reminder test booking works", d.addAppointment(soon) == null);
            }
        }
        if (soon != null) {
            check("student gets a reminder for a near appointment", d.appointmentReminders(ana).stream().anyMatch(s -> s.contains("3:00 PM")));
            d.setPref("u5", "remind-appt", false);
            check("reminder can be switched off", d.appointmentReminders(ana).isEmpty());
            d.setPref("u5", "remind-appt", true);
        }
        check("no reminder for an appointment far away", d.appointmentReminders(maria).stream().noneMatch(s -> s.contains("Academic pressure") && s.contains(far)));
        check("reminders ignore other students' appointments", d.appointmentReminders(juan).stream().noneMatch(s -> s.contains("Reminder test")));

        System.out.println(failures == 0 ? "ALL " + checks + " APPOINTMENT CHECKS PASSED" : failures + " of " + checks + " CHECKS FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static String nextWeekend() {
        LocalDate d = LocalDate.now().plusDays(1);
        while (d.getDayOfWeek().getValue() < 6) d = d.plusDays(1);
        return d.toString();
    }
}
