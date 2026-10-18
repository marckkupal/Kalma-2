import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/** Admin: manage student, Peer Volunteer and administrator accounts (rules in DataStore, see docs/ADMIN_DASHBOARD.md). */
class UsersView extends View {
    private static final String[] FILTERS = {"All accounts", "Students", "Peer Volunteers", "Guidance", "Administrators", "Deactivated"};
    /** Remembered so the filter survives the page refresh after each change. */
    private static String filter = FILTERS[0];

    UsersView(MainWindow w) {
        super(w);
        String q = window.takeSearch().toLowerCase();
        List<User> users = store().getUsers().stream()
                .filter(u -> (u.getName() + " " + u.getEmail()).toLowerCase().contains(q))
                .filter(u -> matches(u, filter))
                .collect(Collectors.toList());
        List<Object[]> rows = users.stream()
                .map(u -> new Object[]{u.getName(), u.getEmail(), u.getRoleLabel(), u.isActive() ? "Active" : "Deactivated",
                        store().isVolunteer(u.getId()) ? "Peer Volunteer" : ""})
                .collect(Collectors.toList());
        JTable t = Theme.table(new String[]{"Name", "Email", "Role", "Status", "Peer Volunteer"}, rows, 170, 210, 120, 100, 110);

        JComboBox<String> show = Theme.combo(FILTERS);
        show.setSelectedItem(filter);
        show.addActionListener(e -> { filter = (String) show.getSelectedItem(); window.refresh(); });
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);
        bar.add(Theme.fieldLabel("Show"));
        bar.add(show);
        bar.add(Theme.primary("Add account", this::addAccount));

        put(Theme.heading("Manage Users"), bar, Theme.scroll(t), Theme.primary("Activate / Deactivate selected", () -> {
            int r = t.getSelectedRow();
            if (r < 0) { window.message("Select a user first."); return; }
            User u = users.get(r);
            boolean activate = !u.isActive();
            if (!activate && !window.confirm("Deactivate " + u.getName() + "? They will not be able to sign in until you activate the account again.")) return;
            String problem = store().setAccountActive(user(), u, activate);
            if (problem != null) { window.message(problem); return; }
            window.refresh();
        }), Theme.secondary("Make / remove Peer Volunteer", () -> {
            int r = t.getSelectedRow();
            if (r < 0) { window.message("Select a student first."); return; }
            User u = users.get(r);
            boolean now = !store().isVolunteer(u.getId());
            if (now && u.getRole().equals("student") && !window.confirm("Make " + u.getFirstName() + " a Peer Volunteer? They will be shown by name on all community "
                    + "posts and must follow the volunteer boundaries in the community guidelines.")) return;
            String problem = store().setVolunteerBy(user(), u, now);
            if (problem != null) { window.message(problem); return; }
            window.refresh();
        }));
    }

    private static boolean matches(User u, String f) {
        DataStore d = DataStore.getInstance();
        return switch (f) {
            case "Students" -> u.getRole().equals("student");
            case "Peer Volunteers" -> d.isVolunteer(u.getId());
            case "Guidance" -> u.getRole().equals("counselor");
            case "Administrators" -> u.getRole().equals("admin");
            case "Deactivated" -> !u.isActive();
            default -> true;
        };
    }

    /** Admin creates a student or administrator account with a starting password to share privately. */
    private void addAccount() {
        JComboBox<String> role = Theme.combo(new String[]{"Student", "Administrator"});
        JTextField name = Theme.field(), email = Theme.field();
        JComboBox<String> program = Theme.combo(Program.labels());
        JComboBox<String> year = Theme.combo(Program.YEARS);
        JPasswordField p1 = Theme.passwordField(), p2 = Theme.passwordField();
        Object[] fields = {"Account type", role, "Full name", name, "School email", email,
                "Program (students only)", program, "Year level (students only)", year,
                "Starting password (8+ characters, with a number)", p1, "Confirm password", p2};
        if (JOptionPane.showConfirmDialog(window, fields, "Add account", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String pass = new String(p1.getPassword());
        if (!pass.equals(new String(p2.getPassword()))) { window.message("Passwords do not match."); return; }
        boolean admin = role.getSelectedIndex() == 1;
        String course = admin ? "" : Program.combine(Program.values()[program.getSelectedIndex()], (String) year.getSelectedItem());
        String problem = store().createAccount(user(), admin ? "admin" : "student", name.getText(), email.getText(), pass, course);
        if (problem != null) { window.message(problem); return; }
        window.message("The account was created. Share the starting password with " + name.getText().trim() + " privately.");
        window.refresh();
    }
}
