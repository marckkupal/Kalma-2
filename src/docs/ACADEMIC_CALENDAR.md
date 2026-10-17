# Academic Stress Calendar

The student Calendar shows school dates (Prelims, Midterms, Finals, holidays, other deadlines) next to the
student's own check-ins. This page lists the rules behind the three features built on those dates. The code is in
`AcademicPlan.java`; `AcademicPlanTest` checks every rule below.

## 1. Managing the dates
Admin and Guidance have an **Academic Calendar** page: Add date, Edit, Remove, with Upcoming / Past / All filters.
Changes are saved to `data/academic-calendar.tsv` (one line per date: start, end, type, title; dates are
`yyyy-mm-dd`; lines starting with `#` are ignored). Until the first save, a built-in sample term is shown and the
pages say so. A saved file always wins, even if every date was removed.

## 2. "Get ready" suggestions
`AcademicPlan.focus` picks one school period, in this order:

| Situation | Phase |
|---|---|
| An exam period is happening today | Happening now |
| Next exam starts in 0 to 7 days | Getting close |
| An exam ended in the last 3 days | Time to recover |
| Next exam starts in 8 to 21 days | Plan ahead |
| An "Other" date (for example a deadline) is 0 to 7 days away | Getting close |
| A holiday is today or within 7 days | Time to rest |

Each phase has three fixed, plain self-help tips, one or two of the existing grounding activities, and resources
from the directory (Self-Help, Wellness Materials, Academic Support). A Guidance booking is offered only while it can
still be made under the 24-hour notice rule. The suggestion shows on the student Calendar page and as a one-line
heads-up on Home. Nothing is predicted about the student.

## 3. Academic demands and reported stress
"Exam time" is the 7 days before each exam period through its last day. The card compares the student's average
reported stress (1 to 5) in exam time with all other days they checked in.

* Shown only with at least 3 check-in days on each side; otherwise the card says how many more are needed.
* Averages within 0.3 of each other are reported as "about the same".
* Only the student's own check-ins are used, only the student sees the result, and days without a check-in are
  never filled in.
* The wording says "tends to be higher/lower" and always states that this does not show exams are the cause.
  It is not a diagnosis. When stress reads higher, a "Talk to Guidance" button is offered.
* Reflection questions are shown but not stored.

The sample student has demo check-ins (not saved to disk) generated from the calendar so the card has data to show.
