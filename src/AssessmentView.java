import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Student "Self-Assessment": the Wellness and Stress Assessment. Shows the start button, the latest (or selected)
 * result with Academic, Social and Personal scores, an overall score, suggested next steps, and the history of
 * earlier results. Informational only, never a diagnosis.
 */
class AssessmentView extends View {
    private static final Color HIGHER_BG = new Color(0xF9E1D2), HIGHER_FG = new Color(0x9A4A1C);
    private static final String TITLE = "Wellness & Stress Check";

    AssessmentView(MainWindow w) {
        super(w);
        build(null);
    }

    /** Builds the page. {@code shown} is the result to display, or null for the most recent one. */
    private void build(AssessmentResult shown) {
        removeAll();
        List<AssessmentResult> all = store().resultsOf(user().getId());   // oldest first
        AssessmentResult current = shown != null ? shown : all.isEmpty() ? null : all.get(all.size() - 1);
        int idx = current == null ? -1 : all.indexOf(current);
        AssessmentResult previous = idx > 0 ? all.get(idx - 1) : null;

        put(StudentUI.header(window, "Self-Assessment",
                "A short, private check on academic, social and personal stress."), intro());
        if (current != null) {
            put(resultCard(current, previous, current == all.get(all.size() - 1)));
            put(recommendationCard(current));
        }
        put(historyCard(all, current));
        revalidate();
        repaint();
    }

    // ----- intro and start -----
    private JPanel intro() {
        RoundPanel card = Staff.card("\uD83D\uDCCB  " + TITLE, null, null);
        card.add(new WrapText("18 short statements about the past 2 weeks, plus 5 questions about how the stress felt (motivating, manageable, "
                + "overwhelming or hard to recover from), about 5 minutes. Stress is not always bad: you will get a score for "
                + "Academic, Social and Personal stress, whether it looks more like eustress or distress, plus ideas that may help. " + AssessmentService.NOT_DIAGNOSIS
                + " Your results are private to you.", 600, Brand.MUTED, 12.5f, false));
        card.add(Staff.gap(12));
        JPanel buttons = new HugPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(0f);
        YButton start = YButton.yellow("Start assessment");
        start.addActionListener(e -> start());
        YButton how = YButton.outline("How scoring works");
        how.addActionListener(e -> window.message(AssessmentService.howScoringWorks()));
        buttons.add(start);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(how);
        card.add(buttons);
        return card;
    }

    private void start() {
        Student st = user() instanceof Student s ? s : null;
        List<Question> qs = store().getQuestions();
        List<Question> shown = new java.util.ArrayList<>(qs);
        shown.addAll(AssessmentService.APPRAISAL);      // how the stress felt: motivating, manageable, overwhelming, recovery
        int[] all = AssessmentDialog.run(window, st, TITLE, shown, qs.size());
        if (all == null) return;      // closed early: nothing is scored or saved
        int[] answers = java.util.Arrays.copyOfRange(all, 0, qs.size());
        int[] appraisal = java.util.Arrays.copyOfRange(all, qs.size(), all.length);
        store().addResult(AssessmentService.score(user().getId(), LocalDate.now(), qs, answers, appraisal));
        window.refresh();
    }

    // ----- result -----
    private JPanel resultCard(AssessmentResult r, AssessmentResult previous, boolean isLatest) {
        ConcernLevel overall = AssessmentService.classify(r.overall());
        RoundPanel card = Staff.card((isLatest ? "Your latest result" : "Result from " + r.date().format(StudentUI.DAY)),
                null, null);

        JPanel top = new HugPanel(new BorderLayout(14, 0));
        top.setOpaque(false);
        top.setAlignmentX(0f);
        JLabel big = new JLabel(r.overall() + "%");
        big.setFont(new Font("SansSerif", Font.BOLD, 34));
        big.setForeground(Brand.INK);
        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        JLabel sub = new JLabel("Overall \u2022 " + r.date().format(StudentUI.DAY));
        sub.setFont(Theme.BASE.deriveFont(12f));
        sub.setForeground(Brand.MUTED);
        sub.setAlignmentX(0f);
        mid.add(sub);
        mid.add(Staff.gap(2));
        WrapText msg = new WrapText(overall.message(), 420, Brand.INK, 13f, false);
        mid.add(msg);
        top.add(Staff.centered(big), BorderLayout.WEST);
        top.add(mid, BorderLayout.CENTER);
        top.add(Staff.centered(levelPill(overall)), BorderLayout.EAST);
        card.add(top);
        card.add(Staff.gap(8));
        card.add(new WrapText(AssessmentService.trendSentence(r, previous), 600, Brand.MUTED, 12f, false));
        card.add(Staff.gap(14));
        if (r.profile() != null) {
            card.add(profileBlock(r));
            card.add(Staff.gap(14));
        }

        for (String d : AssessmentService.DOMAINS) {
            ConcernLevel lv = AssessmentService.classify(r.score(d));
            card.add(domainRow(d, r.score(d), lv, previous == null ? null : previous.score(d)));
            card.add(Staff.gap(10));
        }
        card.add(new WrapText(AssessmentService.NOT_DIAGNOSIS + " " + AssessmentService.CRISIS_NOTE,
                600, Brand.MUTED, 11.5f, false));
        return card;
    }

