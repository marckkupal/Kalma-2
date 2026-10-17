import javax.swing.*;
import java.awt.*;
/** Small yellow face used in card headers and the Latest Mood card. */
class FaceIcon extends JComponent {
    private final int mood, size;

    FaceIcon(int size, int mood) {
        this.size = size;
        this.mood = mood;
        Dimension d = new Dimension(size, size);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        MoodFace.paint(g, 0, 0, size, mood, true);
        g.dispose();
    }
}
