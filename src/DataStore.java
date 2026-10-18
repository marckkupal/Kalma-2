import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
final class DataStore {
    /** Bookable start times. Built from BookingRules, so lunch is never in the list. */
    static final String[] TIME_SLOTS = BookingRules.slots().toArray(new String[0]);
    private static final DataStore INSTANCE = new DataStore();

    private final List<User> users = new ArrayList<>();
    private final List<Appointment> appointments = new ArrayList<>();
    private final List<CheckIn> checkIns = new ArrayList<>();
    /** Check-ins students entered themselves. Only these are written to data/checkins.tsv (demo data is not). */
    private final List<CheckIn> savedCheckIns = new ArrayList<>();
    private static final Path CHECKIN_FILE = Paths.get("data", "checkins.tsv");
    private static final Path JOURNAL_FILE = Paths.get("data", "journal.tsv");
    private static final Path ACADEMIC_FILE = Paths.get("data", "academic-calendar.tsv");
    private static final Path RESOURCE_FILE = Paths.get("data", "resources.tsv");
    private static final Path SHARE_FILE = Paths.get("data", "shared-summaries.tsv");
    /** Wording version of the sharing consent screen (ExportView). Saved with every share. */
    static final String CONSENT_VERSION = "2026-10";
    private final List<SharedSummary> shares = new ArrayList<>();
    private final List<JournalEntry> journal = new ArrayList<>();
    private final List<AcademicEvent> academic = new ArrayList<>();
    private boolean academicIsSample = true;
    private final List<AssessmentResult> results = new ArrayList<>();
    private final List<Question> questions = new ArrayList<>();
    private final List<Resource> resources = new ArrayList<>();
    private final List<Solution> solutions = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();
    private final List<Announcement> announcements = new ArrayList<>();
    private final List<Activity> activity = new ArrayList<>();
    private final List<Guide> guides = new ArrayList<>();
    private final List<CounselorInfo> counselors = List.of(
            new CounselorInfo("c1", "Ms. Grace Lim, RGC", "Registered Guidance Counselor \u2022 stress, academics, adjustment", "guidance@tip.edu.ph"),
            new CounselorInfo("c2", "Mr. Paolo Dizon, RPsy", "Registered Psychometrician \u2022 assessments, personal concerns", "guidance@tip.edu.ph"));

    private DataStore() {}
    static DataStore getInstance() { return INSTANCE; }

    void seed() {
        if (!users.isEmpty()) return;
        users.add(new Student("u1", "Maria Santos", "student@tip.edu.ph", "student123", "BSCS 3rd Year"));
        users.add(new Student("u2", "Juan Dela Cruz", "juan@tip.edu.ph", "student123", "BSIT 2nd Year"));
        users.add(new Counselor("u3", "Grace Lim", "guidance@tip.edu.ph", "guidance123"));
        users.add(new Admin("u4", "Alex Rivera", "admin@tip.edu.ph", "admin123"));
        users.add(new Student("u5", "Ana Reyes", "ana@tip.edu.ph", "student123", "BSIT 1st Year"));
        users.add(new Student("u6", "Carlo Mendoza", "carlo@tip.edu.ph", "student123", "BSCS 2nd Year"));

        questions.addAll(AssessmentService.QUESTIONS);

        resources.addAll(defaultResources());
        loadResources();

        guides.add(new Guide("Stress Management Guide", "Practical steps for handling academic pressure.",
                "Notice stress early and reset: slow breathing, a short walk, and writing down what worries you.",
                true, "\uD83D\uDCD6"));
        guides.add(new Guide("Healthy Sleep Habits", "Build a routine that helps you rest and recharge.",
                "Keep a steady sleep time, stop screens 30 minutes before bed, and avoid late caffeine.",
                true, "\u2600"));
        guides.add(new Guide("Study-Life Balance", "Make room for rest, friends, and things you enjoy.",
                "Plan study blocks and rest blocks, and protect at least one free evening each week.",
                true, "\u2764"));

        solutions.add(new Solution("Academic", "Break big tasks down", List.of(
                "List every deliverable", "Split each into 25-minute steps", "Do the smallest one first",
                "Ask a professor about extensions early")));
        solutions.add(new Solution("Personal", "Reset your body", List.of(
                "Try slow breathing for 5 minutes", "Take a short walk", "Drink water",
                "Write down what is worrying you")));
        solutions.add(new Solution("Social", "Reach out small", List.of(
                "Message one classmate", "Join a peer community", "Attend one campus activity this week")));

        int[] m1 = {3, 3, 2, 3, 4, 3, 4};
        int[] s1 = {4, 4, 5, 4, 4, 4, 3};
        int[] m2 = {2, 3, 2, 2, 3, 2, 3};
        int[] s2 = {3, 3, 4, 3, 4, 3, 3};
        for (int i = 0; i < 7; i++) {
            checkIns.add(new CheckIn("u1", LocalDate.now().minusDays(6 - i), m1[i], s1[i]));
            checkIns.add(new CheckIn("u2", LocalDate.now().minusDays(6 - i), m2[i], s2[i]));
            // Demo only: two more students so campus averages reach the Admin privacy minimum (AdminRules.MIN_GROUP).
            checkIns.add(new CheckIn("u5", LocalDate.now().minusDays(6 - i), m2[(i + 2) % 7], s2[(i + 3) % 7]));
            checkIns.add(new CheckIn("u6", LocalDate.now().minusDays(6 - i), m1[(i + 1) % 7], s1[(i + 4) % 7]));
        }
        loadCheckIns();
        loadJournal();
        loadAcademic();
        loadSettings();
        loadShares();
        seedDemoStressHistory();

        // Demo history for the sample student (not written to disk). Real results are saved in data/assessments.tsv.
        results.add(new AssessmentResult("u1", LocalDate.now().minusDays(49), 56, 33, 28, 39, AssessmentService.VERSION));
        results.add(new AssessmentResult("u1", LocalDate.now().minusDays(35), 61, 39, 33, 44, AssessmentService.VERSION));
        results.add(new AssessmentResult("u1", LocalDate.now().minusDays(21), 50, 33, 33, 39, AssessmentService.VERSION));
        loadResults();

        String today = LocalDate.now().toString();
        appointments.add(new Appointment("u1", "c1", LocalDate.now().minusDays(20).toString(), "10:00",
                "Academic pressure", Status.COMPLETED));
        appointments.add(new Appointment("u2", "c1", nextWeekdays(2).get(1), "13:00",
                "College adjustment", Status.PENDING));
        appointments.add(new Appointment("u1", "c1", today, "09:00", "Academic pressure", Status.CONFIRMED));
        appointments.add(new Appointment("u2", "c1", today, "10:00", "Follow-up session", Status.CONFIRMED));
        appointments.add(new Appointment("u5", "c1", today, "13:00", "College adjustment", Status.PENDING));
        appointments.add(new Appointment("u6", "c1", today, "15:00", "Study stress", Status.CONFIRMED));

        LocalDateTime now = LocalDateTime.now();
        messages.add(new Message("u3", "u1", "Can I move my appointment to Friday?", now.minusMinutes(12)));
        messages.add(new Message("u3", "u2", "Thank you for the follow-up session.", now.minusHours(2)));
        messages.add(new Message("u3", "u5", "Is walk-in available this afternoon?", now.minusHours(5)));
        messages.add(new Message("u3", "u6", "I need to reschedule my consultation.", now.minusDays(1)));
        messages.add(new Message("u3", "u1", "Question about the breathing guide.", now.minusDays(2)));
        messages.get(1).markRead();
        messages.get(3).markRead();

        announcements.add(new Announcement("Wellness Week",
                "Join us at the Guidance Office for mood check-ins and peer support booths all week.",
                LocalDate.now().minusDays(14)));
        announcements.add(new Announcement("Guidance Office Hours Update",
                "The Guidance Office is open Monday to Friday, 9 AM to 4 PM.", LocalDate.now().minusDays(22)));

        activity.add(new Activity("New accounts approved", "4 students", now.minusMinutes(2)));
        activity.add(new Activity("Weekly report generated", "System", now.minusHours(1)));
        activity.add(new Activity("Announcement published", "Wellness Week", now.minusDays(1)));
        activity.add(new Activity("New guidance account", "Ms. Lim", now.minusDays(2)));
        seedCommunities();
    }

