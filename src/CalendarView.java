import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Student "Calendar & Journal": daily, weekly or monthly view of check-ins, the school calendar
 * (Prelims, Midterms, Finals, holidays) and a private journal. Days with a check-in and days without one
 * look clearly different, and a missing day is never filled in with a guess.
 */
class CalendarView extends View {
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final String[] DOW = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final int DAY = 0, WEEK = 1, MONTH_MODE = 2;

    private final String uid;
    private int mode = MONTH_MODE;
    private LocalDate anchor = LocalDate.now();
    private LocalDate selected = LocalDate.now();

    private final JLabel periodLabel = new JLabel();
    private final JPanel gridHolder = holder();
    private final JPanel detailHolder = holder();
    private final JPanel lowerHolder = holder();
    private final JPanel readyHolder = holder();
    private final PillToggle[] modeBtns = {new PillToggle("Day"), new PillToggle("Week"), new PillToggle("Month")};

    CalendarView(MainWindow w) {
        super(w);
        uid = user().getId();

        YButton prev = YButton.outline("\u2039");
        YButton next = YButton.outline("\u203A");
        YButton todayBtn = YButton.yellow("Today");
        for (YButton b : new YButton[]{prev, next}) b.setBorder(new EmptyBorder(6, 16, 6, 16));
        todayBtn.setBorder(new EmptyBorder(6, 16, 6, 16));
        prev.addActionListener(e -> shift(-1));
        next.addActionListener(e -> shift(1));
        todayBtn.addActionListener(e -> { anchor = LocalDate.now(); selected = anchor; rebuild(); });
        periodLabel.setFont(Theme.BASE.deriveFont(Font.BOLD, 16f));
        periodLabel.setForeground(Brand.INK);

        ButtonGroup bg = new ButtonGroup();
        JPanel modes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        modes.setOpaque(false);
        for (int i = 0; i < modeBtns.length; i++) {
            int m = i;
            bg.add(modeBtns[i]);
            modeBtns[i].setBorder(new EmptyBorder(7, 14, 7, 14));
            modeBtns[i].addActionListener(e -> {
                mode = m;
                if (mode == DAY) anchor = selected;
                rebuild();
            });
            modes.add(modeBtns[i]);
        }
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nav.setOpaque(false);
        nav.add(prev);
        nav.add(next);
        nav.add(todayBtn);
        nav.add(periodLabel);
        JPanel controls = new HugPanel(new BorderLayout());
        controls.setOpaque(false);
        controls.setAlignmentX(0f);
        controls.add(nav, BorderLayout.WEST);
        controls.add(modes, BorderLayout.EAST);

        RoundPanel calCard = Staff.card(null, null, null);
        calCard.add(controls);
        calCard.add(Staff.gap(10));
        calCard.add(legend());
        calCard.add(Staff.gap(10));
        calCard.add(gridHolder);

        put(StudentUI.header(w, "Calendar & Journal",
                        "See your check-ins and school dates by day, week or month, and keep a private journal."),
                calCard, detailHolder, readyHolder, lowerHolder);
        rebuild();
    }

