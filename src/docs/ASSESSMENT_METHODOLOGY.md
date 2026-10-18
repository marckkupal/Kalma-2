# Wellness and Stress Assessment: Methodology and Rationale

**Version:** v1.1 (matches `AssessmentService.VERSION`)
**Status:** DRAFT for review by the instructor and the Guidance Office. The scoring formula and thresholds below are provisional until signed off (see section 10).

---

## 1. Purpose and limits

The assessment helps a student **reflect** on stress in three areas of student life: **Academic**, **Social** and **Personal**. It gives each area a score, an overall score, a plain-language result band, suggested next steps, and a private history so the student can see change over time.

It is **not**:

- a diagnostic tool, a clinical screener, or a substitute for assessment by a licensed professional;
- a validated psychometric instrument. Its scores and cut points have **not** been validated against a clinical or population sample, and the app says so wherever scores are shown;
- a measure of any mental-health condition. It does not ask about, or score, symptoms of depression, anxiety, or any other DSM-5 disorder.

Results are worded as "low / moderate / higher **concern**" (how much seems to be weighing on the student), never as illness, severity of a disorder, or risk.

## 2. Original content, not a copy of existing tests

All 18 statements were written for this project from the stress literature (section 3). They were not copied or adapted from any published scale. Existing tools were consulted only to understand *response formats* (a "how often" frequency scale is common practice) and *constructs*, not wording.

Two safeguards:

1. The earlier prototype contained mood items ("felt down or low in mood", sleep, and so on) that resembled depression-screening content. Those items and the separate "Mood Reflection" and "Sleep and Energy" assessments were **removed**, in line with the rule that DSM-5 criteria must not be turned into an unvalidated test.
2. `AssessmentTest` fails if stock phrases from common screeners appear in the item text.

## 3. Research basis for the three domains

Stress is treated here as a mismatch between demands and a person's perceived resources (Lazarus and Folkman's transactional model). Student-stress research repeatedly finds the same clusters of demands, which became the three domains:

| Domain | What it covers | Research basis |
|---|---|---|
| **Academic** | Workload, deadlines and exams, expectations of self, family and teachers, procrastination and catching up, keeping up with lessons | Academic stress and its link with time management and anxiety in college students (Misra and McKean, 2000); reviews of stressors in secondary and higher education (Pascoe, Hetrick and Parker, 2020) |
| **Social** | Belonging, peer tension, social pressure to fit in, avoidance, having someone to turn to | Belonging as a basic human need (Baumeister and Leary, 1995); social stressors in student reviews (Pascoe et al., 2020) |
| **Personal** | Family worries, money, life changes and loss, too many responsibilities, coping alone | Life-change events as stressors (Holmes and Rahe, 1967); financial and family stressors in student reviews (Pascoe et al., 2020); transactional coping appraisal (Lazarus and Folkman, 1984) |

Design choices informed by the literature:

- **"How often" response format over a 2-week window.** Frequency-of-experience wording is the common format in perceived-stress measurement (for example Cohen, Kamarck and Mermelstein, 1983) and is easy for students to answer without recalling exact events. The 2-week window keeps the focus on *recent* stress.
- **Four options** (Never, Sometimes, Often, Almost always), scored 0 to 3. Each option has a plain, concrete meaning.
- **Items name situations, not symptoms.** For example "I put off schoolwork and then felt pressured to catch up" describes a demand, not a mental state.
- **One positively worded item per domain, reverse-scored,** to discourage automatic "same answer all the way down" responding and to include a coping/belonging signal.
- **Context-appropriate content.** The money item names tuition, allowance and daily expenses, which are common student concerns in the Philippines.
- **Bilingual.** Every item exists in English and Filipino with identical scoring. *The Filipino wording has not been reviewed by a native-speaker or language specialist and should be before use.*

## 4. The questions

Items appear to students in this order (domains alternate; positive items are spread out). "Reverse" means the score is flipped so a higher number always means more stress.

