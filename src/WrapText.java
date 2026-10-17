import javax.swing.*;
import java.awt.*;
/** Plain wrapped text. Wraps to the width its parent gives it, so it does not depend on screen DPI like HTML labels do. */
class WrapText extends JTextArea {
    private final int fallbackWidth;

    WrapText(String text, int fallbackWidth, Color color, float size, boolean bold) {
        super();
        // Read-only text: a caret must not scroll the page to this label. Set before the text so no update is queued.
        if (getCaret() instanceof javax.swing.text.DefaultCaret c) c.setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        setText(text);
        this.fallbackWidth = fallbackWidth;
        setEditable(false);
        setFocusable(false);
        setOpaque(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setBorder(null);
        setFont(Theme.BASE.deriveFont(bold ? Font.BOLD : Font.PLAIN, size));
        setForeground(color);
        setAlignmentX(0f);
    }

    @Override public Dimension getPreferredSize() {
        Container p = getParent();
        int w = p != null && p.getWidth() > 0 ? p.getWidth() : fallbackWidth;
        Dimension old = getSize();
        setSize(w, Short.MAX_VALUE);
        Dimension d = super.getPreferredSize();
        setSize(old);
        return new Dimension(w, d.height);
    }

    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
}
