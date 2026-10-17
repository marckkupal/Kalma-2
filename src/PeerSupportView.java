import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Peer Support Communities. Students browse and join communities by shared experience, read and join discussions,
 * post (anonymously where the community allows it), and report anything that worries them.
 * Works on its own: it does not depend on any buddy or mentoring program.
 */
class PeerSupportView extends View {
    /** Starter list of crisis wording. A post that matches is still published, but Guidance is alerted. Review with Guidance. */
    private static final String[] CRISIS_WORDS = {"kill myself", "end my life", "suicide", "want to die", "hurt myself",
            "self-harm", "self harm", "magpakamatay", "ayoko nang mabuhay"};

    PeerSupportView(MainWindow w) {
        super(w);
        String id = window.communityId();
        Optional<Community> c = id == null ? Optional.empty() : store().communityById(id);
        if (c.isPresent()) showCommunity(c.get());
        else showList();
    }

    // ----- browse -----

    private void showList() {
        String q = window.takeSearch().toLowerCase();
        List<Community> all = store().getCommunities().stream()
                .filter(c -> (c.name() + " " + c.description()).toLowerCase().contains(q))
                .sorted(Comparator.comparing((Community c) -> !store().isMember(user().getId(), c.id())))
                .collect(Collectors.toList());

        JButton rules = Theme.secondary("Community guidelines", this::showGuidelines);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Theme.heading("Peer Support Communities"), BorderLayout.WEST);
        head.add(rules, BorderLayout.EAST);
        put(head, Theme.text("Find students who are going through something similar. Join a community to take part in "
                + "its discussions. These are peer spaces, not counseling. The Guidance Office is there for professional help."));

