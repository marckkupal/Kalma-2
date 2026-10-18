import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wellness Summary Export (docs/WELLNESS_EXPORT.md). Builds one student's chosen wellness information into a
 * {@link Report}, then writes it as a printable PDF, a CSV file, or plain text (the text is what a counselor sees
 * after the student agrees to share). Nothing here reads other students' records, and nothing is sent anywhere:
 * sharing goes through DataStore.shareSummary, which refuses without consent.
 *
 * <p>The same gentle rules as the Summary page apply: days without a check-in are left out (never filled in), averages
 * use only days that have a check-in, and the wording never judges a gap or a change. The student's private check-in
 * notes are left out unless the student switches them on.
 */
final class WellnessExport {
    private WellnessExport() {}

    /** How far back to look, in days. 0 means everything the student has saved. */
    static final int ALL_TIME = 0;
    static final String NOTE = "This summary comes from the student's own check-ins and assessments. Ups and downs are "
            + "a normal part of student life. It is a self-reflection summary, not a diagnosis.";

    /** What the student chose to include. */
    record Options(int days, boolean checkIns, boolean assessments, boolean trends, boolean notes) {
        boolean any() { return checkIns || assessments || trends; }
        String periodLabel() {
            return days == ALL_TIME ? "All saved records" : "Last " + days + " days";
        }
        /** The sections included, in plain words, for the consent screen. */
        String sectionsLabel() {
            List<String> l = new ArrayList<>();
            if (checkIns) l.add("check-in records" + (notes ? " with your notes" : " (without your notes)"));
            if (assessments) l.add("assessment summaries");
            if (trends) l.add("wellness trends");
            return l.isEmpty() ? "nothing" : String.join(", ", l);
        }
    }

    /** One block of up to 7 days in the trend table. */
    record Week(LocalDate start, LocalDate end, int checkInDays, Double avgMood, Double avgStress) {}

    /**
     * @param avgMood average mood 1 to 5 over the period, or null with no check-ins
     * @param change gentle comparison of the last week with the first week, or null when there is not enough to compare
     */
    record Trend(int checkInDays, Double avgMood, Double avgStress, LocalDate bestDay, List<Week> weeks, String change) {}

    record Report(String studentName, String course, LocalDate from, LocalDate to, LocalDate generatedOn,
                  Options options, List<CheckIn> checkIns, List<AssessmentResult> assessments, Trend trend) {
        int recordCount() {
            return (options.checkIns() ? checkIns.size() : 0) + (options.assessments() ? assessments.size() : 0)
                    + (options.trends() && trend != null ? trend.checkInDays() : 0);
        }
        /** True when the chosen sections hold nothing at all. */
        boolean isEmpty() {
            return (!options.checkIns() || checkIns.isEmpty()) && (!options.assessments() || assessments.isEmpty())
                    && (!options.trends() || trend == null || trend.checkInDays() == 0);
        }
    }

    // ----- building -----
    static Report build(String name, String course, List<CheckIn> allCheckIns, List<AssessmentResult> allResults,
                        Options o, LocalDate today) {
        List<CheckIn> ci = allCheckIns.stream().filter(c -> !c.date().isAfter(today)).collect(Collectors.toList());
        List<AssessmentResult> as = allResults.stream().filter(r -> !r.date().isAfter(today)).collect(Collectors.toList());
        LocalDate from;
        if (o.days() == ALL_TIME) {
            LocalDate m = today;
            for (CheckIn c : ci) if (c.date().isBefore(m)) m = c.date();
            for (AssessmentResult r : as) if (r.date().isBefore(m)) m = r.date();
            from = m;
        } else {
            from = today.minusDays(o.days() - 1L);
        }
        final LocalDate f = from;
        List<CheckIn> sci = ci.stream().filter(c -> !c.date().isBefore(f)).sorted(Comparator.comparing(CheckIn::date))
                .collect(Collectors.toList());
        List<AssessmentResult> sas = as.stream().filter(r -> !r.date().isBefore(f)).sorted(Comparator.comparing(AssessmentResult::date))
                .collect(Collectors.toList());
        Trend trend = o.trends() ? trend(sci, from, today) : null;
        return new Report(name, course, from, today, today, o, sci, sas, trend);
    }

