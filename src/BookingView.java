import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
class BookingView extends View {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);

    /** Mint callout like Theme.notice, but its text wraps at a fixed width so longer copy is never clipped. */
    static JPanel noticeBox(String text) {
        RoundPanel p = new RoundPanel(new java.awt.BorderLayout(), Theme.MINT, null, 20);
        p.setBorder(new javax.swing.border.EmptyBorder(14, 18, 14, 18));
        p.add(Theme.wrapLabel(text, 600, Theme.INK, 13f, false), java.awt.BorderLayout.CENTER);
        return p;
    }

    /** "2026-10-19" becomes "Mon, Oct 19, 2026". */
    static String dayLabel(String isoDate) { return LocalDate.parse(isoDate).format(DAY); }

    BookingView(MainWindow w) {
        super(w);
        List<CounselorInfo> counselors = store().getCounselors();
        List<String> dates = DataStore.nextWeekdays(BookingRules.BOOKING_WINDOW_WEEKDAYS);
        JComboBox<String> counselor = Theme.combo(counselors.stream().map(CounselorInfo::name).toArray(String[]::new));
        JComboBox<String> date = Theme.combo(dates.stream().map(BookingView::dayLabel).toArray(String[]::new));
        JComboBox<String> time = Theme.combo(new String[0]);
        JLabel slotNote = Staff.note(" ");
        JTextField reason = Theme.field();
        List<String> open = new ArrayList<>();   // the times in the "Available time" box, in the same order
        Runnable updateSlots = () -> {
            open.clear();
            open.addAll(store().freeSlots(counselors.get(counselor.getSelectedIndex()).id(), dates.get(date.getSelectedIndex())));
            time.removeAllItems();
            open.forEach(t -> time.addItem(Staff.clock(t)));
            slotNote.setText(open.isEmpty() ? "No free times on this date. Try another day or counselor."
                    : open.size() + (open.size() == 1 ? " time" : " times") + " available. Lunch (" + BookingRules.lunchLabel() + ") is not bookable.");
        };
        counselor.addActionListener(e -> updateSlots.run());
        date.addActionListener(e -> updateSlots.run());
        updateSlots.run();

        put(Theme.heading("Book an In-Person Consultation"),
                noticeBox("Consultations are held in person at the TIP QC Guidance Office (Mon-Fri, 9 AM-4 PM, closed for lunch "
                        + BookingRules.lunchLabel() + "). You can book from tomorrow, up to "
                        + BookingRules.BOOKING_WINDOW_WEEKDAYS + " weekdays ahead, and Guidance will confirm your request. "
                        + BookingRules.studentRulesText() + " Keep the reason general; in an emergency call 1553 or 911."),
                Theme.fieldLabel("Counselor"), counselor,
                Theme.fieldLabel("Date"), date,
                Theme.fieldLabel("Available time"), time, slotNote,
                Theme.fieldLabel("General reason"), reason,
                Theme.primary("Submit request", () -> {
                    int ti = time.getSelectedIndex();
                    if (ti < 0) { window.message("No free times that day. Choose another date."); return; }
                    if (reason.getText().trim().length() < 5) { window.message("Please add a short general reason."); return; }
                    String cid = counselors.get(counselor.getSelectedIndex()).id();
                    String problem = store().addAppointment(new Appointment(user().getId(), cid,
                            dates.get(date.getSelectedIndex()), open.get(ti), reason.getText().trim(), Status.PENDING));
                    if (problem != null) { window.message(problem); updateSlots.run(); return; }
                    window.message("Appointment Request Submitted");
                    window.open("My Appointments");
                }));
    }

    /**
     * Asks for a new date and time for this appointment, offering only that counselor's free times.
     * Returns {isoDate, hhmm}, or null if the person cancelled or there was nothing to choose.
     * Used by the student's My Appointments page and the Guidance Appointments page.
     */
    static String[] pickNewSlot(MainWindow window, Appointment a, int weekdaysAhead) {
        DataStore store = DataStore.getInstance();
        List<String> dates = DataStore.nextWeekdays(weekdaysAhead);
        JComboBox<String> d = Theme.combo(dates.stream().map(BookingView::dayLabel).toArray(String[]::new));
        JComboBox<String> t = Theme.combo(new String[0]);
        JLabel note = Staff.note(" ");
        List<String> free = new ArrayList<>();
        Runnable update = () -> {
            free.clear();
            free.addAll(store.freeSlots(a.getCounselorId(), dates.get(d.getSelectedIndex())));
            t.removeAllItems();
            free.forEach(s -> t.addItem(Staff.clock(s)));
            note.setText(free.isEmpty() ? "No free times on this date." : "Lunch is not bookable.");
        };
        d.addActionListener(e -> update.run());
        update.run();
        int ok = JOptionPane.showConfirmDialog(window, new Object[]{"New date", d, "New time", t, note},
                "Reschedule", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return null;
        if (t.getSelectedIndex() < 0) { window.message("No free times on that date. Choose another."); return null; }
        return new String[]{dates.get(d.getSelectedIndex()), free.get(t.getSelectedIndex())};
    }
}
