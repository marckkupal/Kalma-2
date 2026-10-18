import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Academic Calendar (Admin and Guidance): add, edit or remove the school dates students see on their Calendar.
 * Students use these dates for the "Get ready" suggestions and the stress reflection, so keeping them
 * accurate matters. Changes are saved to data/academic-calendar.tsv.
 */
class AcademicCalendarAdminView extends View {
    private static final String UPCOMING = "Upcoming", PAST = "Past", ALL = "All";
    private static final String[] TYPES = {AcademicEvent.PRELIMS, AcademicEvent.MIDTERMS, AcademicEvent.FINALS,
            AcademicEvent.HOLIDAY, AcademicEvent.OTHER};
    private String filter = UPCOMING;
    private final JPanel list = new HugPanel(new BorderLayout());

    AcademicCalendarAdminView(MainWindow w) {
        super(w);
        long exams = store().academicEvents().stream().filter(AcademicEvent::isExam).count();
        RoundPanel card = Staff.card("\uD83C\uDF93  School dates", null, null);

        JPanel bar = new HugPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setAlignmentX(0f);
        bar.add(Staff.note(store().academicEvents().size() + " dates  \u2022  " + exams + " exam periods"), BorderLayout.WEST);
        YButton add = YButton.yellow("+  Add date");
        add.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        add.setBorder(new EmptyBorder(8, 18, 8, 18));
        add.addActionListener(e -> edit(null));
        bar.add(add, BorderLayout.EAST);
        card.add(bar);
        card.add(Staff.gap(10));

        JPanel chips = new HugPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        chips.setAlignmentX(0f);
        ButtonGroup g = new ButtonGroup();
        for (String c : new String[]{UPCOMING, PAST, ALL}) {
            PillToggle t = new PillToggle("  " + c + "  ");
            t.setSelected(c.equals(filter));
            t.addActionListener(e -> { filter = c; fill(); });
            g.add(t);
            chips.add(t);
        }
        card.add(chips);
        card.add(Staff.gap(10));
        list.setOpaque(false);
        list.setAlignmentX(0f);
        card.add(list);
        fill();

        if (store().academicIsSample()) {
            card.add(Staff.gap(8));
            card.add(Staff.note("These are sample dates. Edit or replace them with the official TIP Quezon City calendar."));
        }
        put(Staff.header("Academic Calendar", "Manage the exam periods, holidays and deadlines students see.",
                () -> window.open("Dashboard")), card, howItIsUsed());
    }

    private RoundPanel howItIsUsed() {
        RoundPanel c = Staff.card("How students use these dates", null, null);
        c.add(new WrapText("Exam periods (Prelims, Midterms, Finals) start the \"Get ready\" suggestions up to "
                + AcademicPlan.PLAN_DAYS + " days ahead, and a recovery suggestion for " + AcademicPlan.RECOVER_DAYS
                + " days after. Students also see how their own reported stress compares inside and outside exam time, "
                + "counting from " + AcademicPlan.LEAD_DAYS + " days before each exam. Holidays suggest rest and "
                + "\"Other\" dates (for example a deadline) suggest preparing. Students' check-ins are never shown here.",
                700, Brand.MUTED, 12f, false));
        return c;
    }

    private void fill() {
        LocalDate today = LocalDate.now();
        List<AcademicEvent> rows = store().academicEvents().stream()
                .filter(e -> filter.equals(ALL) || (filter.equals(UPCOMING) ? !e.end().isBefore(today) : e.end().isBefore(today)))
                .collect(Collectors.toList());
        if (filter.equals(PAST)) Collections.reverse(rows);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        if (rows.isEmpty()) col.add(Staff.note(filter.equals(PAST) ? "No past dates." : "No dates here. Use \"Add date\" to create one."));
        for (AcademicEvent e : rows) {
            col.add(row(e, today));
            col.add(Staff.gap(6));
        }
        list.removeAll();
        list.add(col, BorderLayout.CENTER);
        list.revalidate();
        list.repaint();
    }

    private RoundPanel row(AcademicEvent e, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, e.start());
        String when = e.covers(today) ? "Happening now" : e.end().isBefore(today) ? "Ended"
                : days == 1 ? "Tomorrow" : "In " + days + " days";
        RoundPanel row = Staff.row();
        row.add(Staff.twoLines(e.title(), e.range() + ", " + e.end().getYear() + "  \u2022  " + when), BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(new Pill(e.type(), e.bg(), e.fg()));
        YButton edit = YButton.yellow("Edit");
        edit.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        edit.setBorder(new EmptyBorder(6, 14, 6, 14));
        edit.addActionListener(x -> edit(e));
        YButton del = YButton.outline("Remove");
        del.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        del.setBorder(new EmptyBorder(6, 14, 6, 14));
        del.addActionListener(x -> remove(e));
        right.add(edit);
        right.add(del);
        row.add(Staff.centered(right), BorderLayout.EAST);
        return row;
    }

    private void remove(AcademicEvent e) {
        if (!window.confirm("Remove \"" + e.title() + "\" (" + e.range() + ") from the academic calendar?\n"
                + "Students will stop seeing it, and it will no longer count as exam time in their reflection.")) return;
        store().removeAcademic(e);
        store().log("Academic date removed", e.title());
        window.refresh();
    }

    private static JSpinner dateSpinner(LocalDate d) {
        JSpinner sp = new JSpinner(new SpinnerDateModel(java.util.Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                null, null, Calendar.DAY_OF_MONTH));
        sp.setEditor(new JSpinner.DateEditor(sp, "MMM d, yyyy"));
        return sp;
    }

    private static LocalDate valueOf(JSpinner sp) {
        return ((java.util.Date) sp.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Adds a new date when {@code old} is null, otherwise edits it. */
    private void edit(AcademicEvent old) {
        boolean isNew = old == null;
        JTextField title = Theme.field();
        title.setText(isNew ? "" : old.title());
        JComboBox<String> type = Theme.combo(TYPES);
        type.setSelectedItem(isNew ? AcademicEvent.MIDTERMS : old.type());
        LocalDate startDay = isNew ? LocalDate.now() : old.start();
        JSpinner start = dateSpinner(startDay);
        JSpinner end = dateSpinner(isNew ? startDay : old.end());
        Object[] fields = {"Title (for example \"Midterm Examinations\")", title,
                "Type (exam types start the \"Get ready\" suggestions)", type,
                "First day", start, "Last day (same as first day for a single day)", end};
        if (JOptionPane.showConfirmDialog(window, fields, isNew ? "Add school date" : "Edit school date",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String t = title.getText().trim();
        LocalDate s = valueOf(start), e = valueOf(end);
        if (t.length() < 3) { window.message("Please add a title for this date."); return; }
        if (e.isBefore(s)) { window.message("The last day cannot be before the first day."); return; }
        if (ChronoUnit.DAYS.between(s, e) > 60) { window.message("That range is longer than 60 days. Please check the dates."); return; }
        AcademicEvent made = new AcademicEvent(s, e, String.valueOf(type.getSelectedItem()), t);
        if (isNew) store().addAcademic(made);
        else store().replaceAcademic(old, made);
        store().log(isNew ? "Academic date added" : "Academic date updated", t);
        window.refresh();
    }
}
