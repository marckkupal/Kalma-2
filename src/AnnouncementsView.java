import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
/** Admin announcements: write a new one and see what is already published. */
class AnnouncementsView extends View {
    AnnouncementsView(MainWindow w) {
        super(w);
        RoundPanel form = Staff.card("\uD83D\uDCE3  New Announcement", null, null);
        JTextField title = Theme.field();
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, title.getPreferredSize().height));
        title.setAlignmentX(0f);
        JTextArea msg = new JTextArea(4, 40);
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        msg.setFont(Theme.BASE);
        msg.setBackground(Theme.BG);
        msg.setForeground(Theme.INK);
        msg.setCaretColor(Theme.INK);
        msg.setBorder(new EmptyBorder(7, 10, 7, 10));
        JScrollPane msgScroll = new JScrollPane(msg);
        msgScroll.setBorder(new LineBorder(Theme.GRID, 1, true));
        msgScroll.setAlignmentX(0f);
        msgScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        YButton publish = YButton.yellow("Publish  \u2192");
        publish.setAlignmentX(0f);
        publish.addActionListener(e -> {
            String t = title.getText().trim(), m = msg.getText().trim();
            if (t.length() < 3 || m.length() < 5) { window.message("Please add a title and a short message."); return; }
            store().addAnnouncement(new Announcement(t, m, LocalDate.now()));
            window.message("Announcement published. Students will see it on their Home page.");
            window.refresh();
        });

        JLabel l1 = Theme.fieldLabel("Title");
        JLabel l2 = Theme.fieldLabel("Message");
        l1.setAlignmentX(0f);
        l2.setAlignmentX(0f);
        form.add(l1);
        form.add(Staff.gap(5));
        form.add(title);
        form.add(Staff.gap(10));
        form.add(l2);
        form.add(Staff.gap(5));
        form.add(msgScroll);
        form.add(Staff.gap(12));
        form.add(publish);

        RoundPanel published = Staff.card("\uD83D\uDCE2  Published", null, null);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
        List<Announcement> list = store().getAnnouncements();
        if (list.isEmpty()) published.add(Staff.note("Nothing published yet."));
        for (Announcement a : list) {
            RoundPanel r = Staff.row();
            r.setToolTipText(a.body());
            r.add(Staff.twoLines(a.title(), "Published " + a.date().format(fmt)), BorderLayout.CENTER);
            r.add(Staff.centered(new Pill("Live", Staff.GOOD_BG, Staff.GOOD_FG)), BorderLayout.EAST);
            published.add(r);
            published.add(Staff.gap(6));
        }

        put(Staff.header("Announcements", "Share wellness updates and events with students.", () -> window.open("Dashboard")),
                form, published);
    }
}
