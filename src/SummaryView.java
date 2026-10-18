import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
/**
 * Student "Summary": a weekly or monthly recap of the student's own check-ins with a mood and stress chart,
 * a gentle comparison with the period before, an encouraging message and three next steps (WellnessSummary).
 */
class SummaryView extends View {
    private final List<CheckIn> all;
    private final JPanel dynamic = new JPanel();

    SummaryView(MainWindow w) {
        super(w);
        all = store().checkInsOf(user().getId());
        dynamic.setOpaque(false);
        dynamic.setLayout(new BoxLayout(dynamic, BoxLayout.Y_AXIS));
        dynamic.setAlignmentX(0f);

        PillToggle week = new PillToggle("This week"), month = new PillToggle("This month");
        ButtonGroup bg = new ButtonGroup();
        bg.add(week);
        bg.add(month);
        week.addActionListener(e -> render(7));
        month.addActionListener(e -> render(30));
        JPanel toggles = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toggles.setOpaque(false);
        toggles.add(week);
        toggles.add(month);
        YButton export = YButton.outline("Export or share  →");
        export.setBorder(new javax.swing.border.EmptyBorder(6, 16, 6, 16));
        export.addActionListener(e -> window.open("Export Summary"));
        toggles.add(export);
        week.setSelected(true);
        render(7);

        put(StudentUI.header(w, "Weekly and Monthly Summary", "A gentle recap based on your own check-ins. Only you can see this."),
                toggles, dynamic);
    }

    private void render(int days) {
        WellnessSummary.Result r = WellnessSummary.summarize(all, days, LocalDate.now());
        String period = WellnessSummary.periodName(days);
        dynamic.removeAll();

        String best = r.bestDay() == null ? "–" : r.bestDay().format(StudentUI.SHORT_DAY);
        add(StudentUI.row(14,
                StudentUI.statTile("🙂", "Check-ins this " + period, String.valueOf(r.checkInDays())),
                StudentUI.statTile("☺", "Average mood", r.avgMood() == null ? "–" : String.format("%.1f / 5", r.avgMood())),
                StudentUI.statTile("📈", "Average stress", r.avgStress() == null ? "–" : String.format("%.1f / 5", r.avgStress())),
                StudentUI.statTile("☀", "Brightest day", best)));

        add(new StreakCard(all, LocalDate.now(), true));

        RoundPanel note = new RoundPanel(new BorderLayout(), Staff.BANNER, new Color(0xF1E3A0), 16);
        note.setBorder(new javax.swing.border.EmptyBorder(14, 18, 14, 18));
        note.add(Theme.wrapLabel("🌱  " + r.message(), 840, Brand.INK, 13f, false), BorderLayout.CENTER);
        note.setAlignmentX(0f);
        add(note);

        RoundPanel chartCard = Staff.card("📈  Mood and stress, last " + days + " days", null, null);
        TrendChart chart = new TrendChart();
        chart.setFramed(false);
        chart.setColors(Brand.YELLOW, Brand.BLACK);
        chart.setPreferredSize(new Dimension(600, 240));
        chart.setAlignmentX(0f);
        chart.setDaily(all, days);
        chartCard.add(chart);
        chartCard.add(Staff.gap(4));
        chartCard.add(Staff.note("Yellow is mood, black is stress. Days without a check-in are simply left blank. Nothing is guessed."));
        add(chartCard);

        RoundPanel compare = Staff.card("🔍  Compared with the " + period + " before", null, null);
        if (r.canCompare()) {
            compare.add(Staff.twoLines("Mood: " + String.format("%.1f", r.prevMood()) + " → " + String.format("%.1f", r.avgMood())
                            + "  (" + WellnessSummary.moodChange(r.avgMood(), r.prevMood()) + ")",
                    "Stress: " + String.format("%.1f", r.prevStress()) + " → " + String.format("%.1f", r.avgStress())
                            + "  (" + WellnessSummary.stressChange(r.avgStress(), r.prevStress()) + ")"));
            compare.add(Staff.gap(6));
            compare.add(Staff.note("Averages out of 5. Ups and downs are a normal part of student life, and this is not a diagnosis."));
        } else {
            compare.add(Staff.note("There is not enough to compare yet. That is fine. It will appear on its own after a few more check-ins."));
        }
        add(compare);

        add(StudentUI.row(14,
                StudentUI.actionCard("🌿", "Take a breather",
                        "A short breathing exercise can help on busy days.", "Open guide",
                        () -> window.message(Kalma.BREATHING)),
                StudentUI.actionCard("👥", "Reach out",
                        "Peer support communities are here when you want company.", "Peer support",
                        () -> window.open("Peer Support")),
                StudentUI.actionCard("🏛️", "Talk to someone",
                        "The Guidance Office is available for a one-on-one chat.", "Guidance office",
                        () -> window.open("Guidance Office"))));
        dynamic.revalidate();
        dynamic.repaint();
    }

    private void add(JComponent c) {
        c.setAlignmentX(0f);
        dynamic.add(c);
        dynamic.add(Staff.gap(12));
    }
}
