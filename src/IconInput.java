import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
/** White rounded input with a leading icon. Wraps a JTextField or JPasswordField. */
class IconInput extends JPanel {
    IconInput(String icon, JTextField field) {
        super(new BorderLayout(8, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 12, 0, 12));
        JLabel ic = new JLabel(icon);
        ic.setFont(Theme.BASE.deriveFont(12f));
        ic.setForeground(Brand.MUTED);
        field.setBorder(new EmptyBorder(9, 0, 9, 0));
        field.setOpaque(false);
        field.setFont(Theme.BASE.deriveFont(12f));
        field.setForeground(Brand.INK);
        field.setCaretColor(Brand.INK);
        add(ic, BorderLayout.WEST);
        add(field, BorderLayout.CENTER);
    }

    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        g.setColor(Color.WHITE);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g.setColor(Brand.LINE);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        g.dispose();
        super.paintComponent(g0);
    }
}