    static Trend trend(List<CheckIn> inRange, LocalDate from, LocalDate to) {
        Double mood = avg(inRange, true), stress = avg(inRange, false);
        LocalDate best = inRange.stream().max(Comparator.comparingInt(CheckIn::mood).thenComparing(CheckIn::date))
                .map(CheckIn::date).orElse(null);
        List<Week> weeks = new ArrayList<>();
        for (LocalDate s = from; !s.isAfter(to); s = s.plusDays(7)) {
            LocalDate e = s.plusDays(6).isAfter(to) ? to : s.plusDays(6);
            final LocalDate fs = s, fe = e;
            List<CheckIn> w = inRange.stream().filter(c -> !c.date().isBefore(fs) && !c.date().isAfter(fe)).collect(Collectors.toList());
            weeks.add(new Week(s, e, w.size(), avg(w, true), avg(w, false)));
        }
        String change = null;
        if (weeks.size() >= 2) {
            Week a = weeks.get(0), b = weeks.get(weeks.size() - 1);
            if (a.checkInDays() >= WellnessSummary.MIN_FOR_COMPARISON && b.checkInDays() >= WellnessSummary.MIN_FOR_COMPARISON) {
                change = "Compared with the first week, the latest week shows mood that is "
                        + WellnessSummary.moodChange(b.avgMood(), a.avgMood()) + " and stress that is "
                        + WellnessSummary.stressChange(b.avgStress(), a.avgStress()) + ".";
            }
        }
        return new Trend(inRange.size(), mood, stress, best, weeks, change);
    }

    private static Double avg(List<CheckIn> l, boolean mood) {
        if (l.isEmpty()) return null;
        return l.stream().mapToInt(mood ? CheckIn::mood : CheckIn::stress).average().orElse(0);
    }

    // ----- shared wording -----
    static String moodWord(int m) { return Kalma.MOODS[m - 1].substring(4); }
    static String stressWord(int s) { return CheckInView.STRESS[s - 1]; }
    static String f1(Double d) { return d == null ? "" : String.format(Locale.ROOT, "%.1f", d); }
    static String range(Report r) { return r.from().format(StudentUI.DAY) + " to " + r.to().format(StudentUI.DAY); }

    static String typeOf(AssessmentResult r) {
        return r.profile() == null ? "" : r.profile().typeLabel();
    }

    static String sourcesOf(AssessmentResult r) {
        return r.profile() == null || r.profile().categories().isEmpty() ? "" : String.join("; ", r.profile().categories());
    }

    static String fileName(LocalDate today, String ext) { return "kalma-wellness-summary-" + today + "." + ext; }

