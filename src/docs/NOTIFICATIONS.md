# Reminders and Notifications

The bell in the top bar opens the notification list. The rules are in `Notifications.java`; `NotificationTest`
checks every rule below.

## What shows up
| Notice | Who | When |
|---|---|---|
| Unread messages | Everyone | While there are unread messages |
| "You have not checked in today" | Students | Every day until they check in (opens Check-In) |
| Appointment reminder | Everyone | Appointment today or in the next 2 days (`BookingRules.REMIND_DAYS`); staff see today's count and pending requests |
| School date | Students | An Academic Calendar date that starts within 7 days, or is happening now (`Notifications.ACADEMIC_DAYS`). Opens Calendar |
| Announcement | Students | Published in the last 14 days (`Notifications.ACTIVITY_DAYS`). View shows the full text |

The red badge on the bell is the number of notices still in the list. Students also see appointment, school-date
and announcement notices in a "Reminders" box on Home.

## Managing them
* Each row has **Open** / **View** and **Dismiss**. The footer has **Dismiss all** and (students) **Preferences**.
  Opening a notice dismisses it. A dismissed notice stays gone; a new one (a new day's check-in nudge, a date that
  moves from "coming up" to "happening now", a new message count) appears again.
* **Profile > Notification preferences** (also reachable from the bell): check-in reminders, appointment
  reminders, school dates, announcements. All are on by default. Unread-message notices cannot be switched off.
* Preferences and dismissed notices are saved in `data/notification-settings.tsv`
  (`off` / `on` / `dismissed`, user id, key; tab separated), so they survive a restart.

## Email reminders (optional)
Kalma has no mail service built in, so email is off and the option is hidden (the dialog says reminders are
in-app only). If the school has one, implement `Notifications.EmailService` (`available()` and `send(to, subject,
body)`) and call `Notifications.setEmailService(...)` at startup. Students then see "Also email these reminders",
which is **off until they turn it on**. When on, one digest is sent at most once a day when they sign in; it holds
the same notices except messages, and a sending failure never interrupts the app.
