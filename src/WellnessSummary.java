import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Weekly and monthly wellness summaries from a student's own check-ins (docs/WELLNESS_DASHBOARD.md).
 * The wording is deliberately gentle: it never says a check-in was "missed", never scolds, treats ups and downs as
 * normal, and says "not enough to compare yet" instead of guessing. Days without a check-in are never filled in.
 * WellnessSummaryTest checks the numbers and the wording rules.
 */
final class WellnessSummary {
    /** Averages closer together than this are called "about the same". */
    static final double SAME = 0.3;
    /** A comparison needs at least this many check-in days in both periods. */
    static final int MIN_FOR_COMPARISON = 2;

    private WellnessSummary() {}

    /**
     * @param days days in the period (7 for a week, 30 for a month), ending today
     * @param checkInDays how many days in the period have a check-in
     * @param avgMood average mood 1 to 5, or null when there are no check-ins
     * @param avgStress average stress 1 to 5, or null when there are no check-ins
     * @param bestDay the day with the highest mood (the latest one if tied), or null
     * @param prevMood average mood in the period before, or null when it is too small to compare
     * @param prevStress average stress in the period before, or null when it is too small to compare
     * @param message one or two encouraging sentences about the period
     */
    record Result(int days, int checkInDays, Double avgMood, Double avgStress, LocalDate bestDay,
                  Double prevMood, Double prevStress, String message) {
        boolean canCompare() { return prevMood != null && prevStress != null && avgMood != null; }
    }

    static String periodName(int days) { return days <= 7 ? "week" : "month"; }

    static Result summarize(List<CheckIn> all, int days, LocalDate today) {
        List<CheckIn> now = within(all, today.minusDays(days - 1), today);
        List<CheckIn> before = within(all, today.minusDays(2L * days - 1), today.minusDays(days));
        Double mood = avg(now, true), stress = avg(now, false);
        LocalDate best = now.stream().max(Comparator.comparingInt(CheckIn::mood).thenComparing(CheckIn::date))
                .map(CheckIn::date).orElse(null);
        boolean compare = now.size() >= MIN_FOR_COMPARISON && before.size() >= MIN_FOR_COMPARISON;
        Double pm = compare ? avg(before, true) : null, ps = compare ? avg(before, false) : null;
        return new Result(days, now.size(), mood, stress, best, pm, ps, message(periodName(days), now.size(), mood, stress, pm, ps));
    }

    private static List<CheckIn> within(List<CheckIn> all, LocalDate from, LocalDate to) {
        return all.stream().filter(c -> !c.date().isBefore(from) && !c.date().isAfter(to)).collect(Collectors.toList());
    }

    private static Double avg(List<CheckIn> l, boolean mood) {
        if (l.isEmpty()) return null;
        return l.stream().mapToInt(mood ? CheckIn::mood : CheckIn::stress).average().orElse(0);
    }

    /** "about the same", or how the new average sits against the old one. For mood, higher is better. */
    static String moodChange(double now, double before) {
        double d = now - before;
        return Math.abs(d) < SAME ? "about the same" : d > 0 ? "a little higher" : "a little lower";
    }

    static String stressChange(double now, double before) {
        double d = now - before;
        return Math.abs(d) < SAME ? "about the same" : d > 0 ? "a little higher" : "a little lower";
    }

    /** The encouraging text. No guilt for few or no check-ins, and no alarm when stress or mood shifts. */
    static String message(String period, int checkInDays, Double mood, Double stress, Double prevMood, Double prevStress) {
        if (checkInDays == 0) {
            return "There are no check-ins this " + period + ", and that is completely okay. "
                    + "Whenever you feel like it, a check-in takes less than a minute.";
        }
        String thanks = "Thank you for taking time to notice how you feel"
                + (checkInDays == 1 ? " on 1 day this " : " on " + checkInDays + " days this ") + period + ". ";
        if (prevMood == null || prevStress == null) {
            return thanks + "Every check-in helps you see your own patterns. "
                    + "A comparison with earlier " + period + "s will appear once there are a few more.";
        }
        double dm = mood - prevMood, ds = stress - prevStress;
        boolean stressUp = ds >= SAME, stressDown = ds <= -SAME, moodUp = dm >= SAME, moodDown = dm <= -SAME;
        if (stressUp || moodDown) {
            return thanks + "Things felt a little heavier than the " + period + " before. "
                    + "That is normal, especially around busy school dates, and it does not mean you are doing anything wrong. "
                    + "Be kind to yourself. A breathing break or a chat with someone you trust may help.";
        }
        if (stressDown || moodUp) {
            return thanks + "Things look a little lighter than the " + period + " before. "
                    + "Nice work looking after yourself. Keep doing what helps.";
        }
        return thanks + "Things look steady compared with the " + period + " before. "
                + "Steady is good, and you can keep going at your own pace.";
    }
}
