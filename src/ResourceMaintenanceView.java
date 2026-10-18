import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Maintenance (Guidance and Admin): add, edit, hide or remove the entries students see on the Resources page.
 * Every row has an Edit button; changes are saved to data/resources.tsv.
 */
class ResourceMaintenanceView extends View {
    private static final String ALL = "All";
    private String category = ALL;
    private final JPanel list = new HugPanel(new BorderLayout());

    ResourceMaintenanceView(MainWindow w) {
        super(w);
        List<Resource> all = store().getResources();
        long visible = all.stream().filter(Resource::visible).count();

        JPanel bar = new HugPanel(new BorderLayout());
        bar.setOpaque(false);
        YButton add = YButton.yellow("+  Add resource");
        add.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        add.setBorder(new EmptyBorder(8, 18, 8, 18));
        add.addActionListener(e -> edit(new Resource("", Resource.SELF_HELP, "", "", "", "", false, true), true));
        JLabel count = Staff.note(all.size() + " entries  \u2022  " + visible + " visible to students");
        bar.setAlignmentX(0f);
        bar.add(count, BorderLayout.WEST);
        bar.add(add, BorderLayout.EAST);

        List<String> cats = new ArrayList<>();
        cats.add(ALL);
        for (Resource r : all) if (!cats.contains(r.category())) cats.add(r.category());
        if (!cats.contains(category)) category = ALL;
        JPanel chips = new HugPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        chips.setAlignmentX(0f);
        ButtonGroup g = new ButtonGroup();
        for (String c : cats) {
            PillToggle t = new PillToggle("  " + c + "  ");
            t.setSelected(c.equals(category));
            t.addActionListener(e -> { category = c; fill(); });
            g.add(t);
            chips.add(t);
        }

        RoundPanel card = Staff.card("\uD83D\uDEE0\uFE0F  Resource entries", null, null);
        card.add(bar);
        card.add(Staff.gap(10));
        card.add(chips);
        card.add(Staff.gap(10));
        list.setOpaque(false);
        list.setAlignmentX(0f);
        card.add(list);
        fill();

        put(Staff.header("Maintenance", "Add, edit or remove what students see on the Resources page.",
                () -> window.open("Dashboard")), card);
    }

    private void fill() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        List<Resource> rows = store().getResources().stream()
                .filter(r -> category.equals(ALL) || r.category().equals(category)).collect(Collectors.toList());
        if (rows.isEmpty()) col.add(Staff.note("Nothing here yet. Use \"Add resource\" to create an entry."));
        for (Resource r : rows) {
            col.add(row(r));
            col.add(Staff.gap(6));
        }
        list.removeAll();
        list.add(col, BorderLayout.CENTER);
        list.revalidate();
        list.repaint();
    }

    private RoundPanel row(Resource r) {
        RoundPanel row = Staff.row();
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(r.title());
        t.setFont(Theme.BOLD);
        t.setForeground(Brand.INK);
        t.setAlignmentX(0f);
        WrapText c = new WrapText(r.contact().isEmpty() ? "No contact set" : r.contact(), 360, Brand.MUTED, 12f, false);
        text.add(t);
        text.add(Box.createVerticalStrut(2));
        text.add(c);
        row.add(text, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        if (!r.visible()) right.add(new Pill("Hidden", Staff.WARN_BG, Staff.WARN_FG));
        if (r.open247() && !r.category().equals(Resource.CRISIS)) right.add(new Pill("24/7", Staff.GOOD_BG, Staff.GOOD_FG));
        right.add(new Pill(r.category(), Staff.NEUTRAL_BG, Staff.NEUTRAL_FG));
        YButton edit = YButton.yellow("Edit");
        edit.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        edit.setBorder(new EmptyBorder(6, 14, 6, 14));
        edit.addActionListener(e -> edit(r, false));
        YButton del = YButton.outline("Remove");
        del.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
        del.setBorder(new EmptyBorder(6, 14, 6, 14));
        del.addActionListener(e -> remove(r));
        right.add(edit);
        right.add(del);
        row.add(Staff.centered(right), BorderLayout.EAST);
        return row;
    }

    private void remove(Resource r) {
        if (!window.confirm("Remove \"" + r.title() + "\" from the Resources page?\n"
                + "Tip: untick \"Show to students\" in Edit if you only want to hide it for now.")) return;
        store().removeResource(r);
        store().log("Resource removed", r.title());
        window.refresh();
    }

    private static JTextArea area(String text, int rows) {
        JTextArea a = new JTextArea(text, rows, 36);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(Theme.BASE);
        a.setBackground(Theme.BG);
        a.setForeground(Theme.INK);
        a.setCaretColor(Theme.INK);
        a.setBorder(new EmptyBorder(7, 10, 7, 10));
        return a;
    }

    private static JScrollPane scroll(JTextArea a) {
        JScrollPane sp = new JScrollPane(a);
        sp.setBorder(new LineBorder(Theme.GRID, 1, true));
        return sp;
    }

    private void edit(Resource r, boolean isNew) {
        JTextField title = Theme.field();
        title.setText(r.title());
        List<String> cats = new ArrayList<>(Resource.CATEGORIES);
        for (Resource o : store().getResources()) if (!cats.contains(o.category())) cats.add(o.category());
        JComboBox<String> cat = Theme.combo(cats.toArray(String[]::new));
        cat.setEditable(true);
        cat.setSelectedItem(r.category());
        JTextArea desc = area(r.description(), 3);
        JTextField contact = Theme.field();
        contact.setText(r.contact());
        JTextField hours = Theme.field();
        hours.setText(r.hours());
        JTextArea access = area(r.access(), 3);
        JCheckBox all = new JCheckBox("Available 24/7 (shows in \"Need support right now?\" for crisis support)", r.open247());
        JCheckBox show = new JCheckBox("Show to students", r.visible());
        all.setOpaque(false);
        show.setOpaque(false);
        all.setForeground(Theme.INK);
        show.setForeground(Theme.INK);

        Object[] fields = {"Name", title, "Category (pick one or type a new one)", cat,
                "What it is", scroll(desc), "Contact (phone, email or where to go)", contact,
                "Hours", hours, "How students can access it", scroll(access), all, show};
        if (JOptionPane.showConfirmDialog(window, fields, isNew ? "Add resource" : "Edit resource",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String t = title.getText().trim();
        String c = String.valueOf(cat.getSelectedItem()).trim();
        if (t.length() < 3) { window.message("Please add a name for this resource."); return; }
        if (c.isEmpty() || c.equals("null")) { window.message("Please choose or type a category."); return; }
        if (contact.getText().trim().isEmpty()) { window.message("Please add a contact so students know how to reach it."); return; }
        r.update(t, c, desc.getText().trim(), contact.getText().trim(), hours.getText().trim(),
                access.getText().trim(), all.isSelected(), show.isSelected());
        if (isNew) store().addResource(r);
        else store().saveResources();
        store().log(isNew ? "Resource added" : "Resource updated", t);
        window.refresh();
    }
}
