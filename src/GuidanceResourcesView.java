import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
/**
 * Guidance - Resources (Figma frame): cards for guides students can open from their home screen.
 * Each card has an Edit button. Shared guides appear as cards on the students' Resources page.
 */
class GuidanceResourcesView extends View {
    GuidanceResourcesView(MainWindow w) {
        super(w);
        JPanel grid = new HugPanel(new GridLayout(0, 3, 14, 14));
        grid.setOpaque(false);
        for (Guide g : store().getGuides()) grid.add(guideCard(g));

        JPanel bar = new HugPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setOpaque(false);
        YButton add = YButton.outline("+  New guide");
        add.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        add.setBorder(new EmptyBorder(7, 16, 7, 16));
        add.addActionListener(e -> edit(new Guide("", "", "", false, "\uD83D\uDCD6"), true));
        bar.add(add);

        put(Staff.header("Resources", "Guides students can open from their home screen.", () -> window.open("Dashboard")),
                grid, bar);
    }

    private JPanel guideCard(Guide g) {
        RoundPanel p = new RoundPanel(null, Color.WHITE, Theme.GRID, 20);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(16, 18, 16, 18));

        IconBadge badge = new IconBadge(g.getIcon());
        JLabel title = new JLabel(g.getTitle());
        title.setFont(Theme.BOLD);
        title.setForeground(Brand.INK);
        JLabel state = new JLabel(g.isShared() ? "Shared with all students." : "Draft \u2022 not yet shared.");
        state.setFont(Theme.BASE.deriveFont(11f));
        state.setForeground(Brand.MUTED);
        YButton edit = YButton.yellow("Edit  \u2192");
        edit.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        edit.setBorder(new EmptyBorder(6, 14, 6, 14));
        edit.addActionListener(e -> edit(g, false));

        for (JComponent c : new JComponent[]{badge, title, state, edit}) c.setAlignmentX(0f);
        p.add(badge);
        p.add(Staff.gap(10));
        p.add(title);
        p.add(Staff.gap(3));
        p.add(state);
        p.add(Staff.gap(12));
        p.add(edit);
        return p;
    }

    private void edit(Guide g, boolean isNew) {
        JTextField title = Theme.field();
        title.setText(g.getTitle());
        JTextField summary = Theme.field();
        summary.setText(g.getSummary());
        JTextArea body = new JTextArea(g.getBody(), 6, 36);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setFont(Theme.BASE);
        body.setBackground(Theme.BG);
        body.setForeground(Theme.INK);
        body.setCaretColor(Theme.INK);
        body.setBorder(new EmptyBorder(7, 10, 7, 10));
        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(new LineBorder(Theme.GRID, 1, true));
        JCheckBox shared = new JCheckBox("Share with all students", g.isShared());
        shared.setOpaque(false);
        shared.setForeground(Theme.INK);

        Object[] fields = {"Title", title, "Short description (shown on the card)", summary,
                "Content (shown when a student taps Read)", bodyScroll, shared};
        if (JOptionPane.showConfirmDialog(window, fields, isNew ? "New guide" : "Edit guide",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String t = title.getText().trim();
        if (t.length() < 3) { window.message("Please add a guide title."); return; }
        g.update(t, summary.getText().trim(), body.getText().trim(), shared.isSelected());
        if (isNew) store().addGuide(g);
        store().log(isNew ? "Guide created" : "Guide updated", t);
        window.refresh();
    }
}
