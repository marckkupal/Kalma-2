import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
/**
 * Student "Profile" (Figma "Student - Profile"): identity card and an Account list with
 * Notification preferences, Privacy and Log out.
 */
class ProfileView extends View {
    ProfileView(MainWindow w) {
        super(w);
        User u = user();

        String program = programName(u.getCourse());
        String line = u.getRoleLabel() + (program.isEmpty() ? "" : " \u2022 " + program) + " \u2022 T.I.P. Quezon City";

        JLabel name = new JLabel(u.getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 26));
        name.setForeground(Brand.INK);
        JLabel details = new JLabel(line);
        details.setFont(Theme.BASE.deriveFont(14f));
        details.setForeground(Brand.MUTED);
        JLabel mail = new JLabel(u.getEmail());
        mail.setFont(Theme.BASE.deriveFont(13f));
        mail.setForeground(Brand.MUTED);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{name, details, mail}) c.setAlignmentX(0f);
        col.add(name);
        col.add(Staff.gap(6));
        col.add(details);
        col.add(Staff.gap(6));
        col.add(mail);

        RoundPanel who = new RoundPanel(new BorderLayout(24, 0), Color.WHITE, Theme.GRID, 20);
        who.setBorder(new EmptyBorder(22, 28, 22, 28));
        who.add(Staff.centered(new Avatar(u.getInitials(), Brand.BLACK, Brand.YELLOW, 88)), BorderLayout.WEST);
        who.add(StudentUI.leftMiddle(col), BorderLayout.CENTER);

        RoundPanel account = Staff.card("\uD83D\uDC64  Account", null, null);
        account.add(settingRow("Notification preferences", "Reminders for check-ins, appointments, school dates and announcements", this::notifications));
        account.add(Staff.gap(6));
        account.add(settingRow("Privacy", "Choose what you share with peers", this::privacy));
        account.add(Staff.gap(6));
        if (u instanceof Student) {
            account.add(settingRow("Program", programName(u.getCourse()) + " \u2022 " + Program.yearOf(u.getCourse()), this::editProgram));
            account.add(Staff.gap(6));
        }
        RoundPanel out = Staff.row();
        out.add(Staff.twoLines("Log out", "Sign out of Kalma"), BorderLayout.CENTER);
        Staff.click(out, () -> {
            if (window.confirm("Log out of Kalma?")) window.showLogin();
        });
        account.add(out);

        put(StudentUI.header(w, "Profile", "Manage your account and preferences."), who, account);
    }

    /** "BSCS 3rd Year" becomes "BSCS - Bachelor of Science in Computer Science". Unknown programs are shown as typed. */
    private static String programName(String course) {
        Program p = Program.fromCourse(course);
        return p != null ? p.toString() : course.replaceAll("\\s*\\d.*$", "").trim();
    }

    private JPanel settingRow(String title, String sub, Runnable edit) {
        RoundPanel r = Staff.row();
        r.add(Staff.twoLines(title, sub), BorderLayout.CENTER);
        YButton b = YButton.yellow("Edit");
        b.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        b.setBorder(new EmptyBorder(5, 14, 5, 14));
        b.addActionListener(e -> edit.run());
        r.add(Staff.centered(b), BorderLayout.EAST);
        return r;
    }

    /** Lets the student change their program and year level, then reopens the profile so it shows the new value. */
    private void editProgram() {
        Program current = Program.fromCourse(user().getCourse());
        JComboBox<String> program = Theme.combo(Program.labels());
        if (current != null) program.setSelectedIndex(current.ordinal());
        JComboBox<String> year = Theme.combo(Program.YEARS);
        year.setSelectedItem(Program.yearOf(user().getCourse()));
        Object[] fields = {"Program (TIP Quezon City)", program, "Year level", year};
        if (JOptionPane.showConfirmDialog(window, fields, "Update program", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        Program chosen = Program.values()[program.getSelectedIndex()];
        user().setCourse(Program.combine(chosen, (String) year.getSelectedItem()));
        window.message("Your program was updated to " + chosen.abbr + ".");
        window.refresh();
    }

    private void notifications() { notificationPreferences(window); }

    /** Notification choices: what the bell and the Home page remind the student about, and the optional email digest. */
    static void notificationPreferences(MainWindow window) {
        User u = window.getUser();
        DataStore store = DataStore.getInstance();
        String[][] options = Notifications.STUDENT_OPTIONS;
        boolean email = Notifications.emailAvailable();
        JCheckBox[] boxes = new JCheckBox[options.length + (email ? 1 : 0)];
        List<Object> fields = new ArrayList<>();
        String[] keys = new String[boxes.length];
        for (int i = 0; i < boxes.length; i++) {
            keys[i] = i < options.length ? options[i][0] : Notifications.EMAIL_OPTION;
            String label = i < options.length ? options[i][1] : "Also email these reminders to " + u.getEmail() + " (at most once a day)";
            boxes[i] = new JCheckBox(label, store.pref(u.getId(), keys[i]));
            boxes[i].setOpaque(false);
            boxes[i].setForeground(Theme.INK);
            fields.add(boxes[i]);
        }
        if (!email) {
            JLabel off = new JLabel("Email reminders are not set up for this copy of Kalma, so reminders appear in the app only.");
            off.setFont(Theme.BASE.deriveFont(11f));
            off.setForeground(Brand.MUTED);
            fields.add(off);
        }
        if (JOptionPane.showConfirmDialog(window, fields.toArray(), "Notification preferences", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        for (int i = 0; i < boxes.length; i++) store.setPref(u.getId(), keys[i], boxes[i].isSelected());
        window.message("Your notification preferences were saved.");
        window.refresh();
    }

    private void privacy() {
        choose("Privacy", new String[][]{
                {"community-course", "Show my program and year next to my name on community posts"}});
    }

    /** Shows one checkbox per option, saves the answers, and tells the student it worked. */
    private void choose(String title, String[][] options) {
        JCheckBox[] boxes = new JCheckBox[options.length];
        Object[] fields = new Object[options.length];
        for (int i = 0; i < options.length; i++) {
            boxes[i] = new JCheckBox(options[i][1], store().pref(user().getId(), options[i][0]));
            boxes[i].setOpaque(false);
            boxes[i].setForeground(Theme.INK);
            fields[i] = boxes[i];
        }
        if (JOptionPane.showConfirmDialog(window, fields, title, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        for (int i = 0; i < options.length; i++) store().setPref(user().getId(), options[i][0], boxes[i].isSelected());
        window.message("Your " + title.toLowerCase() + " were saved.");
    }
}
