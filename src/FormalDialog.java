import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
/** Kalma's message pop-up: white card, a yellow bar, clear wrapped text and a yellow OK button. */
final class FormalDialog {
    private FormalDialog() {}

    static void show(Component parent, String text) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(owner, "Kalma", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JPanel bar = new JPanel();
        bar.setBackground(Brand.YELLOW);
        bar.setPreferredSize(new Dimension(10, 6));
        root.add(bar, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 26, 18, 26));
        JLabel head = new JLabel("Kalma");
        head.setFont(Theme.BASE.deriveFont(Font.BOLD, 15f));
        head.setForeground(Brand.INK);
        body.add(head, BorderLayout.NORTH);
        body.add(TextWrap.block(text.replaceAll("<[^>]+>", ""), 380, Theme.BASE.deriveFont(13.5f), Brand.INK), BorderLayout.CENTER);

        YButton ok = YButton.yellow("OK");
        ok.setBorder(new EmptyBorder(7, 30, 7, 30));
        ok.addActionListener(e -> d.dispose());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        foot.setOpaque(false);
        foot.add(ok);
        body.add(foot, BorderLayout.SOUTH);
        root.add(body, BorderLayout.CENTER);

        d.setContentPane(root);
        d.getRootPane().setDefaultButton(ok);
        d.setResizable(false);
        d.pack();
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }
}
