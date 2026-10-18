import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;
/**
 * Moderation for Peer Support Communities. Only Admin and Guidance accounts get this page (see User.getMenu()),
 * and DataStore re-checks that the actor is staff before it hides or restores anything.
 * Staff can see who is behind an anonymous post here, so they can follow up on safety concerns.
 */
class ModerationView extends View {
    ModerationView(MainWindow w) {
        super(w);
        DataStore d = store();
        List<ContentReport> open = d.openReports();
        long urgent = open.stream().filter(ContentReport::isUrgent).count();

        JPanel stats = new HugPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(Staff.stat("\u26A0\uFE0F", "Open Reports", String.valueOf(open.size()), null));
        stats.add(Staff.stat("\uD83D\uDEA8", "Urgent", String.valueOf(urgent), null));
        stats.add(Staff.stat("\uD83D\uDE48", "Hidden Posts", String.valueOf(d.hiddenPostCount()), null));

        RoundPanel queue = Staff.card("Reports to review", null, null);
        if (open.isEmpty()) queue.add(Staff.note("No open reports. Nice and quiet."));
        for (ContentReport r : open) {
            queue.add(reportRow(r));
            queue.add(Staff.gap(6));
        }

        RoundPanel handled = Staff.card("Recently handled", null, null);
        List<ContentReport> done = d.handledReports();
        if (done.isEmpty()) handled.add(Staff.note("Nothing handled yet."));
        for (ContentReport r : done.stream().limit(5).toList()) {
            RoundPanel row = Staff.row();
            row.add(Staff.twoLines(reasonShort(r) + "  \u00B7  " + communityName(r.getPostId()),
                    "By " + d.nameOf(r.getHandledBy()) + "  \u00B7  " + DataStore.ago(r.getHandledAt())
                            + (r.getNote().isBlank() ? "" : "  \u00B7  " + clip(r.getNote(), 60))), BorderLayout.CENTER);
            boolean actioned = r.getState() == ContentReport.State.ACTIONED;
            row.add(Staff.centered(new Pill(actioned ? "Post hidden" : "Dismissed",
                    actioned ? Staff.WARN_BG : Staff.NEUTRAL_BG, actioned ? Staff.WARN_FG : Staff.NEUTRAL_FG)), BorderLayout.EAST);
            handled.add(row);
            handled.add(Staff.gap(6));
        }

        RoundPanel recent = Staff.card("Recent discussions", null, null);
        recent.add(Staff.note("Hide or restore any post here, with or without a report."));
        recent.add(Staff.gap(8));
        for (CommunityPost p : d.allPosts().stream().limit(12).toList()) {
            recent.add(postRow(p));
            recent.add(Staff.gap(6));
        }

        put(Staff.header("Moderation", "Review reports and moderate Peer Support Communities.",
                        () -> window.open("Dashboard")),
                stats, queue, handled, recent);
    }

    private JPanel reportRow(ContentReport r) {
        DataStore d = store();
        RoundPanel row = Staff.row(r.isUrgent());
        String who = d.postById(r.getPostId()).map(p -> clip(p.getText(), 70)).orElse("(post missing)");
        String by = r.getReporterId().equals(ContentReport.SYSTEM) ? "Flagged automatically" : "Reported by " + d.nameOf(r.getReporterId());
        row.add(Staff.twoLines((r.isUrgent() ? "URGENT  \u00B7  " : "") + reasonShort(r) + "  \u00B7  " + communityName(r.getPostId()),
                "\"" + who + "\"  \u00B7  " + by + "  \u00B7  " + DataStore.ago(r.getAt())), BorderLayout.CENTER);
        row.add(Staff.centered(Theme.primary("Review", () -> review(r))), BorderLayout.EAST);
        return row;
    }

    private JPanel postRow(CommunityPost p) {
        DataStore d = store();
        RoundPanel row = Staff.row();
        String who = d.nameOf(p.getAuthorId()) + (p.isAnonymous() ? " (posted anonymously)" : "")
                + (d.isVolunteer(p.getAuthorId()) ? " (Peer Volunteer)" : "");
        row.add(Staff.twoLines(communityName(p.getId()) + (p.isReply() ? "  \u00B7  reply" : ""),
                "\"" + clip(p.getText(), 48) + "\"  \u00B7  " + who + "  \u00B7  " + DataStore.ago(p.getAt())), BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        if (p.isHidden()) right.add(new Pill("Hidden", Staff.WARN_BG, Staff.WARN_FG));
        right.add(Theme.secondary(p.isHidden() ? "Restore" : "Hide", () -> {
            if (!p.isHidden() && !window.confirm("Hide this post from students?")) return;
            store().setPostHidden(p.getId(), !p.isHidden(), user());
            window.refresh();
        }));
        row.add(Staff.centered(right), BorderLayout.EAST);
        return row;
    }

    private void review(ContentReport r) {
        DataStore d = store();
        CommunityPost p = d.postById(r.getPostId()).orElse(null);
        if (p == null) { window.message("That post no longer exists."); return; }
        StringBuilder sb = new StringBuilder();
        sb.append("Community: ").append(communityName(p.getId())).append('\n');
        sb.append("Posted by: ").append(d.nameOf(p.getAuthorId())).append("  (").append(d.courseOf(p.getAuthorId()))
                .append(")").append(p.isAnonymous() ? "  \u2013 shown to students as Anonymous" : "")
                .append(d.isVolunteer(p.getAuthorId()) ? "  \u2013 Peer Volunteer" : "").append('\n');
        sb.append("Reason: ").append(r.getReason()).append('\n');
        if (!r.getDetails().isBlank()) sb.append("Details: ").append(r.getDetails()).append('\n');
        long others = d.getReports().stream().filter(x -> x.isOpen() && x.getPostId().equals(p.getId())).count();
        if (others > 1) sb.append("Open reports on this post: ").append(others).append('\n');
        sb.append("\n").append(p.getText());
        if (r.isUrgent()) sb.append("\n\nUrgent: follow up with this student directly. ").append(CommunityGuidelines.CRISIS_LINE);

        JTextArea body = new JTextArea(sb.toString(), 12, 48);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setEditable(false);
        body.setFont(Theme.BASE);
        body.setBorder(new EmptyBorder(6, 8, 6, 8));
        JTextField note = Theme.field();
        Object[] content = {new JScrollPane(body), "Note for the record (optional)", note};
        Object[] options = {"Hide post", "Dismiss report", "Cancel"};
        int choice = JOptionPane.showOptionDialog(window, content, "Review report", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
        if (choice != 0 && choice != 1) return;
        store().resolveReport(r.getId(), choice == 0, user(), note.getText().trim());
        window.refresh();
    }

    private String communityName(String postId) {
        return store().postById(postId).flatMap(p -> store().communityById(p.getCommunityId())).map(Community::name).orElse("?");
    }

    private static String reasonShort(ContentReport r) {
        String s = r.getReason();
        int i = s.indexOf(" (");
        return i > 0 ? s.substring(0, i) : s;
    }

    private static String clip(String s, int n) {
        String one = s.replace('\n', ' ');
        return one.length() <= n ? one : one.substring(0, n - 1) + "\u2026";
    }
}