| # | Domain | Statement ("In the past 2 weeks...") | Reverse |
|---|---|---|---|
| 1 | Academic | I felt I had more schoolwork than I could realistically finish. | No |
| 2 | Social | I felt left out, or like I did not belong, in my classes or groups. | No |
| 3 | Personal | Worries about my family or home affected my focus on other things. | No |
| 4 | Academic | Upcoming exams or deadlines made it hard for me to relax. | No |
| 5 | Social | I felt pressure to act or come across in a certain way to fit in. | No |
| 6 | Personal | Money worries, such as tuition, allowance, or daily expenses, added to my stress. | No |
| 7 | Academic | I felt I had enough time and energy to do my schoolwork well. | Yes |
| 8 | Social | Tension or disagreements with friends, classmates, or groupmates weighed on me. | No |
| 9 | Personal | A recent change in my life, such as a move, a new routine, or a loss, was hard to handle. | No |
| 10 | Academic | I worried that my results would not meet what I, my family, or my teachers expect. | No |
| 11 | Social | I felt comfortable and accepted when I was with people at school. | Yes |
| 12 | Personal | My responsibilities left me little time for rest or for things I enjoy. | No |
| 13 | Academic | I put off schoolwork and then felt pressured to catch up. | No |
| 14 | Social | I avoided social situations because they felt tiring or uncomfortable. | No |
| 15 | Personal | I felt I could cope with the personal challenges that came up. | Yes |
| 16 | Academic | I had trouble keeping up with the lessons in one or more subjects. | No |
| 17 | Social | I felt I had no one to turn to when I needed to talk. | No |
| 18 | Personal | I felt my personal problems were too much to handle on my own. | No |

Answers: Never = 0, Sometimes = 1, Often = 2, Almost always = 3.

## 5. Scoring formula

**Item points.** For a normal item, points = the answer (0 to 3). For a reverse item, points = 3 minus the answer.

**Domain score** (0 to 100):

```
domain % = (sum of the 6 item points in the domain) / (6 items x 3) x 100      [rounded to a whole number]
```

**Overall score** (0 to 100):

```
overall % = (sum of all 18 item points) / (18 items x 3) x 100                 [rounded to a whole number]
```

Because each domain has the same number of items, the overall score equals the **plain average of the three domain scores**. No domain is weighted more than another.

*Rationale for equal weighting:* there is no evidence yet, for this student population, that one kind of stress should count more than another, so any unequal weights would be arbitrary. Equal weighting is the simplest rule that can be explained to a student in one sentence.

*Rationale for percent of maximum:* it puts every score on the same 0 to 100 scale, is easy to read, and lets the same cut points apply to a domain and to the overall score.

**Worked example.** A student answers Sometimes (1) to every item. Each domain has five normal items (1 point each) and one reverse item (answer 1 becomes 2 points): 5 + 2 = 7 points. 7 / 18 = 38.9%, so each domain is 39% and the overall score is 39% (moderate concern).

**Protection against a masked problem.** An average can hide one very high area (for example Academic 100, Social 0, Personal 0 gives an overall of only 33). Therefore the result band for each domain is shown separately, and **any single domain in the higher band triggers a Guidance Office recommendation regardless of the overall score** (section 7).

## 6. Result bands and thresholds

| Band | Score (any domain, or overall) | Meaning of the cut point |
|---|---|---|
| **Low concern** | below 33% | Average answer is below "Sometimes" (an average item score under 1 out of 3) |
| **Moderate concern** | 33% to 65% | Average answer is between "Sometimes" and "Often" |
| **Higher concern** | 66% or more | Average answer is "Often" or more (an average item score of 2 out of 3 or above, which is 66.7%) |

*Rationale.* The cut points are **anchored to the wording of the answer options** instead of to a clinical outcome: the lower cut is where, on average, a student reports stress about "sometimes", and the upper cut is where, on average, they report it "often". This makes each band explainable in everyday language ("on average these things happened often"). They are the two cut points that divide the 0 to 100 range into thirds.

**What this is not.** These are *descriptive* bands. They are not derived from a validation study, and they are **not** sensitivity/specificity-based cut-offs for any condition. A student in "higher concern" is not being identified as having a disorder; they are being told that a lot seems to be weighing on them and being pointed to support.

**Known limits of the bands.**
- A single answer shifts a domain score by about 5.6 points (1 of 18), so scores near a cut point can change band from one answer. Results are shown with their numbers and the history chart, not as labels alone.
- Positive items mean that answering "Never" to everything still gives a small score (17% per domain), which is intended.

**Adjusting after review.** The two cut points are in `AssessmentService` (defaults 33 and 66) and can be overridden **without recompiling** by placing `data/assessment-thresholds.properties` next to the app:

```
moderate=33
higher=66
```

Values must satisfy 0 < moderate < higher < 100, otherwise the defaults are used. If the Guidance Office changes the cut points or the items, increase `AssessmentService.VERSION`, since each saved result records the version that scored it.

## 6A. Type of stress and main source of stress

Besides the severity band, each new result carries two descriptive labels (`StressProfile`). Neither is a diagnosis.

