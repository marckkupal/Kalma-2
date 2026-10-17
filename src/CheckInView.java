import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Student "Daily Check-In": pick a mood and a stress level. Once both are chosen, the follow-up question
 * appears (English or Filipino) with an optional note. The note can be skipped. Saved privately per day.
 */
class CheckInView extends View {
    /** Set by the Home mood tiles so the chosen face is already selected when this page opens. */
    static int preselectMood = 0;

    static final String[] STRESS = {"Very Low", "Low", "Moderate", "High", "Very High"};
    static final String ASK_EN = "What do you think made you feel this way?";
    static final String ASK_TL = "Ano sa tingin mo ang dahilan kung bakit ganito ang nararamdaman mo?";
    static final String PLACEHOLDER = "Share what's on your mind... (optional)";

    private boolean filipino;
    private final JLabel ask = new JLabel();
    private final JLabel help = new JLabel();
    private final PillToggle en = new PillToggle("English");
    private final PillToggle tl = new PillToggle("Filipino");

    CheckInView(MainWindow w) {
        super(w);
        String uid = user().getId();
        Student student = user() instanceof Student s ? s : null;
        filipino = student != null && "tl".equals(student.getAssessmentLanguage());
        List<CheckIn> mine = store().checkInsOf(uid);
        Optional<CheckIn> today = mine.stream().filter(c -> c.date().equals(LocalDate.now())).findFirst();
        int[] pick = {today.map(CheckIn::mood).orElse(0), today.map(CheckIn::stress).orElse(0)};
        if (preselectMood > 0) { pick[0] = preselectMood; preselectMood = 0; }

        // ----- mood -----
        MoodTile[] tiles = new MoodTile[Kalma.MOODS.length];
        JPanel grid = new JPanel(new GridLayout(1, tiles.length, 15, 0));
        grid.setOpaque(false);
        JPanel faces = new HugPanel(new BorderLayout());
        faces.setOpaque(false);
        faces.setAlignmentX(0f);
        faces.add(grid, BorderLayout.WEST);

        // ----- stress -----
        JPanel stressRow = new JPanel(new GridLayout(1, STRESS.length, 8, 0));
        stressRow.setOpaque(false);
        PillToggle[] stressBtns = new PillToggle[STRESS.length];
        ButtonGroup sg = new ButtonGroup();
        JPanel stressWrap = new HugPanel(new BorderLayout());
        stressWrap.setOpaque(false);
        stressWrap.setAlignmentX(0f);
        stressWrap.add(stressRow, BorderLayout.WEST);

        // ----- follow-up (hidden until mood and stress are both chosen) -----
        JTextArea note = new JTextArea(today.map(CheckIn::note).orElse(""), 4, 40) {
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                if (getText().isEmpty()) {
                    Graphics2D g = (Graphics2D) g0.create();
                    Brand.aa(g);
                    g.setColor(new Color(0x9CA3AF));
                    g.setFont(getFont());
                    g.drawString(PLACEHOLDER, getInsets().left, getInsets().top + g.getFontMetrics().getAscent());
                    g.dispose();
                }
            }
        };
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setFont(Theme.BASE);
        note.setForeground(Brand.INK);
        note.setBorder(new EmptyBorder(12, 14, 12, 14));
        JScrollPane noteBox = new JScrollPane(note);
        noteBox.setBorder(new LineBorder(Brand.LINE, 1, true));
        noteBox.setAlignmentX(0f);
        noteBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        ask.setFont(Theme.BASE.deriveFont(Font.BOLD, 13f));
        ask.setForeground(Brand.INK);
        help.setFont(Theme.BASE.deriveFont(11f));
        help.setForeground(Brand.MUTED);
        help.setAlignmentX(0f);
        ButtonGroup lg = new ButtonGroup();
        lg.add(en);
        lg.add(tl);
        en.addActionListener(e -> setLanguage(student, false));
        tl.addActionListener(e -> setLanguage(student, true));
        JPanel langs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        langs.setOpaque(false);
        langs.add(en);
        langs.add(tl);
        JPanel askRow = new HugPanel(new BorderLayout(10, 0));
        askRow.setOpaque(false);
        askRow.setAlignmentX(0f);
        askRow.add(ask, BorderLayout.CENTER);
        askRow.add(langs, BorderLayout.EAST);

