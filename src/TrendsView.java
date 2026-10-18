import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
/** Student "Wellness Trends": averages, check-in days and the 7-day mood and stress chart. */
class TrendsView extends View {
    TrendsView(MainWindow w) {
        super(w);
        List<CheckIn> all = store().checkInsOf(user().getId());
        List<CheckIn> week = StudentUI.lastWeek(all);
        String mood = week.isEmpty() ? "No data"
                : String.format("%.1f / 5", week.stream().mapToInt(CheckIn::mood).average().orElse(0));
        String stress = week.isEmpty() ? "No data"
                : String.format("%.1f / 5", week.stream().mapToInt(CheckIn::stress).average().orElse(0));
        int daysIn = week.size();

        TrendChart chart = new TrendChart();
        chart.setFramed(false);
        chart.setLegend(false);
        chart.setColors(Brand.YELLOW, Brand.BLACK);
        chart.setPreferredSize(new Dimension(600, 270));
        chart.setAlignmentX(0f);

        JLabel title = new JLabel();
        title.setFont(Theme.BASE.deriveFont(Font.BOLD, 14f));
        title.setForeground(Brand.INK);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        legend.setOpaque(false);
        PillToggle d7 = new PillToggle("7 days"), d30 = new PillToggle("30 days");
        ButtonGroup bg = new ButtonGroup();
        bg.add(d7);
        bg.add(d30);
        legend.add(d7);
        legend.add(d30);
        for (Object[] k : new Object[][]{{Brand.YELLOW, "Mood"}, {Brand.BLACK, "Stress"}}) {
            legend.add(new Dot((Color) k[0], 9));
            JLabel l = new JLabel((String) k[1]);
            l.setFont(Theme.BASE.deriveFont(11f));
            l.setForeground(Brand.INK);
            legend.add(l);
        }
        JPanel head = new HugPanel(new BorderLayout());
        head.setOpaque(false);
        head.setAlignmentX(0f);
        head.add(title, BorderLayout.WEST);
        head.add(Staff.centered(legend), BorderLayout.EAST);

        RoundPanel card = Staff.card(null, null, null);
        card.add(head);
        card.add(Staff.gap(8));
        card.add(chart);

        // Day-by-day log. A day without a check-in says "No check-in" and is never guessed.
        RoundPanel log = Staff.card("\uD83D\uDCC5  Daily log", null, null);
        JPanel logRows = new JPanel();
        logRows.setOpaque(false);
        logRows.setLayout(new BoxLayout(logRows, BoxLayout.Y_AXIS));
        logRows.setAlignmentX(0f);
        log.add(logRows);

        Runnable show7 = () -> showPeriod(7, all, title, chart, logRows);
        Runnable show30 = () -> showPeriod(30, all, title, chart, logRows);
        d7.addActionListener(e -> show7.run());
        d30.addActionListener(e -> show30.run());
        d7.setSelected(true);
        show7.run();

        put(StudentUI.header(w, "Wellness Trends", "See how your mood and stress have changed over the past week."),
                StudentUI.row(14, StudentUI.statTile("🙂", "Average Mood", mood),
                        StudentUI.statTile("📈", "Average Stress", stress),
                        StudentUI.statTile("📅", "Check-ins (last 7 days)", daysIn + (daysIn == 1 ? " day" : " days"))),
                card, log);
    }

    private void showPeriod(int days, List<CheckIn> all, JLabel title, TrendChart chart, JPanel logRows) {
        title.setText("\uD83D\uDCC8  Mood and Stress \u2013 Last " + days + " Days");
        chart.setDaily(all, days);
        logRows.removeAll();
        for (int i = 0; i < days; i++) {
            LocalDate d = LocalDate.now().minusDays(i);
            Optional<CheckIn> c = all.stream().filter(x -> x.date().equals(d)).findFirst();
            RoundPanel row;
            if (c.isPresent()) {
                CheckIn ci = c.get();
                row = StudentUI.listRow(d.format(StudentUI.DAY), "Mood: " + Kalma.MOODS[ci.mood() - 1].substring(4)
                        + "  \u2022  Stress: " + CheckInView.STRESS[ci.stress() - 1], "Logged");
            } else {
                row = Staff.row();
                row.add(Staff.twoLines(d.format(StudentUI.DAY), "Nothing logged this day"), BorderLayout.CENTER);
                row.add(Staff.centered(new Pill("No check-in", Staff.NEUTRAL_BG, Staff.NEUTRAL_FG)), BorderLayout.EAST);
            }
            row.setAlignmentX(0f);
            logRows.add(row);
            logRows.add(Staff.gap(6));
        }
        logRows.revalidate();
        logRows.repaint();
    }
}

