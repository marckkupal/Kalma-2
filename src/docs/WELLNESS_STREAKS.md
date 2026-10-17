# Wellness Streaks

Tracks consistent participation in daily check-ins and encourages healthy routines, without penalties or guilt.

## What the student sees
- **Where:** a "Wellness Streaks" card on Home (narrow, right column) and on the Weekly and Monthly Summary page (wide). Formal look: no emoji, thin dividers, yellow accents. After saving a check-in, the confirmation also shows a supportive line.
- **Last 7 days:** soft dots, filled with a check when the student checked in, plain outline when not. No red, no crosses.
- **Progress that only goes up:** check-in days this week, this month and all together, a longest run in a row (shown from 3 days), and milestones at 1, 3, 7, 14, 30, 60 and 100 total check-in days.
- **Supportive messages:** thanks for checking in, a note about a run of days in a row (only from 2 days), a warm "Welcome back" after a break.

## How it avoids guilt
- A day without a check-in is just blank. There is no zero, no "streak lost" and no countdown.
- Today is never held against anyone: a run still counts when today has no check-in yet.
- Milestones use **total** check-in days, so a break cannot take them away.
- Messages never say how long it has been, and never use words like missed, lost, broke, behind or should have.
- The week-to-week note only celebrates more days or calls the pace steady. If there were fewer days it says nothing.
- A run of days in a row is a small bonus, not the goal. Personal progress (total and weekly days) comes first.

## Code
- `WellnessStreak.java` holds all the numbers and wording (no UI).
- `StreakCard.java` draws the card; `TextWrap.java` wraps its text; `FormalDialog.java` is the pop-up used by every message.
- `WellnessStreakTest.java` checks the counts and that every message stays gentle. Run: `java -cp out WellnessStreakTest`.
