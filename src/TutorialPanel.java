import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
class TutorialPanel extends JPanel {
    // title, description, icon name (see TutorialArt)
    private static final String[][] GENERAL = {
            {"Check in with your mood", "Log how you feel each day and watch your wellness trends grow over time.", "smiley"},
            {"Explore support and resources", "Find possible solutions, guided breathing, and guides made for TIP students.", "bulb"},
            {"Connect with people who care", "Book guidance office consultations and join peer support communities.", "people"}};
    private static final String[][] STUDENT = {
            {"Check in with your mood", "Log how you feel each day and watch your wellness trends grow over time.", "smiley"},
            {"Explore support and resources", "Find possible solutions, guided breathing, and resources made for TIP students.", "bulb"},
            {"Connect with people who care", "Book guidance office consultations and join peer support communities.", "people"}};
    private static final String[][] ADMIN = {
            {"Manage users and access", "Add, review, and manage student and guidance accounts in one place.", "people"},
            {"Review wellness reports", "See anonymized mood and stress trends across sections and programs.", "chart"},
            {"Publish announcements", "Share campus-wide wellness updates and events with every student.", "bell"}};
    private static final String[][] GUIDANCE = {
            {"Manage appointments", "Confirm, reschedule, and track consultation requests from students.", "calendar"},
            {"Follow up with students", "Review check-in summaries and reach out to students who may need support.", "people"},
            {"Share helpful resources", "Publish guides and tools that students can open from their home screen.", "book"}};

    private final MainWindow window;
    private final User user;   // null when shown before login
    private final String[][] steps;
    private final TutorialArt art = new TutorialArt();
    private final JPanel content = new JPanel();
    private int step = 0;

    TutorialPanel(MainWindow window, User user) {
        super(new BorderLayout());
        this.window = window;
        this.user = user;
        this.steps = user == null ? GENERAL : switch (user.getRole()) {
            case "admin" -> ADMIN;
            case "counselor" -> GUIDANCE;
            default -> STUDENT;
        };
        setBackground(Brand.CREAM);

        // top bar: logo left, skip right
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(24, 40, 0, 40));
        YButton skip = YButton.outline("Skip tutorial");
        skip.setFont(Theme.BASE.deriveFont(11f));
        skip.setBorder(new EmptyBorder(7, 16, 7, 16));
        skip.addActionListener(e -> finish());
        JPanel skipWrap = new JPanel(new GridBagLayout());
        skipWrap.setOpaque(false);
        skipWrap.add(skip);
        top.add(new BrandLogo(28, 20f, null, false), BorderLayout.WEST);
        top.add(skipWrap, BorderLayout.EAST);

        // card: yellow art on the left, text on the right
        TutorialCard card = new TutorialCard();
        card.setPreferredSize(new Dimension(740, 300));
        art.setPreferredSize(new Dimension(300, 300));
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 36, 24, 36));
        card.add(art, BorderLayout.WEST);
        card.add(content, BorderLayout.CENTER);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(card);

        JLabel note = new JLabel(Kalma.NOTE);
        note.setFont(Theme.BASE.deriveFont(9f));
        note.setForeground(new Color(0x8A8F98));
        note.setHorizontalAlignment(SwingConstants.CENTER);
        note.setBorder(new EmptyBorder(0, 0, 24, 0));

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(note, BorderLayout.SOUTH);
        render();
    }

    private void finish() {
        if (user == null) window.showLogin();
        else window.enter(user);
    }

    private void render() {
        art.setIcon(steps[step][2]);
        content.removeAll();

        Chip chip = new Chip("Step " + (step + 1) + " of " + steps.length);
        JLabel title = new JLabel("<html><body style='width:330px'>" + steps[step][0] + "</body></html>");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Brand.INK);
        JLabel desc = Theme.wrapLabel(steps[step][1], 330, Brand.MUTED, 12f, false);
        Dots dots = new Dots(steps.length, step);

        JPanel buttons = new HugPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        if (step > 0) {
            YButton back = YButton.outline("\u2190  Back");
            back.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
            back.setBorder(new EmptyBorder(8, 16, 8, 16));
            back.addActionListener(e -> { step--; render(); });
            buttons.add(back);
            buttons.add(Box.createHorizontalStrut(8));
        }
        boolean last = step == steps.length - 1;
        YButton next = YButton.yellow(last ? (user == null ? "Log in  \u2192" : "\uD83C\uDFE0  Go to Home") : "Next  \u2192");
        next.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        next.setBorder(new EmptyBorder(8, 16, 8, 16));
        next.addActionListener(e -> { if (last) finish(); else { step++; render(); } });
        buttons.add(next);

        for (JComponent c : new JComponent[]{chip, title, desc, dots, buttons}) c.setAlignmentX(0f);
        content.add(Box.createVerticalGlue());
        content.add(chip);
        content.add(Box.createVerticalStrut(12));
        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(desc);
        content.add(Box.createVerticalStrut(14));
        content.add(dots);
        content.add(Box.createVerticalStrut(12));
        content.add(buttons);
        content.add(Box.createVerticalGlue());
        content.revalidate();
        content.repaint();
    }

    /** Rounded white card that clips its children (so the yellow art gets rounded left corners). */
    private static class TutorialCard extends JPanel {
        TutorialCard() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override public void paint(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Brand.aa(g);
            Shape shape = new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 28, 28);
            g.setColor(Color.WHITE);
            g.fill(shape);
            g.clip(shape);
            super.paint(g);
            g.dispose();
            Graphics2D b = (Graphics2D) g0.create();
            Brand.aa(b);
            b.setColor(Brand.LINE);
            b.draw(new java.awt.geom.RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 28, 28));
            b.dispose();
        }
    }

    /** Small pale-yellow label ("Step 1 of 3"). */
    private static class Chip extends JLabel {
        Chip(String text) {
            super(text);
            setFont(Theme.BASE.deriveFont(Font.BOLD, 10f));
            setForeground(Brand.INK);
            setBorder(new EmptyBorder(4, 10, 4, 10));
            setOpaque(false);
        }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Brand.aa(g);
            g.setColor(Brand.YELLOW_SOFT);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Progress dots: the current step is a long yellow pill. */
    private static class Dots extends JComponent {
        private final int count, active;
        Dots(int count, int active) { this.count = count; this.active = active; }
        @Override public Dimension getPreferredSize() { return new Dimension(count * 14 + 14, 8); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Brand.aa(g);
            int x = 0;
            for (int i = 0; i < count; i++) {
                boolean on = i == active;
                int w = on ? 22 : 6;
                g.setColor(on ? Brand.YELLOW : new Color(0xD1D5DB));
                g.fillRoundRect(x, 1, w, 6, 6, 6);
                x += w + 6;
            }
            g.dispose();
        }
    }
}