    // ----- CSV -----
    /** Blocks separated by a blank line; each has a title row and a header row. UTF-8 with a BOM so Excel reads it. */
    static byte[] toCsv(Report r) {
        StringBuilder b = new StringBuilder("\uFEFF");
        line(b, "Kalma Wellness Summary");
        line(b, "Student", r.studentName());
        line(b, "Course", r.course());
        line(b, "Period", range(r));
        line(b, "Included", r.options().sectionsLabel());
        line(b, "Created on", r.generatedOn().toString());
        line(b, "Note", NOTE);
        if (r.options().checkIns()) {
            blank(b);
            line(b, "CHECK-IN RECORDS");
            if (r.options().notes()) line(b, "Date", "Mood (1-5)", "Mood", "Stress (1-5)", "Stress", "Note");
            else line(b, "Date", "Mood (1-5)", "Mood", "Stress (1-5)", "Stress");
            for (CheckIn c : r.checkIns()) {
                if (r.options().notes())
                    line(b, c.date().toString(), "" + c.mood(), moodWord(c.mood()), "" + c.stress(), stressWord(c.stress()), c.note());
                else line(b, c.date().toString(), "" + c.mood(), moodWord(c.mood()), "" + c.stress(), stressWord(c.stress()));
            }
        }
        if (r.options().assessments()) {
            blank(b);
            line(b, "ASSESSMENT SUMMARIES");
            line(b, "Date", "Overall %", "Level", "Academic %", "Social %", "Personal %", "Stress type", "Main sources");
            for (AssessmentResult a : r.assessments())
                line(b, a.date().toString(), "" + a.overall(), AssessmentService.classify(a.overall()).label(),
                        "" + a.academic(), "" + a.social(), "" + a.personal(), typeOf(a), sourcesOf(a));
        }
        if (r.options().trends() && r.trend() != null) {
            Trend t = r.trend();
            blank(b);
            line(b, "WELLNESS TRENDS");
            line(b, "Check-in days", "" + t.checkInDays());
            line(b, "Average mood (1-5)", f1(t.avgMood()));
            line(b, "Average stress (1-5)", f1(t.avgStress()));
            line(b, "Brightest day", t.bestDay() == null ? "" : t.bestDay().toString());
            if (t.change() != null) line(b, "Change", t.change());
            blank(b);
            line(b, "Week starting", "Week ending", "Check-in days", "Average mood", "Average stress");
            for (Week w : t.weeks())
                line(b, w.start().toString(), w.end().toString(), "" + w.checkInDays(), f1(w.avgMood()), f1(w.avgStress()));
        }
        return b.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void blank(StringBuilder b) { b.append("\r\n"); }

    private static void line(StringBuilder b, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) b.append(',');
            b.append(csv(cells[i]));
        }
        b.append("\r\n");
    }

    /**
     * Quotes a cell when needed. A cell that starts with = + - @ (or a tab or return) could be run as a formula when
     * the file is opened in a spreadsheet, so a leading apostrophe is added. Notes are free text, so this matters.
     */
    static String csv(String s) {
        if (s == null) return "";
        String v = s;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r"))
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    // ----- plain text (preview, and what a counselor reads after consent) -----
    static String toText(Report r) {
        StringBuilder b = new StringBuilder();
        b.append("KALMA WELLNESS SUMMARY\n");
        b.append("Student: ").append(r.studentName()).append("\n");
        if (!r.course().isEmpty()) b.append("Course: ").append(r.course()).append("\n");
        b.append("Period: ").append(range(r)).append("\n");
        b.append("Included: ").append(r.options().sectionsLabel()).append("\n");
        b.append("Created on: ").append(r.generatedOn().format(StudentUI.DAY)).append("\n\n");
        if (r.options().trends() && r.trend() != null) {
            Trend t = r.trend();
            b.append("WELLNESS TRENDS\n");
            if (t.checkInDays() == 0) b.append("No check-ins in this period, and that is okay.\n");
            else {
                b.append("Check-in days: ").append(t.checkInDays()).append("\n");
                b.append("Average mood: ").append(f1(t.avgMood())).append(" / 5\n");
                b.append("Average stress: ").append(f1(t.avgStress())).append(" / 5\n");
                if (t.bestDay() != null) b.append("Brightest day: ").append(t.bestDay().format(StudentUI.DAY)).append("\n");
                if (t.change() != null) b.append(t.change()).append("\n");
                if (t.weeks().size() > 1) {
                    b.append("\nBy week (mood / stress, days with a check-in):\n");
                    for (Week w : t.weeks())
                        b.append("  ").append(w.start().format(StudentUI.SHORT_DAY)).append(" to ").append(w.end().format(StudentUI.SHORT_DAY))
                                .append(": ").append(w.checkInDays() == 0 ? "no check-ins" : f1(w.avgMood()) + " / " + f1(w.avgStress())
                                        + " (" + w.checkInDays() + " day" + (w.checkInDays() == 1 ? "" : "s") + ")").append("\n");
                }
            }
            b.append("\n");
        }
        if (r.options().checkIns()) {
            b.append("CHECK-IN RECORDS\n");
            if (r.checkIns().isEmpty()) b.append("No check-ins in this period, and that is okay.\n");
            for (CheckIn c : r.checkIns()) {
                b.append(c.date().format(StudentUI.DAY)).append(": mood ").append(c.mood()).append(" (").append(moodWord(c.mood()))
                        .append("), stress ").append(c.stress()).append(" (").append(stressWord(c.stress())).append(")");
                if (r.options().notes() && !c.note().isBlank()) b.append(" - ").append(c.note().replace('\n', ' '));
                b.append("\n");
            }
            b.append("\n");
        }
        if (r.options().assessments()) {
            b.append("ASSESSMENT SUMMARIES\n");
            if (r.assessments().isEmpty()) b.append("No assessments in this period.\n");
            for (AssessmentResult a : r.assessments()) {
                b.append(a.date().format(StudentUI.DAY)).append(": overall ").append(a.overall()).append("% (")
                        .append(AssessmentService.classify(a.overall()).label()).append("); academic ").append(a.academic())
                        .append("%, social ").append(a.social()).append("%, personal ").append(a.personal()).append("%");
                if (!typeOf(a).isEmpty()) b.append("; ").append(typeOf(a));
                if (!sourcesOf(a).isEmpty()) b.append("; main sources: ").append(sourcesOf(a));
                b.append("\n");
            }
            b.append("\n");
        }
        b.append(NOTE).append("\n");
        return b.toString();
    }

    // ----- PDF -----
    static byte[] toPdf(Report r) {
        PdfWriter p = new PdfWriter("Kalma Wellness Summary  |  " + r.studentName() + "  |  Private and confidential");
        p.heading("Kalma Wellness Summary", 22);
        p.text(r.studentName() + (r.course().isEmpty() ? "" : "  |  " + r.course()), 11, true, 0);
        p.muted("Period: " + range(r), 10);
        p.muted("Included: " + r.options().sectionsLabel(), 10);
        p.muted("Created on " + r.generatedOn().format(StudentUI.DAY), 10);
        p.space(4);
        p.text(NOTE, 9, false, 0);

        if (r.options().trends() && r.trend() != null) {
            Trend t = r.trend();
            p.section("Wellness trends");
            if (t.checkInDays() == 0) p.text("No check-ins in this period, and that is okay.", 10, false, 0);
            else {
                p.text("Check-in days: " + t.checkInDays() + "     Average mood: " + f1(t.avgMood()) + " / 5     Average stress: "
                        + f1(t.avgStress()) + " / 5", 10, false, 0);
                if (t.bestDay() != null) p.text("Brightest day: " + t.bestDay().format(StudentUI.DAY), 10, false, 0);
                if (t.change() != null) p.text(t.change(), 10, false, 0);
                p.space(6);
                chartOf(p, r);
                if (t.weeks().size() > 1) {
                    p.space(4);
                    double[] xs = {0, 150, 250, 340}, ws = {145, 95, 85, 100};
                    p.row(xs, ws, new String[]{"Week", "Check-in days", "Avg mood", "Avg stress"}, 9, true);
                    for (Week w : t.weeks())
                        p.row(xs, ws, new String[]{w.start().format(StudentUI.SHORT_DAY) + " to " + w.end().format(StudentUI.SHORT_DAY),
                                "" + w.checkInDays(), f1(w.avgMood()).isEmpty() ? "-" : f1(w.avgMood()),
                                f1(w.avgStress()).isEmpty() ? "-" : f1(w.avgStress())}, 9, false);
                }
            }
        }
        if (r.options().checkIns()) {
            p.section("Check-in records");
            if (r.checkIns().isEmpty()) p.text("No check-ins in this period, and that is okay.", 10, false, 0);
            else {
                boolean n = r.options().notes();
                double[] xs = n ? new double[]{0, 85, 175, 255} : new double[]{0, 110, 240, 360};
                double[] ws = n ? new double[]{80, 85, 75, 245} : new double[]{105, 125, 115, 100};
                p.row(xs, ws, n ? new String[]{"Date", "Mood", "Stress", "Note"} : new String[]{"Date", "Mood", "Stress"}, 9, true);
                for (CheckIn c : r.checkIns()) {
                    String[] cells = n
                            ? new String[]{c.date().format(StudentUI.DAY), c.mood() + " " + moodWord(c.mood()), c.stress() + " " + stressWord(c.stress()), c.note()}
                            : new String[]{c.date().format(StudentUI.DAY), c.mood() + " " + moodWord(c.mood()), c.stress() + " " + stressWord(c.stress())};
                    p.row(xs, ws, cells, 9, false);
                }
            }
        }
        if (r.options().assessments()) {
            p.section("Assessment summaries");
            if (r.assessments().isEmpty()) p.text("No assessments in this period.", 10, false, 0);
            for (AssessmentResult a : r.assessments()) {
                p.text(a.date().format(StudentUI.DAY) + "  -  " + AssessmentService.classify(a.overall()).label(), 10, true, 0);
                p.text("Overall " + a.overall() + "%   Academic " + a.academic() + "%   Social " + a.social() + "%   Personal " + a.personal() + "%", 9, false, 8);
                if (!typeOf(a).isEmpty()) p.text(typeOf(a) + (sourcesOf(a).isEmpty() ? "" : "; main sources: " + sourcesOf(a)), 9, false, 8);
                p.space(4);
            }
            p.muted("Scores are percentages and can change from one time to the next. They are not grades.", 8);
        }
        return p.toBytes();
    }

    /** One point per calendar day (null where there is no check-in), capped to the last 90 days so it stays readable. */
    private static void chartOf(PdfWriter p, Report r) {
        LocalDate from = r.from();
        long span = ChronoUnit.DAYS.between(from, r.to()) + 1;
        if (span > 90) { from = r.to().minusDays(89); span = 90; }
        int n = (int) span;
        Double[] mood = new Double[n], stress = new Double[n];
        Map<LocalDate, CheckIn> by = new HashMap<>();
        for (CheckIn c : r.checkIns()) by.put(c.date(), c);
        for (int i = 0; i < n; i++) {
            CheckIn c = by.get(from.plusDays(i));
            if (c != null) { mood[i] = (double) c.mood(); stress[i] = (double) c.stress(); }
        }
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < n; i++) labels.add(from.plusDays(i).format(StudentUI.SHORT_DAY));
        p.chart(labels, mood, stress, 110);
    }
}
