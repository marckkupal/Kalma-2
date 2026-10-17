# Kalma (Java Swing) - split into components

One class per file, all in `src/` (default package, same as before, so nothing else changed).

```
./compile.sh          # javac -d out src/*.java   (errors show the file + line)
./run.sh              # starts the app
./run-smoketest.sh    # opens every page for every role, prints OK / FAIL per page
```

Windows: `javac -d out src\*.java` then `java -cp out Kalma`.

## Where to look when something breaks

| Symptom | File(s) |
|---|---|
| App start, window flow, sidebar, top bar, bell, search | `MainWindow`, `Kalma` |
| Splash screen | `SplashPanel` |
| Tutorial (3 steps + art) | `TutorialPanel`, `TutorialArt` |
| Login / register | `LoginPanel`, `RoleTabs`, `IconInput` |
| Student Home | `DashboardView`, `MoodTile`, `MoodFace`, `FaceIcon`, `QuoteBar` |
| Student pages | `AssessmentView`, `CheckInView`, `TrendsView`, `SummaryView`, `SolutionsView`, `ResourcesView`, `GuidanceOfficeView`, `PeerSupportView`, `BuddyView`, `BookingView`, `AppointmentsView`, `ProfileView` |
| Shared student pieces (header, stat tile, action card) | `StudentUI` |
| Admin pages | `AdminDashboardView`, `UsersView`, `ReportsView`, `AnnouncementsView` |
| Guidance pages | `GuidanceDashboardView`, `GuidanceAppointmentsView`, `StudentsView`, `MessagesView`, `GuidanceResourcesView` |
| Shared staff pieces (banner, header, card, rows, pills) | `Staff`, `Pill`, `IconBadge` |
| Colors / fonts / L&F | `Theme` (old sage theme), `Brand` (black + yellow) |
| Buttons | `YButton` (yellow/outline), `RoundButton` (sidebar), `PillToggle` |
| Layout helpers | `View` (page base class), `HugPanel`, `RoundPanel`, `WrapText` |
| Charts | `TrendChart` |
| Small widgets | `Avatar`, `Dot`, `Logo`, `BrandLogo`, `SearchField` |
| Data + seed data | `DataStore` |
| Assessment scoring | `AssessmentService`, `ConcernLevel` |
| Model | `User`, `Student`, `Counselor`, `Admin`, `Appointment`, `Status`, `Message`, `Guide`, `CheckIn`, `AssessmentResult`, `Announcement`, `Activity`, `Question`, `Resource`, `Solution`, `CounselorInfo` |

## Adding a new page

1. Create `MyNewView.java` extending `View`.
2. Add its key to the user's `getMenu()` (`Student`, `Counselor` or `Admin`) if it needs a sidebar item.
3. Add one `case` line in `MainWindow.createView`.
4. Add the key to `SmokeTest` if it is not in a menu.

Notes: `Logo`, `PillToggle`, `Theme.card/navButton`, `DataStore.getResults/allResources` are unused leftovers from the old design; they are kept so behavior is identical to your single file.
