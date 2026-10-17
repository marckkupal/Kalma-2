# Admin Moderation Dashboard

Administrators sign in to the Dashboard, Users, Reports, Appointments, Moderation, Announcements, Maintenance and
Academic Calendar pages. The rules are in `DataStore` (accounts) and `AdminRules` (what may be shown);
`AdminRulesTest` checks them.

| Duty | Where |
|---|---|
| Manage student, Peer Volunteer and administrator accounts | **Users**: filter by type, Add account, Activate / Deactivate, Make / remove Peer Volunteer |
| Manage appointment schedules and requests | **Appointments** (confirm, reschedule, cancel, block times) |
| Maintain wellness resources and academic calendar | **Maintenance**, **Academic Calendar** |
| Review flagged posts and reported content | **Moderation** (the dashboard shows the open-report count) |
| Moderate peer-support communities | **Moderation** (hide or restore posts, resolve reports) |
| Aggregated system activity | **Dashboard** and **Reports** |

## Account rules
* Only an active administrator can add accounts, change their status or name Peer Volunteers.
* Add account creates a **student** (program and year required) or an **administrator**, with a starting password
  (8+ characters, one number) to share privately. Duplicate or invalid emails are refused. Guidance counselor
  accounts are fixed, because the booking list is built from them.
* An administrator cannot deactivate themselves, and the last active administrator can never be deactivated.
  Deactivated accounts cannot sign in. Only active students can be Peer Volunteers.

## Privacy rules
* Reports and the dashboard show **counts only**: accounts, appointments by status, open and urgent reports, hidden
  posts, posts this week. No names, check-in notes, journal text, assessment results or stress scores of any student.
* Check-in figures (campus mood and stress, "students checking in") are shown only when at least
  `AdminRules.MIN_GROUP` (3) different students are behind them. Chart days below that are left out.
* Appointment reasons are visible to Guidance only; the Admin schedule shows who and when.
* The Recent Activity feed never names a student (a student message appears as "Student to Guidance").
* Moderation still lets staff see who wrote a reported or anonymous post, so safety concerns can be followed up.
