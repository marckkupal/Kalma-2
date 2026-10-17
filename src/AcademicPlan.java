import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Turns the academic calendar into (1) wellness suggestions for the next important school period and
 * (2) a gentle comparison between exam periods and the student's own reported stress.
 *
 * <p>Nothing here is a diagnosis or a prediction. The suggestions are fixed, plain self-help steps chosen by how
 * close the date is, and the comparison only describes what the student reported. It never claims that exams
 * caused the stress (see docs/ACADEMIC_CALENDAR.md).
 */
final class AcademicPlan {
    private AcademicPlan() {}

    /** Days before an exam that count as "exam time" when comparing stress (revision and waiting included). */
    static final int LEAD_DAYS = 7;
    /** Days after an exam period that still get a recovery suggestion. */
    static final int RECOVER_DAYS = 3;
    /** Exams further away than this get no suggestion yet. */
    static final int PLAN_DAYS = 21;
    /** Fewest recorded check-ins needed on each side before any comparison is shown. */
    static final int MIN_DAYS = 3;
    /** Average stress (1 to 5) must differ by at least this much to be called higher or lower. */
    static final double NOTICEABLE = 0.3;

    enum Phase {
        PLAN("Plan ahead"), PREPARE("Getting close"), DURING("Happening now"), RECOVER("Time to recover"), BREAK("Time to rest");
        final String label;
        Phase(String label) { this.label = label; }
    }

    record Focus(AcademicEvent event, Phase phase, long days) {
        /** "Today" is day 0, so an event starting tomorrow is 1 day away. */
        String when() {
            return switch (phase) {
                case DURING -> "Happening now";
                case RECOVER -> "Ended " + (days == 0 ? "today" : -days == 1 ? "yesterday" : (-days) + " days ago");
                default -> days == 0 ? "Today" : days == 1 ? "Tomorrow" : "In " + days + " days";
            };
        }
    }

    // ----- what to prepare for -----
    /** The one school period worth acting on today, or empty when nothing important is close. */
    static Optional<Focus> focus(List<AcademicEvent> events, LocalDate today) {
        List<AcademicEvent> sorted = new ArrayList<>(events);
        sorted.sort(Comparator.comparing(AcademicEvent::start));

        for (AcademicEvent e : sorted)
            if (e.isExam() && e.covers(today)) return Optional.of(new Focus(e, Phase.DURING, 0));

        AcademicEvent nextExam = null, nextDeadline = null, nextBreak = null;
        for (AcademicEvent e : sorted) {
            if (e.start().isBefore(today)) continue;
            if (e.isExam() && nextExam == null) nextExam = e;
            else if (e.type().equals(AcademicEvent.OTHER) && nextDeadline == null) nextDeadline = e;
            else if (e.type().equals(AcademicEvent.HOLIDAY) && nextBreak == null) nextBreak = e;
        }
        if (nextExam != null && days(today, nextExam) <= LEAD_DAYS)
            return Optional.of(new Focus(nextExam, Phase.PREPARE, days(today, nextExam)));

        AcademicEvent recent = null;
        for (AcademicEvent e : sorted)
            if (e.isExam() && e.end().isBefore(today) && ChronoUnit.DAYS.between(e.end(), today) <= RECOVER_DAYS) recent = e;
        if (recent != null) return Optional.of(new Focus(recent, Phase.RECOVER, -ChronoUnit.DAYS.between(recent.end(), today)));

        if (nextExam != null && days(today, nextExam) <= PLAN_DAYS)
            return Optional.of(new Focus(nextExam, Phase.PLAN, days(today, nextExam)));
        if (nextDeadline != null && days(today, nextDeadline) <= LEAD_DAYS)
            return Optional.of(new Focus(nextDeadline, Phase.PREPARE, days(today, nextDeadline)));
        for (AcademicEvent e : sorted)
            if (e.type().equals(AcademicEvent.HOLIDAY) && e.covers(today)) return Optional.of(new Focus(e, Phase.BREAK, 0));
        if (nextBreak != null && days(today, nextBreak) <= LEAD_DAYS)
            return Optional.of(new Focus(nextBreak, Phase.BREAK, days(today, nextBreak)));
        return Optional.empty();
    }

    private static long days(LocalDate today, AcademicEvent e) { return ChronoUnit.DAYS.between(today, e.start()); }

    /** Short, practical steps for this phase. */
    static List<String> tips(Focus f) {
        return switch (f.phase()) {
            case PLAN -> List.of(
                    "List every subject and exam date, then split your review into 25-minute blocks.",
                    "Pick a steady bedtime now. It is easier to keep than to fix during exam week.",
                    "Check in daily so you can see how your stress changes as the dates get closer.");
            case PREPARE -> List.of(
                    "Study one subject at a time and stop at a fixed hour.",
                    "Protect your sleep. Short, steady nights help more than an all-nighter.",
                    "If worry is building, book a Guidance consultation early (bookings need "
                            + BookingRules.MIN_NOTICE_HOURS + " hours' notice).");
            case DURING -> List.of(
                    "Eat, drink water and take a short break between exams.",
                    "Try a two-minute breathing exercise before you go in.",
                    "After an exam, skip the answer comparing with classmates and rest your mind.");
            case RECOVER -> List.of(
                    "Rest properly: sleep, eat well and spend time with people you like.",
                    "Feeling flat or tired after exams is common. A check-in can help you notice how you are doing.",
                    "If the stress does not ease, the Guidance Office is happy to talk.");
            case BREAK -> List.of(
                    "Use the break to rest. Keep a light routine so the return to class is smoother.",
                    "Reach out to a friend or family member you have not talked to in a while.",
                    "Note anything coming up after the break so it does not catch you by surprise.");
        };
    }

