import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

/**
 * Checks the wellness export (docs/WELLNESS_EXPORT.md): what goes into the CSV and PDF, that private notes stay out
 * unless chosen, that sharing is refused without consent, who can see a shared summary, and that stopping works.
 * No test framework needed. Run it from an empty scratch folder so it does not touch real data.
 *   java WellnessExportTest            runs all checks (writes a test share to ./data/shared-summaries.tsv)
 *   java WellnessExportTest reload     second run: confirms the saved shares were loaded again
 */
public class WellnessExportTest {
    private static int failures = 0, checks = 0;

    static void check(String name, boolean ok) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL  " + name); }
    }

    static final LocalDate T = LocalDate.of(2026, 10, 17);
    static CheckIn ci(int daysAgo, int mood, int stress, String note) { return new CheckIn("u", T.minusDays(daysAgo), mood, stress, note); }
    static WellnessExport.Options opts(int days, boolean c, boolean a, boolean t, boolean n) { return new WellnessExport.Options(days, c, a, t, n); }
    static String csv(WellnessExport.Report r) { return new String(WellnessExport.toCsv(r), StandardCharsets.UTF_8); }
    static String pdf(WellnessExport.Report r) { return new String(WellnessExport.toPdf(r), StandardCharsets.ISO_8859_1); }

    static final String[] GUILT = {"missed", "failed", "forgot", "behind", "should have", "streak", "worse", "bad ", "warning", "must", "lazy", "disappoint"};
    static boolean gentle(String text) {
        String t = text.toLowerCase();
        for (String g : GUILT) if (t.contains(g)) return false;
        return true;
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("reload")) { reload(); return; }

        List<CheckIn> data = List.of(ci(0, 4, 2, "Good day, finished my project"), ci(1, 2, 4, "=SUM(1+1) stressed, \"quoted\""),
                ci(3, 5, 1, ""), ci(9, 3, 3, "line one\nline two"), ci(40, 3, 3, "old note"));
        AssessmentResult a1 = new AssessmentResult("u", T.minusDays(5), 56, 33, 28, 39, "v1");
        AssessmentResult a2 = new AssessmentResult("u", T.minusDays(60), 61, 39, 33, 44, "v1");
        List<AssessmentResult> results = List.of(a1, a2);

        // ----- choosing the period and sections -----
        WellnessExport.Report w = WellnessExport.build("Maria Santos", "BSCS 3rd Year", data, results, opts(7, true, true, true, false), T);
        check("7 days keeps check-ins from day 0 to day 6", w.checkIns().size() == 3);
        check("older check-ins are not included", w.checkIns().stream().noneMatch(c -> c.date().isBefore(T.minusDays(6))));
        check("only recent assessments in 7 days", w.assessments().size() == 1);
        WellnessExport.Report all = WellnessExport.build("Maria Santos", "BSCS 3rd Year", data, results, opts(WellnessExport.ALL_TIME, true, true, true, false), T);
        check("all saved records covers everything", all.checkIns().size() == 5 && all.assessments().size() == 2);
        check("all saved records starts at the first record", all.from().equals(T.minusDays(60)));
        check("a check-in dated tomorrow is ignored", WellnessExport.build("x", "", List.of(new CheckIn("u", T.plusDays(1), 5, 1)), List.of(), opts(7, true, true, true, false), T).checkIns().isEmpty());
        check("nothing chosen is detected", !opts(7, false, false, false, false).any());
        check("an empty period is detected", WellnessExport.build("x", "", List.of(), List.of(), opts(30, true, true, true, false), T).isEmpty());
        check("trends alone count as content only with check-ins", WellnessExport.build("x", "", List.of(), List.of(a1), opts(30, false, false, true, false), T).isEmpty());

        // ----- CSV -----
        String c = csv(w);
        check("CSV starts with a BOM so Excel reads UTF-8", c.startsWith("\uFEFF"));
        check("CSV uses CRLF line endings", c.contains("\r\n") && !c.replace("\r\n", "").contains("\n"));
        check("CSV has all three sections", c.contains("CHECK-IN RECORDS") && c.contains("ASSESSMENT SUMMARIES") && c.contains("WELLNESS TRENDS"));
        check("CSV names the student and period", c.contains("Maria Santos") && c.contains("BSCS 3rd Year") && c.contains("Oct 11, 2026 to Oct 17, 2026"));
        check("CSV rows have date, mood and stress", c.contains("2026-10-17,4,Good,2,Low") && c.contains("2026-10-14,5,Very Good,1,Very Low"));
        check("CSV leaves private notes out by default", !c.contains("finished my project") && !c.contains("Note\r\n") && !c.contains(",Note"));
        check("CSV leaves out check-ins outside the period", !c.contains("2026-10-08") && !c.contains("old note"));
        check("CSV assessment row", c.contains("2026-10-12,39," + AssessmentService.classify(39).label() + ",56,33,28"));
        check("CSV weekly table", c.contains("Week starting,Week ending,Check-in days,Average mood,Average stress"));
        WellnessExport.Report withNotes = WellnessExport.build("Maria Santos", "", data, results, opts(7, true, false, false, true), T);
        String cn = csv(withNotes);
        check("CSV includes notes only when chosen", cn.contains("Good day, finished my project".replace(",", ",")) || cn.contains("\"Good day, finished my project\""));
        check("a comma in a note is quoted", cn.contains("\"Good day, finished my project\""));
        check("a quote in a note is doubled", cn.contains("\"\"quoted\"\""));
        check("a note starting with = cannot run as a formula", cn.contains("'=SUM(1+1)") && !cn.contains(",=SUM") && !cn.contains(",\"=SUM"));
        check("a note with a line break stays in one quoted cell", WellnessExport.csv("line one\nline two").equals("\"line one\nline two\""));
        check("only chosen sections appear", !cn.contains("ASSESSMENT SUMMARIES") && !cn.contains("WELLNESS TRENDS") && cn.contains("CHECK-IN RECORDS"));
        check("csv() guards + - @ and tab", WellnessExport.csv("+1").equals("'+1") && WellnessExport.csv("-2").equals("'-2")
                && WellnessExport.csv("@x").equals("'@x") && WellnessExport.csv("\tx").startsWith("'"));
        check("csv() leaves normal text alone", WellnessExport.csv("hello").equals("hello") && WellnessExport.csv(null).isEmpty());
        check("notes option is ignored when check-ins are not chosen", !csv(WellnessExport.build("x", "", data, results, opts(7, false, true, false, true), T)).contains("finished my project"));

        // ----- trends -----
        List<CheckIn> month = new ArrayList<>();
        for (int i = 0; i < 28; i++) if (i % 2 == 0) month.add(ci(i, i < 10 ? 4 : 2, i < 10 ? 2 : 4, ""));
        WellnessExport.Trend t = WellnessExport.build("x", "", month, List.of(), opts(28, false, false, true, false), T).trend();
        check("28 days make 4 weekly rows", t.weeks().size() == 4);
        check("check-in days are counted without filling gaps", t.checkInDays() == 14 && t.weeks().stream().mapToInt(WellnessExport.Week::checkInDays).sum() == 14);
        check("brightest day is the highest mood, latest if tied", t.bestDay().equals(T));
        check("first and last week are compared when both have 2 days", t.change() != null && t.change().contains("mood that is") && t.change().contains("stress that is"));
        WellnessExport.Trend thin = WellnessExport.build("x", "", List.of(ci(0, 3, 3, ""), ci(20, 3, 3, "")), List.of(), opts(28, false, false, true, false), T).trend();
        check("no comparison when a week has fewer than 2 check-in days", thin.change() == null);
        WellnessExport.Trend none = WellnessExport.build("x", "", List.of(), List.of(), opts(7, false, false, true, false), T).trend();
        check("no check-ins: no averages", none.avgMood() == null && none.avgStress() == null && none.bestDay() == null);

        // ----- plain text (preview and what a counselor reads) -----
        String txt = WellnessExport.toText(w);
        check("text names the student", txt.contains("Student: Maria Santos"));
        check("text leaves notes out by default", !txt.contains("finished my project"));
        check("text includes notes when chosen", WellnessExport.toText(withNotes).contains("finished my project"));
        check("text says it is not a diagnosis", txt.contains("not a diagnosis"));
        for (WellnessExport.Report r : List.of(w, all, withNotes, WellnessExport.build("x", "", List.of(), List.of(), opts(7, true, true, true, false), T)))
            check("gentle wording in the text", gentle(WellnessExport.toText(r)));
        check("empty period says that is okay", WellnessExport.toText(WellnessExport.build("x", "", List.of(), List.of(), opts(7, true, true, true, false), T)).contains("that is okay"));

        // ----- PDF -----
        byte[] bytes = WellnessExport.toPdf(w);
        String p = pdf(w);
        check("PDF has the header and ends with %%EOF", p.startsWith("%PDF-1.4") && p.trim().endsWith("%%EOF"));
        check("PDF shows the student and sections", p.contains("Maria Santos") && p.contains("Wellness trends") && p.contains("Check-in records") && p.contains("Assessment summaries"));
        check("PDF leaves notes out by default", !p.contains("finished my project"));
        check("PDF includes notes when chosen", pdf(WellnessExport.build("Maria Santos", "", data, results, opts(7, true, false, false, true), T)).contains("finished my project"));
        check("PDF cross-reference table points at every object", xrefOk(bytes));
        check("PDF page count matches /Count", pageCount(p) == countPages(p) && pageCount(p) >= 1);
        List<CheckIn> many = new ArrayList<>();
        for (int i = 0; i < 400; i++) many.add(ci(i, 1 + i % 5, 1 + (i + 2) % 5, "note " + i));
        byte[] big = WellnessExport.toPdf(WellnessExport.build("Maria Santos", "BSCS", many, results, opts(WellnessExport.ALL_TIME, true, true, true, true), T));
        check("a long history runs onto more pages and stays valid", pageCount(new String(big, StandardCharsets.ISO_8859_1)) > 3 && xrefOk(big));
        List<CheckIn> odd = List.of(new CheckIn("u", T, 3, 3, "Café (hot) \\ back\\slash 😀 → ñandú"));
        String op = pdf(WellnessExport.build("Niño Reyes", "", odd, List.of(), opts(7, true, false, false, true), T));
        check("brackets and backslashes in notes are escaped", op.contains("Caf\\351 \\(hot\\) \\\\ back\\\\slash"));
        check("Filipino letters are kept and emoji do not break the file", op.contains("\\361andu") || op.contains("\\361and\\372") || op.contains("and\\372"));
        check("odd text still gives a valid PDF", xrefOk(WellnessExport.toPdf(WellnessExport.build("Niño Reyes", "", odd, List.of(), opts(7, true, true, true, true), T))));
        check("a very long unbroken word cannot run off the page", PdfWriter.wrap("x".repeat(500), 10, 200).stream().allMatch(l -> PdfWriter.width(l, 10) <= 200 + 1e-6));
        check("text wrapping keeps lines inside the width", PdfWriter.wrap("one two three four five six seven eight nine ten ".repeat(8), 10, 150).stream().allMatch(l -> PdfWriter.width(l, 10) <= 150 + 1e-6));
        check("empty report still writes a valid PDF", xrefOk(WellnessExport.toPdf(WellnessExport.build("x", "", List.of(), List.of(), opts(7, true, true, true, false), T))));
        check("file name", WellnessExport.fileName(T, "pdf").equals("kalma-wellness-summary-2026-10-17.pdf"));

        // ----- sharing needs consent -----
        DataStore store = DataStore.getInstance();
        store.seed();
        User maria = store.findByEmail("student@tip.edu.ph").orElseThrow();
        User juan = store.findByEmail("juan@tip.edu.ph").orElseThrow();
        User counselor = store.findByEmail("guidance@tip.edu.ph").orElseThrow();
        User admin = store.findByEmail("admin@tip.edu.ph").orElseThrow();
        WellnessExport.Report mine = WellnessExport.build(maria.getName(), maria.getCourse(), store.checkInsOf(maria.getId()),
                store.resultsOf(maria.getId()), opts(WellnessExport.ALL_TIME, true, true, true, false), LocalDate.now());
        check("the report holds only this student's data", mine.checkIns().size() == store.checkInsOf(maria.getId()).size()
                && mine.assessments().size() == store.resultsOf(maria.getId()).size() && mine.checkIns().stream().allMatch(x -> x.userId().equals(maria.getId()))
                && mine.assessments().stream().allMatch(x -> x.userId().equals(maria.getId())) && !mine.checkIns().isEmpty());
        check("the report text never names another student", !WellnessExport.toText(mine).contains("Juan") && !WellnessExport.toText(mine).contains("Ana Reyes"));

        long messagesBefore = store.messagesFor(counselor.getId()).size();
        boolean refused = false;
        try { store.shareSummary(maria, mine, false); } catch (IllegalStateException e) { refused = true; }
        check("sharing without consent is refused", refused);
        check("a refused share leaves nothing behind", store.sharesBy(maria.getId()).isEmpty() && store.sharesTo(counselor.getId()).isEmpty()
                && store.messagesFor(counselor.getId()).size() == messagesBefore);
        boolean staffRefused = false;
        try { store.shareSummary(counselor, mine, true); } catch (IllegalArgumentException e) { staffRefused = true; }
        check("only students can share", staffRefused);
        boolean emptyRefused = false;
        try { store.shareSummary(maria, WellnessExport.build("x", "", List.of(), List.of(), opts(7, true, true, true, false), LocalDate.now()), true); }
        catch (IllegalArgumentException e) { emptyRefused = true; }
        check("an empty summary cannot be shared", emptyRefused);

        SharedSummary s = store.shareSummary(maria, mine, true);
        check("with consent the share is saved with the consent time and wording version", s.isActive() && s.consentAt() != null
                && s.consentVersion().equals(DataStore.CONSENT_VERSION) && s.studentId().equals(maria.getId()));
        check("the counselor can see it", store.sharesTo(counselor.getId()).size() == 1 && store.sharesTo(counselor.getId()).get(0).id().equals(s.id()));
        check("the counselor is told in the inbox", store.messagesFor(counselor.getId()).size() == messagesBefore + 1);
        check("another student cannot see it", store.sharesTo(juan.getId()).isEmpty() && store.sharesBy(juan.getId()).isEmpty());
        check("the admin cannot see it", store.sharesTo(admin.getId()).isEmpty());
        check("the student can see what they shared", store.sharesBy(maria.getId()).size() == 1);
        check("the shared text matches what the student saw", s.text().equals(WellnessExport.toText(mine)));
        check("the shared text says private notes are left out", s.text().contains("without your notes"));
        store.saveCheckIn(new CheckIn(maria.getId(), LocalDate.now(), 1, 5, "later check-in"));
        check("later check-ins do not change what was shared", !store.sharesTo(counselor.getId()).get(0).text().contains("later check-in"));
        check("another student cannot stop someone else's share", !store.revokeShare(s.id(), juan.getId()) && store.sharesTo(counselor.getId()).size() == 1);
        SharedSummary second = store.shareSummary(maria, mine, true);
        check("each share needs its own consent and its own id", !second.id().equals(s.id()) && store.sharesBy(maria.getId()).size() == 2);
        check("the student can stop sharing", store.revokeShare(s.id(), maria.getId()));
        check("a stopped share is gone for the counselor but still listed for the student",
                store.sharesTo(counselor.getId()).size() == 1 && store.sharesTo(counselor.getId()).get(0).id().equals(second.id())
                        && store.sharesBy(maria.getId()).stream().anyMatch(x -> x.id().equals(s.id()) && !x.isActive()));
        check("stopping twice does nothing", !store.revokeShare(s.id(), maria.getId()));
        check("the admin activity feed does not name the student", store.recentActivity(10).stream().noneMatch(a -> a.toString().contains("Maria")
                && a.toString().contains("summary")));
        check("the page menus include the new pages", Arrays.asList(maria.getMenu()).contains("Export Summary")
                && Arrays.asList(counselor.getMenu()).contains("Shared Summaries") && !Arrays.asList(admin.getMenu()).contains("Shared Summaries")
                && !Arrays.asList(juan.getMenu()).contains("Shared Summaries"));

        System.out.println(failures == 0 ? "ALL " + checks + " EXPORT CHECKS PASSED" : failures + " of " + checks + " CHECKS FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ----- PDF structure helpers -----
    static int pageCount(String p) {
        Matcher m = Pattern.compile("/Type /Pages /Kids \\[[^\\]]*\\] /Count (\\d+)").matcher(p);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    static int countPages(String p) {
        Matcher m = Pattern.compile("/Type /Page /Parent").matcher(p);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /** Every xref entry must point at "N 0 obj", and startxref must point at "xref". */
    static boolean xrefOk(byte[] b) {
        String p = new String(b, StandardCharsets.ISO_8859_1);
        Matcher sx = Pattern.compile("startxref\\n(\\d+)\\n%%EOF").matcher(p);
        if (!sx.find()) return false;
        int at = Integer.parseInt(sx.group(1));
        if (!p.startsWith("xref", at)) return false;
        Matcher size = Pattern.compile("xref\\n0 (\\d+)\\n").matcher(p);
        if (!size.find(at)) return false;
        int n = Integer.parseInt(size.group(1));
        Matcher e = Pattern.compile("(\\d{10}) 00000 n \\n").matcher(p);
        int i = 1;
        int from = at;
        while (e.find(from)) {
            int off = Integer.parseInt(e.group(1));
            if (!p.startsWith(i + " 0 obj", off)) return false;
            from = e.end();
            i++;
        }
        return i == n;
    }

    private static void reload() {
        DataStore store = DataStore.getInstance();
        store.seed();
        User maria = store.findByEmail("student@tip.edu.ph").orElseThrow();
        User counselor = store.findByEmail("guidance@tip.edu.ph").orElseThrow();
        List<SharedSummary> mine = store.sharesBy(maria.getId());
        boolean ok = mine.size() == 2 && mine.stream().filter(SharedSummary::isActive).count() == 1
                && store.sharesTo(counselor.getId()).size() == 1
                && mine.stream().allMatch(x -> x.consentAt() != null && x.consentVersion().equals(DataStore.CONSENT_VERSION))
                && store.sharesTo(counselor.getId()).get(0).text().contains("KALMA WELLNESS SUMMARY")
                && store.sharesTo(counselor.getId()).get(0).text().contains("\n");
        System.out.println(ok ? "RELOAD OK: shares and the stopped state were loaded again" : "RELOAD FAILED: " + mine);
        System.exit(ok ? 0 : 1);
    }
}
