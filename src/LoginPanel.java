import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
class LoginPanel extends JPanel {
    /** label, demo email, demo password, role id used by the data model */
    private static final String[][] ROLES = {
            {"Student", "student@tip.edu.ph", "student123", "student"},
            {"Admin", "admin@tip.edu.ph", "admin123", "admin"},
            {"Guidance", "guidance@tip.edu.ph", "guidance123", "counselor"}};

    private final MainWindow window;
    private final JTextField email = new JTextField();
    private final JPasswordField pw = new JPasswordField();
    private final YButton loginBtn = YButton.yellow("Log in");
    private RoleTabs tabs;
    private int role = 0;

    LoginPanel(MainWindow window) {
        super(new GridBagLayout());
        this.window = window;
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.4;
        add(buildLeft(), c);
        c.gridx = 1;
        c.weightx = 0.6;
        add(buildRight(), c);
        selectRole(0);
    }

    private JPanel buildLeft() {
        JPanel left = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(Brand.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
                Brand.stripes((Graphics2D) g0, getWidth(), getHeight());
            }
        };
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(36, 40, 40, 40));
        left.setPreferredSize(new Dimension(400, 700));

        BrandLogo logo = new BrandLogo(34, 26f, "Your Mental Wellness Companion", true);
        JLabel headline = new JLabel("<html>A safe space<br>for every student.</html>");
        headline.setFont(new Font("SansSerif", Font.BOLD, 30));
        headline.setForeground(Color.WHITE);
        JLabel sub = Theme.wrapLabel("Check in, find support, and connect with people who care about you.",
                300, new Color(0xC9CACB), 12f, false);
        for (JComponent c : new JComponent[]{logo, headline, sub}) c.setAlignmentX(0f);

        left.add(logo);
        left.add(Box.createVerticalGlue());
        left.add(headline);
        left.add(Box.createVerticalStrut(16));
        left.add(sub);
        left.add(Box.createVerticalGlue());
        left.add(Box.createVerticalGlue());
        return left;
    }

    private JPanel buildRight() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Brand.CREAM);
        right.setPreferredSize(new Dimension(600, 700));

        JLabel title = new JLabel("Welcome back");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Brand.INK);
        JLabel hint = new JLabel("Choose your access and log in to continue.");
        hint.setFont(Theme.BASE.deriveFont(11f));
        hint.setForeground(Brand.MUTED);

        tabs = new RoleTabs(new String[]{ROLES[0][0], ROLES[1][0], ROLES[2][0]}, this::selectRole);

        email.addActionListener(e -> login());
        pw.addActionListener(e -> login());
        IconInput emailBox = new IconInput("\u2709", email);
        IconInput pwBox = new IconInput("\uD83D\uDD12", pw);

        loginBtn.setBorder(new EmptyBorder(12, 22, 12, 22));
        loginBtn.addActionListener(e -> login());
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JButton create = new JButton("Create student account");
        create.setBorderPainted(false);
        create.setContentAreaFilled(false);
        create.setFocusPainted(false);
        create.setForeground(Brand.MUTED);
        create.setFont(Theme.BASE.deriveFont(11f));
        create.setMargin(new Insets(0, 0, 0, 0));
        create.setHorizontalAlignment(SwingConstants.LEFT);
        create.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        create.addActionListener(e -> register());

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        Component[] items = {
                Box.createRigidArea(new Dimension(380, 0)),
                title, Box.createVerticalStrut(6), hint, Box.createVerticalStrut(18),
                tabs, Box.createVerticalStrut(18),
                Theme.fieldLabel("Email"), Box.createVerticalStrut(5), emailBox, Box.createVerticalStrut(12),
                Theme.fieldLabel("Password"), Box.createVerticalStrut(5), pwBox, Box.createVerticalStrut(18),
                loginBtn, Box.createVerticalStrut(12), create};
        for (Component c : items) {
            ((JComponent) c).setAlignmentX(0f);
            form.add(c);
        }

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(form);

        JLabel note = new JLabel("<html><div style='text-align:center;width:360px'>" + Kalma.NOTE + "</div></html>");
        note.setFont(Theme.BASE.deriveFont(9f));
        note.setForeground(new Color(0x8A8F98));
        note.setHorizontalAlignment(SwingConstants.CENTER);
        note.setBorder(new EmptyBorder(0, 0, 28, 0));

        right.add(center, BorderLayout.CENTER);
        right.add(note, BorderLayout.SOUTH);
        return right;
    }

    private void selectRole(int i) {
        role = i;
        tabs.setSelected(i);
        email.setText(ROLES[i][1]);
        pw.setText(ROLES[i][2]);
        loginBtn.setText("Log in as " + ROLES[i][0] + "  \u2192");
    }

    private void login() {
        Optional<User> found = DataStore.getInstance().findByEmail(email.getText().trim());
        if (found.isEmpty() || !found.get().passwordMatches(new String(pw.getPassword()))) {
            window.message("Email or password is incorrect.");
            return;
        }
        User u = found.get();
        if (!u.isActive()) { window.message("This account is deactivated. Contact the Admin."); return; }
        if (!u.getRole().equals(ROLES[role][3])) {
            window.message("This account is not a " + ROLES[role][0] + " account. Pick the right tab and try again.");
            return;
        }
        window.afterLogin(u);
    }

    private void register() {
        JTextField name = Theme.field(), em0 = Theme.field();
        JComboBox<String> program = Theme.combo(Program.labels());
        program.setSelectedIndex(-1);
        JComboBox<String> year = Theme.combo(Program.YEARS);
        JPasswordField p1 = Theme.passwordField(), p2 = Theme.passwordField();
        Object[] fields = {"Full name", name, "School email", em0,
                "Program (TIP Quezon City)", program, "Year level", year,
                "Password (8+ characters, with a number)", p1, "Confirm password", p2};
        if (JOptionPane.showConfirmDialog(window, fields, "Create student account", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String pass = new String(p1.getPassword());
        String em = em0.getText().trim().toLowerCase();
        DataStore store = DataStore.getInstance();
        if (name.getText().isBlank()) window.message("Full name is required.");
        else if (program.getSelectedIndex() < 0) window.message("Please select your program.");
        else if (!em.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) window.message("Enter a valid email address.");
        else if (store.findByEmail(em).isPresent()) window.message("That email is already registered.");
        else if (pass.length() < 8 || !pass.matches(".*\\d.*")) window.message("Password needs 8+ characters and at least one number.");
        else if (!pass.equals(new String(p2.getPassword()))) window.message("Passwords do not match.");
        else {
            Program chosen = Program.values()[program.getSelectedIndex()];
            String course = Program.combine(chosen, (String) year.getSelectedItem());
            Student st = new Student("u" + UUID.randomUUID().toString().substring(0, 6), name.getText().trim(), em, pass, course);
            store.addUser(st);
            window.afterLogin(st);
        }
    }
}
