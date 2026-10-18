import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Student "Export Summary" (docs/WELLNESS_EXPORT.md). The student picks a period and what to include, sees exactly
 * what will be exported, and can save it as a PDF or CSV for printing. Sharing with a Guidance counselor is a
 * separate, voluntary step that needs the student's agreement on a consent screen every time, and can be stopped later.
 */
class ExportView extends View {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);
    private static final int[] PERIODS = {7, 30, 90, WellnessExport.ALL_TIME};
    private static final String[] PERIOD_LABELS = {"Last 7 days", "Last 30 days", "Last 90 days", "All saved records"};

    private int days = 30;
    private final JCheckBox checkIns = box("Check-in records (mood and stress for each day)", true);
    private final JCheckBox assessments = box("Assessment summaries (scores and main sources of stress)", true);
    private final JCheckBox trends = box("Wellness trends (averages, a weekly table and a mood and stress chart)", true);
    private final JCheckBox notes = box("Also include the private notes I wrote in my check-ins", false);
    private final JLabel count = new JLabel();
    private final JTextArea preview = new JTextArea(12, 60);
    private final JPanel sharedList = new JPanel();

    ExportView(MainWindow w) {
        super(w);

        // 1. what to include
        RoundPanel choose = Staff.card("1   Choose what to include", null, null);
        PillToggle[] pills = new PillToggle[PERIODS.length];
        ButtonGroup bg = new ButtonGroup();
        JPanel pillRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pillRow.setOpaque(false);
        pillRow.setAlignmentX(0f);
        for (int i = 0; i < PERIODS.length; i++) {
            final int d = PERIODS[i];
            pills[i] = new PillToggle(PERIOD_LABELS[i]);
            pills[i].addActionListener(e -> { days = d; update(); });
            bg.add(pills[i]);
            pillRow.add(pills[i]);
        }
        pills[1].setSelected(true);
        choose.add(pillRow);
        choose.add(Staff.gap(10));
        for (JCheckBox c : List.of(checkIns, assessments, trends, notes)) {
            c.addActionListener(e -> update());
            choose.add(c);
            choose.add(Staff.gap(2));
        }
        choose.add(Staff.gap(4));
        choose.add(Staff.note("Your notes stay out unless you switch them on. Nothing is saved or shared until you choose to."));

        // 2. preview
        RoundPanel prev = Staff.card("2   Check what will be included", null, null);
        count.setFont(Theme.BASE.deriveFont(Font.BOLD, 12f));
        count.setForeground(Brand.INK);
        count.setAlignmentX(0f);
        prev.add(count);
        prev.add(Staff.gap(8));
        preview.setEditable(false);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        preview.setBackground(Theme.BG);
        preview.setForeground(Theme.INK);
        preview.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane sp = new JScrollPane(preview);
        sp.setBorder(new LineBorder(Theme.GRID, 1, true));
        sp.setPreferredSize(new Dimension(600, 240));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        sp.setAlignmentX(0f);
        prev.add(sp);
        prev.add(Staff.gap(6));
        prev.add(Staff.note("The PDF also shows your mood and stress chart. Days without a check-in are left blank."));

        // 3. save or share
        RoundPanel out = Staff.card("3   Save a copy or share it", null, null);
        YButton pdf = YButton.yellow("Save as PDF");
        YButton csv = YButton.outline("Save as CSV");
        YButton share = YButton.outline("Share with Guidance counselor");
        pdf.addActionListener(e -> save("pdf"));
        csv.addActionListener(e -> save("csv"));
        share.addActionListener(e -> share());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(0f);
        buttons.add(pdf);
        buttons.add(csv);
        buttons.add(share);
        out.add(buttons);
        out.add(Staff.gap(8));
        out.add(Staff.note("PDF is best for printing. CSV opens in Excel or Google Sheets. A saved file is on your own device, so keep it somewhere private."));
        out.add(Staff.gap(2));
        out.add(Staff.note("Sharing is your choice and never required. You will be asked to agree before anything is sent."));

        // already shared
        sharedList.setOpaque(false);
        sharedList.setLayout(new BoxLayout(sharedList, BoxLayout.Y_AXIS));
        sharedList.setAlignmentX(0f);

        put(StudentUI.header(w, "Export Wellness Summary",
                        "Make a printable summary of your check-ins, assessments and trends. Only you decide who sees it."),
                choose, prev, out, sharedList);
        update();
        renderShared();
    }

    private static JCheckBox box(String text, boolean on) {
        JCheckBox c = new JCheckBox(text, on);
        c.setOpaque(false);
        c.setFont(Theme.BASE);
        c.setForeground(Brand.INK);
        c.setAlignmentX(0f);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return c;
    }

    // ----- building the report from the page choices -----
    private WellnessExport.Options options() {
        return new WellnessExport.Options(days, checkIns.isSelected(), assessments.isSelected(), trends.isSelected(),
                notes.isSelected() && checkIns.isSelected());
    }

    private WellnessExport.Report report() {
        String uid = user().getId();
        return WellnessExport.build(user().getName(), user().getCourse(), store().checkInsOf(uid), store().resultsOf(uid),
                options(), LocalDate.now());
    }

    private void update() {
        notes.setEnabled(checkIns.isSelected());
        WellnessExport.Report r = report();
        if (!r.options().any()) {
            count.setText("Choose at least one thing to include.");
            preview.setText("");
            return;
        }
        List<String> parts = new ArrayList<>();
        if (r.options().checkIns()) parts.add(r.checkIns().size() + " check-in" + (r.checkIns().size() == 1 ? "" : "s"));
        if (r.options().assessments()) parts.add(r.assessments().size() + " assessment" + (r.assessments().size() == 1 ? "" : "s"));
        if (r.options().trends()) parts.add("trends");
        count.setText(r.options().periodLabel() + "  •  " + String.join("  •  ", parts));
        preview.setText(WellnessExport.toText(r));
        preview.setCaretPosition(0);
    }

    /** A report is ready to use only when something is chosen and there is something to put in it. */
    private WellnessExport.Report readyReport() {
        WellnessExport.Report r = report();
        if (!r.options().any()) { window.message("Choose at least one thing to include first."); return null; }
        if (r.isEmpty()) {
            window.message("There is nothing to include for these choices yet, and that is okay.\n"
                    + "Try a longer period, or come back after a few check-ins.");
            return null;
        }
        return r;
    }

    // ----- save as PDF or CSV -----
    private void save(String ext) {
        WellnessExport.Report r = readyReport();
        if (r == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save wellness summary as " + ext.toUpperCase());
        String suggested = WellnessExport.fileName(LocalDate.now(), ext);
        fc.setSelectedFile(new File(fc.getCurrentDirectory(), suggested));
        fc.setFileFilter(new FileNameExtensionFilter(ext.toUpperCase() + " files", ext));
        if (fc.showSaveDialog(window) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        // A bare file name would be saved next to the app, not in the folder the student picked.
        if (!f.isAbsolute()) f = new File(fc.getCurrentDirectory(), f.getName());
        if (!f.getName().toLowerCase(Locale.ROOT).endsWith("." + ext)) f = new File(f.getPath() + "." + ext);
        if (f.exists() && JOptionPane.showConfirmDialog(window, f.getName() + " already exists. Replace it?", "Replace file",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            Files.write(f.toPath(), ext.equals("pdf") ? WellnessExport.toPdf(r) : WellnessExport.toCsv(r));
            window.message("Saved to " + f.getPath() + "\n\nThis file is on your device only. Keep it somewhere private.");
        } catch (IOException ex) {
            window.message("Sorry, the file could not be saved (" + ex.getMessage() + "). Please try another folder.");
        }
    }

    // ----- sharing, with consent -----
    private void share() {
        WellnessExport.Report r = readyReport();
        if (r == null) return;
        Optional<User> to = store().shareRecipient();
        if (to.isEmpty()) { window.message("No counselor is available right now."); return; }
        String counselor = to.get().getRoleLabel() + ", " + to.get().getName();
        if (!askConsent(r, counselor)) return;
        try {
            store().shareSummary(user(), r, true);
            window.message("Shared with the Guidance Office.\n\nYou can stop sharing at any time from this page.");
        } catch (RuntimeException ex) {
            window.message("The summary was not shared: " + ex.getMessage());
        }
        renderShared();
    }

    /** The consent screen. Share stays switched off until the student ticks the box. Returns true only on agreement. */
    private boolean askConsent(WellnessExport.Report r, String counselor) {
        Window owner = window;
        JDialog d = new JDialog(owner, "Share with Guidance counselor", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 20, 12, 20));
        body.setBackground(Color.WHITE);

        JLabel title = new JLabel("Share this summary with " + counselor + "?");
        title.setFont(Theme.BASE.deriveFont(Font.BOLD, 15f));
        title.setForeground(Brand.INK);
        title.setAlignmentX(0f);
        body.add(title);
        body.add(Staff.gap(8));
        String[] points = {
                "Sharing is your choice. You can use every part of Kalma without sharing anything.",
                "Included: " + r.options().sectionsLabel() + ", for " + WellnessExport.range(r) + ".",
                "Only the Guidance counselor can read it. It is not shown to other students or to administrators.",
                "They will see exactly the text below. Check-ins you add later are not added to it.",
                "You can stop sharing at any time from this page, and the counselor will no longer be able to open it.",
                "This is not an emergency service. If you need help right away, call 1553 or 911."};
        for (String p : points) {
            JLabel l = Theme.wrapLabel("•  " + p, 520, Brand.INK, 12f, false);
            l.setAlignmentX(0f);
            body.add(l);
            body.add(Staff.gap(4));
        }
        body.add(Staff.gap(4));
        JTextArea shown = new JTextArea(WellnessExport.toText(r), 10, 56);
        shown.setEditable(false);
        shown.setLineWrap(true);
        shown.setWrapStyleWord(true);
        shown.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        shown.setBackground(Theme.BG);
        shown.setForeground(Theme.INK);
        shown.setBorder(new EmptyBorder(8, 10, 8, 10));
        shown.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(shown);
        sp.setBorder(new LineBorder(Theme.GRID, 1, true));
        sp.setAlignmentX(0f);
        body.add(sp);
        body.add(Staff.gap(10));

        JCheckBox agree = box("I have read this and I agree to share this summary with " + counselor + ".", false);
        body.add(agree);
        body.add(Staff.gap(10));

        final boolean[] ok = {false};
        YButton cancel = YButton.outline("Not now");
        YButton go = YButton.yellow("Share summary");
        go.setEnabled(false);
        agree.addActionListener(e -> go.setEnabled(agree.isSelected()));
        cancel.addActionListener(e -> d.dispose());
        go.addActionListener(e -> { ok[0] = agree.isSelected(); d.dispose(); });
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        foot.setOpaque(false);
        foot.setAlignmentX(0f);
        foot.add(cancel);
        foot.add(go);
        body.add(foot);

        d.setContentPane(body);
        d.pack();
        d.setLocationRelativeTo(window);
        d.setVisible(true);
        return ok[0];
    }

    // ----- what the student has already shared -----
    private void renderShared() {
        sharedList.removeAll();
        RoundPanel card = Staff.card("Shared with Guidance", null, null);
        List<SharedSummary> mine = store().sharesBy(user().getId());
        if (mine.isEmpty()) {
            card.add(Staff.note("You have not shared any summaries. If you do, they will be listed here and you can stop sharing at any time."));
        }
        for (SharedSummary s : mine) {
            RoundPanel row = Staff.row();
            row.add(Staff.twoLines(s.period(), "Shared " + s.sharedAt().format(WHEN) + "  •  " + s.sections()
                    + (s.isActive() ? "" : "  •  stopped " + s.revokedAt().format(WHEN))), BorderLayout.CENTER);
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            right.setOpaque(false);
            right.add(s.isActive() ? new Pill("Shared", Staff.GOOD_BG, Staff.GOOD_FG)
                    : new Pill("Stopped", Staff.NEUTRAL_BG, Staff.NEUTRAL_FG));
            if (s.isActive()) {
                YButton stop = YButton.outline("Stop sharing");
                stop.setBorder(new EmptyBorder(6, 14, 6, 14));
                stop.addActionListener(e -> {
                    if (!window.confirm("Stop sharing this summary?\nThe counselor will no longer be able to open it.")) return;
                    store().revokeShare(s.id(), user().getId());
                    renderShared();
                });
                right.add(stop);
            }
            row.add(Staff.centered(right), BorderLayout.EAST);
            card.add(row);
            card.add(Staff.gap(6));
        }
        sharedList.add(card);
        sharedList.revalidate();
        sharedList.repaint();
    }
}
