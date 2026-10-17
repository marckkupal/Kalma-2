import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
/**
 * One question at a time with an English / Filipino switch. Switching re-translates the current question
 * on the spot. Answers are stored as 0 to 3, so scoring is identical in both languages.
 */
final class AssessmentDialog {
    private final JDialog dlg;
    private final Student student;
    private final String title;
    private final List<Question> qs;
    private final int[] answers;
    private final boolean[] answered;
    private int idx = 0;
    private boolean filipino;
    private boolean finished = false;

    private final JLabel head = new JLabel();
    private final JLabel lang = new JLabel();
    private final JPanel prompt = new JPanel();
    private final JPanel choices = new JPanel(new GridLayout(0, 1, 0, 8));
    private final PillToggle en = new PillToggle("English");
    private final PillToggle tl = new PillToggle("Filipino");
    private final YButton back = YButton.outline("");

    private AssessmentDialog(MainWindow w, Student s, String title, List<Question> qs) {
        this.student = s;
        this.title = title;
        this.qs = qs;
        this.answers = new int[qs.size()];
        this.answered = new boolean[qs.size()];
        this.filipino = s != null && "tl".equals(s.getAssessmentLanguage());
        dlg = new JDialog(w, title, true);

        head.setFont(Theme.BASE.deriveFont(Font.BOLD, 12f));
        head.setForeground(Brand.MUTED);
        lang.setFont(Theme.BASE.deriveFont(11f));
        lang.setForeground(Brand.MUTED);
        ButtonGroup g = new ButtonGroup();
        g.add(en);
        g.add(tl);
        en.addActionListener(e -> setLanguage(false));
        tl.addActionListener(e -> setLanguage(true));
        back.addActionListener(e -> { if (idx > 0) { idx--; render(); } });

        prompt.setOpaque(false);
        prompt.setLayout(new BoxLayout(prompt, BoxLayout.Y_AXIS));
        JPanel toggles = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toggles.setOpaque(false);
        toggles.add(lang);
        toggles.add(Box.createHorizontalStrut(8));
        toggles.add(en);
        toggles.add(Box.createHorizontalStrut(6));
        toggles.add(tl);
        // Two rows (title and question number, then the language switch) so the texts never overlap.
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        head.setAlignmentX(0f);
        toggles.setAlignmentX(0f);
        top.add(head);
        top.add(Box.createVerticalStrut(8));
        top.add(toggles);

        choices.setOpaque(false);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(back);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(Brand.CREAM);
        body.setBorder(new EmptyBorder(20, 24, 20, 24));
        JPanel mid = new JPanel(new BorderLayout(0, 14));
        mid.setOpaque(false);
        mid.add(prompt, BorderLayout.NORTH);
        mid.add(choices, BorderLayout.CENTER);
        body.add(top, BorderLayout.NORTH);
        body.add(mid, BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);
        dlg.setContentPane(body);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setPreferredSize(new Dimension(540, 520));
        render();
        dlg.pack();
        dlg.setLocationRelativeTo(w);
    }

    /**
     * Runs the questions. Returns the raw answers (0 to 3, in question order), or null if the student closed
     * the window early. Nothing is scored or saved for an unfinished assessment.
     */
    static int[] run(MainWindow w, Student s, String title, List<Question> qs) {
        AssessmentDialog d = new AssessmentDialog(w, s, title, qs);
        d.dlg.setVisible(true);
        return d.finished ? d.answers.clone() : null;
    }

    private void setLanguage(boolean f) {
        filipino = f;
        if (student != null) student.setAssessmentLanguage(f ? "tl" : "en");
        render();
    }

    private void render() {
        en.setSelected(!filipino);
        tl.setSelected(filipino);
        head.setText(filipino ? title + "  \u2022  Tanong " + (idx + 1) + " ng " + qs.size()
                : title + "  \u2022  Question " + (idx + 1) + " of " + qs.size());
        lang.setText(filipino ? "Wika ng pagsusulit:" : "Assessment language:");
        back.setText(filipino ? "Bumalik" : "Back");
        back.setVisible(idx > 0);
        String lead = filipino ? "Sa nakalipas na 2 linggo:" : "In the past 2 weeks:";
        prompt.removeAll();
        JLabel leadLabel = new JLabel(lead);
        leadLabel.setFont(Theme.BASE.deriveFont(12f));
        leadLabel.setForeground(Brand.MUTED);
        leadLabel.setAlignmentX(0f);
        prompt.add(leadLabel);
        prompt.add(Box.createVerticalStrut(4));
        prompt.add(TextWrap.block(qs.get(idx).text(filipino), 450, Theme.BASE.deriveFont(Font.BOLD, 16f), Brand.INK));
        prompt.revalidate();
        choices.removeAll();
        String[] opts = AssessmentService.options(filipino);
        for (int i = 0; i < opts.length; i++) {
            final int score = i;
            YButton b = answers[idx] == i && answered[idx] ? YButton.yellow(opts[i]) : YButton.outline(opts[i]);
            b.addActionListener(e -> choose(score));
            choices.add(b);
        }
        choices.revalidate();
        choices.repaint();
        dlg.getContentPane().revalidate();
        dlg.getContentPane().repaint();
    }

    private void choose(int score) {
        answers[idx] = score;
        answered[idx] = true;
        if (idx == qs.size() - 1) {
            finished = true;
            dlg.dispose();
        } else {
            idx++;
            render();
        }
    }
}
