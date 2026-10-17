# Peer Support Communities

Peer Support Communities are a standalone part of Kalma. They do not depend on any buddy-matching or Payo Pitik
program. Those were removed from this version.

## What students can do
- Browse communities by shared experience and join or leave them (search box in the top bar filters the list).
- Read discussions before joining; post and reply once joined.
- Post anonymously in communities that allow it.
- Report any post or reply.

## Communities
| Community | Anonymous posting |
|---|---|
| Academic Stress | Yes |
| Time Management | No (practical, tips-based) |
| Homesickness | Yes |
| Social Anxiety | Yes |
| Family and Personal Concerns | Yes |
| College Adjustment | No (practical, Q&A) |
| Burnout | Yes |

Change `anonymousAllowed` in `DataStore.seedCommunities()` to alter this.

## Anonymity
Other students see "Anonymous Student". The real author is stored and visible to Admin and Guidance only, so they can
follow up on reports and safety concerns. Students are told this next to the anonymous checkbox.

## Reporting and moderation
- Reason options: possible danger or self-harm (urgent), harassment, harmful advice, private information, spam, other.
- Reports go to the **Moderation** page, available to Admin and Guidance accounts only. Urgent reports are listed first.
- A reviewer can **hide the post** (closes every open report on it) or **dismiss the report**, with an optional note.
  Hidden posts show "removed by a moderator" to students and can be restored.
- Staff can also hide or restore any recent post without a report.
- The reporter is not shown to the person who posted.
- Crisis wording in a post (starter keyword list in `PeerSupportView.CRISIS_WORDS`) does not block the post. The author
  sees the crisis line (NCMH 1553, 911) and an urgent report is filed for Guidance. Have Guidance review that word list.

## Peer Volunteer boundaries
Admins mark students as Peer Volunteers (Users page). Volunteers get a badge and are always shown by name.

**May:** welcome new members, share their own experience and simple tips, point to Kalma resources and the Guidance
Office, report worrying posts.

**May not:** give counseling, diagnoses or medical advice; handle a crisis alone (report as urgent and direct the student
to Guidance or 1553); promise secrecy; move conversations to private messages or ask for personal details; hide or
remove posts or see who is behind an anonymous post; post anonymously.

The in-app text lives in `CommunityGuidelines.java`. Keep it in step with this file.

## Not yet done
- Posts, memberships and reports are held in memory (like the rest of the demo data) and reset when the app closes.
  Persisting them to `data/*.tsv`, as check-ins are, is the natural next step.
- Students cannot edit or delete their own posts.
- Students are not notified when their post is hidden.
