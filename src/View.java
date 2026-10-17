import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
abstract class View extends JPanel implements Scrollable {
    protected final MainWindow window;

    protected View(MainWindow window) {
        this.window = window;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Brand.CREAM);
        setBorder(new EmptyBorder(24, 28, 24, 28));
    }

    /** Adds components top-to-bottom, left aligned, with consistent spacing. */
    protected void put(Component... components) {
        for (Component c : components) {
            ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            add(c);
            JComponent gap = (JComponent) Box.createVerticalStrut(12);
            gap.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(gap);
        }
    }

    protected User user() { return window.getUser(); }
    protected DataStore store() { return DataStore.getInstance(); }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
