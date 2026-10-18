import javax.swing.*;
import java.awt.*;
/** Breaks text into lines that fit a width, measured with the real font (no HTML), so nothing runs past its box. */
final class TextWrap {
    private TextWrap() {}

    /** A block of left-aligned lines. "\n" starts a new paragraph. */
    static JComponent block(String text, int width, Font font, Color color) {
        FontMetrics fm = new JLabel().getFontMetrics(font);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setAlignmentX(0f);
        String[] paragraphs = text.split("\n", -1);
        for (String para : paragraphs) {
            if (para.isEmpty()) { box.add(Box.createVerticalStrut(fm.getHeight() / 2 + 2)); continue; }
            StringBuilder cur = new StringBuilder();
            for (String word : para.split(" ")) {
                if (cur.length() > 0 && fm.stringWidth(cur + " " + word) > width) { box.add(line(cur.toString(), font, color)); cur.setLength(0); }
                if (cur.length() > 0) cur.append(' ');
                cur.append(word);
            }
            if (cur.length() > 0) box.add(line(cur.toString(), font, color));
        }
        return box;
    }

    private static JLabel line(String s, Font font, Color color) {
        JLabel l = new JLabel(s);
        l.setFont(font);
        l.setForeground(color);
        l.setAlignmentX(0f);
        return l;
    }
}