**Research basis.** Kalma helps students see that stress is not always negative. *Eustress* (positive stress) can motivate students to prepare for examinations, meet deadlines, learn new skills or reach personal goals; when manageable it can support motivation, focus and performance. *Distress* (negative stress) occurs when stress becomes overwhelming or hard to manage and may go with worry, exhaustion, difficulty concentrating, sleep problems or reduced motivation. The design follows studies of student stress that separate these experiences and look at how students *perceive* the demands they face and their ability to cope:

- Gibbons, C., Dempster, M., and Moutray, M. (2008). Stress and eustress in nursing students. *Journal of Advanced Nursing.*
- Gong, W., and Geertshuis, S. A. (2023). Distress and eustress: An analysis of the stress experiences of offshore international students. *Frontiers in Psychology.*

These studies inform the feature design. They do **not** validate Kalma's custom scoring formula, appraisal items or cut points.

**Design principles.** (1) Not every stressful experience is treated as harmful. (2) Eustress and distress are descriptive labels for a self-reflection tool and are **never medical diagnoses**. (3) The label takes into account how the student perceives the stress (motivating, manageable, overwhelming, hard to recover from), the demands they report (the 18 statements) and their ability to cope.

**Appraisal items.** After the 18 statements the student answers 5 more questions with the same four-option frequency scale (Never to Almost always, "in the past 2 weeks"). They are shown as "About how the stress felt" and exist in English and Filipino (the Filipino wording needs the same native-speaker review as the rest, item 8 in section 10).

| # | Theme | Statement | Used for |
|---|---|---|---|
| A1 | Motivating | The pressure I felt helped me get started or stay focused on what mattered. | Motivation |
| A2 | Motivating | I saw some of my demands as a challenge I wanted to meet. | Motivation |
| A3 | Manageable | I felt I could manage my demands with the time, skills and support I had. | Strain (reverse-scored) |
| A4 | Overwhelming | My demands felt overwhelming, as if there was too much to handle at once. | Strain |
| A5 | Hard to recover | It was hard to recover or bounce back after stressful days. | Strain |

```
motivation % = (A1 + A2) / 6 x 100
strain %     = ((3 - A3) + A4 + A5) / 9 x 100          [both rounded to a whole number]
```

The appraisal items are **not** part of the Academic, Social, Personal or overall scores, so the formula in section 5 is unchanged. They only decide the type of stress and add one line of wording to the result (for example "You described your demands as sometimes motivating and manageable"). The two numbers are saved in a tenth column of `data/assessments.tsv`.

**Type of stress.** "Top band" is the highest band reached by the overall score or any domain (section 6).

| Condition | Type shown |
|---|---|
| Strain in the higher band (66 or more by default) | **Distress**: stress that feels overwhelming or hard to manage |
| Top band is higher **and** strain is at least moderate (33 or more) | **Distress** |
| Top band is higher but strain is below moderate | **Eustress**: heavy demands that the student feels able to manage |
| Top band is moderate, or strain is moderate, otherwise | **Eustress**: stress that feels manageable and can be motivating |
| Top band is low and strain is below moderate | Low reported stress (neither label applies) |

Strain uses the same two cut points as section 6 so there are no extra thresholds to review. If the appraisal was not answered (results saved before v1.1), the type is inferred from the bands alone, as in v1.0: higher is distress, moderate is eustress, low is low.

*Effect on suggestions.* A higher band **still** lists a Guidance Office consultation first, whatever the appraisal says, so a student who feels fine about heavy demands is still offered a person to talk to. Distress adds a short relaxation exercise. Eustress with moderate or higher demands adds "Use manageable pressure well" (set one small goal and schedule rest after it). The motivation score only changes the wording of the type message.

*Limits.* Five items, two of them motivation, make coarse scores. The strain rule and the pairing of bands and strain are design choices for a self-reflection tool, not validated cut-offs; review them with the Guidance Office (section 10, item 11). Eustress here means "manageable and potentially motivating", not a measured positive emotional state.

**Main source of stress.** Every item is tagged with one source: Academic (4 items), Financial (1), Social (6), Personal or family (4) or Time management (3). Tags never change the domain scores. A source score is its item points divided by its maximum, as a percent.

- A source is **reported** at the moderate cut point (33). A source with fewer than 3 items needs the higher cut point (66), so a single "Sometimes" does not flag it. Currently this affects only Financial.
- The student can have several reported sources. The highest, plus any within 10 points of it, are the **main** sources. The others are **secondary**.
- Three or more main sources are shown as **Other or mixed concerns**. No item maps only to "Other"; it is a label for spread-out results.
- Example summary: "Moderate reported stress — primarily academic, with some financial concerns."

*Limit.* Financial rests on one item, so it is a weaker signal than the other sources. Consider adding a second money item (with a Filipino version) after review.

