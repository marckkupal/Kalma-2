import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
/** Kalma theme: sage green with soft pastel accents. */
final class Theme {
    static final Color GREEN_DARK = new Color(0x2F5D50);
    static final Color GREEN = new Color(0x5E9C76);
    static final Color SAGE = new Color(0xFFFFFF);       // sidebar
    static final Color SAGE_SEL = new Color(0xEBEDF0);   // selected nav item
    static final Color BG = new Color(0xF3F4F6);         // page background
    static final Color MINT = new Color(0xDCEBE0);
    static final Color PEACH = new Color(0xF9E8DD);
    static final Color SKY = new Color(0xE3E9F8);
    static final Color LAVENDER = new Color(0xEAE4F5);
    static final Color ORANGE = new Color(0xD9814E);
    static final Color INK = new Color(0x111827);
    static final Color WHITE = Color.WHITE;
    static final Color GRID = new Color(0xE5E7EB);
    static final Color MUTED = new Color(0x6B7280);
    static final Color[] PASTELS = {MINT, PEACH, SKY, LAVENDER};

    static final Font BASE = new Font("SansSerif", Font.PLAIN, 13);
    static final Font BOLD = BASE.deriveFont(Font.BOLD);
    static final Font TITLE = new Font("SansSerif", Font.BOLD, 22);

    private Theme() {}

    @SuppressWarnings("deprecation")
    static void install() {
        try {
            MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme() {
                @Override protected ColorUIResource getPrimary1() { return new ColorUIResource(GREEN_DARK); }
                @Override protected ColorUIResource getPrimary2() { return new ColorUIResource(GREEN); }
                @Override protected ColorUIResource getPrimary3() { return new ColorUIResource(MINT); }
                @Override protected ColorUIResource getSecondary1() { return new ColorUIResource(0x9DB3A6); }
                @Override protected ColorUIResource getSecondary2() { return new ColorUIResource(0xC9D8CE); }
                @Override protected ColorUIResource getSecondary3() { return new ColorUIResource(WHITE); }
            });
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) { }

        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(key) instanceof FontUIResource) UIManager.put(key, new FontUIResource(BASE));
        }
        UIManager.put("Panel.background", WHITE);
        UIManager.put("OptionPane.background", WHITE);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.background", INK);
        UIManager.put("Button.foreground", WHITE);
        UIManager.put("Button.select", new Color(0x374151));
        UIManager.put("Button.focus", GREEN);
        UIManager.put("ComboBox.background", WHITE);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("ComboBox.selectionBackground", MINT);
        UIManager.put("ComboBox.selectionForeground", INK);
        UIManager.put("TextField.selectionBackground", MINT);
        UIManager.put("TextField.selectionForeground", INK);
        UIManager.put("PasswordField.selectionBackground", MINT);
        UIManager.put("PasswordField.selectionForeground", INK);
        UIManager.put("Slider.background", BG);
        UIManager.put("Slider.foreground", GREEN_DARK);
        UIManager.put("Slider.focus", BG);
        UIManager.put("ScrollPane.background", BG);
        UIManager.put("Viewport.background", BG);
    }

    // ----- text -----
    static JLabel heading(String s) {
        JLabel l = new JLabel(s);
        l.setFont(TITLE);
        l.setForeground(INK);
        l.setBorder(new EmptyBorder(0, 0, 4, 0));
        return l;
    }

    static JLabel subheading(String s) {
        JLabel l = new JLabel(s);
        l.setFont(BASE.deriveFont(Font.BOLD, 15f));
        l.setForeground(INK);
        return l;
    }

    static JLabel fieldLabel(String s) {
        JLabel l = new JLabel(s);
        l.setFont(BOLD.deriveFont(12f));
        l.setForeground(INK);
        return l;
    }

    static JLabel wrapLabel(String s, int width, Color color, float size, boolean bold) {
        JLabel l = new JLabel("<html><body style='width:" + width + "px'>" + s + "</body></html>");
        l.setFont(BASE.deriveFont(bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    static JTextArea text(String s) {
        JTextArea a = new JTextArea(s) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        a.setColumns(50);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setEditable(false);
        a.setFocusable(false);
        a.setOpaque(false);
        a.setFont(BASE);
        a.setForeground(INK);
        a.setBorder(null);
        return a;
    }

    /** Soft mint callout box. */
    static JPanel notice(String s) {
        RoundPanel p = new RoundPanel(new BorderLayout(), MINT, null, 20);
        p.setBorder(new EmptyBorder(14, 18, 14, 18));
        p.add(text(s), BorderLayout.CENTER);
        return p;
    }

    /** Small summary card with a pastel background. */
    static JPanel card(String title, String value, Color bg) {
        RoundPanel p = new RoundPanel(null, bg, null, 20);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel t = new JLabel(title.toUpperCase());
        t.setFont(BOLD.deriveFont(11f));
        t.setForeground(MUTED);
        JLabel v = wrapLabel(value, 170, INK, 15f, true);
        t.setAlignmentX(0f);
        v.setAlignmentX(0f);
        p.add(t);
        p.add(Box.createVerticalStrut(6));
        p.add(v);
        return p;
    }

    // ----- inputs -----
    static JTextField field() {
        JTextField f = new JTextField(22);
        styleInput(f);
        return f;
    }

    static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField(22);
        styleInput(f);
        return f;
    }

    private static void styleInput(JTextField f) {
        f.setBorder(new CompoundBorder(new LineBorder(GRID, 1, true), new EmptyBorder(7, 10, 7, 10)));
        f.setBackground(BG);
        f.setForeground(INK);
        f.setCaretColor(INK);
    }

    static JComboBox<String> combo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setBackground(WHITE);
        c.setForeground(INK);
        return c;
    }

    // ----- buttons -----
    static JButton primary(String text, Runnable action) {
        RoundButton b = new RoundButton(text, INK, new Color(0x374151), WHITE, null);
        b.addActionListener(e -> action.run());
        return b;
    }

    static JButton secondary(String text, Runnable action) {
        RoundButton b = new RoundButton(text, new Color(0xF3F4F6), new Color(0xE5E7EB), INK, GRID);
        b.addActionListener(e -> action.run());
        return b;
    }

    static JButton navButton(String text, Runnable action) {
        RoundButton b = new RoundButton(text, SAGE, SAGE_SEL, INK, null);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        b.setAlignmentX(0f);
        b.addActionListener(e -> action.run());
        setNavSelected(b, false);
        return b;
    }

    static void setNavSelected(JButton b, boolean selected) {
        ((RoundButton) b).setColors(selected ? SAGE_SEL : SAGE, selected ? SAGE_SEL : new Color(0xF3F4F6),
                INK);
        b.setFont(selected ? BOLD : BASE);
    }

    // ----- tables -----
    static JTable table(String[] head, List<Object[]> rows, int... widths) {
        DefaultTableModel m = new DefaultTableModel(head, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        rows.forEach(m::addRow);
        JTable t = new JTable(m);
        t.setRowHeight(30);
        t.setGridColor(GRID);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);
        t.setBackground(WHITE);
        t.setForeground(INK);
        t.setSelectionBackground(MINT);
        t.setSelectionForeground(INK);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setBackground(BG);
        h.setForeground(INK);
        h.setFont(BOLD);
        for (int i = 0; i < widths.length && i < head.length; i++) t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        return t;
    }

    static JScrollPane scroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(GRID, 1));
        sp.getViewport().setBackground(WHITE);
        sp.setPreferredSize(new Dimension(600, 260));
        return sp;
    }
}
