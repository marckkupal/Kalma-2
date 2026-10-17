/**
 * The ground rules for Peer Support Communities, including what Peer Volunteers may and may not do.
 * Shown in the app (Guidelines button) and mirrored in docs/PEER_SUPPORT_COMMUNITIES.md. Edit both together.
 */
final class CommunityGuidelines {
    private CommunityGuidelines() {}

    static final String CRISIS_LINE = "If you or someone else is in danger, call NCMH 1553 or emergency services 911, "
            + "or go to the Guidance Office right away.";

    static final String FOR_EVERYONE = String.join("\n",
            "For everyone",
            "\u2022  Be kind. No bullying, hate, or shaming of how someone feels.",
            "\u2022  Share your own experience. Don't diagnose or tell others what medicine or treatment to take.",
            "\u2022  Keep private details out. Don't name classmates, teachers or share contact details.",
            "\u2022  Communities are peer support, not counseling. The Guidance Office is the place for professional help.",
            "\u2022  Use Report on anything that worries you. Guidance staff review every report.",
            "",
            "Anonymous posts",
            "\u2022  Other students see \"Anonymous Student\". Guidance staff can see who posted if a report needs review "
                    + "or if a post suggests someone may be in danger.");

    static final String VOLUNTEER_MAY = String.join("\n",
            "Peer Volunteers may",
            "\u2022  Welcome new members and keep the conversation friendly.",
            "\u2022  Share their own experience and simple study or coping tips.",
            "\u2022  Point students to Kalma resources and the Guidance Office.",
            "\u2022  Report any post that worries them.");

    static final String VOLUNTEER_MAY_NOT = String.join("\n",
            "Peer Volunteers may not",
            "\u2022  Give counseling, diagnoses or medical advice, or act as a therapist.",
            "\u2022  Handle a crisis alone. Report it as urgent and encourage the student to contact Guidance or 1553.",
            "\u2022  Promise to keep something secret. Safety concerns are always passed on to Guidance.",
            "\u2022  Move a conversation into private messages, or ask for personal details.",
            "\u2022  Hide or remove posts, or see who is behind an anonymous post. Only Admin and Guidance moderate.",
            "\u2022  Post anonymously. Volunteers are always shown by name so members know who they are talking to.");

    static String full() { return FOR_EVERYONE + "\n\n" + VOLUNTEER_MAY + "\n\n" + VOLUNTEER_MAY_NOT + "\n\n" + CRISIS_LINE; }
}