    private static JPanel holder() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(0f);
        return p;
    }

    // ----- navigation -----
    private void shift(int dir) {
        anchor = switch (mode) {
            case DAY -> anchor.plusDays(dir);
            case WEEK -> anchor.plusWeeks(dir);
            default -> anchor.plusMonths(dir);
        };
        selected = anchor;
        rebuild();
    }

    private static LocalDate weekStart(LocalDate d) {
        return d.minusDays(d.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : d.getDayOfWeek().getValue());
    }

    private void pick(LocalDate d) {
        selected = d;
        if (mode == DAY) anchor = d;
        rebuild();
    }

    // ----- building -----
    private void rebuild() {
        for (int i = 0; i < modeBtns.length; i++) modeBtns[i].setSelected(i == mode);
        periodLabel.setText(switch (mode) {
            case DAY -> anchor.format(LONG_DAY);
            case WEEK -> weekStart(anchor).format(StudentUI.SHORT_DAY) + " \u2013 " + weekStart(anchor).plusDays(6).format(StudentUI.DAY);
            default -> anchor.format(MONTH);
        });

        Map<LocalDate, CheckIn> byDay = new HashMap<>();
        for (CheckIn c : store().checkInsOf(uid)) byDay.put(c.date(), c);
        Set<LocalDate> journalDays = store().journalOf(uid).stream().map(JournalEntry::date).collect(Collectors.toSet());

        gridHolder.removeAll();
        if (mode == DAY) {
            gridHolder.add(dayStrip(byDay, journalDays));
        } else {
            LocalDate start = mode == WEEK ? weekStart(anchor) : weekStart(anchor.withDayOfMonth(1));
            int count = mode == WEEK ? 7 : (int) Math.ceil((start.until(anchor.withDayOfMonth(anchor.lengthOfMonth()), ChronoUnit.DAYS) + 1) / 7.0) * 7;
            JPanel head = new JPanel(new GridLayout(1, 7, 6, 0));
            head.setOpaque(false);
            head.setAlignmentX(0f);
            for (String s : DOW) {
                JLabel l = new JLabel(s, SwingConstants.CENTER);
                l.setFont(Theme.BASE.deriveFont(Font.BOLD, 11f));
                l.setForeground(Brand.MUTED);
                head.add(l);
            }
            JPanel grid = new JPanel(new GridLayout(0, 7, 6, 6));
            grid.setOpaque(false);
            grid.setAlignmentX(0f);
            for (int i = 0; i < count; i++) {
                LocalDate d = start.plusDays(i);
                boolean in = mode == WEEK || d.getMonth() == anchor.getMonth();
                grid.add(cell(d, byDay, journalDays, mode == WEEK, in));
            }
            gridHolder.add(head);
            gridHolder.add(Staff.gap(6));
            gridHolder.add(grid);
        }

        detailHolder.removeAll();
        detailHolder.add(Staff.gap(12));
        detailHolder.add(detail(byDay.get(selected)));

        readyHolder.removeAll();
        AcademicPlan.focus(store().academicEvents(), LocalDate.now()).ifPresent(f -> {
            readyHolder.add(getReady(f));
            readyHolder.add(Staff.gap(12));
        });
        readyHolder.add(academicReflection());

        lowerHolder.removeAll();
        lowerHolder.add(Staff.gap(12));
        lowerHolder.add(upcoming());
        lowerHolder.add(Staff.gap(12));
        lowerHolder.add(history(byDay));
        lowerHolder.add(Staff.gap(12));
        lowerHolder.add(reflection());
        revalidate();
        repaint();
    }

    private DayCell cell(LocalDate d, Map<LocalDate, CheckIn> byDay, Set<LocalDate> journalDays, boolean expanded, boolean inPeriod) {
        return new DayCell(d, byDay.get(d), journalDays.contains(d), store().eventsOn(d), expanded, inPeriod,
                d.equals(selected), () -> pick(d));
    }

    /** Day view: one large cell for the chosen day. The detail card below has the full information. */
    private JComponent dayStrip(Map<LocalDate, CheckIn> byDay, Set<LocalDate> journalDays) {
        DayCell c = cell(anchor, byDay, journalDays, true, true);
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setAlignmentX(0f);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private JComponent legend() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        p.setOpaque(false);
        p.setAlignmentX(0f);
        p.add(key(new Color(0xF0FDF4), new Color(0xA7E3B7), false, "Checked in"));
        p.add(key(new Color(0xF3F4F6), new Color(0x9CA3AF), true, "No check-in"));
        p.add(key(Color.WHITE, Brand.LINE, false, "Upcoming"));
        JLabel tags = new JLabel("School dates:  \uD83D\uDCDD Exams  /  \uD83C\uDF89 Holiday  /  \uD83D\uDCCC Other  /  \uD83D\uDCD3 Journal entry");
        tags.setFont(Theme.BASE.deriveFont(11f));
        tags.setForeground(Brand.MUTED);
        p.add(tags);
        return p;
    }

    private static JComponent key(Color fill, Color line, boolean dashed, String text) {
        JPanel box = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Brand.aa(g);
                g.setColor(fill);
                g.fillRoundRect(0, 0, 17, 17, 6, 6);
                g.setColor(line);
                if (dashed) g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 2f}, 0f));
                g.drawRoundRect(0, 0, 17, 17, 6, 6);
                g.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(18, 18); }
        };
        box.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setFont(Theme.BASE.deriveFont(11f));
        l.setForeground(Brand.INK);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setOpaque(false);
        row.add(box);
        row.add(l);
        return row;
    }

    // ----- selected day -----
    private RoundPanel detail(CheckIn ci) {
        LocalDate today = LocalDate.now();
        boolean future = selected.isAfter(today);
        RoundPanel card = Staff.card(selected.format(LONG_DAY), null, null);

        Pill status = ci != null ? new Pill("Checked in", Staff.GOOD_BG, Staff.GOOD_FG)
                : future ? new Pill("Upcoming", Staff.NEUTRAL_BG, Staff.NEUTRAL_FG)
                : selected.equals(today) ? new Pill("Today\u2019s check-in is open", Staff.NEUTRAL_BG, Staff.NEUTRAL_FG)
                : new Pill("No check-in", Staff.NEUTRAL_BG, Staff.NEUTRAL_FG);
        JPanel st = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        st.setOpaque(false);
        st.setAlignmentX(0f);
        st.add(status);
        card.add(st);
        card.add(Staff.gap(10));

        if (ci != null) {
            card.add(Staff.twoLines(Emoji.mood(ci.mood()) + "  Mood: " + Kalma.MOODS[ci.mood() - 1].substring(4),
                    "Stress: " + CheckInView.STRESS[ci.stress() - 1]));
            if (!ci.note().isEmpty()) {
                card.add(Staff.gap(6));
                card.add(Theme.wrapLabel("Your check-in note: " + escape(ci.note()), 760, Brand.MUTED, 12f, false));
            }
        } else if (selected.equals(today)) {
            card.add(Staff.note("Ready for a quick check-in? It only takes a minute, whenever you like."));
            card.add(Staff.gap(8));
            YButton go = YButton.yellow("Check in now  \u2192");
            go.setAlignmentX(0f);
            go.addActionListener(e -> window.open("Daily Check-In"));
            card.add(go);
        } else if (!future) {
            card.add(Staff.note("No mood or stress was recorded for this day. Check-ins are recorded on the day itself."));
        } else {
            card.add(Staff.note("Nothing to show yet."));
        }

        List<AcademicEvent> evs = store().eventsOn(selected);
        if (!evs.isEmpty()) {
            card.add(Staff.gap(12));
            card.add(Theme.fieldLabel("School dates"));
            card.add(Staff.gap(6));
            for (AcademicEvent e : evs) {
                JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                r.setOpaque(false);
                r.setAlignmentX(0f);
                r.add(new Pill(e.type(), e.bg(), e.fg()));
                JLabel t = new JLabel(e.title() + "  (" + e.range() + ")");
                t.setFont(Theme.BASE.deriveFont(12f));
                t.setForeground(Brand.INK);
                r.add(t);
                card.add(r);
            }
        }

        card.add(Staff.gap(14));
        card.add(Theme.fieldLabel("Journal (optional and private)"));
        card.add(Staff.gap(6));
        if (future) {
            card.add(Staff.note("You can write a journal entry on or after this day."));
            return card;
        }
        Optional<JournalEntry> existing = store().journalOn(uid, selected);
        JTextArea text = new JTextArea(existing.map(JournalEntry::text).orElse(""), 6, 40) {
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                if (getText().isEmpty()) {
                    Graphics2D g = (Graphics2D) g0.create();
                    Brand.aa(g);
                    g.setColor(new Color(0x9CA3AF));
                    g.setFont(getFont());
                    g.drawString("Write about your day, your thoughts, or what helped you today...", getInsets().left,
                            getInsets().top + g.getFontMetrics().getAscent());
                    g.dispose();
                }
            }
        };
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(Theme.BASE);
        text.setForeground(Brand.INK);
        text.setBorder(new EmptyBorder(12, 14, 12, 14));
        JScrollPane box = new JScrollPane(text);
        box.setBorder(new LineBorder(Brand.LINE, 1, true));
        box.setAlignmentX(0f);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        card.add(box);
        card.add(Staff.gap(6));
        card.add(Staff.note("Only you can see your journal. It is not shared with Guidance or Admin."));
        card.add(Staff.gap(10));

        YButton save = YButton.yellow("Save entry");
        save.addActionListener(e -> {
            String t = text.getText().trim();
            if (t.isEmpty()) {
                window.message(existing.isPresent() ? "The entry is empty. Use Delete entry to remove it." : "Write something first, or skip the journal for today.");
                return;
            }
            store().saveJournal(new JournalEntry(uid, selected, t));
            window.message("Journal entry saved.");
            rebuild();
        });
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(0f);
        btns.add(save);
        if (existing.isPresent()) {
            YButton del = YButton.outline("Delete entry");
            del.addActionListener(e -> {
                if (window.confirm("Delete this journal entry?")) {
                    store().deleteJournal(uid, selected);
                    rebuild();
                }
            });
            btns.add(del);
        }
        card.add(btns);
        return card;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ----- lower cards -----
    private RoundPanel upcoming() {
        RoundPanel card = Staff.card("\uD83C\uDF93  School calendar", null, null);
        LocalDate today = LocalDate.now();
        List<AcademicEvent> next = store().academicEvents().stream().filter(e -> !e.end().isBefore(today))
                .sorted(Comparator.comparing(AcademicEvent::start)).limit(6).collect(Collectors.toList());
        if (next.isEmpty()) card.add(Staff.note("No upcoming school dates are listed."));
        for (int i = 0; i < next.size(); i++) {
            AcademicEvent e = next.get(i);
            long days = ChronoUnit.DAYS.between(today, e.start());
            String when = e.covers(today) ? "Happening now" : days == 1 ? "Tomorrow" : "In " + days + " days";
            RoundPanel r = Staff.row();
            r.add(Staff.twoLines(e.title(), e.range() + "  \u2022  " + when), BorderLayout.CENTER);
            r.add(Staff.centered(new Pill(e.type(), e.bg(), e.fg())), BorderLayout.EAST);
            Staff.click(r, () -> { anchor = e.covers(today) ? today : e.start(); selected = anchor; rebuild(); });
            card.add(r);
            if (i < next.size() - 1) card.add(Staff.gap(6));
        }
        if (store().academicIsSample()) {
            card.add(Staff.gap(8));
            card.add(Staff.note("These are sample dates. Confirm them with the official TIP Quezon City academic calendar."));
        }
        return card;
    }

    private RoundPanel history(Map<LocalDate, CheckIn> byDay) {
        RoundPanel card = Staff.card("\uD83D\uDCD3  Previous journal entries", null, null);
        List<JournalEntry> all = store().journalOf(uid).stream()
                .sorted(Comparator.comparing(JournalEntry::date).reversed()).limit(8).collect(Collectors.toList());
        if (all.isEmpty()) card.add(Staff.note("No journal entries yet. Pick a day above and write one if you like."));
        for (int i = 0; i < all.size(); i++) {
            JournalEntry j = all.get(i);
            CheckIn c = byDay.get(j.date());
            String top = j.date().format(StudentUI.DAY) + (c == null ? "  \u2022  No check-in that day"
                    : "  \u2022  " + Emoji.mood(c.mood()) + " Mood: " + Kalma.MOODS[c.mood() - 1].substring(4) + ", Stress: " + CheckInView.STRESS[c.stress() - 1]);
            String flat = j.text().replace('\n', ' ');
            RoundPanel r = Staff.row();
            r.add(Staff.twoLines(top, flat.length() > 110 ? flat.substring(0, 110) + "\u2026" : flat), BorderLayout.CENTER);
            Staff.click(r, () -> { anchor = j.date(); selected = j.date(); rebuild(); });
            card.add(r);
            if (i < all.size() - 1) card.add(Staff.gap(6));
        }
        return card;
    }

    /** Compares the last 30 days with the 30 days before. Only days that were recorded are counted. */
    private RoundPanel reflection() {
        RoundPanel card = Staff.card("\uD83D\uDCC8  Looking back", null, null);
        LocalDate today = LocalDate.now();
        List<CheckIn> all = store().checkInsOf(uid);
        List<CheckIn> now30 = all.stream().filter(c -> !c.date().isBefore(today.minusDays(29)) && !c.date().isAfter(today)).collect(Collectors.toList());
        List<CheckIn> before30 = all.stream().filter(c -> !c.date().isBefore(today.minusDays(59)) && c.date().isBefore(today.minusDays(29))).collect(Collectors.toList());
        card.add(Staff.note("You checked in on " + now30.size() + " day" + (now30.size() == 1 ? "" : "s") + " in the last 30. Thank you for taking the time."));
        card.add(Staff.gap(6));
        if (now30.size() < 2 || before30.size() < 2) {
            card.add(Staff.note("A comparison with earlier weeks will appear here after a few more check-ins. No rush."));
        } else {
            double m1 = now30.stream().mapToInt(CheckIn::mood).average().orElse(0), m0 = before30.stream().mapToInt(CheckIn::mood).average().orElse(0);
            double s1 = now30.stream().mapToInt(CheckIn::stress).average().orElse(0), s0 = before30.stream().mapToInt(CheckIn::stress).average().orElse(0);
            card.add(Staff.twoLines("Mood: " + String.format("%.1f", m0) + " \u2192 " + String.format("%.1f", m1) + "  (" + change(m1 - m0) + ")",
                    "Stress: " + String.format("%.1f", s0) + " \u2192 " + String.format("%.1f", s1) + "  (" + change(s1 - s0) + ")"));
            card.add(Staff.gap(6));
            card.add(Staff.note("Earlier 30 days compared with the last 30 days, out of 5. Days with no check-in are not counted."));
        }
        return card;
    }

    // ----- get ready for the next school period -----
    /** Suggestions chosen by how close the next important date is. Tapping a row opens the activity or resource. */
    private RoundPanel getReady(AcademicPlan.Focus f) {
        AcademicEvent e = f.event();
        RoundPanel card = Staff.card("\uD83C\uDF31  Get ready: " + e.title(), null, null);
        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        meta.setOpaque(false);
        meta.setAlignmentX(0f);
        meta.add(new Pill(f.phase().label, Staff.WARN_BG, Staff.WARN_FG));
        JLabel when = new JLabel(e.range() + "  \u2022  " + f.when());
        when.setFont(Theme.BASE.deriveFont(12f));
        when.setForeground(Brand.MUTED);
        meta.add(when);
        card.add(meta);
        card.add(Staff.gap(10));

        for (String tip : AcademicPlan.tips(f)) {
            WrapText t = new WrapText("\u2022  " + tip, 700, Brand.INK, 12.5f, false);
            card.add(t);
            card.add(Staff.gap(4));
        }

        card.add(Staff.gap(8));
        card.add(Theme.fieldLabel("Try an activity"));
        card.add(Staff.gap(6));
        for (String id : AcademicPlan.activityIds(f)) {
            for (GroundingActivity a : GroundingActivity.ALL) {
                if (!a.id().equals(id)) continue;
                RoundPanel r = Staff.row();
                r.add(Staff.twoLines(a.icon() + "  " + a.title(), a.blurb() + "  \u2022  about " + a.minutes() + " min"), BorderLayout.CENTER);
                r.add(Staff.centered(new Pill("Try it", Brand.YELLOW, Brand.INK)), BorderLayout.EAST);
                Staff.click(r, () -> ActivityDialog.show(window, a));
                card.add(r);
                card.add(Staff.gap(6));
            }
        }

        List<String> cats = AcademicPlan.resourceCategories(f);
        List<Resource> picks = store().visibleResources().stream().filter(r -> cats.contains(r.category())).limit(3).collect(Collectors.toList());
        if (!picks.isEmpty() || AcademicPlan.canStillBook(f)) {
            card.add(Staff.gap(6));
            card.add(Theme.fieldLabel("Resources that may help"));
            card.add(Staff.gap(6));
        }
        if (AcademicPlan.canStillBook(f)) {
            RoundPanel r = Staff.row();
            r.add(Staff.twoLines("\uD83C\uDFDB\uFE0F  Book a Guidance consultation", "Talk it through before " + e.start().format(StudentUI.SHORT_DAY)
                    + ". Bookings need " + BookingRules.MIN_NOTICE_HOURS + " hours' notice."), BorderLayout.CENTER);
            Staff.click(r, () -> window.open("Book Consultation"));
            card.add(r);
            card.add(Staff.gap(6));
        }
        for (Resource res : picks) {
            RoundPanel r = Staff.row();
            r.add(Staff.twoLines(res.title(), res.contact()), BorderLayout.CENTER);
            r.add(Staff.centered(new Pill(res.category(), Staff.NEUTRAL_BG, Staff.NEUTRAL_FG)), BorderLayout.EAST);
            Staff.click(r, () -> window.runSearch(res.title()));
            card.add(r);
            card.add(Staff.gap(6));
        }
        card.add(Staff.gap(4));
        JPanel more = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        more.setOpaque(false);
        more.setAlignmentX(0f);
        more.add(Staff.linkButton("All resources and 24/7 support lines  \u2192", () -> window.open("Resources")));
        card.add(more);
        return card;
    }

    // ----- academic demands and reported stress -----
    /** Compares the student's own stress inside and outside exam time. Describes, never explains or predicts. */
    private RoundPanel academicReflection() {
        RoundPanel card = Staff.card("\uD83D\uDD0E  Academic demands and your stress", null, null);
        LocalDate today = LocalDate.now();
        List<CheckIn> mine = store().checkInsOf(uid);
        List<AcademicEvent> events = store().academicEvents();
        Optional<AcademicPlan.Comparison> cmp = AcademicPlan.compare(mine, events, today);

        if (cmp.isEmpty()) {
            long inExam = mine.stream().filter(c -> !c.date().isAfter(today) && AcademicPlan.inExamTime(c.date(), events)).count();
            long outside = mine.stream().filter(c -> !c.date().isAfter(today) && !AcademicPlan.inExamTime(c.date(), events)).count();
            card.add(Staff.note("Not enough check-ins yet to compare. A comparison needs at least " + AcademicPlan.MIN_DAYS
                    + " check-in days in exam time and " + AcademicPlan.MIN_DAYS + " outside it."));
            card.add(Staff.gap(4));
            card.add(Staff.note("So far: " + inExam + " in exam time, " + outside + " outside it. Days you did not check in are not counted."));
        } else {
            AcademicPlan.Comparison c = cmp.get();
            card.add(Staff.twoLines("Exam time:  " + String.format("%.1f", c.examStress()) + " out of 5",
                    "Average reported stress over " + c.examDays() + " check-in days, from " + AcademicPlan.LEAD_DAYS
                            + " days before each exam to its last day."));
            card.add(Staff.gap(6));
            card.add(Staff.twoLines("Other days:  " + String.format("%.1f", c.otherStress()) + " out of 5",
                    "Average reported stress over " + c.otherDays() + " check-in days."));
            card.add(Staff.gap(8));
            String line = switch (c.direction()) {
                case "higher" -> "Your reported stress tends to be higher around exams.";
                case "lower" -> "Your reported stress tends to be lower around exams.";
                default -> "Your reported stress looks about the same in exam time and on other days.";
            };
            JLabel l = new JLabel(line);
            l.setFont(Theme.BASE.deriveFont(Font.BOLD, 13f));
            l.setForeground(Brand.INK);
            l.setAlignmentX(0f);
            card.add(l);
            card.add(Staff.gap(4));
            card.add(new WrapText("This only reflects the days you checked in. Many things can affect stress at the same time, "
                    + "so it does not show that exams are the cause.", 700, Brand.MUTED, 12f, false));

            List<AcademicPlan.PerEvent> per = AcademicPlan.perEvent(mine, events, today);
            if (!per.isEmpty()) {
                card.add(Staff.gap(10));
                card.add(Theme.fieldLabel("By exam period"));
                card.add(Staff.gap(6));
                for (AcademicPlan.PerEvent p : per.stream().limit(4).collect(Collectors.toList())) {
                    RoundPanel r = Staff.row();
                    r.add(Staff.twoLines(p.event().title(), p.event().range() + "  \u2022  " + p.days() + " check-in days"), BorderLayout.CENTER);
                    int level = (int) Math.max(1, Math.min(5, Math.round(p.stress())));
                    r.add(Staff.centered(new Pill(String.format("%.1f", p.stress()) + " avg  \u2022  " + CheckInView.STRESS[level - 1],
                            Staff.NEUTRAL_BG, Staff.NEUTRAL_FG)), BorderLayout.EAST);
                    Staff.click(r, () -> { anchor = p.event().start(); selected = anchor; rebuild(); });
                    card.add(r);
                    card.add(Staff.gap(6));
                }
            }
        }

        card.add(Staff.gap(10));
        card.add(Theme.fieldLabel("Questions to think about"));
        card.add(Staff.gap(6));
        for (String q : AcademicPlan.prompts(cmp)) {
            card.add(new WrapText("\u2022  " + q, 700, Brand.INK, 12.5f, false));
            card.add(Staff.gap(4));
        }
        card.add(Staff.gap(6));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(0f);
        YButton journal = YButton.yellow("Write in today's journal");
        journal.setBorder(new EmptyBorder(7, 16, 7, 16));
        journal.addActionListener(e -> { anchor = today; selected = today; mode = DAY; rebuild(); });
        btns.add(journal);
        if (cmp.isPresent() && cmp.get().direction().equals("higher")) {
            YButton talk = YButton.outline("Talk to Guidance");
            talk.setBorder(new EmptyBorder(7, 16, 7, 16));
            talk.addActionListener(e -> window.open("Guidance Office"));
            btns.add(talk);
        }
        card.add(btns);
        card.add(Staff.gap(6));
        card.add(Staff.note("This comparison is private to you and is not a diagnosis."));
        return card;
    }

    private static String change(double d) {
        return Math.abs(d) < 0.2 ? "about the same" : d > 0 ? "higher" : "lower";
    }
}
