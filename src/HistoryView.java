import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Student "History": review past check-ins month by month, and every assessment result. Only the student's own
 * records are shown. Days without a check-in are not listed, so the page never points at a gap.
 */
class HistoryView extends View {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private final JPanel content = new JPanel();
    private final List<CheckIn> checkIns;
    private final List<AssessmentResult> results;
    private YearMonth month = YearMonth.now();

    HistoryView(MainWindow w) {
        super(w);
        String uid = user().getId();
        checkIns = store().checkInsOf(uid);
        results = store().resultsOf(uid);
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentX(0f);

        PillToggle ci = new PillToggle("Check-ins"), as = new PillToggle("Assessments");
        ButtonGroup bg = new ButtonGroup();
        bg.add(ci);
        bg.add(as);
        ci.addActionListener(e -> showCheckIns());
        as.addActionListener(e -> showAssessments());
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tabs.setOpaque(false);
        tabs.add(ci);
        tabs.add(as);
        ci.setSelected(true);
        showCheckIns();

        put(StudentUI.header(w, "My History", "Look back at your check-ins and assessments. Only you can see this."), tabs, content);
    }

    // ----- check-ins, one month at a time -----
    private void showCheckIns() {
        content.removeAll();
        List<CheckIn> inMonth = checkIns.stream().filter(c -> YearMonth.from(c.date()).equals(month))
                .sorted(Comparator.comparing(CheckIn::date).reversed()).collect(Collectors.toList());

        YButton prev = YButton.outline("‹  Earlier");
        YButton next = YButton.outline("Later  ›");
        prev.addActionListener(e -> { month = month.minusMonths(1); showCheckIns(); });
        next.addActionListener(e -> { month = month.plusMonths(1); showCheckIns(); });
        next.setEnabled(month.isBefore(YearMonth.now()));
        JLabel title = new JLabel(month.format(MONTH));
        title.setFont(Theme.BASE.deriveFont(Font.BOLD, 16f));
        title.setForeground(Brand.INK);
        JPanel nav = new HugPanel(new BorderLayout(12, 0));
        nav.setOpaque(false);
        nav.add(prev, BorderLayout.WEST);
        nav.add(Staff.centered(title), BorderLayout.CENTER);
        nav.add(next, BorderLayout.EAST);
        add(nav);

        RoundPanel card = Staff.card("🙂  Check-ins in " + month.format(MONTH), null, null);
        if (inMonth.isEmpty()) {
            card.add(Staff.note(month.equals(YearMonth.now())
                    ? "No check-ins yet this month. Whenever you feel like it, a check-in only takes a minute."
                    : "No check-ins this month, and that is okay. Use “Earlier” or “Later” to look at another month."));
        } else {
            double m = inMonth.stream().mapToInt(CheckIn::mood).average().orElse(0);
            double s = inMonth.stream().mapToInt(CheckIn::stress).average().orElse(0);
            card.add(Staff.note(inMonth.size() + " check-in" + (inMonth.size() == 1 ? "" : "s") + "  •  average mood "
                    + String.format("%.1f", m) + " / 5  •  average stress " + String.format("%.1f", s) + " / 5"));
            card.add(Staff.gap(8));
            for (CheckIn c : inMonth) {
                RoundPanel row = Staff.row();
                String sub = "Stress: " + CheckInView.STRESS[c.stress() - 1] + (c.note().isEmpty() ? "" : "  •  " + flat(c.note()));
                row.add(Staff.twoLines(c.date().format(StudentUI.DAY) + "  •  " + Emoji.mood(c.mood()) + " "
                        + Kalma.MOODS[c.mood() - 1].substring(4), sub), BorderLayout.CENTER);
                row.setAlignmentX(0f);
                card.add(row);
                card.add(Staff.gap(6));
            }
        }
        add(card);
        refresh();
    }

    // ----- assessments -----
    private void showAssessments() {
        content.removeAll();
        RoundPanel card = Staff.card("📋  Your assessments", "Open Assessment", () -> window.open("Assessment"));
        if (results.isEmpty()) {
            card.add(Staff.note("No assessments yet. When you are ready, you can take one from the Assessment page."));
        } else {
            if (results.size() >= 2) {
                ScoreHistoryChart chart = new ScoreHistoryChart();
                chart.setData(results);
                chart.setAlignmentX(0f);
                card.add(chart);
                card.add(Staff.gap(8));
            }
            card.add(Staff.note("Newest first. Scores are percentages and can change from one time to the next. They are not grades."));
            card.add(Staff.gap(8));
            for (int i = results.size() - 1; i >= 0; i--) {
                AssessmentResult r = results.get(i);
                String change = "";
                if (i > 0) {
                    int d = r.overall() - results.get(i - 1).overall();
                    change = Math.abs(d) < 3 ? "  •  about the same as last time" : d < 0 ? "  •  lower than last time" : "  •  higher than last time";
                }
                RoundPanel row = Staff.row();
                row.add(Staff.twoLines(r.date().format(StudentUI.DAY) + "  •  " + AssessmentService.classify(r.overall()).label(),
                        "Academic " + r.academic() + "%  •  Social " + r.social() + "%  •  Personal " + r.personal()
                                + "%  •  Overall " + r.overall() + "%" + change), BorderLayout.CENTER);
                row.setAlignmentX(0f);
                card.add(row);
                card.add(Staff.gap(6));
            }
        }
        add(card);
        refresh();
    }

    private static String flat(String s) {
        String f = s.replace('\n', ' ');
        return f.length() > 90 ? f.substring(0, 90) + "…" : f;
    }

    private void add(JComponent c) {
        c.setAlignmentX(0f);
        content.add(c);
        content.add(Staff.gap(12));
    }

    private void refresh() {
        content.revalidate();
        content.repaint();
    }
}
