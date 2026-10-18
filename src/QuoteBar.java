import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
/** Pale yellow quote strip with four shortcuts and a yellow corner wedge. */
class QuoteBar extends JPanel {
    QuoteBar(String quote, String by, java.util.function.Consumer<String> open, String[]... links) {
        super(new BorderLayout(20, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 22, 16, 28));

        JLabel mark = new JLabel("“");
        mark.setFont(new Font("Serif", Font.BOLD, 46));
        mark.setForeground(Brand.INK);
        mark.setVerticalAlignment(SwingConstants.TOP);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        for (String line : quote.split("<br>")) {
            JLabel l = new JLabel(line);
            l.setFont(Theme.BASE.deriveFont(14f));
            l.setForeground(Brand.INK);
            l.setAlignmentX(0f);
            text.add(l);
        }
        JLabel src = new JLabel(by);
        src.setFont(Theme.BASE.deriveFont(11f));
        src.setForeground(Brand.MUTED);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        text.setAlignmentX(0f);
        src.setAlignmentX(0f);
        col.add(text);
        col.add(Box.createVerticalStrut(4));
        col.add(src);
        JPanel left = new JPanel(new BorderLayout(10, 0));
        left.setOpaque(false);
        left.add(mark, BorderLayout.WEST);
        left.add(Staff.centered(col), BorderLayout.CENTER);

        JPanel right = new JPanel(new GridLayout(1, links.length, 0, 0));
        right.setOpaque(false);
        for (int i = 0; i < links.length; i++) {
            String[] it = links[i];
            JPanel item = new JPanel();
            item.setOpaque(false);
            item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
            item.setBorder(new CompoundBorder(new MatteBorder(0, i == 0 ? 0 : 1, 0, 0, new Color(0xE9DFB5)),
                    new EmptyBorder(2, 10, 2, 10)));
            JLabel ic = new JLabel(it[0]);
            ic.setFont(Theme.BASE.deriveFont(18f));
            JLabel lb = new JLabel(it[1]);
            lb.setFont(Theme.BASE.deriveFont(11f));
            lb.setForeground(Brand.INK);
            ic.setAlignmentX(0.5f);
            lb.setAlignmentX(0.5f);
            item.add(ic);
            item.add(Box.createVerticalStrut(3));
            item.add(lb);
            Staff.click(item, () -> open.accept(it[2]));
            right.add(item);
        }
        add(left, BorderLayout.CENTER);
        add(Staff.centered(right), BorderLayout.EAST);
    }

    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        int w = getWidth(), h = getHeight();
        RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, w, h, 24, 24);
        g.setColor(Staff.BANNER);
        g.fill(shape);
        Graphics2D s = (Graphics2D) g.create();
        s.clip(shape);
        Path2D big = new Path2D.Double();
        big.moveTo(w, h - 64);
        big.lineTo(w, h);
        big.lineTo(w - 64, h);
        big.closePath();
        s.setColor(Brand.YELLOW);
        s.fill(big);
        Path2D small = new Path2D.Double();
        small.moveTo(w, h - 26);
        small.lineTo(w, h);
        small.lineTo(w - 26, h);
        small.closePath();
        s.setColor(Brand.BLACK);
        s.fill(small);
        s.dispose();
        g.setColor(new Color(0xF1E3A0));
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 24, 24));
        g.dispose();
    }
}
