import java.time.LocalDate;
import java.util.*;

/**
 * One grounding technique shown on the student "Possible Solutions" page, with its step-by-step instructions.
 * Also decides which techniques to suggest from the latest check-in and assessment result.
 *
 * <p>The matching rules are simple and are not a diagnosis: they only choose which self-help activity to show
 * first, and always say why (see {@link #suggest}). The rules are listed in docs/ASSESSMENT_METHODOLOGY.md, section 7A.
 */
record GroundingActivity(String id, String icon, String title, String blurb, String button, int minutes, List<String> steps) {

    static final String BREATHING = "breathing";
    static final String COLORS = "colors";
    static final String MUSCLES = "muscles";
    static final String SENSES = "senses";

    /** In display order. */
    static final List<GroundingActivity> ALL = List.of(
            new GroundingActivity(BREATHING, "\uD83C\uDF3F", "Breathing Exercise",
                    "Slow, steady breathing to help your body settle.", "Try it", 2, List.of(
                    "Sit comfortably and let your shoulders drop. Rest your hands in your lap.",
                    "Breathe in slowly through your nose for 4 counts.",
                    "Hold gently for 4 counts. If holding feels uncomfortable, skip this part.",
                    "Breathe out slowly through your mouth for 6 counts.",
                    "Repeat steps 2 to 4 for about two minutes. Let each breath be a little slower than the last.",
                    "Return to your normal breathing and notice how your body feels.")),
            new GroundingActivity(COLORS, "\uD83C\uDFA8", "Countdown of Colors",
                    "Name colors around you to bring your attention back.", "Try it", 2, List.of(
                    "Pause and take one slow breath. Look around you.",
                    "Name 5 things you can see that are blue.",
                    "Name 4 things that are green.",
                    "Name 3 things that are red.",
                    "Name 2 things that are yellow, and 1 thing that is white.",
                    "Can't find a color? Pick another one. There is no wrong answer. Take one more slow breath.")),
            new GroundingActivity(MUSCLES, "\uD83E\uDDD8", "Progressive Muscle Relaxation",
                    "Tense and release each muscle group to ease tension.", "Try it", 5, List.of(
                    "Sit or lie down comfortably and take a slow breath. Tense gently, never to the point of pain. "
                            + "Skip any area that is sore or injured.",
                    "Feet and legs: curl your toes and tighten your calves and thighs for 5 seconds. "
                            + "Then let go completely for 10 seconds.",
                    "Stomach and back: gently tighten your stomach for 5 seconds. Release for 10 seconds.",
                    "Hands and arms: make fists and tighten your arms for 5 seconds. Release for 10 seconds.",
                    "Shoulders and neck: lift your shoulders toward your ears for 5 seconds. Let them drop for 10 seconds.",
                    "Face: scrunch your face for 5 seconds. Release and let your jaw loosen.",
                    "Take a slow breath and notice how your body feels now.")),
            new GroundingActivity(SENSES, "\uD83D\uDC40", "Five-Senses Exercise",
                    "Notice what you see, hear, feel, smell and taste.", "Try it", 3, List.of(
                    "Pause and take one slow breath. You are only noticing, nothing needs to change.",
                    "See: name 5 things you can see around you.",
                    "Hear: listen for 4 sounds, near or far.",
                    "Feel: notice 3 things you can touch or feel, like your feet on the floor or the chair under you.",
                    "Smell: find 2 things you can smell. Nothing nearby? Remember a smell you like.",
                    "Taste: notice 1 thing you can taste, or take a sip of water.",
                    "Take one more slow breath. Notice if your mind feels a little calmer.")));

    static GroundingActivity byId(String id) {
        return ALL.stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
    }

    // ----- matching -----

    /** A suggested activity with the plain-language reason it was suggested. */
    record Pick(GroundingActivity activity, String reason) {}

    /** Check-ins older than this are not used to match activities. */
    static final int RECENT_DAYS = 7;

    /** The student's latest check-in from the last {@value #RECENT_DAYS} days, or null. */
    static CheckIn latestCheckIn(List<CheckIn> all) {
        LocalDate oldest = LocalDate.now().minusDays(RECENT_DAYS - 1);
        return all.stream().filter(c -> !c.date().isBefore(oldest)).max(Comparator.comparing(CheckIn::date)).orElse(null);
    }

    /** Rules: high stress -> breathing and five senses; low mood -> color countdown; stress that feels heavy or
     *  moderate stress -> muscle relaxation or breathing. At most 3, each with a reason. Empty when there is no data. */
    static List<Pick> suggest(CheckIn latest, AssessmentResult result) {
        Map<String, Pick> out = new LinkedHashMap<>();
        if (latest != null) {
            boolean highStress = latest.stress() >= 4;
            boolean lowMood = latest.mood() <= 2;
            if (highStress) {
                put(out, BREATHING, "You reported high stress in your check-in. Slow breathing is a quick way to settle.");
                put(out, SENSES, "This brings your attention back to the present when things feel like too much.");
            }
            if (lowMood) {
                put(out, COLORS, "You reported a low mood. This is a gentle, low-effort way to shift your focus.");
            }
            if (!highStress && latest.stress() == 3) {
                put(out, BREATHING, "You reported moderate stress. A short breathing reset can help.");
            }
        }
        if (result != null && StressProfile.typeOf(result) == StressProfile.Type.DISTRESS) {
            put(out, MUSCLES, "Your latest assessment shows stress that feels heavy. Releasing tension in your body can help.");
        }
        return out.values().stream().limit(3).toList();
    }

    private static void put(Map<String, Pick> m, String id, String reason) {
        m.putIfAbsent(id, new Pick(byId(id), reason));
    }

    /**
     * True when talking to the Guidance Office could be especially useful: any higher assessment band (the same rule as
     * AssessmentService.recommend), or three or more recent check-ins in a row that all show high stress or low mood.
     * This is a nudge toward a person, not a crisis detector.
     */
    static boolean suggestGuidance(List<CheckIn> all, AssessmentResult result) {
        if (result != null) {
            boolean higher = AssessmentService.classify(result.overall()).rank() == 2;
            for (String d : AssessmentService.DOMAINS) higher |= AssessmentService.classify(result.score(d)).rank() == 2;
            if (higher) return true;
        }
        LocalDate oldest = LocalDate.now().minusDays(RECENT_DAYS - 1);
        List<CheckIn> recent = all.stream().filter(c -> !c.date().isBefore(oldest))
                .sorted(Comparator.comparing(CheckIn::date).reversed()).limit(3).toList();
        return recent.size() == 3 && recent.stream().allMatch(c -> c.stress() >= 4 || c.mood() <= 2);
    }
}
