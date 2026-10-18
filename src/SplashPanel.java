import javax.swing.*;
import java.awt.*;
class SplashPanel extends JPanel {
    SplashPanel(MainWindow window) {
        super(new GridBagLayout());
        setBackground(Brand.BLACK);

        BrandLogo logo = new BrandLogo(78, 64f, null, true);
        JLabel tag = new JLabel("Your Mental Wellness Companion");
        tag.setFont(Theme.BASE.deriveFont(15f));
        tag.setForeground(Color.WHITE);
        YButton start = YButton.yellow("Get Started  \u2192");
        start.addActionListener(e -> window.showTutorial());

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{logo, tag, start}) c.setAlignmentX(0.5f);
        box.add(logo);
        box.add(Box.createVerticalStrut(14));
        box.add(tag);
        box.add(Box.createVerticalStrut(30));
        box.add(start);
        add(box);
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        g.setFont(Theme.BASE.deriveFont(10f));
        g.setColor(new Color(0x8A8A8A));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(Kalma.NOTE, (getWidth() - fm.stringWidth(Kalma.NOTE)) / 2, getHeight() - 28);
        g.dispose();
        Brand.stripes((Graphics2D) g0, getWidth(), getHeight());
    }
}
