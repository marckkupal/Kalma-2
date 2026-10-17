import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Guidance - Shared Summaries. Lists the wellness summaries students chose to share with this counselor. A summary
 * only appears here after the student agreed on the consent screen, and it disappears when the student stops sharing.
 */
class SharedSummariesView extends View {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    SharedSummariesView(MainWindow w) {
        super(w);
        List<SharedSummary> list = store().sharesTo(user().getId());
        RoundPanel card = Staff.card("\uD83D\uDCE5  Shared with you", null, null);
        if (list.isEmpty()) card.add(Staff.note("No summaries have been shared with you yet. Students choose whether to share."));
        for (SharedSummary s : list) {
            RoundPanel r = Staff.personRow(false, store().initialsOf(s.studentId()), store().nameOf(s.studentId()),
                    s.period() + "  •  " + s.sections(), new Pill(DataStore.ago(s.sharedAt()), Brand.YELLOW, Brand.INK));
            Staff.click(r, () -> open(s));
            card.add(r);
            card.add(Staff.gap(6));
        }
        put(Staff.header("Shared Summaries", "Wellness summaries students chose to share with the Guidance Office.",
                () -> window.open("Dashboard")), card,
                Staff.note("Shared with the student's agreement. Please keep it confidential. These are self-reported signals, not a diagnosis."));
    }

    private void open(SharedSummary s) {
        JTextArea t = new JTextArea(s.text(), 18, 60);
        t.setEditable(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        t.setBackground(Theme.BG);
        t.setForeground(Theme.INK);
        t.setBorder(new EmptyBorder(8, 10, 8, 10));
        t.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(Theme.GRID, 1, true));
        Object[] content = {"Shared by " + store().nameOf(s.studentId()) + " on " + s.sharedAt().format(WHEN)
                + " with their agreement.", sp};
        JOptionPane.showMessageDialog(window, content, "Shared wellness summary", JOptionPane.PLAIN_MESSAGE);
    }
}
