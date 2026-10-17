import java.util.*;

/**
 * Checks the Peer Support Communities rules: joining, anonymous posting, volunteers, reporting and moderation.
 * No test framework needed:  java CommunityTest
 */
public class CommunityTest {
    private static int failures = 0, checks = 0;

    static void check(String name, boolean ok) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL  " + name); }
    }

    public static void main(String[] args) {
        DataStore d = DataStore.getInstance();
        d.seed();
        User maria = d.findById("u1").orElseThrow();
        User counselor = d.findById("u3").orElseThrow();
        User admin = d.findById("u4").orElseThrow();
        User carlo = d.findById("u6").orElseThrow();   // seeded Peer Volunteer

        // ----- communities -----
        Set<String> names = new HashSet<>();
        for (Community c : d.getCommunities()) names.add(c.name());
        check("all seven required categories exist", names.containsAll(List.of("Academic Stress", "Time Management",
                "Homesickness", "Social Anxiety", "Family and Personal Concerns", "College Adjustment", "Burnout")));
        check("anonymous posting is offered only where appropriate",
                d.communityById("burnout").orElseThrow().anonymousAllowed()
                        && !d.communityById("time-management").orElseThrow().anonymousAllowed());

        // ----- joining and posting -----
        check("non-member cannot post", d.addPost("social-anxiety", null, "u1", "Hello everyone", false) == null);
        long before = d.memberCount("social-anxiety");
        d.toggleMember("u1", "social-anxiety");
        check("joining raises the member count", d.memberCount("social-anxiety") == before + 1 && d.isMember("u1", "social-anxiety"));
        CommunityPost anon = d.addPost("social-anxiety", null, "u1", "Presentations make my hands shake.", true);
        check("member can post anonymously where allowed", anon != null && anon.isAnonymous());
        check("real author is still stored for moderators", anon != null && anon.getAuthorId().equals("u1"));
        d.toggleMember("u1", "time-management");
        CommunityPost named = d.addPost("time-management", null, "u1", "Any tips for a weekly planner?", true);
        check("anonymous request is ignored where the community does not allow it", named != null && !named.isAnonymous());
        check("too-short posts are rejected", d.addPost("social-anxiety", null, "u1", "hi", false) == null);
        check("over-long posts are rejected", d.addPost("social-anxiety", null, "u1", "x".repeat(DataStore.MAX_POST_LENGTH + 1), false) == null);
        CommunityPost reply = d.addPost("social-anxiety", anon.getId(), "u1", "Replying to my own post.", false);
        check("replies attach to their post", reply != null && d.repliesTo(anon.getId()).contains(reply));
        check("reply must belong to the same community", d.addPost("burnout", anon.getId(), "u1", "Wrong community reply", false) == null);

        // ----- volunteers -----
        check("seeded volunteer is a volunteer", d.isVolunteer("u6"));
        CommunityPost vol = d.addPost("burnout", null, "u6", "Welcome! Rest is not a reward, it is maintenance.", true);
        check("volunteers can never post anonymously", vol != null && !vol.isAnonymous());

        // ----- reporting -----
        check("student can report a post", d.report(anon.getId(), "u2", "Spam or advertising", ""));
        check("same student cannot report the same post twice", !d.report(anon.getId(), "u2", "Spam or advertising", ""));
        check("reporting an unknown post fails", !d.report("nope", "u2", "Spam or advertising", ""));
        d.report(named.getId(), "u2", ContentReport.URGENT_REASON, "");
        check("urgent reports are listed first", d.openReports().get(0).isUrgent());

        // ----- moderation -----
        ContentReport open = d.openReports().get(0);
        check("a student cannot resolve a report", !d.resolveReport(open.getId(), true, maria, "") && open.isOpen());
        check("a volunteer cannot resolve a report", !d.resolveReport(open.getId(), true, carlo, "") && open.isOpen());
        check("a student cannot hide a post", !d.setPostHidden(anon.getId(), true, maria) && !anon.isHidden());
        ContentReport spamReport = d.getReports().stream().filter(r -> r.getPostId().equals(anon.getId())).findFirst().orElseThrow();
        d.report(anon.getId(), "u5", "Spam or advertising", "");
        check("guidance can hide the post from a report", d.resolveReport(spamReport.getId(), true, counselor, "Spam") && anon.isHidden());
        check("hiding closes every open report on that post",
                d.getReports().stream().filter(r -> r.getPostId().equals(anon.getId())).noneMatch(ContentReport::isOpen));
        check("handled report records who and what", spamReport.getState() == ContentReport.State.ACTIONED
                && spamReport.getHandledBy().equals("u3"));
        ContentReport urgent = d.openReports().stream().filter(ContentReport::isUrgent).findFirst().orElseThrow();
        check("admin can dismiss a report without hiding", d.resolveReport(urgent.getId(), false, admin, "Checked, fine")
                && urgent.getState() == ContentReport.State.DISMISSED && !named.isHidden());
        check("a closed report cannot be resolved again", !d.resolveReport(urgent.getId(), true, admin, ""));
        check("admin can restore a hidden post", d.setPostHidden(anon.getId(), false, admin) && !anon.isHidden());

        // ----- crisis wording -----
        check("crisis wording is detected", PeerSupportView.mentionsCrisis("Some days I want to die")
                && PeerSupportView.mentionsCrisis("Gusto kong magpakamatay"));
        check("ordinary wording is not flagged", !PeerSupportView.mentionsCrisis("This exam is killing my schedule"));

        // ----- guidelines -----
        check("guidelines state volunteer limits and crisis line", CommunityGuidelines.full().contains("may not")
                && CommunityGuidelines.full().contains("1553"));

        System.out.println(failures == 0 ? "ALL " + checks + " COMMUNITY CHECKS PASSED" : failures + " of " + checks + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
