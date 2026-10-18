import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
/**
 * Student "Possible Solutions" (Figma "Student - Possible Solutions"): small things to try today.
 * Top to bottom: an optional Guidance Office prompt, activities matched to the latest check-in and assessment
 * (with the reason shown), the four grounding techniques with step-by-step instructions, and other ideas.
 */
class SolutionsView extends View {
    SolutionsView(MainWindow w) {
        super(w);
        List<CheckIn> checkIns = store().checkInsOf(user().getId());
        List<AssessmentResult> results = store().resultsOf(user().getId());
        AssessmentResult latestResult = results.isEmpty() ? null : results.get(results.size() - 1);

        List<Component> parts = new java.util.ArrayList<>();
        parts.add(StudentUI.header(w, "Possible Solutions",
                "Small, practical ideas you can try today. Pick what feels right for you."));

        if (GroundingActivity.suggestGuidance(checkIns, latestResult)) parts.add(guidancePrompt());

        parts.add(sectionTitle("Recommended for you"));
        List<GroundingActivity.Pick> picks = GroundingActivity.suggest(GroundingActivity.latestCheckIn(checkIns), latestResult);
        if (picks.isEmpty()) {
            parts.add(checkInPrompt());
        } else {
            JPanel row = new HugPanel(new GridLayout(0, 3, 14, 14));
            row.setOpaque(false);
            for (GroundingActivity.Pick p : picks) row.add(activityCard(p.activity(), p.reason()));
            parts.add(row);
        }

        parts.add(sectionTitle("Grounding techniques"));
        JPanel grounding = new HugPanel(new GridLayout(0, 2, 14, 14));
        grounding.setOpaque(false);
        for (GroundingActivity a : GroundingActivity.ALL) grounding.add(activityCard(a, a.blurb()));
        parts.add(grounding);

        parts.add(sectionTitle("More ideas"));
        JPanel grid = new HugPanel(new GridLayout(0, 3, 14, 14));
        grid.setOpaque(false);
        grid.add(StudentUI.actionCard("\uD83D\uDCC4", "Journaling",
                "Write down what is on your mind, privately, and look back on it later.", "Open journal",
                () -> window.open("Calendar")));
        grid.add(StudentUI.actionCard("\u2600", "Short Walk",
                "A few minutes outside can help reset your focus.", "Try it",
                () -> window.message("Step outside for five minutes.\nLeave your phone in your pocket, "
                        + "notice three things you can see, and walk at an easy pace.")));
        grid.add(StudentUI.actionCard("\uD83D\uDC65", "Join a Peer Community",
                "Talk with students who understand what you are going through.", "Browse communities",
                () -> window.open("Peer Support")));
        grid.add(StudentUI.actionCard("\uD83C\uDFDB\uFE0F", "Book a Consultation",
                "Meet a guidance counselor in person at T.I.P. QC.", "Book now",
                () -> window.open("Book Consultation")));
        grid.add(StudentUI.actionCard("\uD83D\uDCA1", "Study Break Tips",
                "Simple ways to rest well between study sessions.", "Read tips",
                () -> window.message("Between study sessions:\n\u2022  Work in 25-minute blocks, then rest for 5\n"
                        + "\u2022  Stand up, stretch and drink water\n\u2022  Look away from your screen for a minute\n"
                        + "\u2022  Save phone scrolling for the end of the day")));
        parts.add(grid);

        put(parts.toArray(new Component[0]));
    }

    private RoundPanel activityCard(GroundingActivity a, String text) {
        return StudentUI.actionCard(a.icon(), a.title(), text, a.button(), () -> ActivityDialog.show(window, a));
    }

    private static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.BASE.deriveFont(Font.BOLD, 16f));
        l.setForeground(Brand.INK);
        l.setBorder(new EmptyBorder(6, 2, 0, 0));
        return l;
    }

    /** Soft prompt toward the Guidance Office. Shown only when GroundingActivity.suggestGuidance says so. */
    private JPanel guidancePrompt() {
        RoundPanel p = new RoundPanel(new BorderLayout(16, 0), Brand.YELLOW_SOFT, new Color(0xF1E3A0), 20);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Talking to someone could help");
        t.setFont(Theme.BASE.deriveFont(Font.BOLD, 15f));
        t.setForeground(Brand.INK);
        t.setAlignmentX(0f);
        col.add(t);
        col.add(Staff.gap(4));
        col.add(new WrapText("Based on your recent check-ins and assessment, a counselor at the Guidance Office "
                + "can listen and help you plan next steps. It is confidential and free.", 500, Brand.MUTED, 12f, false));
        YButton b = YButton.yellow("Book a consultation  \u2192");
        b.setBorder(new EmptyBorder(7, 16, 7, 16));
        b.addActionListener(e -> window.open("Book Consultation"));
        p.add(col, BorderLayout.CENTER);
        p.add(Staff.centered(b), BorderLayout.EAST);
        return p;
    }

    /** Shown instead of recommendations when there is no recent check-in or assessment to match. */
    private JPanel checkInPrompt() {
        RoundPanel p = new RoundPanel(new BorderLayout(16, 0), Color.WHITE, Theme.GRID, 20);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        p.add(new WrapText("Do a quick check-in and Kalma will suggest activities that match how you feel. "
                + "In the meantime, any of the techniques below are a good place to start.",
                500, Brand.MUTED, 13f, false), BorderLayout.CENTER);
        YButton b = YButton.yellow("Check in  \u2192");
        b.setBorder(new EmptyBorder(7, 16, 7, 16));
        b.addActionListener(e -> window.open("Daily Check-In"));
        p.add(Staff.centered(b), BorderLayout.EAST);
        return p;
    }
}