    /** Summary sentence, type of stress and main source(s). Not shown for results saved before this existed. */
    private JPanel profileBlock(AssessmentResult r) {
        StressProfile p = r.profile();
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setAlignmentX(0f);
        box.add(profileLine("Summary", p.summary(r), null));
        box.add(Staff.gap(8));
        box.add(profileLine("Type of stress: " + p.typeLabel(), p.typeMessage(), null));
        box.add(Staff.gap(8));
        if (p.appraisalLine() != null) {
            box.add(profileLine("How the demands felt", p.appraisalLine(), null));
            box.add(Staff.gap(8));
        }
        StringBuilder detail = new StringBuilder();
        for (Map.Entry<String, Integer> e : p.sourceScores().entrySet())
            detail.append(detail.length() == 0 ? "" : "   \u2022   ").append(e.getKey()).append(" ").append(e.getValue()).append("%");
        String names = p.categories().isEmpty() ? "No single source stands out" : String.join(", ", p.categories());
        box.add(profileLine("Main source: " + names, detail.toString(), null));
        return box;
    }

    private static JPanel profileLine(String heading, String body, Color unused) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(0f);
        JLabel h = new JLabel(heading);
        h.setFont(Theme.BASE.deriveFont(Font.BOLD, 13f));
        h.setForeground(Brand.INK);
        h.setAlignmentX(0f);
        row.add(h);
        row.add(Staff.gap(2));
        row.add(new WrapText(body, 600, Brand.MUTED, 12f, false));
        return row;
    }

    private JPanel domainRow(String domain, int percent, ConcernLevel lv, Integer before) {
        JPanel row = new HugPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        row.setAlignmentX(0f);
        JLabel name = new JLabel(domain + " stress  \u2022  " + percent + "%"
                + (before == null ? "" : "  \u2022  " + AssessmentService.changeText(percent, before)));
        name.setFont(Theme.BASE.deriveFont(Font.BOLD, 13f));
        name.setForeground(Brand.INK);
        JPanel head = new HugPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(name, BorderLayout.WEST);
        head.add(levelPill(lv), BorderLayout.EAST);
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(new Bar(percent, fg(lv)));
        bottom.add(Staff.gap(4));
        bottom.add(new WrapText(AssessmentService.domainMessage(domain, lv), 600, Brand.MUTED, 12f, false));
        row.add(head, BorderLayout.NORTH);
        row.add(bottom, BorderLayout.CENTER);
        return row;
    }

    private JPanel recommendationCard(AssessmentResult r) {
        RoundPanel card = Staff.card("\uD83D\uDCA1  Suggested for you", null, null);
        List<AssessmentService.Recommendation> recs = AssessmentService.recommend(r);
        for (int i = 0; i < recs.size(); i++) {
            AssessmentService.Recommendation rec = recs.get(i);
            RoundPanel row = Staff.row();
            row.add(Staff.twoLines(rec.title(), rec.detail()), BorderLayout.CENTER);
            if (rec.target() != null) {
                YButton go = YButton.outline("Open");
                go.addActionListener(e -> window.open(rec.target()));
                row.add(Staff.centered(go), BorderLayout.EAST);
            }
            card.add(row);
            if (i < recs.size() - 1) card.add(Staff.gap(8));
        }
        return card;
    }

    // ----- history -----
    private JPanel historyCard(List<AssessmentResult> all, AssessmentResult current) {
        RoundPanel card = Staff.card("\uD83D\uDCC8  Your progress over time", null, null);
        if (all.isEmpty()) {
            card.add(Staff.note("No assessments yet. Start one above, then retake it every few weeks to see how things change."));
            return card;
        }
        if (all.size() >= 2) {
            ScoreHistoryChart chart = new ScoreHistoryChart();
            chart.setData(all);
            chart.setAlignmentX(0f);
            card.add(chart);
            card.add(Staff.gap(8));
        } else {
            card.add(Staff.note("Take the assessment again in a few weeks and your progress chart will appear here."));
            card.add(Staff.gap(8));
        }
        card.add(Staff.note("Select a row to see that result in detail."));
        card.add(Staff.gap(8));
        for (int i = all.size() - 1; i >= 0; i--) {         // newest first
            AssessmentResult r = all.get(i);
            ConcernLevel lv = AssessmentService.classify(r.overall());
            RoundPanel row = Staff.row(r == current);
            row.add(Staff.twoLines(r.date().format(StudentUI.DAY),
                    "Academic " + r.academic() + "%  \u2022  Social " + r.social() + "%  \u2022  Personal "
                            + r.personal() + "%  \u2022  Overall " + r.overall() + "%"), BorderLayout.CENTER);
            row.add(Staff.centered(levelPill(lv)), BorderLayout.EAST);
            Staff.click(row, () -> build(r));
            card.add(row);
            if (i > 0) card.add(Staff.gap(8));
        }
        return card;
    }

    // ----- small helpers -----
    private static Pill levelPill(ConcernLevel lv) {
        return switch (lv.rank()) {
            case 0 -> new Pill(lv.label(), Staff.GOOD_BG, Staff.GOOD_FG);
            case 1 -> new Pill(lv.label(), Staff.WARN_BG, Staff.WARN_FG);
            default -> new Pill(lv.label(), HIGHER_BG, HIGHER_FG);
        };
    }

    private static Color fg(ConcernLevel lv) {
        return lv.rank() == 0 ? Staff.GOOD_FG : lv.rank() == 1 ? new Color(0xC99A00) : new Color(0xD9814E);
    }

    /** Thin horizontal progress bar from 0 to 100. */
    private static final class Bar extends JPanel {
        private final int percent;
        private final Color color;
        Bar(int percent, Color color) {
            this.percent = percent;
            this.color = color;
            setOpaque(false);
            setAlignmentX(0f);
            getAccessibleContext().setAccessibleName(percent + " percent");
        }
        @Override public Dimension getPreferredSize() { return new Dimension(200, 10); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 10); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Brand.aa(g);
            g.setColor(Theme.GRID);
            g.fillRoundRect(0, 0, getWidth(), 10, 10, 10);
            g.setColor(color);
            g.fillRoundRect(0, 0, Math.max(10, getWidth() * percent / 100), 10, 10, 10);
            g.dispose();
        }
    }
}