Additional recommendations follow the sources: a Financial main source adds budgeting and financial-help guidance (Guidance Office), Time management adds week-planning in time blocks (Calendar), and Distress adds a short relaxation exercise. The Guidance Office consultation is still listed first for any higher band, which is the escalation path to a person.

## 7. Recommendations

Recommendations come from the **band of each domain**, not from individual answers. Rules (implemented in `AssessmentService.recommend`; at most 6 are shown):

| Situation | What the student is offered |
|---|---|
| Any domain **higher**, or overall **higher** | A Guidance Office consultation is listed **first** |
| Academic moderate or higher | Break big tasks into small steps (Possible Solutions), Peer Tutoring (Resources). If higher: also "talk to your professor early" |
| Social moderate or higher | Small outreach and peer support circles (Peer Support), Buddy Matching |
| Personal moderate or higher | Private journaling, short breathing break. If higher: ask the Guidance Office about family, money or life-change help |
| Overall moderate, no domain higher | "Visit the Guidance Office if this continues" |
| All low | "Keep doing what works" and daily check-ins |

Suggestions open the matching page in the app. Wording is supportive and invites, never instructs ("You do not have to handle this alone").

**Safety text.** Every result page, at every level, states that this is not a diagnosis and gives the NCMH hotline (1553) and 911. *Hotline numbers should be confirmed by the Guidance Office before release.*

## 7A. Grounding activities on Possible Solutions

The student "Possible Solutions" page offers four grounding techniques, each with step-by-step instructions shown one step at a time (`ActivityDialog`): **Breathing Exercise**, **Countdown of Colors**, **Progressive Muscle Relaxation** and **Five-Senses Exercise**. They are self-help activities, not treatment. Each lists "about N minutes", tells the student to skip anything uncomfortable (holding the breath, sore muscles), and ends with a closing page that offers a Guidance Office consultation.

**Which activities are suggested first** (implemented in `GroundingActivity.suggest`; at most 3, each shown with the reason in plain words). Only the latest check-in from the **last 7 days** is used. With no recent check-in and no distress result, no matching is claimed and the page invites a check-in instead.

| Signal | Suggested |
|---|---|
| Latest check-in stress **High** or **Very High** (4 or 5) | Breathing Exercise, Five-Senses Exercise |
| Latest check-in mood **1 or 2** | Countdown of Colors |
| Latest check-in stress **Moderate** (3), and not already high | Breathing Exercise |
| Latest assessment type is **Distress** (any higher band) | Progressive Muscle Relaxation |

*Rationale (provisional):* slow breathing and present-moment techniques are commonly used when stress feels overwhelming; attention-redirecting techniques need little effort when mood is low; muscle relaxation suits stress that is held in the body. These pairings are a design choice for a self-reflection tool and **should be reviewed by the Guidance Office**.

**When a Guidance Office prompt is shown at the top of the page** (`GroundingActivity.suggestGuidance`): any domain or the overall score is in the **higher** band (the same rule as section 7), **or** the three most recent check-ins within the last 7 days all show high stress (4 or 5) or low mood (1 or 2). The prompt uses invitational wording and links to booking. It is a nudge toward a person. It is not crisis detection, and Kalma does not claim to detect a crisis.

## 8. Change over time

- Each completed assessment is saved with its date, the three domain scores, the overall score, the formula version, and the stress type and source scores (`data/assessments.tsv`, up to 10 columns; older 7- and 9-column lines still load and simply show no type, source or appraisal). Unfinished assessments are never scored or saved.
- The student sees a line chart (overall plus the three domains, with the moderate and higher cut points drawn) and a dated list. Selecting a row shows that result in full.
- A change of **less than 10 points** between two results is described as "about the same". *Rationale:* one answer moving one step moves a domain by about 6 points, so smaller differences are within ordinary variation. Larger changes are described as "higher by N points" or "lower by N points". The 10-point figure is provisional.
- Trend wording is encouraging when scores fall, and normalising when they rise ("stress often rises during busy periods").

## 9. Privacy and data handling

- Results are **private to the student**: the app does not show individual assessment results to Guidance staff or administrators.
- Data is stored in plain text in `data/assessments.tsv` on the machine running the app (matching how check-ins and journal entries are stored in this project). There is no encryption or access control beyond the operating system. A real deployment would need both.
- No free text is collected in the assessment, which reduces accidental disclosure.

## 10. Items to review with the instructor and Guidance Office

This table is the review checklist. The scoring formula and thresholds should not be treated as final until each row is decided.

