import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/** Guidance - Students (Figma frame): "My Students" list with a status pill. Click a row for details. */
class StudentsView extends View {
    private static final String[] STRESS = {"Very Low", "Low", "Moderate", "High", "Very High"};

    StudentsView(MainWindow w) {
        super(w);
        String q = window.takeSearch().toLowerCase();
        List<User> students = store().students().stream()
                .filter(u -> (u.getName() + " " + u.getCourse()).toLowerCase().contains(q))
                .collect(Collectors.toList());

        RoundPanel card = Staff.card("\uD83D\uDC65  My Students", null, null);
        if (students.isEmpty()) card.add(Staff.note("No students match your search."));
        for (User u : students) {
            boolean any = !store().checkInsOf(u.getId()).isEmpty();
            String reason = store().flagReason(u);
            Pill pill = !any ? new Pill("No check-ins", Staff.NEUTRAL_BG, Staff.NEUTRAL_FG)
                    : reason != null ? new Pill("Following up", Staff.WARN_BG, Staff.WARN_FG)
                    : new Pill("Active", Staff.GOOD_BG, Staff.GOOD_FG);
            RoundPanel r = Staff.personRow(false, u.getInitials(), u.getName(), u.getCourse(), pill);
            r.setToolTipText(reason == null ? "Click for details" : reason);
            Staff.click(r, () -> details(u, reason));
            card.add(r);
            card.add(Staff.gap(6));
        }
        put(Staff.header("Students", "Students you are supporting this term.", () -> window.open("Dashboard")), card);
    }

    private void details(User u, String reason) {
        Optional<CheckIn> last = store().checkInsOf(u.getId()).stream().max(Comparator.comparing(CheckIn::date));
        String status = last.isEmpty() ? "No check-ins yet" : reason != null ? "Following up: " + reason : "On track";
        window.message(u.getName() + "\n" + u.getCourse()
                + "\n\nLatest mood: " + last.map(c -> Kalma.MOODS[c.mood() - 1].substring(4)).orElse("-")
                + "\nLatest stress: " + last.map(c -> STRESS[c.stress() - 1]).orElse("-")
                + "\nStatus: " + status
                + "\n\nThese are signals for support, not a diagnosis.");
    }
}
