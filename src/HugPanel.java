import javax.swing.*;
import java.awt.*;
/** Panel whose maximum height equals its preferred height, so BoxLayout does not stretch it vertically. */
class HugPanel extends JPanel {
    HugPanel(LayoutManager layout) { super(layout); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
}
