import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Wellness Streaks (docs/WELLNESS_STREAKS.md): a gentle look at how often a student checks in.
 * It rewards showing up, never punishes a gap. There is no countdown, no "streak lost", no zero to stare at:
 * a day without a check-in is simply left blank, a returning student is welcomed back, and the milestones are
 * based on total check-in days so they can never be taken away. A run of days in a row is only mentioned when it is
 * 2 or more, and today is never counted against anyone. WellnessStreakTest checks the numbers and the wording.
 */
final class WellnessStreak {
    /** Total check-in days that earn a milestone. These only ever go up. */
    static final int[] MILESTONES = {1, 3, 7, 14, 30, 60, 100};

    private WellnessStreak() {}

    /**
     * @param totalDays all days the student has checked in, ever
     * @param daysThisWeek check-in days in the last 7 days (today included)
     * @param daysLastWeek check-in days in the 7 days before that
     * @param daysThisMonth check-in days in the last 30 days
     * @param currentRun days in a row ending today, or ending yesterday when today has no check-in yet; 0 if none
     * @param bestRun the longest run of days in a row, ever
     * @param checkedInToday whether there is a check-in today
     * @param last7 one flag per day for the last 7 days, oldest first, today last
     * @param reached how many milestones are reached
     * @param nextMilestone the next total to aim for, or 0 when all are reached
     * @param message one or two supportive sentences
     * @param progress a gentle note about this week compared with last week, or "" when there is nothing kind to say
     */
    record Result(int totalDays, int daysThisWeek, int daysLastWeek, int daysThisMonth, int currentRun, int bestRun,
                  boolean checkedInToday, boolean[] last7, int reached, int nextMilestone, String message, String progress) {}

    static Result of(List<CheckIn> all, LocalDate today) {
        TreeSet<LocalDate> days = all.stream().map(CheckIn::date).filter(d -> !d.isAfter(today))
                .collect(Collectors.toCollection(TreeSet::new));
        boolean todayDone = days.contains(today);
        int run = runEndingAt(days, todayDone ? today : today.minusDays(1));
        int best = 0, cur = 0;
        LocalDate prev = null;
        for (LocalDate d : days) {
            cur = prev != null && prev.plusDays(1).equals(d) ? cur + 1 : 1;
            best = Math.max(best, cur);
            prev = d;
        }
        boolean[] last7 = new boolean[7];
        for (int i = 0; i < 7; i++) last7[i] = days.contains(today.minusDays(6 - i));
        int week = count(days, today.minusDays(6), today), before = count(days, today.minusDays(13), today.minusDays(7));
        int month = count(days, today.minusDays(29), today);
        int total = days.size();
        int reached = 0, next = 0;
        for (int m : MILESTONES) { if (total >= m) reached++; else if (next == 0) next = m; }
        LocalDate lastDay = days.isEmpty() ? null : days.last();
        return new Result(total, week, before, month, run, best, todayDone, last7, reached, next,
                message(total, todayDone, run, lastDay, today), progress(week, before));
    }

    private static int runEndingAt(TreeSet<LocalDate> days, LocalDate end) {
        int n = 0;
        for (LocalDate d = end; days.contains(d); d = d.minusDays(1)) n++;
        return n;
    }

    private static int count(TreeSet<LocalDate> days, LocalDate from, LocalDate to) {
        return days.subSet(from, true, to, true).size();
    }

    /** Supportive text. Never mentions how long it has been, never says anything was missed or lost. */
    static String message(int total, boolean todayDone, int run, LocalDate lastDay, LocalDate today) {
        if (total == 0) {
            return "Your first check-in starts your own routine. There is no pressure, and it takes less than a minute.";
        }
        if (todayDone) {
            if (run >= 7) return "You have checked in " + run + " days in a row. That is a lovely routine. "
                    + "Rest days are part of a healthy routine too, so go at your own pace.";
            if (run >= 2) return "Nice, you have checked in " + run + " days in a row. "
                    + "Small, steady moments of noticing how you feel add up.";
            return "Thank you for checking in today. Taking a moment to notice how you feel is a healthy habit.";
        }
        if (lastDay != null && !lastDay.isBefore(today.minusDays(1))) {
            return "Thanks for checking in yesterday. Whenever you feel ready today, a check-in takes less than a minute.";
        }
        return "Welcome back. Every check-in counts, and you can pick up right where you are. "
                + "You have checked in on " + total + (total == 1 ? " day" : " days") + " so far.";
    }

    /** Only warm or neutral: more days than last week is celebrated, the same is called steady, fewer says nothing. */
    static String progress(int thisWeek, int lastWeek) {
        if (thisWeek == 0 || lastWeek == 0) return "";
        if (thisWeek > lastWeek) return "You have checked in on more days than the week before. Lovely progress.";
        if (thisWeek == lastWeek) return "About the same pace as the week before. Steady is good.";
        return "";
    }

    static String milestoneName(int total) {
        return total == 1 ? "First check-in" : total + " check-in days";
    }
}