    /** Grounding activity ids (see {@link GroundingActivity}) that fit this phase, best first. */
    static List<String> activityIds(Focus f) {
        return switch (f.phase()) {
            case PLAN -> List.of(GroundingActivity.BREATHING);
            case PREPARE -> List.of(GroundingActivity.BREATHING, GroundingActivity.MUSCLES);
            case DURING -> List.of(GroundingActivity.BREATHING, GroundingActivity.COLORS);
            case RECOVER -> List.of(GroundingActivity.MUSCLES, GroundingActivity.SENSES);
            case BREAK -> List.of(GroundingActivity.SENSES);
        };
    }

    /** Directory categories worth showing for this phase. Guidance is added separately when it can still be booked. */
    static List<String> resourceCategories(Focus f) {
        return switch (f.phase()) {
            case PLAN, PREPARE -> List.of(Resource.ACADEMIC, Resource.WELLNESS, Resource.SELF_HELP);
            case DURING -> List.of(Resource.SELF_HELP, Resource.WELLNESS);
            case RECOVER, BREAK -> List.of(Resource.SELF_HELP, Resource.WELLNESS);
        };
    }

    /** True if a Guidance booking can still be made for the start of this event under the 24-hour notice rule. */
    static boolean canStillBook(Focus f) {
        return (f.phase() == Phase.PLAN || f.phase() == Phase.PREPARE) && f.days() >= 2;
    }

    // ----- reflection -----
    /** Average reported stress (1 to 5) in exam time versus every other day the student checked in. */
    record Comparison(int examDays, double examStress, int otherDays, double otherStress) {
        double gap() { return examStress - otherStress; }
        String direction() { return Math.abs(gap()) < NOTICEABLE ? "about the same" : gap() > 0 ? "higher" : "lower"; }
    }

    /** One past or current exam period with the student's own numbers. */
    record PerEvent(AcademicEvent event, int days, double stress) {}

    static boolean inExamTime(LocalDate d, List<AcademicEvent> events) {
        for (AcademicEvent e : events)
            if (e.isExam() && !d.isBefore(e.start().minusDays(LEAD_DAYS)) && !d.isAfter(e.end())) return true;
        return false;
    }

    /** Empty unless at least {@link #MIN_DAYS} check-ins fall in exam time and at least that many outside it. */
    static Optional<Comparison> compare(List<CheckIn> checkIns, List<AcademicEvent> events, LocalDate today) {
        double exam = 0, other = 0;
        int ne = 0, no = 0;
        for (CheckIn c : checkIns) {
            if (c.date().isAfter(today)) continue;
            if (inExamTime(c.date(), events)) { exam += c.stress(); ne++; }
            else { other += c.stress(); no++; }
        }
        if (ne < MIN_DAYS || no < MIN_DAYS) return Optional.empty();
        return Optional.of(new Comparison(ne, exam / ne, no, other / no));
    }

    /** Each exam period that has started, newest first, where the student has at least 2 check-ins in its window. */
    static List<PerEvent> perEvent(List<CheckIn> checkIns, List<AcademicEvent> events, LocalDate today) {
        List<PerEvent> out = new ArrayList<>();
        for (AcademicEvent e : events) {
            if (!e.isExam() || e.start().isAfter(today)) continue;
            LocalDate from = e.start().minusDays(LEAD_DAYS);
            int n = 0;
            double sum = 0;
            for (CheckIn c : checkIns)
                if (!c.date().isBefore(from) && !c.date().isAfter(e.end()) && !c.date().isAfter(today)) { n++; sum += c.stress(); }
            if (n >= 2) out.add(new PerEvent(e, n, sum / n));
        }
        out.sort(Comparator.comparing((PerEvent p) -> p.event().start()).reversed());
        return out;
    }

    /** Questions for the student to think about. Not stored and not shared. */
    static List<String> prompts(Optional<Comparison> c) {
        List<String> q = new ArrayList<>();
        if (c.isPresent() && c.get().direction().equals("higher"))
            q.add("Your stress looks higher around exams. Which part feels heaviest: the workload, the time, or the results?");
        else if (c.isPresent() && c.get().direction().equals("lower"))
            q.add("Your stress looks no higher around exams. What have you been doing that seems to help?");
        else
            q.add("How do you usually feel in the week before an exam? What do you notice first?");
        q.add("What helped last time a busy school period ended? Could you do that again?");
        q.add("What is one thing you could move or let go of during the next exam period?");
        return q;
    }
}