        if (all.isEmpty()) put(Staff.note("No communities match \"" + q + "\"."));
        int i = 0;
        for (Community c : all) {
            boolean joined = store().isMember(user().getId(), c.id());
            RoundPanel box = new RoundPanel(new BorderLayout(14, 0), Theme.PASTELS[i++ % Theme.PASTELS.length], null, 20);
            box.setBorder(new EmptyBorder(14, 18, 14, 18));
            JLabel icon = new JLabel(c.icon());
            icon.setFont(Theme.BASE.deriveFont(26f));
            icon.setHorizontalAlignment(SwingConstants.CENTER);
            icon.setPreferredSize(new Dimension(44, 36));
            JLabel name = new JLabel(c.name());
            name.setFont(Theme.BOLD);
            name.setForeground(Theme.GREEN_DARK);
            long members = store().memberCount(c.id());
            JLabel meta = new JLabel(members + (members == 1 ? " member" : " members") + "  \u00B7  "
                    + (c.anonymousAllowed() ? "Anonymous posting available" : "Posts show your name"));
            meta.setFont(Theme.BASE.deriveFont(11f));
            meta.setForeground(Brand.MUTED);
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            for (JComponent part : new JComponent[]{name, Theme.text(c.description()), meta}) {
                part.setAlignmentX(0f);
                text.add(part);
            }
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);
            buttons.add(Theme.primary("Open", () -> window.openCommunity(c.id())));
            buttons.add(joined ? Theme.secondary("Joined (leave)", () -> toggle(c))
                    : Theme.secondary("Join", () -> toggle(c)));
            box.add(Staff.centered(icon), BorderLayout.WEST);
            box.add(text, BorderLayout.CENTER);
            box.add(Staff.centered(buttons), BorderLayout.EAST);
            put(box);
        }
    }

    private void toggle(Community c) {
        store().toggleMember(user().getId(), c.id());
        window.refresh();
    }

    // ----- one community -----

    private void showCommunity(Community c) {
        boolean member = store().isMember(user().getId(), c.id());
        boolean volunteer = store().isVolunteer(user().getId());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(Theme.secondary("\u2039  All communities", () -> window.open("Peer Support")));
        bar.add(Theme.secondary("Community guidelines", this::showGuidelines));
        put(bar, Staff.header(c.icon() + "  " + c.name(), c.description(), null));

        // composer
        RoundPanel composer = Staff.card("Start a discussion", null, null);
        if (!member) {
            composer.add(Staff.note("Join this community to post and reply. You can read the discussions first."));
            composer.add(Staff.gap(8));
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            row.setAlignmentX(0f);
            row.add(Theme.primary("Join " + c.name(), () -> toggle(c)));
            composer.add(row);
        } else {
            JTextArea box = textArea(4);
            JScrollPane sp = new JScrollPane(box);
            sp.setBorder(new LineBorder(Theme.GRID, 1, true));
            sp.setAlignmentX(0f);
            sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            JCheckBox anon = anonymousBox(c, volunteer);
            composer.add(sp);
            composer.add(Staff.gap(8));
            composer.add(anon);
            composer.add(Staff.gap(8));
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            row.setAlignmentX(0f);
            row.add(Theme.primary("Post", () -> submit(c, null, box.getText(), anon.isSelected())));
            composer.add(row);
        }
        put(composer);

        List<CommunityPost> posts = store().postsIn(c.id());
        if (posts.isEmpty()) put(Staff.note("No discussions yet. Be the first to start one."));
        for (CommunityPost p : posts) put(postCard(c, p, member));
    }

    private JPanel postCard(Community c, CommunityPost p, boolean member) {
        RoundPanel card = Staff.card(null, null, null);
        JComponent top = (JComponent) postBody(p, false);
        top.setAlignmentX(0f);
        card.add(top);
        card.add(Staff.gap(6));
        JComponent topActions = (JComponent) postActions(c, p, member, true);
        topActions.setAlignmentX(0f);
        card.add(topActions);
        List<CommunityPost> replies = store().repliesTo(p.getId());
        for (CommunityPost r : replies) {
            card.add(Staff.gap(8));
            JPanel indent = new JPanel(new BorderLayout());
            indent.setOpaque(false);
            indent.setAlignmentX(0f);
            indent.setBorder(new EmptyBorder(0, 34, 0, 0));
            JPanel inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
            JComponent body = (JComponent) postBody(r, true);
            body.setAlignmentX(0f);
            inner.add(body);
            JComponent actions = (JComponent) postActions(c, r, member, false);
            actions.setAlignmentX(0f);
            inner.add(actions);
            indent.add(inner, BorderLayout.CENTER);
            card.add(indent);
        }
        return card;
    }

    /** Name line plus text. Anonymous posts show no identity to other students. */
    private JPanel postBody(CommunityPost p, boolean reply) {
        DataStore d = store();
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        line.setOpaque(false);
        line.setAlignmentX(0f);
        String author = authorLabel(p);
        line.add(new Avatar(p.isAnonymous() || p.isHidden() ? "?" : d.initialsOf(p.getAuthorId()),
                p.isAnonymous() ? Brand.MUTED : Brand.BLACK, Brand.YELLOW, 28));
        JLabel name = new JLabel(p.isHidden() ? "Removed" : author);
        name.setFont(Theme.BOLD);
        name.setForeground(Brand.INK);
        line.add(name);
        if (!p.isHidden() && !p.isAnonymous() && d.isVolunteer(p.getAuthorId()))
            line.add(new Pill("Peer Volunteer", Staff.GOOD_BG, Staff.GOOD_FG));
        JLabel when = new JLabel(DataStore.ago(p.getAt()));
        when.setFont(Theme.BASE.deriveFont(11f));
        when.setForeground(Brand.MUTED);
        line.add(when);
        col.add(line);
        col.add(Staff.gap(4));
        col.setAlignmentX(0f);
        JTextArea text = Theme.text(p.isHidden() ? "This post was removed by a moderator." : p.getText());
        if (p.isHidden()) text.setForeground(Brand.MUTED);
        text.setAlignmentX(0f);
        col.add(text);
        return col;
    }

    private JPanel postActions(Community c, CommunityPost p, boolean member, boolean canReply) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        if (p.isHidden()) return row;
        if (canReply && member) row.add(Staff.linkButton("Reply", () -> reply(c, p)));
        boolean mine = p.getAuthorId().equals(user().getId());
        if (!mine) {
            if (store().hasReported(p.getId(), user().getId())) row.add(Staff.note("Reported"));
            else row.add(Staff.linkButton("Report", () -> report(p)));
        }
        return row;
    }

    /** Real name for open posts (first name, plus program if that student allows it); a neutral label otherwise. */
    private String authorLabel(CommunityPost p) {
        if (p.isAnonymous()) return "Anonymous Student";
        DataStore d = store();
        User u = d.findById(p.getAuthorId()).orElse(null);
        if (u == null) return "Student";
        String course = u.getCourse();
        boolean showCourse = d.pref(u.getId(), "community-course") && course != null && !course.isBlank();
        return u.getFirstName() + (showCourse ? "  \u00B7  " + course : "");
    }

    // ----- actions -----

    private void reply(Community c, CommunityPost parent) {
        JTextArea box = textArea(4);
        JScrollPane sp = new JScrollPane(box);
        sp.setBorder(new LineBorder(Theme.GRID, 1, true));
        JCheckBox anon = anonymousBox(c, store().isVolunteer(user().getId()));
        if (JOptionPane.showConfirmDialog(window, new Object[]{"Reply to " + authorLabel(parent), sp, anon}, "Reply",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        submit(c, parent.getId(), box.getText(), anon.isSelected());
    }

    private void submit(Community c, String parentId, String text, boolean anonymous) {
        String body = text == null ? "" : text.trim();
        if (body.length() < 3) { window.message("Please write a little more before posting."); return; }
        if (body.length() > DataStore.MAX_POST_LENGTH) {
            window.message("Posts can be up to " + DataStore.MAX_POST_LENGTH + " characters. Yours is " + body.length() + ".");
            return;
        }
        CommunityPost p = store().addPost(c.id(), parentId, user().getId(), body, anonymous);
        if (p == null) { window.message("Your post could not be added. Join the community first."); return; }
        if (mentionsCrisis(body)) {
            store().report(p.getId(), ContentReport.SYSTEM, ContentReport.URGENT_REASON, "Flagged automatically: crisis wording.");
            window.message("Thank you for sharing this. You matter, and you don't have to carry it alone.\n\n"
                    + CommunityGuidelines.CRISIS_LINE + "\n\nSo that you get proper support, a Guidance staff member has been "
                    + "asked to look at your post.");
        }
        window.refresh();
    }

    static boolean mentionsCrisis(String text) {
        String t = text.toLowerCase();
        for (String w : CRISIS_WORDS) if (t.contains(w)) return true;
        return false;
    }

    private void report(CommunityPost p) {
        JComboBox<String> reason = Theme.combo(ContentReport.REASONS);
        JTextField details = Theme.field();
        Object[] fields = {"Why are you reporting this?", reason, "Anything else we should know? (optional)", details,
                "Guidance staff will review it. The person who posted is not told who reported."};
        if (JOptionPane.showConfirmDialog(window, fields, "Report this post", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String why = (String) reason.getSelectedItem();
        if (!store().report(p.getId(), user().getId(), why, details.getText().trim())) {
            window.message("You have already reported this post.");
            return;
        }
        window.message(why.equals(ContentReport.URGENT_REASON)
                ? "Thank you. Guidance staff will look at this as a priority.\n\n" + CommunityGuidelines.CRISIS_LINE
                : "Thank you. Guidance staff will review this post.");
        window.refresh();
    }

    private void showGuidelines() {
        JTextArea t = new JTextArea(CommunityGuidelines.full(), 22, 52);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setEditable(false);
        t.setFont(Theme.BASE);
        t.setBorder(new EmptyBorder(8, 10, 8, 10));
        JOptionPane.showMessageDialog(window, new JScrollPane(t), "Community guidelines", JOptionPane.PLAIN_MESSAGE);
    }

    // ----- small helpers -----

    private JCheckBox anonymousBox(Community c, boolean volunteer) {
        String label;
        boolean enabled = c.anonymousAllowed() && !volunteer;
        if (!c.anonymousAllowed()) label = "Anonymous posting is not available here. Your name is shown.";
        else if (volunteer) label = "Peer Volunteers always post under their own name.";
        else label = "Post anonymously (other students will see \"Anonymous Student\"; Guidance staff can see who posted if a report needs review)";
        JCheckBox b = new JCheckBox("<html><body style='width:480px'>" + label + "</body></html>");
        b.setEnabled(enabled);
        b.setOpaque(false);
        b.setForeground(Theme.INK);
        b.setAlignmentX(0f);
        return b;
    }

    private static JTextArea textArea(int rows) {
        JTextArea a = new JTextArea(rows, 40);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(Theme.BASE);
        a.setBackground(Theme.BG);
        a.setForeground(Theme.INK);
        a.setCaretColor(Theme.INK);
        a.setBorder(new EmptyBorder(7, 10, 7, 10));
        return a;
    }
}
