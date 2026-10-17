# Wellness Dashboard and Data Visualization

All of these pages show only the signed-in student's own records. The numbers and wording rules are in
`WellnessSummary.java`; `WellnessSummaryTest` checks them.

| Need | Where |
|---|---|
| Mood and stress trend charts | **Trends** (7 or 30 days, with a day-by-day log), **Summary** chart, Home card, Calendar |
| Weekly and monthly summaries | **Summary**: toggle "This week" / "This month". Check-ins, average mood, average stress, brightest day, a chart, a comparison with the period before, and an encouraging message |
| Review check-in and assessment history | **History**: check-ins one month at a time (mood, stress, note) with Earlier / Later, and every assessment (with the progress chart and change since last time). The Calendar also keeps the journal |
| Export, print or share a summary | **Export Summary**: PDF or CSV of check-ins, assessments and trends, with optional sharing to a Guidance counselor after the student agrees. See `WELLNESS_EXPORT.md` |
| Simple format | Plain numbers out of 5 (check-ins) or percent (assessments), one chart per page, plain-language labels |

## How a summary is built
* Week = today and the 6 days before; month = today and the 29 days before. Averages use only days that have a check-in.
  Days without one are left blank on charts and never filled in or guessed.
* The comparison with the previous period appears only when both periods have at least 2 check-in days. Averages
  within 0.3 of each other are called "about the same".
* The brightest day is the day with the highest mood (the most recent one if tied).

## Encouraging language
* No check-ins in a period: "...completely okay. Whenever you feel like it, a check-in takes less than a minute."
* Stress up or mood down: described as "a little heavier", "normal, especially around busy school dates", "does not
  mean you are doing anything wrong", with a suggestion to take a breathing break or talk to someone. Never an alarm.
* Stress down or mood up: "a little lighter. Nice work looking after yourself."
* The check-in reminder reads "Ready for a quick check-in today? It only takes a minute." Today's empty Calendar
  cell says "Today's check-in is open" in a neutral colour, not a warning colour.
* The streak counter was removed: Trends shows "Check-ins (last 7 days)" instead, because a broken streak can feel
  like failing. Counts are written "on 5 days", never "5 of 7", so nothing reads as a shortfall.
* History does not list days without a check-in. A month with none says "that is okay".
* `WellnessSummaryTest` fails if a summary message ever contains words such as "missed", "failed", "behind",
  "streak" or "worse".
