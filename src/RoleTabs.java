import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
/** Segmented control: Student | Admin | Guidance. The selected tab is yellow. */
class RoleTabs extends JComponent {
    private final String[] labels;
    private final java.util.function.IntConsumer onPick;
    private int selected;

    RoleTabs(String[] labels, java.util.function.IntConsumer onPick) {
        this.labels = labels;
        this.onPick = onPick;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int cell = Math.max(1, (getWidth() - 8) / RoleTabs.this.labels.length);
                int i = Math.max(0, Math.min(RoleTabs.this.labels.length - 1, (e.getX() - 4) / cell));
                RoleTabs.this.onPick.accept(i);
            }
        });
    }

    void setSelected(int i) { selected = i; repaint(); }

    @Override public Dimension getPreferredSize() { return new Dimension(360, 36); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 36); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        int w = getWidth(), h = getHeight();
        g.setColor(Brand.TAB_BG);
        g.fillRoundRect(0, 0, w, h, 14, 14);
        int cell = (w - 8) / labels.length;
        FontMetrics fm;
        for (int i = 0; i < labels.length; i++) {
            int x = 4 + i * cell;
            boolean on = i == selected;
            if (on) {
                g.setColor(Brand.YELLOW);
                g.fillRoundRect(x, 4, cell, h - 8, 10, 10);
            }
            g.setColor(on ? Brand.INK : Brand.MUTED);
            g.setFont(Theme.BASE.deriveFont(on ? Font.BOLD : Font.PLAIN, 11f));
            fm = g.getFontMetrics();
            g.drawString(labels[i], x + (cell - fm.stringWidth(labels[i])) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
        }
        g.dispose();
    }
}
