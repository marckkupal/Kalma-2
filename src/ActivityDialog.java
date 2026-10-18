import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/**
 * Shows one grounding activity a step at a time, with Back and Next. After the last step a closing page says
 * what to do if it did not help and offers the Guidance Office.
 */
final class ActivityDialog extends JDialog {
    private final MainWindow window;
    private final GroundingActivity activity;
    private final List<String> steps;
    private final JPanel content = new JPanel(new BorderLayout());
    private int index = 0;   // steps.size() means the closing page

    private ActivityDialog(MainWindow window, GroundingActivity activity) {
        super(window, activity.title(), true);
        this.window = window;
        this.activity = activity;
        this.steps = activity.steps();
        content.setBackground(Brand.CREAM);
        content.setBorder(new EmptyBorder(22, 26, 22, 26));
        setContentPane(content);
        setSize(480, 370);
        setLocationRelativeTo(window);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        render();
    }

    static void show(MainWindow window, GroundingActivity activity) {
        new ActivityDialog(window, activity).setVisible(true);
    }

    private void render() {
        content.removeAll();
        content.add(header(), BorderLayout.NORTH);
        content.add(index < steps.size() ? stepPage() : closingPage(), BorderLayout.CENTER);
        content.add(buttons(), BorderLayout.SOUTH);
        content.revalidate();
        content.repaint();
    }

    private JComponent header() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);
        JLabel t = new JLabel(activity.title());
        t.setFont(new Font("SansSerif", Font.BOLD, 20));
        t.setForeground(Brand.INK);
        p.add(Staff.centered(new IconBadge(activity.icon(), 42)), BorderLayout.WEST);
        p.add(t, BorderLayout.CENTER);
        p.add(Staff.centered(new Pill("About " + activity.minutes() + " min", Staff.NEUTRAL_BG, Staff.NEUTRAL_FG)),
                BorderLayout.EAST);
        p.setBorder(new EmptyBorder(0, 0, 14, 0));
        return p;
    }

    private JComponent stepPage() {
        RoundPanel card = new RoundPanel(new BorderLayout(0, 12), Color.WHITE, Theme.GRID, 20);
        card.setBorder(new EmptyBorder(20, 22, 20, 22));
        JLabel count = new JLabel("Step " + (index + 1) + " of " + steps.size());
        count.setFont(Theme.BASE.deriveFont(Font.BOLD, 12f));
        count.setForeground(Brand.MUTED);
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(count, BorderLayout.NORTH);
        top.add(progress(), BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);
        card.add(new WrapText(steps.get(index), 360, Brand.INK, 16f, false), BorderLayout.CENTER);
        return wrapTop(card);
    }

    private JComponent closingPage() {
        RoundPanel card = new RoundPanel(new BorderLayout(0, 10), Color.WHITE, Theme.GRID, 20);
        card.setBorder(new EmptyBorder(20, 22, 20, 22));
        JLabel t = new JLabel("Well done for taking a moment.");
        t.setFont(Theme.BASE.deriveFont(Font.BOLD, 16f));
        t.setForeground(Brand.INK);
        card.add(t, BorderLayout.NORTH);
        card.add(new WrapText("Notice how you feel now. You can repeat this any time. "
                + "If you still feel overwhelmed, or things have felt heavy for a while, you do not have to handle it "
                + "alone. A counselor at the Guidance Office can talk it through with you.",
                360, Brand.MUTED, 13f, false), BorderLayout.CENTER);
        return wrapTop(card);
    }

    private static JComponent wrapTop(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.NORTH);
        return p;
    }

    private JComponent progress() {
        final int n = steps.size();
        final int done = index + 1;
        JComponent bar = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Brand.aa(g);
                int gap = 4, w = (getWidth() - gap * (n - 1)) / n;
                for (int i = 0; i < n; i++) {
                    g.setColor(i < done ? Brand.YELLOW : Brand.LINE);
                    g.fillRoundRect(i * (w + gap), 0, w, getHeight(), getHeight(), getHeight());
                }
                g.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(10, 6));
        return bar;
    }

    private JComponent buttons() {
        boolean closing = index >= steps.size();
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 0, 0, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        if (closing) {
            YButton book = YButton.outline("Book a consultation");
            book.addActionListener(e -> {
                dispose();
                window.open("Book Consultation");
            });
            left.add(book);
            YButton close = YButton.yellow("Close");
            close.addActionListener(e -> dispose());
            right.add(close);
        } else {
            YButton back = YButton.outline("Back");
            back.setEnabled(index > 0);
            back.addActionListener(e -> {
                index--;
                render();
            });
            left.add(back);
            YButton next = YButton.yellow(index == steps.size() - 1 ? "Finish" : "Next  \u2192");
            next.addActionListener(e -> {
                index++;
                render();
            });
            right.add(next);
            getRootPane().setDefaultButton(next);
        }
        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }
}