        JPanel followUp = new JPanel();
        followUp.setOpaque(false);
        followUp.setLayout(new BoxLayout(followUp, BoxLayout.Y_AXIS));
        followUp.setAlignmentX(0f);
        followUp.add(askRow);
        followUp.add(Staff.gap(8));
        followUp.add(noteBox);
        followUp.add(Staff.gap(6));
        followUp.add(help);
        followUp.setVisible(pick[0] > 0 && pick[1] > 0);
        updateLanguageText();

        Runnable reveal = () -> {
            followUp.setVisible(pick[0] > 0 && pick[1] > 0);
            followUp.revalidate();
            followUp.repaint();
        };
        for (int i = 0; i < tiles.length; i++) {
            int mood = i + 1;
            tiles[i] = new MoodTile(mood, Kalma.MOODS[i].substring(4), pick[0] == mood, 110, () -> {
                pick[0] = mood;
                for (int j = 0; j < tiles.length; j++) tiles[j].setSelected(j + 1 == mood);
                reveal.run();
            });
            grid.add(tiles[i]);
        }
        for (int i = 0; i < STRESS.length; i++) {
            int level = i + 1;
            stressBtns[i] = new PillToggle(level + " - " + STRESS[i]);
            stressBtns[i].setSelected(pick[1] == level);
            stressBtns[i].setBorder(new EmptyBorder(9, 14, 9, 14));
            stressBtns[i].addActionListener(e -> { pick[1] = level; reveal.run(); });
            sg.add(stressBtns[i]);
            stressRow.add(stressBtns[i]);
        }

        YButton save = YButton.yellow("Save check-in  \u2192");
        save.setAlignmentX(0f);
        save.addActionListener(e -> {
            if (pick[0] == 0 || pick[1] == 0) {
                window.message("Please choose both your mood and your stress level first.");
                return;
            }
            store().saveCheckIn(new CheckIn(uid, LocalDate.now(), pick[0], pick[1], note.getText().trim()));
            window.message("Check-in saved.\n\n" + WellnessStreak.of(store().checkInsOf(uid), LocalDate.now()).message());
            window.refresh();
        });

        RoundPanel form = Staff.card("How are you feeling today?", null, null);
        form.add(Staff.gap(6));
        form.add(faces);
        form.add(Staff.gap(20));
        form.add(label("How stressed do you feel today?"));
        form.add(Staff.gap(8));
        form.add(stressWrap);
        form.add(Staff.gap(20));
        form.add(followUp);
        form.add(Staff.gap(12));
        form.add(save);

        RoundPanel recent = Staff.card("\uD83D\uDE42  Recent Check-ins", null, null);
        List<CheckIn> last = mine.stream().sorted(Comparator.comparing(CheckIn::date).reversed()).limit(5)
                .collect(Collectors.toList());
        if (last.isEmpty()) recent.add(Staff.note("No check-ins yet."));
        for (int i = 0; i < last.size(); i++) {
            CheckIn c = last.get(i);
            String when = c.date().format(StudentUI.DAY) + "  \u2022  Stress: " + STRESS[c.stress() - 1]
                    + (c.note().isEmpty() ? "" : "  \u2022  " + c.note());
            recent.add(StudentUI.listRow(Kalma.MOODS[c.mood() - 1].substring(4), when, "Logged"));
            if (i < last.size() - 1) recent.add(Staff.gap(8));
        }

        put(StudentUI.header(w, "Daily Check-In", "Take a moment to notice how you feel. There are no right or wrong answers."),
                form, recent);
    }

    private static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.BASE.deriveFont(Font.BOLD, 12f));
        l.setForeground(Brand.INK);
        l.setAlignmentX(0f);
        return l;
    }

    private void setLanguage(Student s, boolean f) {
        filipino = f;
        if (s != null) s.setAssessmentLanguage(f ? "tl" : "en");
        updateLanguageText();
    }

    private void updateLanguageText() {
        en.setSelected(!filipino);
        tl.setSelected(filipino);
        ask.setText(filipino ? ASK_TL : ASK_EN);
        help.setText(filipino
                ? "Opsyonal. Maaari mong banggitin ang mga dahilang pang-akademiko, panlipunan, o personal. Ikaw lang ang makakakita nito."
                : "Optional. You can mention academic, social, or personal factors. Only you can see this.");
    }
}