    // ----- users -----
    List<User> getUsers() { return Collections.unmodifiableList(users); }
    void addUser(User u) {
        users.add(u);
        log("New student account", u.getCourse());
    }
    Optional<User> findByEmail(String email) {
        return users.stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
    }
    Optional<User> findById(String id) { return users.stream().filter(u -> u.getId().equals(id)).findFirst(); }
    String nameOf(String userId) { return findById(userId).map(User::getName).orElse("Unknown"); }
    String courseOf(String userId) { return findById(userId).map(User::getCourse).orElse(""); }
    String initialsOf(String userId) { return findById(userId).map(User::getInitials).orElse("?"); }

    // ----- account management (Administrators only) -----
    /** Roles an administrator can create. Guidance counselors are fixed because the booking list is built from them. */
    static final String[] CREATABLE_ROLES = {"student", "admin"};

    /** Creates a student or administrator account. Null on success, otherwise the reason it was refused. */
    String createAccount(User actor, String role, String name, String email, String password, String course) {
        if (actor == null || !actor.getRole().equals("admin") || !actor.isActive()) return "Only an administrator can create accounts.";
        if (!Arrays.asList(CREATABLE_ROLES).contains(role)) return "Accounts of that type cannot be created here.";
        String em = email == null ? "" : email.trim().toLowerCase();
        if (name == null || name.isBlank()) return "Full name is required.";
        if (!em.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) return "Enter a valid email address.";
        if (findByEmail(em).isPresent()) return "That email is already registered.";
        String pw = AdminRules.passwordProblem(password);
        if (pw != null) return pw;
        if (role.equals("student") && (course == null || course.isBlank())) return "Choose the student's program.";
        String id = "u" + UUID.randomUUID().toString().substring(0, 6);
        users.add(role.equals("admin") ? new Admin(id, name.trim(), em, password) : new Student(id, name.trim(), em, password, course));
        log("Account created", role.equals("admin") ? "Administrator" : "Student");
        return null;
    }

    /** Activates or deactivates an account. Admins cannot deactivate themselves or the last active administrator. */
    String setAccountActive(User actor, User target, boolean active) {
        if (actor == null || !actor.getRole().equals("admin") || !actor.isActive()) return "Only an administrator can do that.";
        if (target.isActive() == active) return null;
        if (!active) {
            if (target.getId().equals(actor.getId())) return "You cannot deactivate yourself.";
            long admins = users.stream().filter(u -> u.getRole().equals("admin") && u.isActive()).count();
            if (target.getRole().equals("admin") && admins <= 1) return "At least one administrator account must stay active.";
        }
        target.setActive(active);
        log(active ? "Account activated" : "Account deactivated", "Administrator");
        return null;
    }

    /** Makes a student a Peer Volunteer or removes the role. Null on success, otherwise the reason it was refused. */
    String setVolunteerBy(User actor, User target, boolean on) {
        if (actor == null || !actor.getRole().equals("admin") || !actor.isActive()) return "Only an administrator can do that.";
        if (!target.getRole().equals("student")) return "Only student accounts can be Peer Volunteers.";
        if (!target.isActive()) return "Activate the account first.";
        setVolunteer(target.getId(), on);
        log(on ? "Peer Volunteer added" : "Peer Volunteer removed", "Administrator");
        return null;
    }

    List<User> students() {
        return users.stream().filter(u -> u.getRole().equals("student") && u.isActive()).collect(Collectors.toList());
    }

    // ----- counselors -----
    List<CounselorInfo> getCounselors() { return counselors; }
    String counselorName(String id) {
        return counselors.stream().filter(c -> c.id().equals(id)).map(CounselorInfo::name).findFirst().orElse("?");
    }

    // ----- appointments -----
    /** Times Guidance has closed off, stored as "counselorId|date|time". */
    private final Set<String> blockedTimes = new TreeSet<>();

    List<Appointment> getAppointments() { return Collections.unmodifiableList(appointments); }

    /** Adds a request if the slot is valid and free. Returns null on success, otherwise a message for the student. */
    String addAppointment(Appointment a) {
        String problem = slotProblem(a.getCounselorId(), a.getStudentId(), a.getDate(), a.getTime());
        if (problem != null) return problem;
        appointments.add(a);
        log("New appointment request", "Guidance Office");
        return null;
    }

    List<Appointment> appointmentsFor(User u) {
        return appointments.stream().filter(a -> u.isStaff() || a.getStudentId().equals(u.getId()))
                .sorted(Comparator.comparing(Appointment::getSortKey)).collect(Collectors.toList());
    }

    /** Times the counselor can still be booked on that date: no lunch, nothing taken or blocked, nothing in the past. */
    List<String> freeSlots(String counselorId, String date) {
        Set<String> taken = appointments.stream()
                .filter(a -> a.getCounselorId().equals(counselorId) && a.getDate().equals(date) && a.occupiesSlot())
                .map(Appointment::getTime).collect(Collectors.toSet());
        return BookingRules.slots().stream()
                .filter(t -> !taken.contains(t) && !isBlocked(counselorId, date, t) && isBookableMoment(date, t))
                .collect(Collectors.toList());
    }

