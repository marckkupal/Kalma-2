import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Student "Guidance Office" (Figma "Student - Guidance Office"): three action cards and the student's
 * upcoming appointments.
 */
class GuidanceOfficeView extends View {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    GuidanceOfficeView(MainWindow w) {
        super(w);
        JPanel cards = StudentUI.row(14,
                StudentUI.actionCard("\uD83D\uDCC5", "Book a Consultation",
                        "Choose a date and time that works for you.", "Book now",
                        () -> window.open("Book Consultation")),
                StudentUI.actionCard("\uD83D\uDCCD", "Find the Office",
                        "T.I.P. QC Guidance Office, Quezon City.", "Back to home",
                        () -> window.open("Home")),
                StudentUI.actionCard("\uD83D\uDCAC", "Message a Counselor",
                        "Ask a question before your visit.", "Send message",
                        this::sendMessage));

        RoundPanel upcoming = Staff.card("\uD83D\uDCC5  Upcoming Appointments", null, null);
        List<Appointment> mine = store().appointmentsFor(user()).stream()
                .filter(Appointment::isUpcoming).collect(Collectors.toList());
        if (mine.isEmpty()) upcoming.add(Staff.note("No upcoming appointments. Book one above."));
        for (Appointment a : mine) {
            RoundPanel r = Staff.row();
            r.add(Staff.twoLines("In-Person Consultation", LocalDate.parse(a.getDate()).format(DAY)
                    + " \u2022 " + Staff.clock(a.getTime()) + " \u2022 T.I.P. QC Guidance Office"), BorderLayout.CENTER);
            r.add(Staff.centered(Staff.statusPill(a.getStatus())), BorderLayout.EAST);
            Staff.click(r, () -> window.open("My Appointments"));
            upcoming.add(r);
            upcoming.add(Staff.gap(6));
        }

        put(StudentUI.header(w, "Guidance Office", "Book time with a counselor at the T.I.P. Quezon City Guidance Office."));
        List<String> reminders = store().appointmentReminders(user());
        if (!reminders.isEmpty()) put(Staff.reminders(reminders));
        put(cards, upcoming);
    }

    /** Sends a short question to the Guidance counselor's inbox (shown on the Guidance "Messages" page). */
    private void sendMessage() {
        Optional<User> counselor = store().getUsers().stream()
                .filter(u -> u.getRole().equals("counselor") && u.isActive()).findFirst();
        if (counselor.isEmpty()) { window.message("No counselor is available right now."); return; }
        JTextArea msg = new JTextArea(5, 34);
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        msg.setFont(Theme.BASE);
        msg.setBackground(Theme.BG);
        msg.setForeground(Theme.INK);
        msg.setCaretColor(Theme.INK);
        msg.setBorder(new EmptyBorder(7, 10, 7, 10));
        JScrollPane sp = new JScrollPane(msg);
        sp.setBorder(new LineBorder(Theme.GRID, 1, true));
        Object[] fields = {"Ask a question before your visit. A counselor will reply in person or by school email.\n"
                + "Keep it general. In an emergency call 1553 or 911.", sp};
        if (JOptionPane.showConfirmDialog(window, fields, "Message a Counselor",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String text = msg.getText().trim();
        if (text.length() < 5) { window.message("Please write a short message first."); return; }
        store().addMessage(new Message(counselor.get().getId(), user().getId(), text, LocalDateTime.now()));
        window.message("Message sent to the Guidance Office.");
    }
}
