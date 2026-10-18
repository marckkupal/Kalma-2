import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
/** One selectable mood option: a face over its label. The selected tile is pale yellow with a yellow outline. */
class MoodTile extends JComponent {
    private final int mood;
    private final String label;
    private boolean selected;
    private final int width;
    private boolean hover;

    MoodTile(int mood, String label, boolean selected, Runnable onPick) { this(mood, label, selected, 84, onPick); }

    MoodTile(int mood, String label, boolean selected, int width, Runnable onPick) {
        this.mood = mood;
        this.label = label;
        this.selected = selected;
        this.width = width;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            @Override public void mouseClicked(MouseEvent e) { onPick.run(); }
        });
    }

    void setSelected(boolean on) { selected = on; repaint(); }

    @Override public Dimension getPreferredSize() { return new Dimension(width, 92); }
    @Override public Dimension getMaximumSize() { return getPreferredSize(); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Brand.aa(g);
        int w = getWidth(), h = getHeight();
        g.setColor(selected ? Staff.BANNER : hover ? Staff.ROW : Color.WHITE);
        g.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
        g.setColor(selected ? Brand.YELLOW_HOVER : Brand.LINE);
        g.setStroke(new BasicStroke(selected ? 1.5f : 1f));
        g.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
        double d = 30;
        MoodFace.paint(g, (w - d) / 2, 16, d, mood, selected);
        g.setFont(Theme.BASE.deriveFont(selected ? Font.BOLD : Font.PLAIN, 11f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(selected ? Brand.INK : Brand.MUTED);
        g.drawString(label, (w - fm.stringWidth(label)) / 2, h - 16);
        g.dispose();
    }
}
