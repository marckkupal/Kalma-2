# Wellness Summary Export

Students can turn their own wellness information into a printable file, and can choose to share it with a Guidance
counselor. The logic is in `WellnessExport.java` (report, CSV, PDF) and `PdfWriter.java` (a small PDF writer with no
libraries); the pages are `ExportView` (student, **Export Summary**) and `SharedSummariesView` (Guidance, **Shared
Summaries**). `WellnessExportTest` checks everything below.

| Need | Where |
|---|---|
| Export selected information as PDF or CSV | **Export Summary**: pick a period, tick what to include, then **Save as PDF** (for printing) or **Save as CSV** (Excel / Google Sheets). Also reachable from the **Summary** page |
| Relevant check-ins, assessment summaries and trends | Three sections, each can be switched on or off. Check-ins: date, mood, stress. Assessments: date, overall and domain scores, level, stress type and main sources. Trends: averages, brightest day, a weekly table, a first-week to latest-week comparison, and (PDF) a mood and stress chart |
| Share voluntarily with a Guidance counselor | **Share with Guidance counselor** on the same page. Never required and never automatic |
| Consent before sharing | A consent screen opens every time. **Share summary** stays disabled until the student ticks "I have read this and I agree..." |

## What is included
* Periods: last 7, 30 or 90 days, or all saved records. Only the signed-in student's own records are ever used.
* The student sees a live preview of exactly what will be exported (and what a counselor would read) before saving or sharing.
* Private check-in notes are **off by default** and only included if the student ticks "Also include the private notes...".
* Days without a check-in are left out and blank on the chart. Nothing is filled in or guessed. A line on the chart joins two points only when they are on neighbouring days.
* Wording follows the Summary page rules (docs/WELLNESS_DASHBOARD.md): no guilt for gaps or changes, and every export says it is a self-reflection summary, not a diagnosis. `WellnessExportTest` fails if guilt words such as "missed" or "behind" appear.

## Files
* PDF: A4, Helvetica, page numbers, "Private and confidential" footer. Long histories continue on more pages.
* CSV: UTF-8 with a byte-order mark so Excel reads it, CRLF line endings, one block per section (title row, header row, data rows). Cells are quoted when needed. A cell starting with `=`, `+`, `-` or `@` gets a leading apostrophe so a note cannot run as a spreadsheet formula.
* Default file name: `kalma-wellness-summary-YYYY-MM-DD.pdf` / `.csv`. The student chooses where to save it, and is asked before replacing a file.
* A saved file lives on the student's own device. Kalma does not keep a copy and does not upload it.

## Sharing and consent
* Consent is **per share**, not a standing permission. The consent screen says: sharing is optional, what is included and for which dates, who can read it (the Guidance counselor only, not other students or administrators), that the counselor sees exactly the text shown, that later check-ins are not added, that sharing can be stopped, and that this is not an emergency service (1553 / 911).
* `DataStore.shareSummary(student, report, consentGiven)` refuses with an error when `consentGiven` is false, so a screen cannot skip consent by mistake. It also refuses non-students and empty summaries.
* What is stored is a **snapshot** (the same text as the preview), with the consent time and the consent wording version (`DataStore.CONSENT_VERSION`). Later check-ins never change what the counselor can read.
* The counselor sees it under **Shared Summaries** and gets a short notice in **Messages** (no wellness numbers in the message). Nobody else sees it: other students, and administrators, have no access. The Admin activity feed logs "Wellness summary shared" without a name.
* **Stop sharing** (Export Summary, "Shared with Guidance" list) removes it from the counselor's list straight away. Only the student who shared it can stop it. The student's own list keeps the entry, marked "Stopped".
* Stopping cannot un-read something a counselor already saw. The consent screen says the counselor will "no longer be able to open it", which is exactly what happens.
* Saved in `data/shared-summaries.tsv` (tab separated: id, student, counselor, consent version, consent time, shared time, stopped time, period, sections, text). Demo data is not written there.

## Not included
* Email or any other automatic sending. Sharing is inside Kalma only.
* Journal entries from Calendar & Journal. Those stay private to the student.
* Counselors cannot export or re-share a student's summary from the app.

## Testing
```
javac -d out *.java
mkdir scratch && cd scratch
java -cp ../out WellnessExportTest
java -cp ../out WellnessExportTest reload
```
The first run checks the period and section rules, notes staying out by default, CSV quoting and formula protection, PDF structure (the cross-reference table is verified against every object, long histories, brackets, Filipino letters and emoji), consent being refused when not given, who can and cannot see a shared summary, that later check-ins do not change a share, and stopping. `reload` confirms the saved shares, including the stopped state, are loaded again.