| # | Question for reviewers | Current default | Decision / date / initials |
|---|---|---|---|
| 1 | Are the three domains and the 18 statements appropriate and sensitively worded for this student population? | As listed in section 4 | |
| 2 | Is equal weighting of the three domains acceptable? | Equal | |
| 3 | Are the band cut points (33 and 66) acceptable, or should they be set differently (for example after piloting)? | 33 / 66 | |
| 4 | Is the "about the same" change margin (10 points) acceptable? | 10 | |
| 5 | Should there be a question about safety or self-harm? Currently there is **none**, to avoid creating an unvalidated screening item without a response protocol. Crisis contacts appear on every result instead. | None | |
| 6 | Should Guidance be able to see individual results (with student consent), or only anonymous group summaries? | Neither | |
| 7 | Are the recommendations and referral wording consistent with Guidance Office practice? Are the hotline numbers current? | Section 7 | |
| 8 | Native-speaker review of the Filipino items and options | Not done | |
| 9 | Who owns the pilot and later validation (for example comparing scores with a validated instrument in a consenting group)? | Not planned | |
| 10 | Data retention and consent, including for students who are minors | Not defined | |
| 11 | Are the 5 appraisal items, the strain formula and the rules that turn strain and bands into eustress or distress acceptable? | Section 6A | |

## 11. Limitations

- Self-report, so scores reflect how the student perceives and chooses to report their situation.
- Not validated: reliability (for example internal consistency) and validity have not been tested. With only 6 items per domain, domain scores are coarse.
- Bands are descriptive, not clinical.
- The "past 2 weeks" window may not suit exam periods or breaks; interpret trends with the academic calendar in mind.
- Some students may under-report or over-report; the tool cannot detect this.

## 12. DSM-5 as background only

The DSM-5-TR (American Psychiatric Association, 2022) describes diagnostic criteria for mental disorders. It is mentioned here only to explain what this tool is **not**. Stress is a normal response to demands and is not itself a DSM-5 diagnosis, and diagnostic criteria require clinical judgement, duration and impairment thresholds that a short self-reflection cannot establish. Accordingly, no DSM-5 criterion was converted into an item, no score is mapped to a diagnosis, and no result text uses diagnostic terms. A student who is worried about their mental health is directed to the Guidance Office, which can refer them to a qualified professional.

## 13. References

Please verify each citation against the original source before submission; these were compiled from memory of the literature and were not checked against the originals while this project was built.

- American Psychiatric Association. (2022). *Diagnostic and Statistical Manual of Mental Disorders* (5th ed., text rev.).
- Baumeister, R. F., and Leary, M. R. (1995). The need to belong: Desire for interpersonal attachments as a fundamental human motivation. *Psychological Bulletin, 117*(3), 497-529.
- Cohen, S., Kamarck, T., and Mermelstein, R. (1983). A global measure of perceived stress. *Journal of Health and Social Behavior, 24*(4), 385-396.
- Gibbons, C., Dempster, M., and Moutray, M. (2008). Stress and eustress in nursing students. *Journal of Advanced Nursing.*
- Gong, W., and Geertshuis, S. A. (2023). Distress and eustress: An analysis of the stress experiences of offshore international students. *Frontiers in Psychology.*
- Holmes, T. H., and Rahe, R. H. (1967). The Social Readjustment Rating Scale. *Journal of Psychosomatic Research, 11*(2), 213-218.
- Lazarus, R. S., and Folkman, S. (1984). *Stress, Appraisal, and Coping.* Springer.
- Misra, R., and McKean, M. (2000). College students' academic stress and its relation to their anxiety, time management, and leisure satisfaction. *American Journal of Health Studies, 16*(1), 41-51.
- Pascoe, M. C., Hetrick, S. E., and Parker, A. G. (2020). The impact of stress on students in secondary school and higher education. *International Journal of Adolescence and Youth, 25*(1), 104-112.

## 14. Testing

`AssessmentTest` (run from an empty scratch folder) checks: the item bank (18 items, 6 per domain, 3 reverse-scored, no duplicate or stock-screener wording, both languages present); scoring at the extremes and for worked examples; the overall-equals-average property; the exact band boundaries; threshold overrides and invalid files; change-over-time wording; recommendation rules and that every suggested page exists; the absence of diagnostic words in result text; the stress type, source tagging, mixed and single-item rules, the summary sentence, the appraisal items and strain and motivation scores, that appraisal never changes the domain scores, eustress versus distress for the same demands, and saving and reloading of results (with and without the type, source and appraisal columns).

```
javac -d out *.java
mkdir scratch && cd scratch
java -cp ../out AssessmentTest
java -cp ../out AssessmentTest reload
```