    /** True for a weekday time that is still in the future. */
    private static boolean isBookableMoment(String date, String time) {
        try {
            LocalDate d = LocalDate.parse(date);
            return d.getDayOfWeek().getValue() < 6 && d.atTime(java.time.LocalTime.parse(time)).isAfter(LocalDateTime.now());
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Null when the slot can be booked, otherwise the reason. Checks lunch, hours, weekday, past, blocked, taken, and the student's own clash. */
    private String slotProblem(String counselorId, String studentId, String date, String time) {
        if (!BookingRules.slots().contains(time)) {
            boolean lunch = false;
            try { lunch = BookingRules.overlapsLunch(java.time.LocalTime.parse(time)); } catch (RuntimeException ignored) { }
            return lunch ? "That time is during the lunch break (" + BookingRules.lunchLabel() + "). Please choose another."
                    : "That time is outside consultation hours.";
        }
        if (!isBookableMoment(date, time)) return "That time is not available. Consultations are Monday to Friday, and the time must be in the future.";
        if (isBlocked(counselorId, date, time)) return "The counselor is not available at that time. Please choose another.";
        boolean taken = appointments.stream().anyMatch(a -> a.occupiesSlot() && a.getCounselorId().equals(counselorId)
                && a.getDate().equals(date) && a.getTime().equals(time));
        if (taken) return "That slot is already booked. Please choose another time.";
        boolean clash = appointments.stream().anyMatch(a -> a.occupiesSlot() && a.getStudentId().equals(studentId)
                && a.getDate().equals(date) && a.getTime().equals(time));
        if (clash) return "You already have an appointment at that time.";
        return null;
    }

    /** Moves an appointment to a new slot. The same rules as booking apply, plus the notice and reschedule limit for students. */
    String rescheduleAppointment(Appointment a, String date, String time, User actor) {
        String blocker = BookingRules.changeBlocker(a, actor, true);
        if (blocker != null) return blocker;
        if (a.getDate().equals(date) && a.getTime().equals(time)) return "That is already the appointment time. Choose a different one.";
        String problem = slotProblem(a.getCounselorId(), a.getStudentId(), date, time);
        if (problem != null) return problem;
        a.reschedule(date, time, !actor.isStaff());
        log("Appointment rescheduled", "Guidance Office");
        return null;
    }

    /** Cancels an appointment, freeing its slot. Returns null on success, otherwise the reason it was refused. */
    String cancelAppointment(Appointment a, User actor) {
        String blocker = BookingRules.changeBlocker(a, actor, false);
        if (blocker != null) return blocker;
        a.setStatus(Status.CANCELLED);
        log("Appointment cancelled", "Guidance Office");
        return null;
    }

    /** Guidance and Admin only: Confirm a pending or rescheduled request, or Complete a confirmed visit that has happened. */
    String updateStatus(Appointment a, Status s, User actor) {
        if (!actor.isStaff()) return "Only Guidance or Admin can do that.";
        switch (s) {
            case CONFIRMED:
                if (a.getStatus() != Status.PENDING && a.getStatus() != Status.RESCHEDULED)
                    return "Only pending or rescheduled requests can be confirmed.";
                break;
            case COMPLETED:
                if (a.getStatus() != Status.CONFIRMED) return "Confirm the appointment before marking it completed.";
                if (a.getDate().compareTo(LocalDate.now().toString()) > 0) return "That appointment has not happened yet.";
                break;
            case CANCELLED:
                return cancelAppointment(a, actor);
            default:
                return "That status cannot be set by hand.";
        }
        a.setStatus(s);
        log("Appointment " + s.toString().toLowerCase(), "Guidance Office");
        return null;
    }

    // ----- schedule management (Guidance and Admin) -----
    private static String blockKey(String counselorId, String date, String time) { return counselorId + "|" + date + "|" + time; }
    boolean isBlocked(String counselorId, String date, String time) { return blockedTimes.contains(blockKey(counselorId, date, time)); }

    /** Closes one time (or the whole day when time is null) so students cannot book it. Null on success, otherwise the reason. */
    String blockTime(String counselorId, String date, String time) {
        List<String> targets = time == null ? BookingRules.slots() : List.of(time);
        for (String t : targets) {
            if (!BookingRules.slots().contains(t)) return "That time is not a consultation slot.";
            boolean booked = appointments.stream().anyMatch(a -> a.occupiesSlot() && a.getCounselorId().equals(counselorId)
                    && a.getDate().equals(date) && a.getTime().equals(t));
            if (booked) return "There is already an appointment at " + BookingRules.clock(t)
                    + ". Reschedule or cancel it first, then block the time.";
        }
        for (String t : targets) blockedTimes.add(blockKey(counselorId, date, t));
        log("Schedule updated", "Guidance Office");
        return null;
    }

    /** Opens a blocked time again (or the whole day when time is null). */
    void unblockTime(String counselorId, String date, String time) {
        for (String t : time == null ? BookingRules.slots() : List.of(time)) blockedTimes.remove(blockKey(counselorId, date, t));
        log("Schedule updated", "Guidance Office");
    }

    /** Blocked times from today on, as {counselorId, date, time}, in date order. */
    List<String[]> blockedFromToday() {
        String today = LocalDate.now().toString();
        return blockedTimes.stream().map(k -> k.split("\\|"))
                .filter(p -> p[1].compareTo(today) >= 0)
                .sorted(Comparator.comparing((String[] p) -> p[1] + p[2] + p[0])).collect(Collectors.toList());
    }

    // ----- appointment reminders -----
    /**
     * In-app reminder lines. Students get one per appointment today or in the next few days (if they have the
     * appointment reminder switched on). Guidance and Admin get today's count and the pending requests.
     */
    List<String> appointmentReminders(User u) {
        List<String> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        if (u.isStaff()) {
            long todayN = appointments.stream().filter(a -> a.isUpcoming() && a.getDate().equals(today.toString())).count();
            long pending = appointments.stream().filter(a -> a.getStatus() == Status.PENDING && a.isUpcoming()).count();
            if (todayN > 0) out.add(todayN + " appointment" + (todayN == 1 ? " is" : "s are") + " scheduled today.");
            if (pending > 0) out.add(pending + " appointment request" + (pending == 1 ? " is" : "s are") + " waiting for review.");
            return out;
        }
        if (!pref(u.getId(), "remind-appt")) return out;
        for (Appointment a : appointmentsFor(u)) {
            if (!a.isUpcoming() || !a.startsAt().plusMinutes(BookingRules.SLOT_MINUTES).isAfter(now)) continue;
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.parse(a.getDate()));
            if (days > BookingRules.REMIND_DAYS) continue;
            String when = days == 0 ? "today" : days == 1 ? "tomorrow"
                    : "on " + LocalDate.parse(a.getDate()).format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH));
            out.add("Reminder: consultation with " + counselorName(a.getCounselorId()) + " " + when + " at "
                    + BookingRules.clock(a.getTime()) + (a.getStatus() == Status.PENDING ? " (waiting for confirmation)." : "."));
        }
        return out;
    }

    static List<String> nextWeekdays(int n) {
        List<String> out = new ArrayList<>();
        LocalDate d = LocalDate.now();
        while (out.size() < n) {
            d = d.plusDays(1);
            if (d.getDayOfWeek().getValue() < 6) out.add(d.toString());
        }
        return out;
    }

    // ----- check-ins and assessments -----
    List<CheckIn> getCheckIns() { return Collections.unmodifiableList(checkIns); }
    List<CheckIn> checkInsOf(String userId) {
        return checkIns.stream().filter(c -> c.userId().equals(userId)).collect(Collectors.toList());
    }
    /** One check-in per student per day. Saving again on the same day replaces it. Written to data/checkins.tsv. */
    void saveCheckIn(CheckIn c) {
        checkIns.removeIf(x -> x.userId().equals(c.userId()) && x.date().equals(c.date()));
        savedCheckIns.removeIf(x -> x.userId().equals(c.userId()) && x.date().equals(c.date()));
        checkIns.add(c);
        savedCheckIns.add(c);
        writeCheckIns();
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "").replace("\n", "\\n");
    }

    private static String unesc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                b.append(n == 't' ? '\t' : n == 'n' ? '\n' : n);
            } else b.append(ch);
        }
        return b.toString();
    }

    private void writeCheckIns() {
        try {
            Files.createDirectories(CHECKIN_FILE.getParent());
            List<String> lines = new ArrayList<>();
            for (CheckIn c : savedCheckIns)
                lines.add(c.userId() + "\t" + c.date() + "\t" + c.mood() + "\t" + c.stress() + "\t" + esc(c.note()));
            Files.write(CHECKIN_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save check-ins: " + e.getMessage());
        }
    }

    private void loadCheckIns() {
        if (!Files.exists(CHECKIN_FILE)) return;
        try {
            for (String line : Files.readAllLines(CHECKIN_FILE, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", -1);
                if (p.length < 5) continue;
                try {
                    CheckIn c = new CheckIn(p[0], LocalDate.parse(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), unesc(p[4]));
                    checkIns.removeIf(x -> x.userId().equals(c.userId()) && x.date().equals(c.date()));
                    checkIns.add(c);
                    savedCheckIns.add(c);
                } catch (RuntimeException bad) { /* skip a damaged line */ }
            }
        } catch (IOException e) {
            System.err.println("Could not read check-ins: " + e.getMessage());
        }
    }

    // ----- personal journal (private to each student) -----
    List<JournalEntry> journalOf(String userId) {
        return journal.stream().filter(j -> j.userId().equals(userId)).collect(Collectors.toList());
    }
    Optional<JournalEntry> journalOn(String userId, LocalDate date) {
        return journal.stream().filter(j -> j.userId().equals(userId) && j.date().equals(date)).findFirst();
    }
    /** One entry per student per day. Saving again replaces it. */
    void saveJournal(JournalEntry e) {
        journal.removeIf(j -> j.userId().equals(e.userId()) && j.date().equals(e.date()));
        journal.add(e);
        writeJournal();
    }
    void deleteJournal(String userId, LocalDate date) {
        journal.removeIf(j -> j.userId().equals(userId) && j.date().equals(date));
        writeJournal();
    }
    private void writeJournal() {
        try {
            Files.createDirectories(JOURNAL_FILE.getParent());
            List<String> lines = new ArrayList<>();
            for (JournalEntry j : journal) lines.add(j.userId() + "\t" + j.date() + "\t" + esc(j.text()));
            Files.write(JOURNAL_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save journal: " + e.getMessage());
        }
    }
    private void loadJournal() {
        if (!Files.exists(JOURNAL_FILE)) return;
        try {
            for (String line : Files.readAllLines(JOURNAL_FILE, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", -1);
                if (p.length < 3) continue;
                try { journal.add(new JournalEntry(p[0], LocalDate.parse(p[1]), unesc(p[2]))); }
                catch (RuntimeException bad) { /* skip a damaged line */ }
            }
        } catch (IOException e) {
            System.err.println("Could not read journal: " + e.getMessage());
        }
    }

    /**
     * Demo only (never written to disk): older check-ins for the sample student, higher around exams, so the
     * "Academic demands and your stress" card has something to show. Real students only ever see their own check-ins.
     */
    private void seedDemoStressHistory() {
        LocalDate today = LocalDate.now();
        for (int back = 8; back <= 56; back += 2) {
            LocalDate d = today.minusDays(back);
            if (checkIns.stream().anyMatch(c -> c.userId().equals("u1") && c.date().equals(d))) continue;
            boolean exam = AcademicPlan.inExamTime(d, academic);
            int stress = exam ? 4 : (back % 4 == 0 ? 2 : 3);
            int mood = exam ? 2 : 3;
            checkIns.add(new CheckIn("u1", d, mood, stress));
        }
    }

    // ----- academic calendar -----
    List<AcademicEvent> academicEvents() { return Collections.unmodifiableList(academic); }
    List<AcademicEvent> eventsOn(LocalDate d) {
        return academic.stream().filter(e -> e.covers(d)).collect(Collectors.toList());
    }
    /** True while the dates come from the built-in sample list instead of data/academic-calendar.tsv. */
    boolean academicIsSample() { return academicIsSample; }

    // Managed by Admin and Guidance on the Academic Calendar page. Every change is written to data/academic-calendar.tsv.
    void addAcademic(AcademicEvent e) { academic.add(e); sortAndSaveAcademic(); }
    void replaceAcademic(AcademicEvent old, AcademicEvent e) {
        int i = academic.indexOf(old);
        if (i < 0) academic.add(e); else academic.set(i, e);
        sortAndSaveAcademic();
    }
    void removeAcademic(AcademicEvent e) { academic.remove(e); sortAndSaveAcademic(); }

    private void sortAndSaveAcademic() {
        academic.sort(Comparator.comparing(AcademicEvent::start).thenComparing(AcademicEvent::title));
        academicIsSample = false;
        try {
            Files.createDirectories(ACADEMIC_FILE.getParent());
            List<String> lines = new ArrayList<>();
            lines.add("# start\tend\ttype\ttitle  (edit on the Academic Calendar page, or by hand: dates are yyyy-mm-dd)");
            for (AcademicEvent e : academic)
                lines.add(e.start() + "\t" + e.end() + "\t" + e.type() + "\t" + esc(e.title()));
            Files.write(ACADEMIC_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("Could not save academic calendar: " + ex.getMessage());
        }
    }

    /**
     * Loads data/academic-calendar.tsv if it exists. One event per line: start date, end date, type, title,
     * separated by tabs (dates as yyyy-mm-dd; type is Prelims, Midterms, Finals, Holiday or Other).
     * Without that file, a sample term is used so the page is not empty.
     */
    private void loadAcademic() {
        if (Files.exists(ACADEMIC_FILE)) {
            try {
                for (String line : Files.readAllLines(ACADEMIC_FILE, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\t", -1);
                    if (p.length < 4 || line.startsWith("#")) continue;
                    try { academic.add(new AcademicEvent(LocalDate.parse(p[0].trim()), LocalDate.parse(p[1].trim()), p[2].trim(), unesc(p[3].trim()))); }
                    catch (RuntimeException bad) { /* skip a damaged line */ }
                }
                // A saved file always wins, even when every date was removed on purpose.
                academicIsSample = false;
                academic.sort(Comparator.comparing(AcademicEvent::start));
                return;
            } catch (IOException e) {
                System.err.println("Could not read academic calendar: " + e.getMessage());
            }
        }
        int y = LocalDate.now().getYear();
        academic.add(new AcademicEvent(LocalDate.of(y, 9, 21), LocalDate.of(y, 9, 26), AcademicEvent.PRELIMS, "Prelim Examinations"));
        academic.add(new AcademicEvent(LocalDate.of(y, 10, 19), LocalDate.of(y, 10, 24), AcademicEvent.MIDTERMS, "Midterm Examinations"));
        academic.add(new AcademicEvent(LocalDate.of(y, 11, 1), LocalDate.of(y, 11, 1), AcademicEvent.HOLIDAY, "All Saints' Day"));
        academic.add(new AcademicEvent(LocalDate.of(y, 11, 30), LocalDate.of(y, 11, 30), AcademicEvent.HOLIDAY, "Bonifacio Day"));
        academic.add(new AcademicEvent(LocalDate.of(y, 12, 5), LocalDate.of(y, 12, 5), AcademicEvent.OTHER, "Last day of classes"));
        academic.add(new AcademicEvent(LocalDate.of(y, 12, 7), LocalDate.of(y, 12, 12), AcademicEvent.FINALS, "Final Examinations"));
        academic.add(new AcademicEvent(LocalDate.of(y, 12, 25), LocalDate.of(y, 12, 25), AcademicEvent.HOLIDAY, "Christmas Day"));
        academic.add(new AcademicEvent(LocalDate.of(y, 12, 30), LocalDate.of(y, 12, 30), AcademicEvent.HOLIDAY, "Rizal Day"));
        academic.sort(Comparator.comparing(AcademicEvent::start));
    }

    // ----- stress assessment results (private to each student, saved to data/assessments.tsv) -----
    private static final Path RESULT_FILE = Paths.get("data", "assessments.tsv");
    /** Results students completed themselves. Only these are written to disk (demo data is not). */
    private final List<AssessmentResult> savedResults = new ArrayList<>();

    List<AssessmentResult> getResults() { return Collections.unmodifiableList(results); }
    /** One student's results, oldest first, so changes over time read left to right. */
    List<AssessmentResult> resultsOf(String userId) {
        return results.stream().filter(r -> r.userId().equals(userId))
                .sorted(Comparator.comparing(AssessmentResult::date)).collect(Collectors.toList());
    }
    void addResult(AssessmentResult r) {
        results.add(r);
        savedResults.add(r);
        writeResults();
    }
    private void writeResults() {
        try {
            Files.createDirectories(RESULT_FILE.getParent());
            List<String> lines = new ArrayList<>();
            for (AssessmentResult r : savedResults)
            {
                List<String> cols = new ArrayList<>(List.of(r.userId(), r.date().toString(), "" + r.academic(),
                        "" + r.social(), "" + r.personal(), "" + r.overall(), r.version()));
                if (r.profile() != null) {
                    cols.add(r.profile().type().name());
                    cols.add(r.profile().encodeScores());
                    if (r.profile().hasAppraisal()) cols.add(r.profile().encodeAppraisal());
                }
                lines.add(String.join("\t", cols));
            }
            Files.write(RESULT_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save assessment results: " + e.getMessage());
        }
    }
    private void loadResults() {
        if (!Files.exists(RESULT_FILE)) return;
        try {
            for (String line : Files.readAllLines(RESULT_FILE, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", -1);
                if (p.length < 7) continue;
                try {
                    AssessmentResult r = new AssessmentResult(p[0], LocalDate.parse(p[1]), Integer.parseInt(p[2]),
                            Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]), p[6],
                            p.length >= 10 ? StressProfile.decode(p[7], p[8], p[9]) : p.length >= 9 ? StressProfile.decode(p[7], p[8]) : null);
                    results.add(r);
                    savedResults.add(r);
                } catch (RuntimeException bad) { /* skip a damaged line */ }
            }
        } catch (IOException e) {
            System.err.println("Could not read assessment results: " + e.getMessage());
        }
    }

    // ----- follow-up flags (used by the Guidance dashboard) -----
    /** Reason a student may need a follow-up based on the last 7 days of check-ins, or null. */
    String flagReason(User u) {
        List<CheckIn> week = checkInsOf(u.getId()).stream()
                .filter(c -> !c.date().isBefore(LocalDate.now().minusDays(6))).collect(Collectors.toList());
        if (week.isEmpty()) return null;
        double stress = week.stream().mapToInt(CheckIn::stress).average().orElse(0);
        double mood = week.stream().mapToInt(CheckIn::mood).average().orElse(0);
        if (stress >= 3.5) return "Stress trending up";
        if (mood < 3) return "Mood dipped this week";
        return null;
    }

    int followUpCount() {
        return (int) students().stream().filter(u -> flagReason(u) != null).count();
    }

    /** Check-in highlights grouped by course: {course, note}. Shows no student names. */
    List<String[]> highlights() {
        List<String[]> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (User u : students()) {
            String r = flagReason(u);
            if (r != null && seen.add(u.getCourse() + "|" + r)) out.add(new String[]{u.getCourse(), r});
        }
        Map<String, Integer> requests = new LinkedHashMap<>();
        for (Appointment a : appointments) {
            if (a.getStatus() == Status.PENDING) requests.merge(courseOf(a.getStudentId()), 1, Integer::sum);
        }
        requests.forEach((course, n) ->
                out.add(new String[]{course, n + " new appointment request" + (n == 1 ? "" : "s")}));
        return out;
    }

    // ----- messages -----
    List<Message> messagesFor(String userId) {
        return messages.stream().filter(m -> m.getToId().equals(userId))
                .sorted(Comparator.comparing(Message::getAt).reversed()).collect(Collectors.toList());
    }
    long unreadCount(String userId) {
        return messages.stream().filter(m -> m.getToId().equals(userId) && !m.isRead()).count();
    }
    void addMessage(Message m) {
        messages.add(m);
        log("New student message", "Student to Guidance"); // no name: the Admin feed must not identify students
    }

    // ----- shared wellness summaries (the student must agree first) -----
    /** The counselor account that receives shared summaries, the same one that receives student messages. */
    Optional<User> shareRecipient() {
        return users.stream().filter(u -> u.getRole().equals("counselor") && u.isActive()).findFirst();
    }

    /**
     * Shares a snapshot of the report with the Guidance counselor. Refuses unless the student has given consent, so no
     * screen can skip it by mistake. The student can stop sharing later with {@link #revokeShare}.
     */
    SharedSummary shareSummary(User student, WellnessExport.Report report, boolean consentGiven) {
        if (!consentGiven) throw new IllegalStateException("The student has not agreed to share this summary.");
        if (!student.getRole().equals("student")) throw new IllegalArgumentException("Only students can share a wellness summary.");
        if (report.isEmpty()) throw new IllegalArgumentException("There is nothing in this summary to share.");
        User to = shareRecipient().orElseThrow(() -> new IllegalStateException("No counselor is available right now."));
        LocalDateTime now = LocalDateTime.now();
        SharedSummary s = new SharedSummary(UUID.randomUUID().toString().substring(0, 8), student.getId(), to.getId(),
                CONSENT_VERSION, now, now, null, report.options().periodLabel() + " (" + WellnessExport.range(report) + ")",
                report.options().sectionsLabel(), WellnessExport.toText(report));
        shares.add(s);
        writeShares();
        messages.add(new Message(to.getId(), student.getId(),
                "Shared a wellness summary with you (" + s.period() + "). Open Shared Summaries to read it.", now));
        log("Wellness summary shared", "Student to Guidance"); // no name: the Admin feed must not identify students
        return s;
    }

    /** A student's own shares, newest first, including ones they have stopped. */
    List<SharedSummary> sharesBy(String studentId) {
        return shares.stream().filter(s -> s.studentId().equals(studentId))
                .sorted(Comparator.comparing(SharedSummary::sharedAt).reversed()).collect(Collectors.toList());
    }

    /** What a counselor can open: summaries shared with them that have not been stopped, newest first. */
    List<SharedSummary> sharesTo(String counselorId) {
        return shares.stream().filter(s -> s.counselorId().equals(counselorId) && s.isActive())
                .sorted(Comparator.comparing(SharedSummary::sharedAt).reversed()).collect(Collectors.toList());
    }

    /** Stops sharing. Only the student who shared it can do this. Returns false if there was nothing to stop. */
    boolean revokeShare(String shareId, String studentId) {
        for (int i = 0; i < shares.size(); i++) {
            SharedSummary s = shares.get(i);
            if (s.id().equals(shareId) && s.studentId().equals(studentId) && s.isActive()) {
                shares.set(i, s.revoked(LocalDateTime.now()));
                writeShares();
                log("Wellness summary sharing stopped", "Student to Guidance");
                return true;
            }
        }
        return false;
    }

    private void writeShares() {
        try {
            Files.createDirectories(SHARE_FILE.getParent());
            List<String> lines = new ArrayList<>();
            for (SharedSummary s : shares)
                lines.add(String.join("\t", s.id(), s.studentId(), s.counselorId(), s.consentVersion(), s.consentAt().toString(),
                        s.sharedAt().toString(), s.revokedAt() == null ? "" : s.revokedAt().toString(),
                        esc(s.period()), esc(s.sections()), esc(s.text())));
            Files.write(SHARE_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save shared summaries: " + e.getMessage());
        }
    }

    private void loadShares() {
        if (!Files.exists(SHARE_FILE)) return;
        try {
            for (String line : Files.readAllLines(SHARE_FILE, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", -1);
                if (p.length < 10) continue;
                try {
                    shares.add(new SharedSummary(p[0], p[1], p[2], p[3], LocalDateTime.parse(p[4]), LocalDateTime.parse(p[5]),
                            p[6].isEmpty() ? null : LocalDateTime.parse(p[6]), unesc(p[7]), unesc(p[8]), unesc(p[9])));
                } catch (RuntimeException bad) { /* skip a damaged line */ }
            }
        } catch (IOException e) {
            System.err.println("Could not read shared summaries: " + e.getMessage());
        }
    }

    // ----- announcements and activity -----
    List<Announcement> getAnnouncements() { return Collections.unmodifiableList(announcements); }
    void addAnnouncement(Announcement a) {
        announcements.add(0, a);
        log("Announcement published", a.title());
    }
    void log(String title, String detail) { activity.add(0, new Activity(title, detail, LocalDateTime.now())); }
    List<Activity> recentActivity(int n) { return activity.stream().limit(n).collect(Collectors.toList()); }

    /** "2 min ago", "1 hr ago", "Yesterday" ... */
    static String ago(LocalDateTime t) {
        long m = java.time.Duration.between(t, LocalDateTime.now()).toMinutes();
        if (m < 1) return "Just now";
        if (m < 60) return m + " min ago";
        long h = m / 60;
        if (h < 24) return h + " hr ago";
        long d = h / 24;
        return d == 1 ? "Yesterday" : d + " days ago";
    }

    // ----- peer support communities -----
    private final List<Community> communities = new ArrayList<>();
    private final Set<String> communityMembers = new HashSet<>();
    private final List<CommunityPost> posts = new ArrayList<>();
    private final List<ContentReport> reports = new ArrayList<>();
    /** Students an admin has named Peer Volunteers. They get a badge and always post under their own name. */
    private final Set<String> volunteers = new HashSet<>();
    private int nextPostId = 1, nextReportId = 1;

    List<Community> getCommunities() { return Collections.unmodifiableList(communities); }
    Optional<Community> communityById(String id) { return communities.stream().filter(c -> c.id().equals(id)).findFirst(); }

    boolean isMember(String userId, String communityId) { return communityMembers.contains(userId + "|" + communityId); }
    void toggleMember(String userId, String communityId) {
        String k = userId + "|" + communityId;
        if (!communityMembers.remove(k)) communityMembers.add(k);
    }
    long memberCount(String communityId) {
        return communityMembers.stream().filter(k -> k.endsWith("|" + communityId)).count();
    }

    boolean isVolunteer(String userId) { return volunteers.contains(userId); }
    void setVolunteer(String userId, boolean on) {
        if (on) volunteers.add(userId);
        else volunteers.remove(userId);
    }

    Optional<CommunityPost> postById(String id) { return posts.stream().filter(p -> p.getId().equals(id)).findFirst(); }
    /** Top-level posts of one community, newest first. */
    List<CommunityPost> postsIn(String communityId) {
        return posts.stream().filter(p -> p.getCommunityId().equals(communityId) && !p.isReply())
                .sorted(Comparator.comparing(CommunityPost::getAt).reversed()).collect(Collectors.toList());
    }
    /** Replies to one post, oldest first. */
    List<CommunityPost> repliesTo(String postId) {
        return posts.stream().filter(p -> postId.equals(p.getParentId()))
                .sorted(Comparator.comparing(CommunityPost::getAt)).collect(Collectors.toList());
    }
    /** Every post and reply, newest first (for moderators). */
    List<CommunityPost> allPosts() {
        return posts.stream().sorted(Comparator.comparing(CommunityPost::getAt).reversed()).collect(Collectors.toList());
    }
    long replyCount(String postId) { return posts.stream().filter(p -> postId.equals(p.getParentId())).count(); }

    /**
     * Adds a post (parentId null) or a reply. Only members can post, anonymous posting only works where the community
     * allows it, and Peer Volunteers are never anonymous. Returns null if the post is not allowed.
     */
    CommunityPost addPost(String communityId, String parentId, String authorId, String text, boolean anonymous) {
        Optional<Community> c = communityById(communityId);
        if (c.isEmpty() || !isMember(authorId, communityId)) return null;
        String body = text == null ? "" : text.trim();
        if (body.length() < 3 || body.length() > MAX_POST_LENGTH) return null;
        if (parentId != null && postById(parentId).filter(p -> p.getCommunityId().equals(communityId)).isEmpty()) return null;
        boolean anon = anonymous && c.get().anonymousAllowed() && !isVolunteer(authorId);
        CommunityPost p = new CommunityPost("p" + nextPostId++, communityId, parentId, authorId, body, LocalDateTime.now(), anon);
        posts.add(p);
        return p;
    }
    static final int MAX_POST_LENGTH = 1000;

    /** Files a report. Returns false if this reporter already reported the post, or the post does not exist. */
    boolean report(String postId, String reporterId, String reason, String details) {
        if (postById(postId).isEmpty()) return false;
        boolean dup = reports.stream().anyMatch(r -> r.getPostId().equals(postId) && r.getReporterId().equals(reporterId)
                && r.isOpen());
        if (dup) return false;
        reports.add(new ContentReport("r" + nextReportId++, postId, reporterId, reason, details, LocalDateTime.now()));
        log(reason.equals(ContentReport.URGENT_REASON) ? "Urgent community report" : "Community post reported", "Peer Support");
        return true;
    }
    boolean hasReported(String postId, String reporterId) {
        return reports.stream().anyMatch(r -> r.getPostId().equals(postId) && r.getReporterId().equals(reporterId));
    }
    List<ContentReport> getReports() { return Collections.unmodifiableList(reports); }
    /** Open reports, urgent ones first, then oldest first. */
    List<ContentReport> openReports() {
        return reports.stream().filter(ContentReport::isOpen)
                .sorted(Comparator.comparing((ContentReport r) -> !r.isUrgent()).thenComparing(ContentReport::getAt))
                .collect(Collectors.toList());
    }
    /** Handled reports, newest first. */
    List<ContentReport> handledReports() {
        return reports.stream().filter(r -> !r.isOpen())
                .sorted(Comparator.comparing(ContentReport::getHandledAt).reversed()).collect(Collectors.toList());
    }
    long hiddenPostCount() { return posts.stream().filter(CommunityPost::isHidden).count(); }

    /**
     * Admin or Guidance only. Hiding the post (actioned) also closes any other open reports on it;
     * dismissing closes just this one. Returns false when the actor is not staff or the report is already closed.
     */
    boolean resolveReport(String reportId, boolean hidePost, User actor, String note) {
        if (actor == null || !actor.isStaff()) return false;
        Optional<ContentReport> found = reports.stream().filter(r -> r.getId().equals(reportId) && r.isOpen()).findFirst();
        if (found.isEmpty()) return false;
        ContentReport r = found.get();
        if (hidePost) {
            postById(r.getPostId()).ifPresent(p -> p.hide(actor.getId()));
            for (ContentReport other : reports)
                if (other.isOpen() && other.getPostId().equals(r.getPostId()))
                    other.resolve(ContentReport.State.ACTIONED, actor.getId(), note);
            log("Community post hidden", "Moderation");
        } else {
            r.resolve(ContentReport.State.DISMISSED, actor.getId(), note);
            log("Community report dismissed", "Moderation");
        }
        return true;
    }

    /** Admin or Guidance only: hide or restore any post without a report. */
    boolean setPostHidden(String postId, boolean hidden, User actor) {
        if (actor == null || !actor.isStaff()) return false;
        Optional<CommunityPost> p = postById(postId);
        if (p.isEmpty()) return false;
        if (hidden) p.get().hide(actor.getId());
        else p.get().restore();
        log(hidden ? "Community post hidden" : "Community post restored", "Moderation");
        return true;
    }

    private void seedCommunities() {
        communities.add(new Community("academic-stress", "Academic Stress", "\uD83D\uDCDA",
                "Exams, deadlines and the pressure to keep up. Share what is weighing on you.", true));
        communities.add(new Community("time-management", "Time Management", "\u23F0",
                "Swap study schedules, planning tricks and ways to stop procrastinating.", false));
        communities.add(new Community("homesickness", "Homesickness", "\uD83C\uDFE0",
                "Missing home, family or your old routine? You are not the only one.", true));
        communities.add(new Community("social-anxiety", "Social Anxiety", "\uD83D\uDCAC",
                "Nerves about groups, presentations or making friends, at your own pace.", true));
        communities.add(new Community("family-personal", "Family and Personal Concerns", "\uD83E\uDD1D",
                "Things going on outside school that are hard to carry. Keep details general.", true));
        communities.add(new Community("college-adjustment", "College Adjustment", "\uD83C\uDF93",
                "New to college or to TIP QC? Ask questions and share what helped you settle in.", false));
        communities.add(new Community("burnout", "Burnout", "\uD83D\uDD0B",
                "Running on empty? Talk about rest, limits and getting your energy back.", true));

        volunteers.add("u6");
        for (String[] m : new String[][]{{"u1", "academic-stress"}, {"u1", "burnout"}, {"u2", "academic-stress"},
                {"u2", "time-management"}, {"u5", "college-adjustment"}, {"u5", "homesickness"},
                {"u6", "academic-stress"}, {"u6", "college-adjustment"}, {"u6", "burnout"}, {"u6", "homesickness"}})
            communityMembers.add(m[0] + "|" + m[1]);

        LocalDateTime now = LocalDateTime.now();
        CommunityPost a = seedPost("academic-stress", null, "u2", "Three exams in one week and I can't focus. "
                + "How do you all get through finals week?", false, now.minusHours(26));
        seedPost("academic-stress", a.getId(), "u6", "I do one subject a day and stop at a fixed time. "
                + "Short breaks help me more than long cram nights. You've got this.", false, now.minusHours(24));
        CommunityPost b = seedPost("homesickness", null, "u5", "It's my first month away from my family and the "
                + "evenings are the hardest. Does it get easier?", true, now.minusHours(5));
        seedPost("homesickness", b.getId(), "u6", "It did for me. Calling home at the same time each day made the "
                + "evenings feel less empty.", false, now.minusHours(3));
        seedPost("college-adjustment", null, "u5", "Where do first-years usually eat lunch so we can meet other "
                + "people in our course?", false, now.minusHours(9));
        CommunityPost spam = seedPost("academic-stress", null, "u2", "BUY cheap notes and answer keys here!!! "
                + "Message me for the link", false, now.minusHours(2));
        reports.add(new ContentReport("r" + nextReportId++, spam.getId(), "u1", "Spam or advertising",
                "Selling answer keys.", now.minusMinutes(90)));
    }

    private CommunityPost seedPost(String community, String parent, String author, String text, boolean anon, LocalDateTime at) {
        CommunityPost p = new CommunityPost("p" + nextPostId++, community, parent, author, text, at, anon);
        posts.add(p);
        return p;
    }

    // ----- profile settings (notification and privacy choices; every option is on by default, except opt-in ones) -----
    private static final Path SETTINGS_FILE = Paths.get("data", "notification-settings.tsv");
    /** Options that start switched off and only apply once the student turns them on. */
    private static final Set<String> OPT_IN = Set.of(Notifications.EMAIL_OPTION);
    private final Set<String> prefsOff = new HashSet<>();
    private final Set<String> prefsOn = new HashSet<>();
    /** Notices a person dismissed in the bell, stored as "userId|noticeKey". Oldest are dropped past a limit. */
    private final Set<String> dismissed = new LinkedHashSet<>();
    private static final int MAX_DISMISSED = 500;

    boolean pref(String userId, String key) {
        String k = userId + "|" + key;
        return OPT_IN.contains(key) ? prefsOn.contains(k) : !prefsOff.contains(k);
    }
    void setPref(String userId, String key, boolean on) {
        String k = userId + "|" + key;
        if (OPT_IN.contains(key)) { if (on) prefsOn.add(k); else prefsOn.remove(k); }
        else if (on) prefsOff.remove(k);
        else prefsOff.add(k);
        writeSettings();
    }

    boolean isDismissed(String userId, String noticeKey) { return dismissed.contains(userId + "|" + noticeKey); }
    void dismiss(String userId, String noticeKey) {
        dismissed.add(userId + "|" + noticeKey);
        while (dismissed.size() > MAX_DISMISSED) dismissed.remove(dismissed.iterator().next());
        writeSettings();
    }

    /** Preferences and dismissed notices live in data/notification-settings.tsv: kind, user id, key (tab separated). */
    private void writeSettings() {
        try {
            Files.createDirectories(SETTINGS_FILE.getParent());
            List<String> lines = new ArrayList<>();
            for (String k : prefsOff) lines.add(settingLine("off", k));
            for (String k : prefsOn) lines.add(settingLine("on", k));
            for (String k : dismissed) lines.add(settingLine("dismissed", k));
            Files.write(SETTINGS_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save notification settings: " + e.getMessage());
        }
    }
    private static String settingLine(String kind, String userAndKey) {
        int i = userAndKey.indexOf('|');
        return kind + "\t" + userAndKey.substring(0, i) + "\t" + esc(userAndKey.substring(i + 1));
    }
    private void loadSettings() {
        if (!Files.exists(SETTINGS_FILE)) return;
        try {
            for (String line : Files.readAllLines(SETTINGS_FILE, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", -1);
                if (p.length < 3) continue;
                String k = p[1] + "|" + unesc(p[2]);
                switch (p[0]) {
                    case "off" -> prefsOff.add(k);
                    case "on" -> prefsOn.add(k);
                    case "dismissed" -> dismissed.add(k);
                    default -> { /* skip a damaged line */ }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read notification settings: " + e.getMessage());
        }
    }

    // ----- guides (written by Guidance) -----
    List<Guide> getGuides() { return Collections.unmodifiableList(guides); }
    void addGuide(Guide g) { guides.add(g); }

    // ----- content -----
    List<Question> getQuestions() { return questions; }
    List<Resource> getResources() { return Collections.unmodifiableList(resources); }
    /** Entries students can see. */
    List<Resource> visibleResources() {
        return resources.stream().filter(Resource::visible).collect(Collectors.toList());
    }
    void addResource(Resource r) { resources.add(r); saveResources(); }
    void removeResource(Resource r) { resources.remove(r); saveResources(); }
    /** Call after changing an entry with Resource.update so the change is written to disk. */
    void saveResources() {
        try {
            Files.createDirectories(RESOURCE_FILE.getParent());
            List<String> lines = new ArrayList<>();
            for (Resource r : resources)
                lines.add(String.join("\t", esc(r.title()), esc(r.category()), esc(r.description()), esc(r.contact()),
                        esc(r.hours()), esc(r.access()), String.valueOf(r.open247()), String.valueOf(r.visible())));
            Files.write(RESOURCE_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save resources: " + e.getMessage());
        }
    }

    /** Replaces the starter entries with the saved directory, if Guidance or Admin has edited it before. */
    private void loadResources() {
        if (!Files.exists(RESOURCE_FILE)) return;
        try {
            List<Resource> saved = new ArrayList<>();
            for (String line : Files.readAllLines(RESOURCE_FILE, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", -1);
                if (p.length < 8) continue;
                saved.add(new Resource(unesc(p[0]), unesc(p[1]), unesc(p[2]), unesc(p[3]), unesc(p[4]), unesc(p[5]),
                        Boolean.parseBoolean(p[6]), Boolean.parseBoolean(p[7])));
            }
            resources.clear();
            resources.addAll(saved);
        } catch (IOException e) {
            System.err.println("Could not read resources: " + e.getMessage());
        }
    }

    /** Starter directory. Numbers come from the NCMH, Hopeline PH and In Touch public listings; Guidance should re-check them. */
    private static List<Resource> defaultResources() {
        String office = "Mon to Fri, " + BookingRules.clock("09:00") + " to " + BookingRules.clock("16:00")
                + " (counselors on lunch " + BookingRules.lunchLabel() + ")";
        return new ArrayList<>(List.of(
                new Resource("TIP QC Guidance Office", Resource.GUIDANCE,
                        "Free, confidential consultations with a licensed counselor for stress, academics and personal concerns.",
                        "guidance@tip.edu.ph", office,
                        "Walk in during office hours and ask the front desk, or open Guidance Office > Book a Consultation in Kalma to reserve a slot.",
                        false, true),
                new Resource("NCMH Crisis Hotline", Resource.CRISIS,
                        "Free and confidential 24/7 support from trained counselors, run by the National Center for Mental Health. English and Filipino.",
                        "0917-899-USAP (8727)  \u2022  989-USAP (8727)  \u2022  1553 (Luzon-wide landline, toll-free)",
                        "24 hours, every day",
                        "Call any time. You do not need to be in a crisis; talking about stress or feeling overwhelmed is welcome.",
                        true, true),
                new Resource("Hopeline PH", Resource.CRISIS,
                        "Suicide prevention and emotional support line by the Natasha Goulbourn Foundation.",
                        "2919 (toll-free for Globe/TM)  \u2022  0917-558-4673  \u2022  (02) 8804-4673",
                        "24 hours, every day",
                        "Call any time. Dial 2919 if you use Globe or TM, otherwise use one of the other numbers.",
                        true, true),
                new Resource("In Touch Community Services", Resource.CRISIS,
                        "Crisis line and counseling service for anyone who needs someone to talk to.",
                        "(02) 8893-7603  \u2022  0917-800-1123",
                        "24 hours, every day",
                        "Call any time and say you would like to talk with a crisis counselor.",
                        true, true),
                new Resource("Emergency Services (PH)", Resource.CRISIS,
                        "For immediate danger to you or someone else: police, ambulance and fire.",
                        "911", "24 hours, every day",
                        "Call 911 right away. If you are on campus, also tell the nearest security guard or staff member.",
                        true, true),
                new Resource("Kalma Peer Support", Resource.SELF_HELP,
                        "Moderated student communities where you can share and read how others cope. Not a crisis service.",
                        "Peer Support tab in Kalma", "Open any time (moderated)",
                        "Open Peer Support from the sidebar and join a community that fits what you are going through.",
                        true, true),
                new Resource("5-Minute Breathing Exercise", Resource.SELF_HELP,
                        "Inhale for 4 counts, hold for 4, exhale for 6. Repeat for five minutes.",
                        "Self-guided", "Any time",
                        "Sit comfortably, put your phone aside and follow the counts. Stop if you feel dizzy.",
                        true, true),
                new Resource("Peer Tutoring Program", Resource.ACADEMIC,
                        "Get help with difficult subjects from fellow students.",
                        "Ask your department", "Varies by department",
                        "Ask your department office or class adviser how to sign up.",
                        false, true),
                new Resource("Sleep Hygiene Basics", Resource.WELLNESS,
                        "Simple habits for better rest: steady sleep time, no screens 30 minutes before bed, less late caffeine.",
                        "Guidance Office", "Any time",
                        "Read the Healthy Sleep Habits guide above, or ask the Guidance Office for a printed copy.",
                        true, true)));
    }

    List<Solution> getSolutions() { return solutions; }
}
